# 📱 Android - Delivery Routing Mobile

Aplicación Android para visualización de rutas optimizadas de delivery.

## 📁 Estructura

```
app/src/main/
├── java/com/daniel/deliveryrouting/
│   ├── data/           # Repositorios y fuentes de datos
│   ├── domain/         # Lógica de negocio y casos de uso
│   ├── network/        # API clients y networking
│   ├── presentation/   # UI, ViewModels y pantallas
│   └── MainActivity.kt # Actividad principal
└── res/                # Recursos (layouts, strings, etc.)
```

## 🎯 Funcionalidades

- **Login** con credenciales Colis Privé
- **Mapa interactivo** con Mapbox
- **Visualización de rutas** optimizadas
- **Lista de paquetes** por entregar
- **Arquitectura MVVM** con Jetpack Compose

## 🛠️ Tecnologías

- **Kotlin** + Jetpack Compose
- **Retrofit** para API calls
- **Mapbox SDK** para mapas
- **Hilt** para inyección de dependencias
- **Coroutines** para programación asíncrona

## 🚀 Instalación

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## 🔧 Configuración

Configurar en `local.properties`:
```
mapbox.token=your_mapbox_token
api.base.url=http://your-backend-url
```