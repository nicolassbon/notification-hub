# Notification Hub 🚀

Sistema de notificaciones multi-plataforma construido con Spring Boot que permite enviar mensajes a través de Telegram y Discord de manera unificada. Incluye autenticación JWT, limitación de tasa (rate limiting) y gestión completa de mensajes.

## 🌐 Demo en Vivo

La aplicación está desplegada en Render y disponible públicamente:

- **Swagger UI**: https://notification-hub-1940.onrender.com/swagger-ui/index.html
- **OpenAPI JSON**: https://notification-hub-1940.onrender.com/v3/api-docs

> ⚠️ **Nota**: El servicio gratuito de Render puede tardar ~1 minuto en responder la primera petición si ha estado inactivo (cold start).

## 📋 Descripción del Proyecto

Notification Hub es una API REST que centraliza el envío de notificaciones a múltiples plataformas (Telegram y Discord) con las siguientes características principales:

- **Autenticación JWT**: Sistema seguro de autenticación con tokens que expiran en 24 horas
- **Multi-plataforma**: Soporte para Telegram y Discord con posibilidad de extensión a otras plataformas
- **Rate Limiting**: Control de límite diario de mensajes por usuario (configurable, por defecto 100/día)
- **Gestión de Entregas**: Seguimiento detallado del estado de cada entrega (SUCCESS, PENDING, FAILED)
- **Roles de Usuario**: Sistema de roles (USER, ADMIN) con endpoints administrativos
- **Filtrado Avanzado**: Búsqueda de mensajes por estado, plataforma y rango de fechas
- **Documentación Swagger**: API completamente documentada con OpenAPI 3.0
- **Persistencia**: Base de datos MySQL/PostgreSQL con JPA/Hibernate
- **Testing**: Suite completa de tests unitarios e integración
- **Deployment**: Desplegado en Render con PostgreSQL

## 🛠️ Tecnologías

- **Java 21** - Lenguaje de programación
- **Spring Boot 3.5.7** - Framework principal
- **Spring Security** - Autenticación y autorización
- **Spring Data JPA** - Capa de persistencia
- **MySQL 8.0** (dev) / **PostgreSQL** (prod) - Base de datos
- **JWT (jsonwebtoken 0.12.5)** - Tokens de autenticación
- **MapStruct 1.5.5** - Mapeo de DTOs
- **Lombok** - Reducción de código boilerplate
- **SpringDoc OpenAPI 2.8.8** - Documentación Swagger
- **Docker & Docker Compose** - Containerización
- **H2 Database** - Base de datos en memoria para tests
- **JUnit 5 & Mockito** - Testing
- **Render** - Plataforma de deployment

## 📦 Requisitos Previos

- **Java 21** o superior
- **Docker** y **Docker Compose** (para ejecutar con contenedores)
- **Maven 3.8+** (opcional, incluido mvnw)
- **MySQL 8.0** (si ejecutas sin Docker)

## 🚀 Instalación y Configuración para prueba local

### 1. Clonar el Repositorio

```bash
git clone https://github.com/nicolassbon/notification-hub.git
cd notification-hub
```

### 2. Configurar Variables de Entorno

Crea un archivo `.env` en la raíz del proyecto con las siguientes variables:

```env
# Base de Datos MySQL (desarrollo)
MYSQL_ROOT_PASSWORD=root
MYSQL_DATABASE=notification_hub
DB_USER=app_user
DB_PASS=app_password

# Base de Datos PostgreSQL (producción)
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=notification_hub
POSTGRES_USER=app_user
POSTGRES_PASSWORD=app_password

# JWT
JWT_SECRET=tu-secreto-seguro-de-al-menos-256-bits-para-jwt

# Telegram
TELEGRAM_BOT_TOKEN=tu_bot_token_aqui
TELEGRAM_CHAT_ID=tu_chat_id_por_defecto

# Discord
DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/tu_webhook_url

# Perfil de Spring
SPRING_ACTIVE_PROFILE=dev
```

### 3. Ejecutar con Docker (Recomendado)

```bash
# Construir y ejecutar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f app

# Detener servicios
docker-compose down
```

La aplicación estará disponible en `http://localhost:8080`

### 4. Ejecutar en Desarrollo (Sin Docker)

