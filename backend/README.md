# 🦀 Backend - Delivery Routing API

Backend en Rust con Axum para el sistema de optimización de rutas de delivery.

## 📁 Estructura del Proyecto

```
backend/
├── src/                 # Código fuente principal
│   ├── api/            # Endpoints y controladores
│   ├── models/         # Estructuras de datos y modelos
│   ├── services/       # Lógica de negocio
│   └── main.rs        # Punto de entrada
├── tests/              # Tests unitarios e integración
├── tools/              # Scripts de análisis y desarrollo
└── Cargo.toml         # Configuración Rust
```

## 🚀 Características

- **API REST** con Axum framework
- **Integración Colis Privé** (API web)
- **Geocoding** con Mapbox
- **Base de datos** PostgreSQL
- **Sistema híbrido** de optimización de rutas

## 🔧 Uso

```bash
# Compilar
cargo build --release

# Ejecutar
cargo run

# Tests
cargo test
```

## 🌐 Endpoints Principales

- `POST /api/colis-prive/tournee` - Obtener rutas
- `POST /api/geocode` - Geocodificar direcciones
- `GET /api/health` - Estado del sistema

## 🔒 Configuración

Variables de entorno requeridas en `.env`:
- `DATABASE_URL` - URL PostgreSQL
- `MAPBOX_TOKEN` - Token de Mapbox
- Credenciales Colis Privé