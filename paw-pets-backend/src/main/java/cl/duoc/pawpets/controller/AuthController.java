package cl.duoc.pawpets.controller;

// DTOs que entran y salen por los endpoints de autenticación.
import cl.duoc.pawpets.dto.AuthResponse;
import cl.duoc.pawpets.dto.LoginRequest;
import cl.duoc.pawpets.dto.RegistroRequest;
import cl.duoc.pawpets.exception.ErrorRespuesta;
// El controlador delega la lógica en el servicio (no contiene reglas de negocio).
import cl.duoc.pawpets.service.AuthService;
// Anotaciones de Swagger/OpenAPI: solo documentan, no cambian el comportamiento.
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
// @Valid: ordena a Spring validar el cuerpo con las anotaciones del DTO (@NotBlank, @Email...) antes de entrar al método.
import jakarta.validation.Valid;
// Códigos de estado HTTP (201 Created, etc.).
import org.springframework.http.HttpStatus;
// @AuthenticationPrincipal: inyecta en el parámetro al usuario autenticado de la petición actual (aquí, su JWT).
import org.springframework.security.core.annotation.AuthenticationPrincipal;
// El token ya verificado, del que se leen sub (id) y rol.
import org.springframework.security.oauth2.jwt.Jwt;
// @PostMapping: el método atiende peticiones HTTP POST.
import org.springframework.web.bind.annotation.PostMapping;
// @RequestBody: convierte el JSON del cuerpo de la petición en un objeto Java.
import org.springframework.web.bind.annotation.RequestBody;
// @RequestMapping: ruta base común a todos los métodos de la clase.
import org.springframework.web.bind.annotation.RequestMapping;
// @ResponseStatus: fija el código HTTP de la respuesta exitosa.
import org.springframework.web.bind.annotation.ResponseStatus;
// @RestController: clase que responde JSON directamente (=@Controller + @ResponseBody).
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de autenticación: login, registro y renovación del JWT. */
// CAPA CONTROLLER: recibe peticiones HTTP y devuelve JSON. Spring serializa automáticamente el objeto retornado a JSON.
// Todas las rutas de esta clase empiezan con /api/v1/auth.
// Agrupa estos endpoints bajo "Autenticación" en Swagger.
// Endpoints públicos de login y registro, y refresh (con token).
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, registro de clientes y renovación del token JWT")
public class AuthController {

    // Servicio inyectado por constructor.
    private final AuthService servicio;

    // Spring entrega el AuthService (inyección de dependencias).
    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    // Documentación Swagger de POST /login (resumen, descripción y posibles respuestas 200/400/401).
    // Endpoint: POST /api/v1/auth/login.
    // @RequestBody convierte el JSON {correo, clave} en LoginRequest; @Valid lo valida (si falla, 400 sin llegar al servicio).
    @Operation(summary = "Iniciar sesión", description = "Valida correo y contraseña. Devuelve un JWT y los datos del usuario (sin la clave).")
    @ApiResponse(responseCode = "200", description = "Credenciales correctas")
    @ApiResponse(responseCode = "400", description = "Faltan datos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest datos) {
        // Delega al servicio; el AuthResponse devuelto se convierte a JSON { token, usuario } con código 200.
        return servicio.login(datos);
    }

    // Documentación Swagger de POST /registro.
    // Endpoint: POST /api/v1/auth/registro.
    // Responde 201 Created (en vez del 200 por defecto) porque se crea un recurso.
    // Recibe el formulario de registro ya validado.
    @Operation(summary = "Crear cuenta", description = "Registra un usuario nuevo SIEMPRE con rol cliente y deja la sesión iniciada.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "El correo ya está registrado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registro(@Valid @RequestBody RegistroRequest datos) {
        // El servicio fuerza rol cliente y devuelve token + usuario (queda logueado).
        return servicio.registrar(datos);
    }

    // Documentación Swagger de POST /refresh.
    // En Swagger marca este endpoint como protegido (candado): requiere pegar el token en "Authorize".
    // Endpoint: POST /api/v1/auth/refresh (exige token válido según SecurityConfig).
    // Spring inyecta el JWT ya verificado: no hace falta mandar el id, sale del token firmado.
    @Operation(summary = "Renovar token", description = "Con un token todavía válido entrega uno nuevo (nuevo vencimiento) y los datos actuales del usuario.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Token renovado")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping("/refresh")
    public AuthResponse refresh(@AuthenticationPrincipal Jwt jwt) {
        // getSubject() = claim "sub" = id del usuario (texto) → se convierte a Integer y se pide un token nuevo.
        return servicio.refrescar(Integer.valueOf(jwt.getSubject()));
    }
}
