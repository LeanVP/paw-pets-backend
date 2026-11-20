package cl.duoc.pawpets.security;

import cl.duoc.pawpets.model.Usuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Genera los tokens JWT firmados con HS256 (clave JWT_SECRET del .env).
 * Payload: sub = id del usuario, rol, correo, iat (emisión) y exp (vencimiento).
 * La verificación (firma + vencimiento) la hace el JwtDecoder de SecurityConfig en cada petición.
 */
@Service
public class JwtService {

    public static final String EMISOR = "paw-pets-backend";

    private final JwtEncoder encoder;
    private final long minutosDeVida;

    public JwtService(JwtEncoder encoder, @Value("${jwt.expiracion-minutos}") long minutosDeVida) {
        this.encoder = encoder;
        this.minutosDeVida = minutosDeVida;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .subject(String.valueOf(usuario.getId()))
                .issuedAt(ahora)
                .expiresAt(ahora.plus(minutosDeVida, ChronoUnit.MINUTES))
                .claim("rol", usuario.getRol())
                .claim("correo", usuario.getCorreo())
                .build();
        JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
