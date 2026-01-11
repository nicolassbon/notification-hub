# Notification Hub 🚀

Sistema de notificaciones multi-plataforma construido con Spring Boot que permite enviar mensajes a través de Telegram y Discord de manera unificada. Incluye autenticación JWT, limitación de tasa (rate limiting) y gestión completa de mensajes.

> 📝 **Nota**: Este proyecto fue desarrollado como parte de una prueba técnica. Estuvo desplegado temporalmente en Render con PostgreSQL para demostrar capacidades de deployment en producción. Actualmente el deployment ha expirado, pero el proyecto puede ejecutarse localmente siguiendo las instrucciones más abajo.

## 📋 Descripción del Proyecto

Notification Hub es una API REST que centraliza el envío de notificaciones a múltiples plataformas (Telegram y Discord) con las siguientes características principales:

- **Autenticación JWT**: Sistema seguro de autenticación con tokens que expiran en 24 horas
- **Multi-plataforma**: Soporte para Telegram y Discord con posibilidad de extensión a otras plataformas
- **Rate Limiting**: Control de límite diario de mensajes por usuario (configurable, por defecto 100/día) con protección contra race conditions
- **Gestión de Entregas**: Seguimiento detallado del estado de cada entrega (SUCCESS, PENDING, FAILED) con validación estricta - mensajes solo se guardan si al menos una entrega es exitosa
- **Roles de Usuario**: Sistema de roles (USER, ADMIN) con endpoints administrativos
- **Filtrado Avanzado**: Búsqueda de mensajes por estado, plataforma y rango de fechas
- **Paginación**: Soporte completo de paginación en endpoints de consulta con parámetros configurables
- **Optimización N+1**: Solución al problema N+1 query mediante `JOIN FETCH` para carga eficiente de relaciones
- **Documentación Swagger**: API completamente documentada con OpenAPI 3.0
- **Persistencia**: Base de datos MySQL/PostgreSQL con JPA/Hibernate
- **Testing**: Suite completa de tests unitarios e integración
- **Deployment Ready**: Preparado para deployment con Docker y configuración para Render

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
- **Render** - Plataforma de deployment (configurado)

## 📦 Requisitos Previos

- **Java 21** o superior
- **Docker** y **Docker Compose** (para ejecutar con contenedores)
- **Maven 3.8+** (opcional, incluido mvnw)
- **MySQL 8.0** (si ejecutas sin Docker)

## 🚀 Instalación y Configuración Local

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

## 🌐 Variables de Entorno

### Base de Datos

| Variable              | Descripción                      | Requerido | Ejemplo            |
| --------------------- | -------------------------------- | --------- | ------------------ |
| `MYSQL_ROOT_PASSWORD` | Contraseña root de MySQL         | Sí (dev)  | `root`             |
| `MYSQL_DATABASE`      | Nombre de la base de datos MySQL | Sí (dev)  | `notification_hub` |
| `DB_USER`             | Usuario de la base de datos      | Sí        | `app_user`         |
| `DB_PASS`             | Contraseña de la base de datos   | Sí        | `app_password`     |
| `POSTGRES_HOST`       | Host de PostgreSQL               | Sí (prod) | `localhost`        |
| `POSTGRES_PORT`       | Puerto de PostgreSQL             | Sí (prod) | `5432`             |
| `POSTGRES_DB`         | Nombre de BD PostgreSQL          | Sí (prod) | `notification_hub` |
| `POSTGRES_USER`       | Usuario PostgreSQL               | Sí (prod) | `app_user`         |
| `POSTGRES_PASSWORD`   | Contraseña PostgreSQL            | Sí (prod) | `app_password`     |

### Seguridad

| Variable     | Descripción                                     | Requerido | Ejemplo                               |
| ------------ | ----------------------------------------------- | --------- | ------------------------------------- |
| `JWT_SECRET` | Clave secreta para firmar JWT (mínimo 256 bits) | Sí        | `mi-secreto-super-seguro-de-256-bits` |

### Plataformas de Notificación

| Variable              | Descripción                                       | Requerido | Ejemplo                                |
| --------------------- | ------------------------------------------------- | --------- | -------------------------------------- |
| `TELEGRAM_BOT_TOKEN`  | Token del bot de Telegram (obtener de @BotFather) | Sí        | `123456:ABC-DEF1234ghIkl...`           |
| `TELEGRAM_CHAT_ID`    | ID del chat/canal por defecto de Telegram         | Opcional  | `-1001234567890`                       |
| `DISCORD_WEBHOOK_URL` | URL del webhook de Discord                        | Sí        | `https://discord.com/api/webhooks/...` |

### Aplicación

| Variable                | Descripción                  | Requerido | Valores       | Default |
| ----------------------- | ---------------------------- | --------- | ------------- | ------- |
| `SPRING_ACTIVE_PROFILE` | Perfil de Spring Boot activo | Sí        | `dev`, `prod` | `dev`   |

### Administrador

| Variable        | Descripción                     | Requerido | Ejemplo          |
| --------------- | ------------------------------- | --------- | ---------------- |
| `ADMIN_USERNAME`| Nombre de usuario administrador  | Sí        | `admin`          |
| `ADMIN_PASSWORD`| Contraseña del administrador     | Sí        | `securepassword` |

