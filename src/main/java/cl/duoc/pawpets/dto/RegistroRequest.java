package cl.duoc.pawpets.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos del formulario "Crear cuenta". No incluye rol: el registro siempre crea un cliente. */
public record RegistroRequest(
        @NotBlank(message = "El nombre es obligatorio.") String nombre,
        @NotBlank(message = "Los apellidos son obligatorios.") String apellidos,
        @NotBlank(message = "El correo es obligatorio.") @Email(message = "El correo no es válido.") String correo,
        @NotBlank(message = "La contraseña es obligatoria.")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres.") String clave,
        @NotBlank(message = "La calle es obligatoria.") String calle,
        @NotBlank(message = "La región es obligatoria.") String region,
        @NotBlank(message = "La comuna es obligatoria.") String comuna) {
}
