package cl.duoc.pawpets.dto;

import cl.duoc.pawpets.model.Usuario;

/** Usuario tal como se envía al frontend: SIN la clave. */
public record UsuarioResponse(
        Integer id, String nombre, String apellidos, String correo, String rol,
        String region, String comuna, String calle) {

    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNombre(), u.getApellidos(), u.getCorreo(),
                u.getRol(), u.getRegion(), u.getComuna(), u.getCalle());
    }
}
