package cl.duoc.pawpets.exception;

/** La operación choca con el estado actual de los datos (correo repetido, sin stock...): 409 Conflict. */
// EXCEPCIÓN PROPIA. Extiende RuntimeException (no obliga a declarar throws). Se lanza en los servicios y
// GlobalExceptionHandler la convierte en una respuesta HTTP 409.
public class ConflictoException extends RuntimeException {
    // Constructor: recibe el texto que se mostrará al usuario.
    public ConflictoException(String mensaje) {
        // Pasa el mensaje a RuntimeException; luego se obtiene con e.getMessage().
        super(mensaje);
    }
}
