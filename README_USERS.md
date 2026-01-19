# Users API - Microservicio de Usuarios

Microservicio reactivo para la gestión de usuarios con roles (Admin y Persona) utilizando Spring WebFlux y arquitectura hexagonal.

## 🏗️ Arquitectura

Este proyecto sigue los principios de **Clean Architecture** y **Arquitectura Hexagonal**:

```
domain/
  ├── api/           # Puertos de entrada (UserServicePort)
  ├── spi/           # Puertos de salida (UserPersistencePort)
  ├── model/         # Entidades de dominio (User)
  ├── usecase/       # Casos de uso (UserUseCase)
  ├── enums/         # Enumeraciones (TechnicalMessage)
  └── exceptions/    # Excepciones de negocio

application/
  └── config/        # Configuración de beans (UseCasesConfig)

infrastructure/
  ├── adapters/
  │   └── persistenceadapter/  # Adaptador de persistencia R2DBC
  └── entrypoints/
      ├── handler/   # Handlers reactivos (UserHandlerImpl)
      ├── dto/       # DTOs de entrada/salida
      ├── mapper/    # Mappers (UserMapper)
      └── util/      # Utilidades
```

## 🚀 Tecnologías

- **Java 21**
- **Spring Boot 3.3.6**
- **Spring WebFlux** (Programación Reactiva)
- **Spring Data R2DBC** (Base de datos reactiva)
- **PostgreSQL** con driver R2DBC
- **MapStruct** (Mapeo de objetos)
- **Lombok** (Reducción de boilerplate)
- **Resilience4j** (Circuit breaker, retry, bulkhead)
- **Micrometer** (Métricas y observabilidad)
- **Gradle** (Gestión de dependencias)

## 📦 Modelo de Datos

### User
```java
{
  "id": Long,
  "name": String,       // max 100 caracteres
  "email": String,      // max 150 caracteres, único
  "isAdmin": Boolean    // true = Admin, false = Persona
}
```

### Base de Datos
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    is_admin BOOLEAN NOT NULL DEFAULT FALSE
);
```

## 🔌 Endpoints

### 1. Crear Usuario
```http
POST /users
Content-Type: application/json
x-message-id: {uuid}

Request Body:
{
  "name": "John Doe",
  "email": "john.doe@example.com",
  "isAdmin": false
}

Response: 201 Created
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "isAdmin": false
}
```

### 2. Obtener Usuario por ID
```http
GET /users/{id}
x-message-id: {uuid}

Response: 200 OK
{
  "id": 1,
  "name": "John Doe",
  "email": "john.doe@example.com",
  "isAdmin": false
}
```

### 3. Verificar Existencia de Usuarios
```http
POST /users/check-exists
Content-Type: application/json
x-message-id: {uuid}

Request Body:
{
  "ids": [1, 2, 3, 999]
}

Response: 200 OK
{
  "1": true,
  "2": true,
  "3": true,
  "999": false
}
```

### 4. Obtener Usuarios por IDs
```http
POST /users/by-ids
Content-Type: application/json
x-message-id: {uuid}

Request Body:
{
  "ids": [1, 2, 3]
}

