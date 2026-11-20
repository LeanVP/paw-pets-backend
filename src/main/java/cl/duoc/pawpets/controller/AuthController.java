package cl.duoc.pawpets.controller;

import cl.duoc.pawpets.dto.AuthResponse;
import cl.duoc.pawpets.dto.LoginRequest;
import cl.duoc.pawpets.dto.RegistroRequest;
import cl.duoc.pawpets.exception.ErrorRespuesta;
import cl.duoc.pawpets.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de autenticación: login, registro y renovación del JWT. */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, registro de clientes y renovación del token JWT")
public class AuthController {

    private final AuthService servicio;

    public AuthController(AuthService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Iniciar sesión", description = "Valida correo y contraseña. Devuelve un JWT y los datos del usuario (sin la clave).")
    @ApiResponse(responseCode = "200", description = "Credenciales correctas")
    @ApiResponse(responseCode = "400", description = "Faltan datos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest datos) {
        return servicio.login(datos);
    }

    @Operation(summary = "Crear cuenta", description = "Registra un usuario nuevo SIEMPRE con rol cliente y deja la sesión iniciada.")
    @ApiResponse(responseCode = "201", description = "Cuenta creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "El correo ya está registrado", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registro(@Valid @RequestBody RegistroRequest datos) {
        return servicio.registrar(datos);
    }

    @Operation(summary = "Renovar token", description = "Con un token todavía válido entrega uno nuevo (nuevo vencimiento) y los datos actuales del usuario.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Token renovado")
    @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping("/refresh")
    public AuthResponse refresh(@AuthenticationPrincipal Jwt jwt) {
        return servicio.refrescar(Integer.valueOf(jwt.getSubject()));
    }
}
