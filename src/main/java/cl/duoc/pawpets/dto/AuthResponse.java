package cl.duoc.pawpets.dto;

/** Respuesta del login, registro y renovación: el token JWT y los datos del usuario. */
public record AuthResponse(String token, UsuarioResponse usuario) {
}