```bash
# 1. Iniciar solo MySQL con Docker
docker-compose up -d mysql

# 2. Ejecutar la aplicación con Maven
./mvnw spring-boot:run

# En Windows
mvnw.cmd spring-boot:run
```

### 5. Ejecutar Tests

```bash
# Ejecutar todos los tests
./mvnw test

# Ejecutar tests con reporte de cobertura
./mvnw verify
```

## 🌍 Variables de Entorno

### Base de Datos

| Variable | Descripción | Requerido | Ejemplo |
|----------|-------------|-----------|---------|
| `MYSQL_ROOT_PASSWORD` | Contraseña root de MySQL | Sí (dev) | `root` |
| `MYSQL_DATABASE` | Nombre de la base de datos MySQL | Sí (dev) | `notification_hub` |
| `DB_USER` | Usuario de la base de datos | Sí | `app_user` |
| `DB_PASS` | Contraseña de la base de datos | Sí | `app_password` |
| `POSTGRES_HOST` | Host de PostgreSQL | Sí (prod) | `localhost` |
| `POSTGRES_PORT` | Puerto de PostgreSQL | Sí (prod) | `5432` |
| `POSTGRES_DB` | Nombre de BD PostgreSQL | Sí (prod) | `notification_hub` |
| `POSTGRES_USER` | Usuario PostgreSQL | Sí (prod) | `app_user` |
| `POSTGRES_PASSWORD` | Contraseña PostgreSQL | Sí (prod) | `app_password` |

### Seguridad

| Variable | Descripción | Requerido | Ejemplo |
|----------|-------------|-----------|---------|
| `JWT_SECRET` | Clave secreta para firmar JWT (mínimo 256 bits) | Sí | `mi-secreto-super-seguro-de-256-bits` |

### Plataformas de Notificación

| Variable | Descripción | Requerido | Ejemplo |
|----------|-------------|-----------|---------|
| `TELEGRAM_BOT_TOKEN` | Token del bot de Telegram (obtener de @BotFather) | Sí | `123456:ABC-DEF1234ghIkl...` |
| `TELEGRAM_CHAT_ID` | ID del chat/canal por defecto de Telegram | Opcional | `-1001234567890` |
| `DISCORD_WEBHOOK_URL` | URL del webhook de Discord | Sí | `https://discord.com/api/webhooks/...` |

### Aplicación

| Variable | Descripción | Requerido | Valores | Default |
|----------|-------------|-----------|---------|---------|
| `SPRING_ACTIVE_PROFILE` | Perfil de Spring Boot activo | Sí | `dev`, `prod` | `dev` |

## 📚 Documentación de la API

### Swagger UI

**Producción (Render):**
- **Swagger UI**: https://notification-hub-1940.onrender.com/swagger-ui/index.html
- **OpenAPI JSON**: https://notification-hub-1940.onrender.com/v3/api-docs

**Local:**
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Endpoints Disponibles

#### 🔐 Autenticación (`/api/auth`)

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|---------------|
| `POST` | `/api/auth/register` | Registrar nuevo usuario | No |
| `POST` | `/api/auth/login` | Iniciar sesión | No |

#### 💬 Mensajes (`/api/messages`)

| Método | Endpoint | Descripción | Autenticación | Rol |
|--------|----------|-------------|---------------|-----|
| `POST` | `/api/messages/send` | Enviar mensaje multi-plataforma | Sí | USER |
| `GET` | `/api/messages` | Obtener mis mensajes con filtros | Sí | USER |

#### 👑 Administración (`/api/admin`)

| Método | Endpoint | Descripción | Autenticación | Rol |
|--------|----------|-------------|---------------|-----|
| `GET` | `/api/admin/messages` | Ver todos los mensajes del sistema | Sí | ADMIN |
| `GET` | `/api/admin/metrics` | Ver métricas de todos los usuarios | Sí | ADMIN |

## 🏗️ Estructura del Proyecto

