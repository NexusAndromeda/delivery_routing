package com.daniel.deliveryrouting.presentation.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.core.content.ContextCompat
import com.daniel.deliveryrouting.data.api.models.PackageData
import com.daniel.deliveryrouting.data.api.models.MobileTourneeRequest
import com.daniel.deliveryrouting.data.api.models.MobileTourneeData
import com.daniel.deliveryrouting.data.api.BackendApi
import com.daniel.deliveryrouting.ui.theme.DeliveryRoutingTheme
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.CameraOptions
import com.mapbox.geojson.Point
import androidx.compose.ui.viewinterop.AndroidView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.layers.addLayer

private const val TAG_MAP = "PackageMapScreen"

@Composable
fun PackageMapScreen(
    packages: List<PackageData>,
    onBackClick: () -> Unit
) {
    Log.d(TAG_MAP, "🗺️ PackageMapScreen iniciado con ${packages.size} paquetes")
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var mobileData by remember { mutableStateOf<MobileTourneeData?>(null) }
    var isLoadingMobile by remember { mutableStateOf(false) }
    var mobileError by remember { mutableStateOf<String?>(null) }
    
    // Registrar para solicitar permisos
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        showPermissionDialog = false
        Log.d(TAG_MAP, "📍 Resultado de permisos: ${if (isGranted) "✅ Concedidos" else "❌ Denegados"}")
    }
    
    // Verificar permisos de ubicación
    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        Log.d(TAG_MAP, "📍 Permisos de ubicación: ${if (hasLocationPermission) "✅ Concedidos" else "❌ No concedidos"}")
        
        // Si no tiene permisos, mostrar diálogo automáticamente
        if (!hasLocationPermission) {
            showPermissionDialog = true
        }
        
        // Cargar datos básicos
        loadBasicData()
    }
    
    // Función para cargar datos básicos (sin API móvil temporal)
    fun loadBasicData() {
        Log.d(TAG_MAP, "📦 Cargando datos básicos de paquetes...")
        isLoadingMobile = false
        mobileError = null
        mobileData = null
    }
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Header con botón de regreso
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mapa de Paquetes",
                style = MaterialTheme.typography.headlineMedium
            )
            TextButton(onClick = {
                Log.d(TAG_MAP, "⬅️ Botón regresar presionado")
                onBackClick()
            }) {
                Text("← Regresar")
            }
        }
        
        // Mapa
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            if (hasLocationPermission) {
                // 🗺️ MAPA REAL DE MAPBOX
                Log.d(TAG_MAP, "🗺️ Mostrando mapa real de Mapbox con ${packages.size} paquetes")
                
                // 🗺️ MAPA REAL DE MAPBOX CON MARCADORES
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        MapView(context).apply {
                            Log.d(TAG_MAP, "🗺️ Creando MapView con ${packages.size} paquetes...")
                            
                            // Configurar el mapa según la documentación oficial
                            getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
                                Log.d(TAG_MAP, "🗺️ Estilo MAPBOX_STREETS cargado exitosamente")
                                
                                // Configurar cámara centrada en Paris
                                val cameraOptions = CameraOptions.Builder()
                                    .center(Point.fromLngLat(2.3522, 48.8566)) // Paris
                                    .zoom(12.0)
                                    .build()
                                
                                getMapboxMap().setCamera(cameraOptions)
                                Log.d(TAG_MAP, "🗺️ Cámara configurada en Paris (2.3522, 48.8566)")
                                
                                // 📍 AGREGAR MARCADORES DE PAQUETES (híbrido)
                                if (mobileData != null) {
                                    addMobilePackageMarkers(style, mobileData!!.paquetes)
                                    addOptimizedRoute(style, mobileData!!.rutaOptimizada)
                                } else {
                                    addPackageMarkers(style, packages)
                                }
                            }
                        }
                    }
                )
            } else {
                // Mensaje si no hay permisos
                Log.d(TAG_MAP, "❌ Sin permisos de ubicación - mostrando mensaje")
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🗺️ Mapa de Paquetes",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Se necesitan permisos de ubicación para mostrar el mapa",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Paquetes encontrados: ${packages.size}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Botón para solicitar permisos manualmente
                    Button(
                        onClick = {
                            Log.d(TAG_MAP, "🔄 Usuario solicitó permisos manualmente")
                            showPermissionDialog = true
                        }
                    ) {
                        Text("📍 Solicitar Permisos de Ubicación")
                    }
                }
            }
        }
        
        // Footer con información híbrida
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (isLoadingMobile) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cargando datos híbridos...")
                    }
                } else if (mobileError != null) {
                    Text(
                        text = "❌ Error móvil: $mobileError",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (mobileData != null) {
                    Text(
                        text = "🚀 Sistema Híbrido Activo",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "📦 Paquetes: ${mobileData!!.paquetes.size} | 📊 Eficiencia: ${String.format("%.1f", mobileData!!.estadisticas.eficiencia * 100)}%",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "🛣️ Ruta: ${String.format("%.1f", mobileData!!.rutaOptimizada?.distanciaTotal ?: 0.0)}km | ⏱️ Tiempo: ${mobileData!!.rutaOptimizada?.tiempoEstimado ?: 0}min",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text(
                        text = "📦 Paquetes básicos: ${packages.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (packages.isNotEmpty()) {
                        Text(
                            text = "Primer paquete: ${packages.first().trackingNumber}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
    
    // 🗺️ DIÁLOGO DE PERMISOS DE UBICACIÓN
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { 
                showPermissionDialog = false
                Log.d(TAG_MAP, "❌ Usuario canceló solicitud de permisos")
            },
            title = {
                Text("📍 Permisos de Ubicación")
            },
            text = {
                Text(
                    "Para mostrar el mapa con la ubicación de los paquetes, " +
                    "necesitamos acceso a tu ubicación.\n\n" +
                    "Esto nos permitirá:\n" +
                    "• Mostrar tu posición en el mapa\n" +
                    "• Calcular rutas optimizadas\n" +
                    "• Navegar entre paquetes\n\n" +
                    "¿Permitir acceso a la ubicación?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Log.d(TAG_MAP, "✅ Usuario aceptó solicitar permisos")
                        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                ) {
                    Text("✅ Permitir")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPermissionDialog = false
                        Log.d(TAG_MAP, "❌ Usuario rechazó permisos")
                    }
                ) {
                    Text("❌ Cancelar")
                }
            }
        )
    }
}

// 📍 FUNCIÓN PARA AGREGAR MARCADORES DE PAQUETES USANDO CÍRCULOS SIMPLES
private fun addPackageMarkers(style: Style, packages: List<PackageData>) {
    Log.d(TAG_MAP, "📍 Agregando ${packages.size} marcadores de paquetes usando círculos...")
    
    // Filtrar paquetes que tienen coordenadas
    val packagesWithCoords = packages.filter { 
        it.latitude != null && it.longitude != null 
    }
    
    Log.d(TAG_MAP, "📍 ${packagesWithCoords.size} paquetes tienen coordenadas válidas")
    
    if (packagesWithCoords.isEmpty()) {
        Log.w(TAG_MAP, "⚠️ No hay paquetes con coordenadas para mostrar")
        return
    }
    
    try {
        // Crear FeatureCollection con todos los puntos
        val features = packagesWithCoords.map { packageData ->
            val point = Point.fromLngLat(packageData.longitude!!, packageData.latitude!!)
            com.mapbox.geojson.Feature.fromGeometry(point).apply {
                addStringProperty("id", packageData.id)
                addStringProperty("trackingNumber", packageData.trackingNumber)
                addStringProperty("recipientName", packageData.recipientName)
                addStringProperty("address", packageData.address)
                addStringProperty("status", packageData.status)
                addStringProperty("priority", packageData.priority)
            }
        }
        
        val featureCollection = com.mapbox.geojson.FeatureCollection.fromFeatures(features)
        
        // Agregar fuente de datos
        style.addSource(
            geoJsonSource("packages-source") {
                featureCollection(featureCollection)
            }
        )
        
        // Agregar capa de círculos para los marcadores
        style.addLayer(
            circleLayer("packages-layer", "packages-source") {
                circleRadius(8.0)
                circleColor("#FF0000") // Rojo
                circleStrokeColor("#FFFFFF") // Borde blanco
                circleStrokeWidth(2.0)
            }
        )
        
        Log.d(TAG_MAP, "✅ ${packagesWithCoords.size} marcadores de paquetes agregados exitosamente")
        
        // Log de cada paquete agregado
        packagesWithCoords.forEach { packageData ->
            Log.d(TAG_MAP, "📍 Paquete ${packageData.trackingNumber}: ${packageData.latitude}, ${packageData.longitude}")
        }
        
    } catch (e: Exception) {
        Log.e(TAG_MAP, "❌ Error agregando marcadores: ${e.message}", e)
    }
}

// 📍 FUNCIÓN PARA AGREGAR MARCADORES DE PAQUETES MÓVILES
private fun addMobilePackageMarkers(style: Style, packages: List<com.daniel.deliveryrouting.data.api.models.MobilePackage>) {
    Log.d(TAG_MAP, "📍 Agregando ${packages.size} marcadores de paquetes móviles...")
    
    if (packages.isEmpty()) {
        Log.w(TAG_MAP, "⚠️ No hay paquetes móviles para mostrar")
        return
    }
    
    try {
        // Crear FeatureCollection con todos los puntos
        val features = packages.map { packageData ->
            val point = Point.fromLngLat(packageData.coordenadas.longitud, packageData.coordenadas.latitud)
            com.mapbox.geojson.Feature.fromGeometry(point).apply {
                addStringProperty("id", packageData.id)
                addStringProperty("refColis", packageData.refColis)
                addStringProperty("direccion", packageData.direccion)
                addStringProperty("estado", packageData.estado)
                addStringProperty("prioridad", packageData.prioridad)
                addStringProperty("tipoEntrega", packageData.tipoEntrega)
            }
        }
        
        val featureCollection = com.mapbox.geojson.FeatureCollection.fromFeatures(features)
        
        // Agregar fuente de datos
        style.addSource(
            geoJsonSource("mobile-packages-source") {
                featureCollection(featureCollection)
            }
        )
        
        // Agregar capa de círculos para los marcadores
        style.addLayer(
            circleLayer("mobile-packages-layer", "mobile-packages-source") {
                circleRadius(10.0)
                circleColor("#3b82f6") // Azul para paquetes móviles
                circleStrokeColor("#FFFFFF") // Borde blanco
                circleStrokeWidth(3.0)
            }
        )
        
        Log.d(TAG_MAP, "✅ ${packages.size} marcadores de paquetes móviles agregados exitosamente")
        
        // Log de cada paquete agregado
        packages.forEach { packageData ->
            Log.d(TAG_MAP, "📍 Paquete móvil ${packageData.refColis}: ${packageData.coordenadas.latitud}, ${packageData.coordenadas.longitud}")
        }
        
    } catch (e: Exception) {
        Log.e(TAG_MAP, "❌ Error agregando marcadores móviles: ${e.message}", e)
    }
}

// 🛣️ FUNCIÓN PARA AGREGAR RUTA OPTIMIZADA
private fun addOptimizedRoute(style: Style, route: com.daniel.deliveryrouting.data.api.models.OptimizedRoute?) {
    if (route == null || route.coordenadasRuta.isEmpty()) {
        Log.d(TAG_MAP, "⚠️ No hay ruta optimizada para mostrar")
        return
    }
    
    try {
        Log.d(TAG_MAP, "🛣️ Agregando ruta optimizada con ${route.coordenadasRuta.size} puntos")
        
        // Crear línea de ruta
        val coordinates = route.coordenadasRuta.map { coord ->
            Point.fromLngLat(coord.longitud, coord.latitud)
        }
        
        val lineString = com.mapbox.geojson.LineString.fromLngLats(coordinates)
        val feature = com.mapbox.geojson.Feature.fromGeometry(lineString)
        val featureCollection = com.mapbox.geojson.FeatureCollection.fromFeature(feature)
        
        // Agregar fuente de datos
        style.addSource(
            geoJsonSource("optimized-route-source") {
                featureCollection(featureCollection)
            }
        )
        
        // Agregar capa de línea
        style.addLayer(
            com.mapbox.maps.extension.style.layers.generated.lineLayer("optimized-route-layer", "optimized-route-source") {
                lineColor("#10b981") // Verde para ruta optimizada
                lineWidth(4.0)
                lineCap("round")
                lineJoin("round")
            }
        )
        
        Log.d(TAG_MAP, "✅ Ruta optimizada agregada: ${route.distanciaTotal}km, ${route.tiempoEstimado}min")
        
    } catch (e: Exception) {
        Log.e(TAG_MAP, "❌ Error agregando ruta optimizada: ${e.message}", e)
    }
}
