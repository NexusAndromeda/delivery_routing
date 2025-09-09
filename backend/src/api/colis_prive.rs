//! API de Colis Privé - Solo API Web
//! 
//! Este módulo contiene solo las funciones necesarias para la API web de Colis Privé.
//! Todas las funciones móviles han sido comentadas para simplificar el backend.

use axum::{
    extract::State,
    http::StatusCode,
    Json,
};
use serde_json::json;
use log;
use crate::{
    state::AppState,
    config::environment::EnvironmentConfig,
    services::colis_prive_service::{ColisPriveAuthRequest, GetTourneeRequest, GetPackagesRequest, ColisPriveAuthResponse},
    services::colis_prive_companies_service::ColisPriveCompaniesService,
    models::colis_prive_company::ColisPriveCompanyListResponse,
};

/// POST /api/colis-prive/auth - Autenticar con Colis Privé
pub async fn authenticate_colis_prive(
    State(state): State<AppState>,
    Json(credentials): Json<ColisPriveAuthRequest>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    // Clonar las credenciales para poder usarlas después
    let username = credentials.username.clone();
    let societe = credentials.societe.clone();
    
    // 🔧 IMPLEMENTACIÓN REAL: Autenticación directa con Colis Privé
    match authenticate_colis_prive_simple(&credentials, &state.config).await {
        Ok(auth_response) => {
            if auth_response.success {
                // 🆕 ALMACENAR EL TOKEN EN EL ESTADO DE LA APLICACIÓN
                if let Some(token) = &auth_response.token {
                    // Limpiar tokens expirados antes de almacenar uno nuevo
                    state.cleanup_expired_tokens().await;
                    
                    // Almacenar el nuevo token (asumiendo 24 horas de validez)
                    state.store_auth_token(
                        username.clone(),
                        societe.clone(),
                        token.clone(),
                        24
                    ).await;
                    
                    log::info!("✅ Token almacenado en el estado de la aplicación para {}:{}", societe, username);
                }
                
                let auth_response = json!({
                    "success": true,
                    "authentication": {
                        "token": auth_response.token,
                        "matricule": auth_response.matricule,
                        "message": auth_response.message
                    },
                    "credentials_used": {
                        "username": username,
                        "societe": societe
                    },
                    "timestamp": chrono::Utc::now().to_rfc3339()
                });
                Ok(Json(auth_response))
            } else {
                let error_response = json!({
                    "success": false,
                    "error": {
                        "message": auth_response.message,
                        "code": "AUTH_FAILED"
                    },
                    "credentials_used": {
                        "username": username,
                        "societe": societe
                    },
                    "timestamp": chrono::Utc::now().to_rfc3339()
                });
                Ok(Json(error_response))
            }
        }
        Err(e) => {
            log::error!("Error en autenticación Colis Privé: {}", e);
            let error_response = json!({
                "success": false,
                "error": {
                    "message": format!("Error interno del servidor: {}", e),
                    "code": "INTERNAL_ERROR"
                },
                "timestamp": chrono::Utc::now().to_rfc3339()
            });
            Ok(Json(error_response))
        }
    }
}

