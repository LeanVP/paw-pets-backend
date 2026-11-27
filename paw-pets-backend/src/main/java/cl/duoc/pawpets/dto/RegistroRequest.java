package cl.duoc.pawpets.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
// @Size: restringe el largo de un texto (aquí, mínimo de la contraseña).
import jakarta.validation.constraints.Size;

/** Datos del formulario "Crear cuenta". No incluye rol: el registro siempre crea un cliente. */
// DTO de ENTRADA para el formulario "Crear cuenta" (POST /api/v1/auth/registro).
// No incluye rol a propósito: así nadie puede registrarse como admin mandando "rol":"admin" en el JSON.
public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio.") String nombre,
        @NotBlank(message = "Los apellidos son obligatorios.") String apellidos,
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.") String correo,
        // La contraseña debe tener al menos 6 caracteres.
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.") String clave,
        // Dirección obligatoria: se usa luego para precargar el checkout.
        @NotBlank(message = "La calle es obligatoria.") String calle,
        @NotBlank(message = "La región es obligatoria.") String region,
        @NotBlank(message = "La comuna es obligatoria.") String comuna) {
}
