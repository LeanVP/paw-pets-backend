package cl.duoc.pawpets.exception;

/** El recurso pedido no existe: se responde 404 Not Found. */
// Excepción de negocio → HTTP 404. Se lanza cuando un findById no encuentra el registro.
public class RecursoNoEncontradoException extends RuntimeException {
    // Recibe el mensaje para el usuario.
    public RecursoNoEncontradoException(String mensaje) {
        // Guarda el mensaje en la excepción base.
        super(mensaje);
    }
}
