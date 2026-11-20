package cl.duoc.pawpets.exception;

/** La operación choca con el estado actual de los datos (correo repetido, sin stock...): 409 Conflict. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
