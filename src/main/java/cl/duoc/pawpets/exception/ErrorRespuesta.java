package cl.duoc.pawpets.exception;

import java.util.Map;

/**
 * Formato único de los errores de la API.
 * estado = código HTTP, error = nombre del código, mensaje = explicación,
 * campos = errores de validación por campo (vacío si no aplica).
 */
public record ErrorRespuesta(int estado, String error, String mensaje, Map<String, String> campos) {

    public static ErrorRespuesta de(org.springframework.http.HttpStatus status, String mensaje) {
        return new ErrorRespuesta(status.value(), status.getReasonPhrase(), mensaje, Map.of());
    }
}