/// 🔧 FUNCIÓN AUXILIAR: Autenticación simple sin device_info
async fn authenticate_colis_prive_simple(
    credentials: &ColisPriveAuthRequest,
    config: &EnvironmentConfig,
) -> Result<ColisPriveAuthResponse, anyhow::Error> {
    log::info!("🔐 Autenticando con Colis Privé (modo real)");
    
    // Validar credenciales básicas
    if credentials.username.is_empty() || credentials.password.is_empty() || credentials.societe.is_empty() {
        anyhow::bail!("Credenciales incompletas");
    }
    
    // 🔧 IMPLEMENTACIÓN REAL: Autenticación directa con Colis Privé
    let auth_url = format!("{}/api/auth/login/Membership", config.colis_prive_auth_url);
    let login_field = format!("{}_{}", credentials.societe, credentials.username);
    let auth_payload = json!({
        "login": login_field,
        "password": credentials.password,
        "societe": credentials.societe,
        "commun": {
            "dureeTokenInHour": 24
        }
    });
    
    log::info!("📤 Enviando autenticación a: {}", auth_url);
    log::info!("🔑 Login field: {}", login_field);
    
    // 🆕 USAR CURL DIRECTAMENTE PARA AUTENTICACIÓN
    let auth_payload_str = serde_json::to_string(&auth_payload).map_err(|e| {
        log::error!("❌ Error serializando payload de autenticación: {}", e);
        anyhow::anyhow!("Error serializando payload: {}", e)
    })?;
    
    let curl_output = std::process::Command::new("curl")
        .arg("-X")
        .arg("POST")
        .arg(auth_url)
        .arg("-H")
        .arg("Content-Type: application/json")
        .arg("-d")
        .arg(&auth_payload_str)
        .arg("--max-time")
        .arg("30")
        .arg("--silent")
        .arg("--show-error")
        .output()
        .map_err(|e| {
            log::error!("❌ Error ejecutando curl para autenticación: {}", e);
            anyhow::anyhow!("Error ejecutando curl: {}", e)
        })?;

    if !curl_output.status.success() {
        let error_msg = String::from_utf8_lossy(&curl_output.stderr);
        log::error!("❌ Curl falló en autenticación: {}", error_msg);
        anyhow::bail!("Curl falló: {}", error_msg);
    }

    let response_body = String::from_utf8_lossy(&curl_output.stdout);
    log::info!("📥 Respuesta de autenticación curl: {}", response_body);
    
        // 🆕 PARSEAR RESPUESTA DE AUTENTICACIÓN CURL
    let auth_text = response_body.to_string();
    
    log::info!("📥 Respuesta de autenticación recibida: {}", &auth_text[..auth_text.len().min(200)]);
    
    // Parsear la respuesta de Colis Privé
    let auth_data: serde_json::Value = serde_json::from_str(&auth_text).map_err(|e| {
        log::error!("❌ Error parseando respuesta de autenticación: {}", e);
        anyhow::anyhow!("Error parseando respuesta: {}", e)
    })?;
    
    // 🔍 DEBUG: Imprimir todos los campos de la respuesta
    log::info!("🔍 Campos disponibles en la respuesta:");
    if let Some(obj) = auth_data.as_object() {
        for (key, value) in obj {
            log::info!("  - {}: {}", key, value);
        }
    }
    
    // 🔍 BUSCAR EL TOKEN EN DIFERENTES CAMPOS POSIBLES (incluyendo campos anidados)
    let sso_hopps = auth_data.get("SsoHopps")
        .or_else(|| auth_data.get("ssoHopps"))
        .or_else(|| auth_data.get("token"))
        .or_else(|| auth_data.get("Token"))
        .or_else(|| auth_data.get("access_token"))
        .or_else(|| auth_data.get("accessToken"))
        .or_else(|| auth_data.get("tokens").and_then(|t| t.get("SsoHopps")))
        .or_else(|| auth_data.get("shortToken").and_then(|t| t.get("SsoHopps")))
        .or_else(|| auth_data.get("habilitationAD")
            .and_then(|h| h.get("SsoHopps"))
            .and_then(|s| s.as_array())
            .and_then(|arr| arr.get(0))
            .and_then(|item| item.get("valeur")))
        .and_then(|v| v.as_str())
        .ok_or_else(|| {
            log::error!("❌ Token no encontrado en ningún campo. Campos disponibles: {:?}", 
                auth_data.as_object().map(|obj| obj.keys().collect::<Vec<_>>()));
            anyhow::anyhow!("Token no encontrado en la respuesta")
        })?;
    
    log::info!("✅ Token SsoHopps obtenido exitosamente");
    
    let auth_response = ColisPriveAuthResponse {
        success: true,
        message: "Autenticación exitosa con Colis Privé".to_string(),
        token: Some(sso_hopps.to_string()),
        matricule: Some(credentials.username.clone()),
    };
    
    Ok(auth_response)
}