## 📚 Documentación de la API

### Swagger UI (Ejecución Local)

Una vez ejecutada la aplicación localmente, la documentación Swagger está disponible en:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

### Endpoints Disponibles

#### 🔐 Autenticación (`/api/auth`)

| Método | Endpoint             | Descripción             | Autenticación |
| ------ | -------------------- | ----------------------- | ------------- |
| `POST` | `/api/auth/register` | Registrar nuevo usuario | No            |
| `POST` | `/api/auth/login`    | Iniciar sesión          | No            |

#### 💬 Mensajes (`/api/messages`)

| Método | Endpoint             | Descripción                                     | Autenticación | Rol  |
| ------ | -------------------- | ----------------------------------------------- | ------------- | ---- |
| `POST` | `/api/messages/send` | Enviar mensaje multi-plataforma                 | Sí            | USER |
| `GET`  | `/api/messages`      | Obtener mis mensajes con filtros y paginación  | Sí            | USER |

**Parámetros de Paginación (GET /api/messages):**
- `page`: Número de página (0-indexed, default: 0)
- `size`: Elementos por página (default: 20, máximo recomendado: 100)

**Parámetros de Filtro (GET /api/messages):**
- `status`: Estado de entrega (SUCCESS, FAILED, PENDING)
- `platform`: Plataforma (TELEGRAM, DISCORD)
- `from`: Fecha desde (ISO 8601, ej: 2025-01-01T00:00:00)
- `to`: Fecha hasta (ISO 8601, ej: 2025-12-31T23:59:59)

#### 🛡️ Administración (`/api/admin`)

| Método | Endpoint              | Descripción                                       | Autenticación | Rol   |
| ------ | --------------------- | ---------------------------------------------- | ------------- | ----- |
| `GET`  | `/api/admin/messages` | Ver todos los mensajes del sistema con paginación | Sí            | ADMIN |
| `GET`  | `/api/admin/metrics`  | Ver métricas de todos los usuarios                | Sí            | ADMIN |

**Parámetros de Paginación (GET /api/admin/messages):**
- `page`: Número de página (0-indexed, default: 0)
- `size`: Elementos por página (default: 20, máximo recomendado: 100)

## 🗂️ Estructura del Proyecto

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

La aplicación está configurada para desplegarse en Render. Para replicar el deployment:

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
- **Encriptación**: Contraseñas hasheadas con BCrypt
- **HTTPS**: Configurado para usar HTTPS en producción
- **Configuración Segura de Admin**: Credenciales de administrador configuradas vía propiedades de aplicación en lugar de variables de entorno directas

## ⚡ Optimizaciones de Rendimiento

### Solución al Problema N+1 Query

El proyecto implementa `JOIN FETCH` en consultas JPA para evitar el problema N+1 al cargar relaciones:

**Problema:** Sin optimización, al cargar 100 mensajes con sus entregas (deliveries), se ejecutaban **101 queries** (1 para mensajes + 100 para cada delivery).

**Solución implementada:**

- Uso de consultas JPQL personalizadas con cláusula `JOIN FETCH`
- Carga eager optimizada de las relaciones entre mensajes y entregas
- Ordenamiento directo en la consulta para evitar queries adicionales

**Resultado:** Ahora se ejecuta **1 sola query** con `LEFT JOIN`, mejorando el rendimiento significativamente.

### Protección contra Race Conditions en Rate Limiting

Implementación de bloqueo pesimista y operaciones atómicas para evitar condiciones de carrera en el contador de mensajes diarios:

**Problema:** En escenarios concurrentes (múltiples requests simultáneos), el contador podría incrementarse incorrectamente permitiendo superar el límite diario.

**Solución implementada:**

- **Bloqueo pesimista (PESSIMISTIC_WRITE):** Garantiza que solo un thread pueda leer y modificar el contador a la vez
- **Operaciones atómicas:** Actualización del contador mediante queries nativas que incrementan el valor directamente en la base de datos
- **Transacciones aisladas:** Uso de anotaciones transaccionales para asegurar la consistencia de datos
- **Validación doble:** Verificación del límite antes y después de incrementar el contador

**Beneficios:**

- ✅ Garantiza consistencia del contador en ambiente multi-thread
- ✅ Previene que usuarios excedan su límite diario en requests concurrentes
- ✅ Transacciones ACID completas

### Refactorización de Arquitectura de Repositorios

Reorganización de la lógica de consultas para mejorar el rendimiento y la mantenibilidad:

**Problema:** Consultas de filtrado complejas dispersas en múltiples métodos, potencialmente causando N+1 queries y código duplicado.

**Solución implementada:**

- **Centralización de Filtrado:** Lógica de filtrado avanzado (por estado, plataforma, fechas) movida a `MessageDeliveryRepository` usando un método único con criterios
- **Eliminación de Métodos Redundantes:** Removidos métodos obsoletos en `MessageRepository` que duplicaban funcionalidad
- **JOIN FETCH Optimizado:** Consultas con `LEFT JOIN FETCH` para cargar relaciones en una sola query, evitando problemas N+1
- **Arquitectura Más Segura:** Consultas parametrizadas que previenen inyección SQL

