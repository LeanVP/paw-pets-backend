package cl.duoc.pawpets.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Datos del formulario de inicio de sesión. */
// DTO de ENTRADA para POST /api/v1/auth/login: lo que escribe el usuario en el formulario de login.
public record LoginRequest(
        // Ejemplo que Swagger muestra y precarga en "Try it out".
        // Correo obligatorio y con formato válido.
        @Schema(example = "admin@pawpets.cl")
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.") String correo,
        // Contraseña en texto plano SOLO mientras viaja en la petición; AuthService la compara contra el hash BCrypt.
        @Schema(example = "Admin123")
        @NotBlank(message = "La contraseña es obligatoria.") String clave) {
}
