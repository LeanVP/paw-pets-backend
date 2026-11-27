# PawPets · Guía para defender el proyecto

## 1. Qué es (30 segundos)
Tienda online de mascotas con **arquitectura cliente-servidor desacoplada**:
- **Frontend**: React 19 + Vite (SPA) en `:5173`.
- **Backend**: API REST con Spring Boot 4 / Java 17 en `:8080`.
- **Base de datos**: MongoDB Atlas.
- Se comunican **solo por HTTP + JSON**. El frontend nunca toca la base de datos.

## 2. Arquetipo / patrones
| Patrón | Dónde |
|---|---|
| Cliente-servidor + API REST (versionada `/api/v1`) | todo el sistema |
| **Arquitectura por capas** controller → service → repository → model | backend |
| **DTO** (`*Request` / `*Response`) | `dto/` (ej: `UsuarioResponse` oculta la clave) |
| **Inyección de dependencias** por constructor | todo el backend |
| **Stateless + JWT** (sin sesión en el servidor) | `SecurityConfig`, `JwtService` |
| **Manejo central de errores** | `GlobalExceptionHandler` |
| **SPA + Context + Hooks + caché observable** | frontend (`AuthProvider`, `db.js`, `useColeccion`) |
| **Configuración dirigida por datos** | `entidades.jsx` → 2 pantallas sirven 3 entidades |

## 3. Flujo de datos (ejemplo: un cliente compra)
1. `Checkout.jsx` valida el formulario (`useFormulario`) y llama `db.crear('boletas', {...items:[{productoId,cantidad}]})`. **No envía precios.**
2. `db.js` → `api.post('/boletas')`.
3. `api.js` arma `fetch` con `Authorization: Bearer <JWT>` (token leído de `localStorage`).
4. Navegador hace *preflight* `OPTIONS`; `CorsConfig` autoriza `localhost:5173`.
5. `SecurityConfig` verifica firma HS256 + vencimiento y convierte `rol` en `ROLE_CLIENTE`; regla `POST /boletas → CLIENTE o ADMIN`. Sin token → 401, rol incorrecto → 403.
6. `BoletaController.crear`: `@Valid` valida el `BoletaRequest`; el id del cliente sale del **JWT**, no del cuerpo.
7. `BoletaService.crear`: busca cada producto, valida stock (409), calcula precios/subtotales/total, decide `Pagada`/`Rechazada`, descuenta stock **atómicamente** y compensa si algo falla.
8. `BoletaRepository.save` → inserta en MongoDB.
9. Respuesta `201` JSON → `db.js` actualiza la caché → React redibuja → navega a `/compra/exitosa/:id` o `/compra/error/:id`.
10. Cualquier error → `GlobalExceptionHandler` devuelve `{estado, error, mensaje, campos}` → `ErrorApi` → `AlertaError`.

**Login:** `Login.jsx` → `AuthProvider.iniciarSesion` → `POST /auth/login` → `AuthService` compara BCrypt → `JwtService` firma el token (sub=id, rol, exp=60 min) → se guarda en `localStorage` → el frontend lo reenvía en cada petición y lo renueva cada 15 min (`/auth/refresh`).

## 4. Endpoints
| Recurso | Ruta | Acceso |
|---|---|---|
| Auth | `POST /auth/login`, `/auth/registro`, `/auth/refresh` | público, público, con token |
| Productos / Categorías | `GET` | público |
| | `POST/PUT/DELETE` | solo ADMIN |
| Usuarios | todo `/usuarios` | solo ADMIN |
| Boletas | `POST` | CLIENTE, ADMIN |
| | `GET` | los 3 roles (cliente solo las suyas) |

Códigos: 200, 201 (creado), 204 (borrado), 400 (validación), 401 (sin sesión), 403 (sin permiso), 404, 409 (conflicto: correo repetido, sin stock, categoría con productos).

## 5. Notaciones / etiquetas (qué significan)
**Spring (Java)**
- `@SpringBootApplication` arranque + autoconfiguración · `@RestController` clase que responde JSON · `@RequestMapping/@GetMapping/@PostMapping/@PutMapping/@DeleteMapping` ruta + verbo.
- `@PathVariable` (valor de la URL) · `@RequestBody` (JSON → objeto) · `@Valid` (activa validaciones) · `@AuthenticationPrincipal` (usuario del JWT) · `@ResponseStatus` (código HTTP).
- `@Service` lógica · `@Component`/`@Configuration`/`@Bean` objetos administrados por Spring · `@Value` lee propiedades.
- `@Document` (clase = colección Mongo) · `@Id` (clave) · `MongoRepository` (CRUD gratis; `findByXxx` se deduce del nombre).
- `@NotBlank @NotNull @Min @Email @Size @Pattern` validaciones · `@RestControllerAdvice` + `@ExceptionHandler` errores centralizados.
- Lombok `@Getter/@Setter` genera getters/setters · Swagger `@Tag @Operation @ApiResponse @Schema` solo documentan.
- `record` (Java 17): clase inmutable compacta, usada para DTOs.

**React (JS)**
- JSX = HTML dentro de JS · `props` datos del padre · `useState` estado · `useEffect` efectos · `useCallback` función estable · `useContext` leer contexto · `useSyncExternalStore` leer `db.js`.
- `Context/Provider` comparte sesión y carrito sin pasar props · `<Routes>/<Route>/<Outlet>/<Navigate>` navegación · `async/await` + `fetch` peticiones.
- `{cond && <X/>}` render condicional · `.map()` listas (con `key`) · `...spread` copiar objetos sin mutar.

**Configuración**: `pom.xml` (Maven, dependencias) · `application.properties` (puerto, Mongo, JWT, CORS) · `.env` (secretos: `MONGODB_URI`, `JWT_SECRET`, `CORS_ORIGIN`, `VITE_API_URL`).

## 6. Decisiones de seguridad que conviene destacar
1. **El servidor calcula precios y total**; el navegador solo manda ids y cantidades.
2. Contraseñas con **BCrypt** (hash con sal); `UsuarioResponse` nunca envía la clave.
3. Registro público **siempre rol cliente**.
4. **JWT firmado HS256** con vencimiento; el rol sale del token firmado, no editable desde `localStorage`.
5. Mismo mensaje de error para correo inexistente y clave mala (no revela qué cuentas existen).
6. Cliente que pide boleta ajena recibe **404**, no 403.
7. **Descuento de stock atómico** (`$inc` condicionado) + **contador de ids atómico** → sin condiciones de carrera.
8. Seguridad real en el backend; el frontend (`RutaAdmin`) solo mejora la experiencia.

## 7. Preguntas típicas
- **¿Por qué DTO y no el modelo?** Para no exponer la clave y controlar qué datos entran.
- **¿Por qué stateless?** Escala sin memoria de sesión; cada petición trae su JWT.
- **¿Para qué CORS?** El navegador bloquea llamadas entre `:5173` y `:8080` salvo que el backend las autorice.
- **¿Por qué `db.js`?** Caché central: evita pedir lo mismo varias veces y notifica a React al cambiar.
- **¿Cómo se evita vender sin stock?** Validación previa + descuento atómico + compensación si falla.