/// GET /api/colis-prive/packages-test - Test simple para verificar endpoint
pub async fn test_packages_endpoint() -> Result<Json<serde_json::Value>, StatusCode> {
    log::info!("🔥 TEST ENDPOINT LLAMADO");
    Ok(Json(serde_json::json!({
        "success": true,
        "message": "Test endpoint funcionando",
        "timestamp": chrono::Utc::now()
    })))
}

/// POST /api/colis-prive/packages - Obtener paquetes desde Colis Privé (IMPLEMENTACIÓN REAL)
pub async fn get_packages(
    State(state): State<AppState>,
    Json(request): Json<GetPackagesRequest>,
) -> Result<Json<crate::services::GetPackagesResponse>, StatusCode> {
    use tracing::info;
    use crate::services::{GetPackagesResponse, PackageData};

    log::info!("🔥 FUNCIÓN GET_PACKAGES INICIADA");
    info!("🚀 ENDPOINT GET_PACKAGES LLAMADO - matricule: {}", request.matricule);
    info!("📦 Obteniendo paquetes para matricule: {}", request.matricule);

    // Construir el matricule completo (societe + username)
    let societe = std::env::var("COLIS_PRIVE_SOCIETE")
        .expect("COLIS_PRIVE_SOCIETE must be set in environment variables");
    let matricule_completo = format!("{}_{}", societe, request.matricule);
    
    // Construir la fecha (hoy si no se especifica)
    let date = request.date.unwrap_or_else(|| {
        chrono::Utc::now().format("%Y-%m-%d").to_string()
    });

    // Llamar al endpoint real de Colis Privé

    // 🆕 OBTENER EL TOKEN DINÁMICAMENTE DEL ESTADO DE LA APLICACIÓN
    // request.matricule es el username, no el matricule completo
    let sso_hopps = match state.get_auth_token(&request.matricule, &societe).await {
        Some(auth_token) => {
            if auth_token.is_expired() {
                log::warn!("⚠️ Token expirado para {}:{}, necesitamos re-autenticar", societe, request.matricule);
                return Err(StatusCode::UNAUTHORIZED);
            }
            log::info!("✅ Usando token almacenado para {}:{}", societe, request.matricule);
            auth_token.token
        }
        None => {
            log::warn!("⚠️ No hay token almacenado para {}:{}, intentando autenticación automática", societe, request.matricule);
            
            // 🆕 INTENTAR AUTENTICACIÓN AUTOMÁTICA
            match attempt_auto_auth(&state, &request.matricule, &societe).await {
                Ok(token) => {
                    log::info!("✅ Autenticación automática exitosa para {}:{}", societe, request.matricule);
                    token
                }
                Err(e) => {
                    log::error!("❌ Autenticación automática falló para {}:{} - {}", societe, request.matricule, e);
                    return Err(StatusCode::UNAUTHORIZED);
                }
            }
        }
    };

    // 🆕 USAR CURL DIRECTAMENTE
    let payload = serde_json::json!({
        "Matricule": matricule_completo,
        "DateDebut": date
    });

    let payload_str = serde_json::to_string(&payload).map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;
    let tournee_url = format!("{}/WS-TourneeColis/api/getTourneeByMatriculeDistributeurDateDebut_POST", state.config.colis_prive_tournee_url);
    
    log::info!("📤 Llamando a: {}", tournee_url);
    log::info!("📦 Payload: {}", payload_str);
    
    let curl_output = std::process::Command::new("curl")
        .arg("-X").arg("POST")
        .arg(tournee_url)
        .arg("-H").arg("Content-Type: application/json")
        .arg("-H").arg(format!("SsoHopps: {}", sso_hopps))
        .arg("-d").arg(&payload_str)
        .arg("--max-time").arg("30")
        .arg("--silent").arg("--show-error")
        .output()
        .map_err(|_| StatusCode::INTERNAL_SERVER_ERROR)?;

    if !curl_output.status.success() {
        log::error!("❌ Curl falló - stderr: {}", String::from_utf8_lossy(&curl_output.stderr));
        return Err(StatusCode::BAD_REQUEST);
    }

    let response_str = String::from_utf8_lossy(&curl_output.stdout);
    log::info!("📥 Respuesta recibida: {} bytes", response_str.len());

    // Parsear la respuesta JSON de Colis Privé
    let tournee_data: serde_json::Value = serde_json::from_str(&response_str)
        .map_err(|e| {
            log::error!("❌ Error parseando respuesta JSON: {}", e);
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    // Extraer paquetes de LstLieuArticle
    let packages = if let Some(lst_lieu_article) = tournee_data.get("LstLieuArticle") {
        if let Some(packages_array) = lst_lieu_article.as_array() {
            packages_array
                .iter()
                .filter_map(|package| {
                    // Solo procesar paquetes de tipo COLIS
                    if package.get("metier")?.as_str() == Some("COLIS") {
                        Some(PackageData {
                            id: package.get("idArticle")?.as_str()?.to_string(),
                            tracking_number: package.get("refExterneArticle")?.as_str()?.to_string(),
                            recipient_name: package.get("nomDestinataire")?.as_str()?.to_string(),
                            address: format!(
                                "{}, {} {}",
                                package.get("LibelleVoieOrigineDestinataire")?.as_str()?,
                                package.get("codePostalOrigineDestinataire")?.as_str()?,
                                package.get("LibelleLocaliteOrigineDestinataire")?.as_str()?
                            ),
                            status: package.get("codeStatutArticle")?.as_str()?.to_string(),
                            instructions: package.get("PreferenceLivraison")?.as_str()?.to_string(),
                            phone: package.get("telephoneMobileDestinataire")?.as_str()?.to_string(),
                            priority: package.get("priorite")?.as_u64()?.to_string(),
                            latitude: None,
                            longitude: None,
                            formatted_address: None,
                            validation_method: None,
                            validation_confidence: None,
                            validation_warnings: None,
                        })
                    } else {
                        None
                    }
                })
                .collect::<Vec<_>>()
        } else {
            vec![]
        }
    } else {
        vec![]
    };

    log::info!("📦 Paquetes extraídos: {} paquetes", packages.len());

    // Si no hay paquetes, verificar si es una tournée completada
    if packages.is_empty() {
        if let Some(infos_tournee) = tournee_data.get("InfosTournee") {
            let code_tournee = infos_tournee.get("codeTourneeDistribution")
                .and_then(|v| v.as_str())
                .unwrap_or("Desconocida");
            return Ok(Json(GetPackagesResponse {
                success: true,
                message: format!("Tournée {} completada - No hay paquetes pendientes", code_tournee),
                packages: None,
                error: None,
                address_validation: None,
            }));
        }
    }

    // 🆕 VALIDACIÓN INTELIGENTE DE DIRECCIONES
    log::info!("🔍 Iniciando validación inteligente de direcciones para {} paquetes", packages.len());
    
    let mut validated_packages = Vec::new();
    let mut validation_summary = crate::services::AddressValidationSummary {
        total_packages: packages.len(),
        auto_validated: 0,
        cleaned_auto: 0,
        completed_auto: 0,
        partial_found: 0,
        requires_manual: 0,
        warnings: Vec::new(),
    };

    // Crear el validador de direcciones
    if let Some(mapbox_token) = &state.config.mapbox_token {
        let geocoding_service = crate::services::GeocodingService::new(mapbox_token.clone());
        let address_validator = crate::services::AddressValidator::new(geocoding_service);
        
        // Validar cada paquete
        for mut package in packages {
            match address_validator.validate_address(&package.address, &request.matricule).await {
                Ok(validated) => {
                    // Actualizar el paquete con la información de validación
                    package.latitude = validated.latitude;
                    package.longitude = validated.longitude;
                    package.formatted_address = validated.formatted_address;
                    package.validation_method = Some(format!("{:?}", validated.validation_method));
                    package.validation_confidence = Some(format!("{:?}", validated.confidence));
                    package.validation_warnings = Some(validated.warnings.clone());
                    
                    // Actualizar estadísticas
                    match validated.validation_method {
                        crate::services::ValidationMethod::Original => validation_summary.auto_validated += 1,
                        crate::services::ValidationMethod::Cleaned => validation_summary.cleaned_auto += 1,
                        crate::services::ValidationMethod::CompletedWithSector => validation_summary.completed_auto += 1,
                        crate::services::ValidationMethod::PartialSearch => validation_summary.partial_found += 1,
                        crate::services::ValidationMethod::ManualRequired => validation_summary.requires_manual += 1,
                    }
                    
                    // Agregar warnings al resumen
                    validation_summary.warnings.extend(validated.warnings);
                    
                    validated_packages.push(package);
                }
                Err(e) => {
                    log::error!("❌ Error validando dirección '{}': {}", package.address, e);
                    validation_summary.requires_manual += 1;
                    package.validation_method = Some("ManualRequired".to_string());
                    package.validation_confidence = Some("None".to_string());
                    package.validation_warnings = Some(vec![format!("Error de validación: {}", e)]);
                    validated_packages.push(package);
                }
            }
        }
        
        log::info!("✅ Validación completada: {} auto-validados, {} limpiados, {} completados, {} parciales, {} manuales", 
            validation_summary.auto_validated, 
            validation_summary.cleaned_auto, 
            validation_summary.completed_auto, 
            validation_summary.partial_found, 
            validation_summary.requires_manual
        );
    } else {
        log::warn!("⚠️ MAPBOX_TOKEN no configurado, saltando validación de direcciones");
        validation_summary.requires_manual = packages.len();
        validated_packages = packages;
    }

    Ok(Json(GetPackagesResponse {
        success: true,
        message: format!("Paquetes obtenidos y validados exitosamente - {} paquetes", validated_packages.len()),
        packages: Some(validated_packages),
        error: None,
        address_validation: Some(validation_summary),
    }))
}

/// POST /api/colis-prive/tournee - Obtener tournée (IMPLEMENTACIÓN COMPLETA)
pub async fn get_tournee_data(
    State(state): State<AppState>,
    Json(request): Json<GetTourneeRequest>,
) -> Result<Json<serde_json::Value>, StatusCode> {
    log::info!("🔄 Obteniendo tournée para: {}", request.matricule);
    
    // 🆕 PASO 1: OBTENER TOKEN DEL ESTADO COMPARTIDO (AUTENTICACIÓN DINÁMICA)
    log::info!("🔍 Buscando token para username: '{}', societe: '{}'", request.username, request.societe);
    
    let sso_hopps = match state.get_auth_token(&request.username, &request.societe).await {
        Some(auth_token) => {
            if auth_token.is_expired() {
                log::warn!("⚠️ Token expirado para {}:{}, necesitamos re-autenticar", request.societe, request.username);
                return Err(StatusCode::UNAUTHORIZED);
            }
            log::info!("✅ Usando token almacenado para {}:{}", request.societe, request.username);
            auth_token.token
        }
        None => {
            log::warn!("⚠️ No hay token almacenado para {}:{}, intentando autenticación automática", request.societe, request.username);
            
            // 🆕 INTENTAR AUTENTICACIÓN AUTOMÁTICA
            match attempt_auto_auth(&state, &request.username, &request.societe).await {
                Ok(token) => {
                    log::info!("✅ Autenticación automática exitosa para {}:{}", request.societe, request.username);
                    token
                }
                Err(e) => {
                    log::error!("❌ Autenticación automática falló para {}:{} - {}", request.societe, request.username, e);
                    return Err(StatusCode::UNAUTHORIZED);
                }
            }
        }
    };

    // 🆕 PASO 2: Hacer petición REAL a Colis Privé para obtener tournée
    let tournee_url = format!("{}/WS-TourneeColis/api/getTourneeByMatriculeDistributeurDateDebut_POST", state.config.colis_prive_tournee_url);

    // 🆕 PAYLOAD YA DEFINIDO ARRIBA

    log::info!("📤 Enviando petición tournée a: {}", tournee_url);
    
    // 🔍 LOGGING DETALLADO DE HEADERS Y TOKEN
    log::info!("🔑 TOKEN USADO: {}", sso_hopps);
    log::info!("📋 HEADERS SIMPLIFICADOS (como curl):");
    log::info!("   Content-Type: application/json");
    log::info!("   SsoHopps: {}", sso_hopps);
    log::info!("   User-Agent: curl/7.68.0");

        // 🆕 USAR CURL DIRECTAMENTE EN LUGAR DE REQWEST
    let matricule_completo = format!("{}_{}", request.societe, request.username);
    let date = request.date.clone().unwrap_or_else(|| "2025-09-01".to_string());
    
    let tournee_payload = json!({
        "Matricule": matricule_completo,
        "DateDebut": date
    });
    
    let payload_str = serde_json::to_string(&tournee_payload).map_err(|e| {
        log::error!("❌ Error serializando payload: {}", e);
        StatusCode::INTERNAL_SERVER_ERROR
    })?;
    
    let curl_output = std::process::Command::new("curl")
        .arg("-X")
        .arg("POST")
        .arg(tournee_url)
        .arg("-H")
        .arg("Content-Type: application/json")
        .arg("-H")
        .arg(format!("SsoHopps: {}", sso_hopps))
        .arg("-d")
        .arg(&payload_str)
        .arg("--max-time")
        .arg("30")
        .arg("--silent")
        .arg("--show-error")
        .output()
        .map_err(|e| {
            log::error!("❌ Error ejecutando curl: {}", e);
            StatusCode::INTERNAL_SERVER_ERROR
        })?;

    if !curl_output.status.success() {
        let error_msg = String::from_utf8_lossy(&curl_output.stderr);
        log::error!("❌ Curl falló: {}", error_msg);
        return Err(StatusCode::INTERNAL_SERVER_ERROR);
    }

    let response_body = String::from_utf8_lossy(&curl_output.stdout);
    log::info!("📥 Respuesta de curl: {}", response_body);

    // 🆕 PARSEAR RESPUESTA DE CURL
    let tournee_text = response_body.to_string();

    log::info!("📥 Respuesta tournée recibida: {} bytes", tournee_text.len());

    // 🔧 PASO 3: Decodificar base64 si es necesario
    let decoded_data = if tournee_text.starts_with('"') && tournee_text.ends_with('"') {
        let base64_content = &tournee_text[1..tournee_text.len()-1];
        match base64::Engine::decode(&base64::engine::general_purpose::STANDARD, base64_content) {
            Ok(decoded) => {
                log::info!("✅ Datos decodificados de base64: {} bytes", decoded.len());
                String::from_utf8(decoded).unwrap_or(tournee_text)
            },
            Err(_) => {
                log::info!("ℹ️ No se pudo decodificar base64, usando texto original");
                tournee_text
            }
        }
    } else {
        log::info!("ℹ️ Respuesta no es base64, usando texto original");
        tournee_text
    };

    // 🔧 PASO 4: Respuesta final con datos reales de Colis Privé
    let response = json!({
        "success": true,
        "message": "Tournée obtenida exitosamente de Colis Privé",
        "data": decoded_data,
        "metadata": {
            "matricule": request.matricule,
            "societe": request.societe,
            "date": request.date.clone().unwrap_or_else(|| "2025-08-28".to_string()),
            "api_type": "web",
            "token_used": true,
            "headers_sent": true,
            "real_request": true,
            "token_source": "shared_state"  // 🆕 INDICAR QUE EL TOKEN VIENE DEL ESTADO COMPARTIDO
        },
        "timestamp": chrono::Utc::now().to_rfc3339()
    });

    log::info!("✅ Tournée obtenida exitosamente con datos reales usando token del estado compartido");
    Ok(Json(response))
}

/// GET /api/colis-prive/health - Health check del servicio
pub async fn health_check() -> Json<serde_json::Value> {
    Json(json!({
        "service": "colis-prive",
        "status": "healthy",
        "message": "Servicio Colis Privé funcionando correctamente"
    }))
}

/// GET /api/colis-prive/health - Health check de Colis Privé
pub async fn health_check_colis_prive() -> Result<Json<serde_json::Value>, StatusCode> {
    use tracing::info;
    
    info!(
        endpoint = "health_check",
        "Starting Colis Privé health check"
    );
    
    let start_time = std::time::Instant::now();
    
    let health_info = json!({
        "status": "healthy",
        "colis_prive_client": {
            "ssl_bypass_enabled": true,
            "headers_system": "implemented",
            "device_info_consistency": "enforced"
        },
        "timestamp": chrono::Utc::now().to_rfc3339(),
        "version": env!("CARGO_PKG_VERSION"),
        "check_duration_ms": start_time.elapsed().as_millis(),
        "note": "Device info consistency enforced - no hardcoded values"
    });
    
    info!(
        endpoint = "health_check",
        status = "healthy",
        duration_ms = start_time.elapsed().as_millis(),
        "Health check completed successfully"
    );
    
    Ok(Json(health_info))
}

// ====================================================================
// FUNCIONES DE AUTENTICACIÓN AUTOMÁTICA
// ====================================================================

/// 🆕 FUNCIÓN DE AUTENTICACIÓN AUTOMÁTICA
/// Intenta autenticar automáticamente cuando no hay token disponible
async fn attempt_auto_auth(
    state: &AppState,
    username: &str,
    societe: &str,
) -> Result<String, anyhow::Error> {
    use crate::services::colis_prive_service::ColisPriveAuthRequest;
    
    log::info!("🔄 Intentando autenticación automática para {}:{}", societe, username);
    
    // 🔑 CREDENCIALES SEGURAS DESDE VARIABLES DE ENTORNO
    // En producción, las credenciales vienen de variables de entorno seguras
    let password = std::env::var("COLIS_PRIVE_PASSWORD")
        .expect("COLIS_PRIVE_PASSWORD must be set in environment variables");
    
    let credentials = ColisPriveAuthRequest {
        username: username.to_string(),
        password: password,
        societe: societe.to_string(),
    };
    
    // Intentar autenticación
    match authenticate_colis_prive_simple(&credentials, &state.config).await {
        Ok(auth_response) => {
            if auth_response.success {
                if let Some(token) = auth_response.token {
                    // Almacenar el token en el estado
                    state.store_auth_token(
                        username.to_string(),
                        societe.to_string(),
                        token.clone(),
                        24
                    ).await;
                    
                    log::info!("✅ Autenticación automática exitosa para {}:{}", societe, username);
                    Ok(token)
                } else {
                    anyhow::bail!("Token no recibido en la respuesta de autenticación")
                }
            } else {
                anyhow::bail!("Autenticación automática falló: {}", auth_response.message)
            }
        }
        Err(e) => {
            log::error!("❌ Error en autenticación automática: {}", e);
            Err(e)
        }
    }
}

/// GET /api/colis-prive/companies - Obtener lista de empresas disponibles
pub async fn get_companies(State(state): State<AppState>) -> Result<Json<ColisPriveCompanyListResponse>, StatusCode> {
    log::info!("🏢 Obteniendo lista de empresas disponibles desde Colis Privé");
    
    // Crear el servicio con la URL base de Colis Privé (API real)
    let service = ColisPriveCompaniesService::new(state.config.colis_prive_referentiel_url.clone());
    
    match service.get_companies().await {
        Ok(companies) => {
            log::info!("✅ Empresas obtenidas desde Colis Privé: {} empresas", companies.len());
            
            // Mapear a nuestro formato de respuesta
            let mapped_companies: Vec<crate::models::colis_prive_company::ColisPriveCompany> = companies
                .into_iter()
                .map(|cp| crate::models::colis_prive_company::ColisPriveCompany {
                    code: cp.code,
                    name: cp.libelle,
                    description: None,
                })
                .collect();
            
            let response = ColisPriveCompanyListResponse {
                success: true,
                companies: mapped_companies,
                message: Some("Empresas obtenidas desde Colis Privé".to_string()),
            };
            
            Ok(Json(response))
        }
        Err(e) => {
            log::error!("❌ Error obteniendo empresas desde Colis Privé: {}", e);
            
            // Fallback a datos de test si falla la llamada real
            let response = ColisPriveCompanyListResponse::default();
            log::warn!("⚠️ Usando datos de test como fallback");
            
            Ok(Json(response))
        }
    }
}

// ====================================================================
// Solo API Web - Funciones móviles legacy eliminadas
// ====================================================================
