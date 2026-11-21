# PawPets – Backend (Spring Boot + MongoDB Atlas + JWT)

API REST de la tienda PawPets (DSY1104 – Evaluación Parcial 3). La consume el frontend
[paw-pets-front](https://github.com/LeanVP/paw-pets-front).

> Este proyecto es **Java + Spring Boot + Maven**. No usa `npm`: se ejecuta con el Maven Wrapper (`mvnw.cmd`).

## Requisitos

- JDK **17** (`java -version`)
- Un cluster de **MongoDB Atlas** (o MongoDB local) y su URI de conexión

## Ejecutar (Windows)

```powershell
copy .env.example .env        # completa MONGODB_URI y JWT_SECRET (32+ caracteres)
.\mvnw.cmd spring-boot:run
```

Cuando arranca aparece `Started Application`. La primera vez carga categorías, productos y usuarios de prueba
desde `src/main/resources/data/*.json`.

- API: <http://localhost:8080/api/v1>
- Swagger UI: <http://localhost:8080/swagger-ui.html> (botón **Authorize** para pegar el token)
- OpenAPI: <http://localhost:8080/v3/api-docs>

| Comando | Qué hace |
|---|---|
| `.\mvnw.cmd test` | 43 pruebas unitarias y de integración (JUnit 5 + Mockito + MockMvc, no necesitan MongoDB) |
| `.\mvnw.cmd clean install` | Compila, prueba y genera el JAR en `target/` |

## Configuración (`.env`, nunca se sube a GitHub)

| Variable | Ejemplo |
|---|---|
| `MONGODB_URI` | `mongodb+srv://usuario:clave@cluster0.xxxxx.mongodb.net/pawpets?retryWrites=true&w=majority` |
| `JWT_SECRET` | frase secreta de 32 caracteres o más |
| `CORS_ORIGIN` | `http://localhost:5173` |

`application.properties` lo lee con `spring.config.import=optional:file:.env[.properties]`.

## Estructura (`src/main/java/cl/duoc/pawpets`)

| Carpeta | Contenido |
|---|---|
| `model/` | Documentos de MongoDB: `Categoria`, `Producto`, `Usuario`, `Boleta`, `DetalleBoleta` (Figura 1) |
| `repository/` | `MongoRepository` de cada colección |
| `service/` | Lógica de negocio (compra, stock, precios, reglas de cada entidad, autenticación) |
| `controller/` | Endpoints REST `/api/v1/...` documentados con springdoc |
| `dto/` | Datos de entrada validados y respuestas sin la clave |
| `security/` | Spring Security + JWT (HS256), roles y BCrypt |
| `config/` | CORS, Swagger y carga inicial de datos |
| `exception/` | Manejo global de errores (`@RestControllerAdvice`) |

## Endpoints

| Método | Ruta | Acceso |
|---|---|---|
| POST | `/auth/login`, `/auth/registro` | Público |
| POST | `/auth/refresh` | Con token |
| GET | `/productos`, `/productos/{id}`, `/categorias`, `/categorias/{id}` | Público |
| POST, PUT, DELETE | `/productos[/{id}]`, `/categorias[/{id}]` | Admin |
| GET, POST, PUT, DELETE | `/usuarios[/{id}]` | Admin |
| GET | `/boletas`, `/boletas/{id}` | Admin y vendedor (todas), cliente (las suyas) |
| POST | `/boletas` | Cliente y admin |

## Cuentas de prueba

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@pawpets.cl` | `Admin123` |
| Vendedor | `vendedor@pawpets.cl` | `Vendedor123` |
| Cliente | `camila@correo.cl` | `Cliente123` |

Documentación completa (instalación, arquitectura, guía de defensa): carpeta [`docs/`](docs/).