```
notification-hub/
├── src/
│   ├── main/
│   │   ├── java/com/notificationhub/
│   │   │   ├── config/              # Configuraciones de Spring
│   │   │   ├── controller/          # Controladores REST
│   │   │   ├── dto/                 # Data Transfer Objects
│   │   │   │   ├── request/         # DTOs de petición
│   │   │   │   ├── response/        # DTOs de respuesta
│   │   │   │   └── criteria/        # DTOs de filtrado
│   │   │   ├── entity/              # Entidades JPA
│   │   │   ├── enums/               # Enumeraciones
│   │   │   ├── exception/           # Excepciones personalizadas
│   │   │   ├── mapper/              # Mappers MapStruct
│   │   │   ├── repository/          # Repositorios JPA
│   │   │   ├── security/            # Configuración de seguridad
│   │   │   ├── service/             # Lógica de negocio
│   │   │   │   └── platform/        # Servicios por plataforma
│   │   │   │       ├── discord/     # Implementación Discord
│   │   │   │       └── telegram/    # Implementación Telegram
│   │   │   └── utils/               # Utilidades (JWT, etc.)
│   │   └── resources/
│   │       ├── application.yml      # Configuración principal
│   │       ├── application-dev.yml  # Configuración desarrollo
│   │       └── application-prod.yml # Configuración producción
│   └── test/                        # Tests unitarios e integración
├── docker-compose.yml               # Configuración Docker Compose
├── Dockerfile                       # Imagen Docker de la app
├── pom.xml                          # Dependencias Maven
└── .env                             # Variables de entorno (no versionado)
```

## 🚀 Deployment en Render

La aplicación está configurada para desplegarse automáticamente en Render:

### Configuración en Render

1. **Build Command**: `./mvnw clean package -DskipTests`
2. **Start Command**: `java -jar target/notification-hub-0.0.1-SNAPSHOT.jar`
3. **Environment**: Java 21
4. **Base de datos**: PostgreSQL (managed database)

### Variables de Entorno en Render

Configurar las siguientes variables en el dashboard de Render:

- `SPRING_PROFILES_ACTIVE=prod`
- `POSTGRES_HOST` (auto-generado por Render)
- `POSTGRES_PORT` (auto-generado por Render)
- `POSTGRES_DB` (auto-generado por Render)
- `POSTGRES_USER` (auto-generado por Render)
- `POSTGRES_PASSWORD` (auto-generado por Render)
- `JWT_SECRET`
- `TELEGRAM_BOT_TOKEN`
- `TELEGRAM_CHAT_ID`
- `DISCORD_WEBHOOK_URL`

## 🔒 Seguridad

- **Autenticación JWT**: Endpoints que requieren token JWT
- **Rate Limiting**: Límite diario de mensajes por usuario
- **Validación**: Validación de entrada con Bean Validation
- **Roles**: Sistema de roles para control de acceso (USER, ADMIN)
- **Encriptación**: Contraseñas hasheadas
- **HTTPS**: Todas las peticiones en producción usan HTTPS

## 📊 Límites y Restricciones

- **Longitud máxima del mensaje**: 4000 caracteres
- **Rate limit por defecto**: 100 mensajes/día
- **Expiración del token JWT**: 24 horas (86400000 ms)
- **Longitud del username**: Máximo 50 caracteres
- **Cold start en Render**: ~30-60 segundos si el servicio está inactivo

## 🧪 Tests

El proyecto incluye una suite completa de tests:

```bash
# Ejecutar todos los tests
./mvnw test

# Tests específicos
./mvnw test -Dtest=AuthControllerTest
./mvnw test -Dtest=MessageServiceImplTest

# Con reporte de cobertura
./mvnw verify
```

**Cobertura de tests:**
- ✅ Controllers (AuthController, MessageController, AdminController)
- ✅ Services (AuthService, MessageService, RateLimitService, Platform Services)
- ✅ Repositories (UserRepository, MessageRepository, DailyMessageCountRepository)
- ✅ Utils (JwtUtils)

## 🐛 Manejo de Errores

La API devuelve respuestas de error consistentes:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Descripción del error",
  "timestamp": "2025-10-27T10:30:00",
  "path": "/api/messages/send",
  "details": [
    "content: no debe estar en blanco",
    "destinations: debe tener al menos un elemento"
  ]
}
```

## 🚧 Estado del Proyecto

✅ **Funcionalidades Completadas:**
- Sistema de autenticación JWT
- Envío a Telegram y Discord
- Rate limiting por usuario
- Filtrado avanzado de mensajes
- Panel administrativo
- Documentación Swagger completa
- Suite de tests completa
- **Deployment en producción (Render)**

## 👥 Contribuciones

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request
6. 
---

