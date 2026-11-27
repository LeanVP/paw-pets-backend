package cl.duoc.pawpets.dto;

// Se importa el modelo para poder convertirlo a DTO.
import cl.duoc.pawpets.model.Usuario;

/** Usuario tal como se envía al frontend: SIN la clave. */
// DTO de SALIDA: el usuario tal como lo ve el frontend. Es igual a Usuario pero SIN el campo clave:
// así el hash de la contraseña jamás sale del servidor.
public record UsuarioResponse(
        Integer id, String nombre, String apellidos, String correo, String rol,
        String region, String comuna, String calle) {

    // Método fábrica: convierte un Usuario (modelo) en UsuarioResponse (DTO). Se usa como UsuarioResponse.de(usuario).
    public static UsuarioResponse de(Usuario u) {
        // Copia campo por campo, omitiendo a propósito getClave().
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getRol(), u.getRegion(), u.getComuna(), u.getCalle());
    }
}
