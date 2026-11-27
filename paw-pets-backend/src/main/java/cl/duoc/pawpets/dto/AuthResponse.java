package cl.duoc.pawpets.dto;

/** Respuesta del login, registro y renovación: el token JWT y los datos del usuario. */
// DTO (Data Transfer Object) de SALIDA. "record" = clase inmutable de Java 17 que ya trae constructor, getters, equals y toString.
// Es lo que recibe el frontend tras login/registro/refresh: { token, usuario }.
public record AuthResponse(String token, UsuarioResponse usuario) {
}
