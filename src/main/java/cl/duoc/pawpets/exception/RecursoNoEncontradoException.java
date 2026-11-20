package cl.duoc.pawpets.exception;

/** El recurso pedido no existe: se responde 404 Not Found. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