Response: 200 OK
[
  {
    "id": 1,
    "name": "John Doe",
    "email": "john.doe@example.com",
    "isAdmin": false
  },
  {
    "id": 2,
    "name": "Jane Admin",
    "email": "jane.admin@example.com",
    "isAdmin": true
  }
]
```

## ✅ Validaciones

El microservicio implementa las siguientes validaciones:

- **Nombre**: Requerido, máximo 100 caracteres
- **Email**: Requerido, formato válido, máximo 150 caracteres, único
- **isAdmin**: Requerido (true o false)
- **Duplicación**: No se permite crear usuarios con el mismo email

## 🎯 Principios SOLID Aplicados

### Single Responsibility Principle (SRP)
- Cada clase tiene una única responsabilidad
- `UserUseCase`: Lógica de negocio
- `UserPersistenceAdapter`: Acceso a datos
- `UserHandlerImpl`: Manejo de peticiones HTTP

### Open/Closed Principle (OCP)
- Interfaces (`UserServicePort`, `UserPersistencePort`) abiertas para extensión
- Implementaciones cerradas para modificación

### Liskov Substitution Principle (LSP)
- Las implementaciones pueden ser sustituidas por sus interfaces sin afectar el comportamiento

### Interface Segregation Principle (ISP)
- Interfaces específicas y cohesivas
- Cada puerto tiene métodos relacionados únicamente con su contexto

### Dependency Inversion Principle (DIP)
- Las capas dependen de abstracciones (puertos), no de implementaciones
- `UserUseCase` depende de `UserPersistencePort`, no de `UserPersistenceAdapter`

## 🧪 Buenas Prácticas

### Clean Code
- Nombres descriptivos y significativos
- Métodos pequeños y enfocados
- Comentarios solo cuando son necesarios
- Manejo explícito de errores

### Programación Reactiva
- Uso de `Mono` y `Flux` de Project Reactor
- Operadores reactivos (`flatMap`, `map`, `filter`, `switchIfEmpty`)
- Non-blocking I/O
- Backpressure handling

### Manejo de Errores
- Excepciones personalizadas (`BusinessException`, `TechnicalException`)
- Mensajes descriptivos y códigos HTTP apropiados
- Logs estructurados con nivel apropiado

## 🏃 Ejecución

### Requisitos
- JDK 21
- PostgreSQL (o usar Docker)
- Gradle

### Configuración
Actualiza `application.yaml` con tus credenciales de base de datos:
```yaml
spring:
  r2dbc:
    url: r2dbc:postgresql://localhost:5432/users_db
    username: your_user
    password: your_password
```

### Compilar y ejecutar
```bash
# Compilar
./gradlew clean build

# Ejecutar
./gradlew bootRun

# O ejecutar el JAR
java -jar build/libs/resilient-api-0.0.1-SNAPSHOT.jar
```

## 🔄 Integración con otros Microservicios

Este microservicio está diseñado para integrarse con:

### Bootcamp API
- **POST /users/by-ids**: Obtener información de usuarios inscritos en bootcamps
- **POST /users/check-exists**: Validar que los usuarios existen antes de inscribirlos

### Ejemplo de uso desde Bootcamp API:
```java
// Verificar que los usuarios existen antes de inscribirlos
webClient.post()
    .uri("http://users-api/users/check-exists")
    .bodyValue(Map.of("ids", List.of(1L, 2L, 3L)))
    .retrieve()
    .bodyToMono(new ParameterizedTypeReference<Map<Long, Boolean>>() {})
    .map(result -> result.values().stream().allMatch(exists -> exists));

// Obtener información de usuarios para reportes
webClient.post()
    .uri("http://users-api/users/by-ids")
    .bodyValue(Map.of("ids", userIds))
    .retrieve()
    .bodyToFlux(UserDTO.class)
    .collectList();
```

## 📊 Observabilidad

- **Actuator**: `/actuator/health`, `/actuator/metrics`
- **Prometheus**: `/actuator/prometheus`
- **Logs**: Structured logging con SLF4J y Logback
- **Tracing**: Micrometer con Brave

## 🔐 Seguridad

Para proyectos de producción, considera agregar:
- Spring Security con JWT
- Rate limiting
- CORS configuration
- Input sanitization
- SQL injection protection (ya incluido con R2DBC parameterized queries)

## 📝 Decisiones de Diseño

### ¿Por qué un campo booleano `isAdmin` en vez de una tabla de roles?
Para este proyecto específico que **no escalará**, un campo booleano es suficiente:
- ✅ **Simplicidad**: Solo dos roles (Admin y Persona)
- ✅ **Performance**: No requiere JOINs adicionales
- ✅ **Mantenibilidad**: Menos complejidad para un proyecto pequeño

Si el proyecto escalara, se recomienda:
```sql
CREATE TABLE roles (id BIGSERIAL PRIMARY KEY, name VARCHAR(50));
CREATE TABLE user_roles (user_id BIGINT, role_id BIGINT);
```

## 🤝 Contribución

Este proyecto sigue:
- Conventional Commits
- Clean Code principles
- SOLID principles
- Reactive programming best practices

---

**Autor**: Brayan Barco  
**Versión**: 1.0.0  
**Última actualización**: Enero 2026
