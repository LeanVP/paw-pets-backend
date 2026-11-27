package cl.duoc.pawpets.security;

// Se necesita el usuario para escribir su id, rol y correo dentro del token.
import cl.duoc.pawpets.model.Usuario;
import java.time.Instant;
// ChronoUnit.MINUTES: unidad de tiempo para calcular el vencimiento.
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
// MacAlgorithm: algoritmos de firma con clave secreta compartida (HS256 = HMAC-SHA256).
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
// JwsHeader: la cabecera del JWT (indica el algoritmo de firma).
import org.springframework.security.oauth2.jwt.JwsHeader;
// JwtClaimsSet: el "payload" del JWT: los datos (claims) que viajan dentro del token.
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
// JwtEncoder: componente que firma y construye el texto final del token.
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
// @Service: marca la clase como componente de lógica; Spring la crea y la inyecta donde se pida.
import org.springframework.stereotype.Service;

/**
 * Genera los tokens JWT firmados con HS256 (clave JWT_SECRET del .env).
 * Payload: sub = id del usuario, rol, correo, iat (emisión) y exp (vencimiento).
 * La verificación (firma + vencimiento) la hace el JwtDecoder de SecurityConfig en cada petición.
 */
// Spring administra esta clase como bean (singleton).
// Servicio que FABRICA tokens JWT al iniciar sesión. La verificación ocurre en el JwtDecoder de SecurityConfig.
@Service
public class JwtService {

    // Nombre del emisor (claim "iss"). El decoder rechaza tokens cuyo emisor sea distinto.
    public static final String EMISOR = "paw-pets-backend";

    // Firmador de tokens (definido como @Bean en SecurityConfig con la clave JWT_SECRET).
    private final JwtEncoder encoder;
    // Duración del token (60 minutos según application.properties).
    private final long minutosDeVida;

    // Constructor con inyección: Spring entrega el encoder y el valor de jwt.expiracion-minutos.
    public JwtService(JwtEncoder encoder, @Value("${jwt.expiracion-minutos}") long minutosDeVida) {
        this.encoder = encoder;
        this.minutosDeVida = minutosDeVida;
    }

    // Recibe un usuario ya autenticado y devuelve el texto del token: tres partes base64 separadas por puntos (cabecera.payload.firma).
    public String generarToken(Usuario usuario) {
        // Momento de emisión: base para calcular el vencimiento.
        Instant ahora = Instant.now();
        // Construye el payload paso a paso (patrón Builder).
        JwtClaimsSet claims = JwtClaimsSet.builder()
                // "iss": quién emitió el token (este backend).
                .issuer(EMISOR)
                // "sub": a quién pertenece el token. Guarda el ID del usuario; así el backend sabe quién es sin consultar sesión.
                .subject(String.valueOf(usuario.getId()))
                // "iat": cuándo se emitió.
                .issuedAt(ahora)
                // "exp": cuándo vence (ahora + 60 min). Pasado ese momento, el decoder lo rechaza con 401.
                .expiresAt(ahora.plus(minutosDeVida, ChronoUnit.MINUTES))
                // Claim propio "rol": permite al backend aplicar permisos por rol sin ir a la base de datos.
                .claim("rol", usuario.getRol())
                // Claim propio "correo": informativo.
                .claim("correo", usuario.getCorreo())
                // Termina de armar el payload.
                .build();
        // Cabecera: indica que el token se firma con HS256.
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        // Firma payload+cabecera con la clave secreta y devuelve el token en texto. Si alguien altera el rol, la firma deja de coincidir.
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
