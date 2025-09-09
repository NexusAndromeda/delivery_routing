//! Configuración de variables de entorno
//! 
//! Este módulo maneja la configuración del entorno y variables de configuración.

use std::env;

/// Configuración del entorno
#[derive(Debug, Clone)]
pub struct EnvironmentConfig {
    pub environment: String,
    pub port: u16,
    pub host: String,
    pub jwt_secret: String,
    pub jwt_expiration: u64,
    pub cors_origins: Vec<String>,
    pub rate_limit_requests: u32,
    pub rate_limit_window: u64,
    pub mapbox_token: Option<String>,
    // URLs de Colis Privé
    pub colis_prive_auth_url: String,
    pub colis_prive_tournee_url: String,
    pub colis_prive_detail_url: String,
    pub colis_prive_gestion_url: String,
    pub colis_prive_referentiel_url: String,
}

impl Default for EnvironmentConfig {
    fn default() -> Self {
        Self {
            environment: env::var("ENVIRONMENT").unwrap_or_else(|_| "development".to_string()),
            port: env::var("PORT")
                .unwrap_or_else(|_| "3000".to_string())
                .parse()
                .unwrap_or(3000),
            host: env::var("HOST").unwrap_or_else(|_| "0.0.0.0".to_string()),
            jwt_secret: env::var("JWT_SECRET").unwrap_or_else(|_| "your-secret-key".to_string()),
            jwt_expiration: env::var("JWT_EXPIRATION")
                .unwrap_or_else(|_| "86400".to_string())
                .parse()
                .unwrap_or(86400),
            cors_origins: env::var("CORS_ORIGINS")
                .unwrap_or_else(|_| "http://localhost:3000,http://localhost:3001".to_string())
                .split(',')
                .map(|s| s.trim().to_string())
                .collect(),
            rate_limit_requests: env::var("RATE_LIMIT_REQUESTS")
                .unwrap_or_else(|_| "100".to_string())
                .parse()
                .unwrap_or(100),
            rate_limit_window: env::var("RATE_LIMIT_WINDOW")
                .unwrap_or_else(|_| "3600".to_string())
                .parse()
                .unwrap_or(3600),
            mapbox_token: env::var("MAPBOX_TOKEN").ok(),
            // URLs de Colis Privé
            colis_prive_auth_url: env::var("COLIS_PRIVE_AUTH_URL")
                .unwrap_or_else(|_| "https://wsauthentificationexterne.colisprive.com".to_string()),
            colis_prive_tournee_url: env::var("COLIS_PRIVE_TOURNEE_URL")
                .unwrap_or_else(|_| "https://wstournee-v2.colisprive.com".to_string()),
            colis_prive_detail_url: env::var("COLIS_PRIVE_DETAIL_URL")
                .unwrap_or_else(|_| "https://wstournee-v2.colisprive.com".to_string()),
            colis_prive_gestion_url: env::var("COLIS_PRIVE_GESTION_URL")
                .unwrap_or_else(|_| "https://gestiontournee.colisprive.com".to_string()),
            colis_prive_referentiel_url: env::var("COLIS_PRIVE_REFERENTIEL_URL")
                .unwrap_or_else(|_| "https://wsreferentiel-v2.colisprive.com/WS_RefDistributeur/RefDistributeurConsolideExtranetToExterne.svc".to_string()),
        }
    }
}

impl EnvironmentConfig {
    /// Verificar si estamos en modo desarrollo
    pub fn is_development(&self) -> bool {
        self.environment == "development"
    }

    /// Verificar si estamos en modo producción
    pub fn is_production(&self) -> bool {
        self.environment == "production"
    }

    /// Obtener la URL del servidor
    pub fn server_url(&self) -> String {
        format!("{}:{}", self.host, self.port)
    }
}

// Las credenciales de Colis Privé ahora se reciben dinámicamente via HTTP requests
// No hay credenciales hardcodeadas en el código

