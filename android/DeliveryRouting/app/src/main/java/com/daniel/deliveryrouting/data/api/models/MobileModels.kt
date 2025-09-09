package com.daniel.deliveryrouting.data.api.models

// Modelos para la API móvil
data class MobileTourneeRequest(
    val driverId: String,
    val date: String
)

data class MobileTourneeResponse(
    val success: Boolean,
    val data: MobileTourneeData?,
    val message: String?
)

data class MobileTourneeData(
    val paquetes: List<MobilePackage>,
    val rutaOptimizada: OptimizedRoute?,
    val estadisticas: MobileStats?
)

data class MobilePackage(
    val id: String,
    val refColis: String,
    val coordenadas: Coordinates,
    val direccion: String,
    val estado: String,
    val prioridad: String,
    val tipoEntrega: String
)

data class Coordinates(
    val latitud: Double,
    val longitud: Double
)

data class OptimizedRoute(
    val coordenadasRuta: List<Coordinates>,
    val distanciaTotal: Double,
    val tiempoEstimado: Int
)

data class MobileStats(
    val totalPaquetes: Int,
    val paquetesEntregados: Int,
    val paquetesPendientes: Int
)

data class UpdatePackageStatusRequest(
    val packageId: String,
    val status: String,
    val notes: String?
)

data class UpdatePackageStatusResponse(
    val success: Boolean,
    val message: String?
)

data class MobileStatsResponse(
    val success: Boolean,
    val data: MobileStats?,
    val message: String?
)
