package cl.duoc.pawpets.exception;

// Map: estructura clave→valor, aquí { campo → mensaje de error }.
import java.util.Map;

/**
 * Formato único de los errores de la API.
 * estado = código HTTP, error = nombre del código, mensaje = explicación,
 * campos = errores de validación por campo (vacío si no aplica).
 */
// DTO de salida con la forma ÚNICA de todos los errores de la API. El frontend (api.js) lee "mensaje" y "campos" sin importar qué falló.
public record ErrorRespuesta(int estado, String error, String mensaje, Map<String, String> campos) {

    // Método fábrica para errores simples (sin errores por campo).
    public static ErrorRespuesta de(org.springframework.http.HttpStatus status, String mensaje) {
        // status.value() = número (404); getReasonPhrase() = texto ("Not Found"); Map.of() = sin errores por campo.
        return new ErrorRespuesta(status.value(), status.getReasonPhrase(), mensaje, Map.of());
    }
}