**Beneficios:**

- ✅ Mejora significativa en rendimiento de consultas con filtros
- ✅ Reducción de código duplicado y mantenimiento simplificado
- ✅ Prevención de vulnerabilidades de inyección SQL
- ✅ Suite de tests completa para validar el comportamiento

**Testing:** Arquitectura refactorizada cubierta por tests unitarios exhaustivos que validan filtrado, ordenamiento y carga eficiente de relaciones.

### Validación Estricta de Entregas de Mensajes

Implementación de lógica estricta para garantizar integridad en el envío de mensajes:

**Problema:** Anteriormente, mensajes fallidos se guardaban en la base de datos y consumían el límite diario del usuario, creando inconsistencias.

**Solución implementada:**

- **Validación Pre-Guardado:** Verificación de al menos una entrega exitosa antes de persistir el mensaje
- **Prevención de Desperdicio de Límites:** Rate limit solo se incrementa si hay entregas exitosas
- **Excepciones Específicas:** Lanza `MessageDeliveryException` con mensaje claro cuando todas las entregas fallan
- **Transacciones Atómicas:** Toda la operación (validación, guardado, incremento de contador) en una transacción

**Beneficios:**

- ✅ Integridad de datos: Solo mensajes con entregas exitosas se persisten
- ✅ Uso eficiente del rate limit: No se consumen slots por envíos fallidos
- ✅ Mejor experiencia de usuario: Errores claros cuando todas las plataformas fallan
- ✅ Prevención de spam accidental: Falla rápida sin efectos secundarios

**Testing:** Lógica de validación cubierta por tests unitarios que verifican excepciones, no guardado de mensajes fallidos y correcto manejo del rate limit.

### Paginación y Filtrado Optimizado

Implementación de paginación eficiente para manejar grandes volúmenes de datos:

**Características:**
- **Paginación Spring Data JPA**: Uso de `Pageable` con `Page<T>` para consultas eficientes
- **Ordenamiento Consistente**: Resultados ordenados por fecha de creación descendente
- **Metadatos Completos**: Respuestas incluyen total de elementos, páginas disponibles, tamaño actual, etc.
- **Filtros Combinables**: Combinación de filtros (estado, plataforma, fechas) con paginación
- **Límites de Rendimiento**: Tamaño de página por defecto 20, máximo recomendado 100

**Beneficios:**
- ✅ Reducción de carga de memoria y tiempo de respuesta
- ✅ Navegación eficiente a través de grandes datasets
- ✅ API consistente con estándares de paginación REST
- ✅ Optimización automática de queries en base de datos

**Testing:** Paginación cubierta por tests exhaustivos que validan metadatos, navegación entre páginas, filtros combinados y límites de página.

## 📊 Límites y Restricciones

- **Longitud máxima del mensaje**: 4000 caracteres
- **Rate limit por defecto**: 100 mensajes/día
- **Expiración del token JWT**: 24 horas (86400000 ms)
- **Longitud del username**: Máximo 50 caracteres

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
- ✅ Repositories (UserRepository, MessageRepository, MessageDeliveryRepository, DailyMessageCountRepository)
- ✅ Utils (JwtUtils)

## 🛠️ Manejo de Errores

La API devuelve respuestas de error consistentes, incluyendo excepciones específicas para fallos en entregas de mensajes:

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Failed to deliver message to any platform",
  "timestamp": "2025-10-27T10:30:00",
  "details": []
}
```

**Excepciones Personalizadas:**
- `MessageDeliveryException`: Lanzada cuando todas las entregas de mensaje fallan
- `RateLimitExceededException`: Cuando se supera el límite diario de mensajes
- `InvalidCredentialsException`: Credenciales inválidas en autenticación

## 🚧 Estado del Proyecto

✅ **Funcionalidades Completadas:**

- Sistema de autenticación JWT
- Envío a Telegram y Discord
- Rate limiting por usuario con protección contra race conditions
- Filtrado avanzado de mensajes
- Panel administrativo
- Documentación Swagger completa
- Suite de tests completa
- Configuración para deployment en producción
- Optimización de queries (solución al problema N+1)
- Operaciones atómicas y bloqueos transaccionales
- **Resolución de Issues de Seguridad**: Configuración segura de credenciales de administrador
- **Refactorización de Arquitectura de Repositorios**: Consultas optimizadas y centralizadas para mejor rendimiento y seguridad
- **Validación Estricta de Entregas**: Mensajes solo se guardan si al menos una entrega es exitosa, previniendo desperdicio de rate limits
- **Paginación Completa**: Implementación de paginación en endpoints de consulta con metadatos completos (total elementos, páginas, etc.)

## 💡 Posibles Mejoras Futuras

- Integración con más plataformas (Slack, Microsoft Teams)
- Sistema de notificaciones programadas
- Dashboard web para administración
- Métricas en tiempo real
- WebSockets para notificaciones en vivo
