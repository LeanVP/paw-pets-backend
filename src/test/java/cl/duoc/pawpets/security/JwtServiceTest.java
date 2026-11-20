package cl.duoc.pawpets.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cl.duoc.pawpets.model.Usuario;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.crypto.SecretKey;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/** Prueba la generación y verificación real de tokens con los beans de SecurityConfig. */
class JwtServiceTest {

    static final String SECRETO = "clave-de-pruebas-de-al-menos-32-caracteres-123";
    final SecurityConfig config = new SecurityConfig();

    @Test
    @DisplayName("El token contiene id, rol y correo, y se verifica con la misma clave")
    void generaYVerifica() {
        SecretKey clave = config.claveJwt(SECRETO);
        String token = new JwtService(config.jwtEncoder(clave), 60).generarToken(admin());

        Jwt jwt = config.jwtDecoder(clave).decode(token);

        assertThat(jwt.getSubject()).isEqualTo("1");
        assertThat(jwt.getClaimAsString("rol")).isEqualTo("admin");
        assertThat(jwt.getClaimAsString("correo")).isEqualTo("admin@pawpets.cl");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    @DisplayName("Un token firmado con otra clave se rechaza (no se puede falsificar el rol)")
    void firmaAjena() {
        String token = new JwtService(config.jwtEncoder(config.claveJwt(SECRETO)), 60).generarToken(admin());
        JwtDecoder otroDecoder = config.jwtDecoder(config.claveJwt("otra-clave-distinta-de-al-menos-32-caracteres"));

        assertThatThrownBy(() -> otroDecoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Un token vencido se rechaza")
    void tokenVencido() {
        SecretKey clave = config.claveJwt(SECRETO);
        // Emitido hace 70 minutos y vencido hace 10 (más que la tolerancia de 60 s del validador).
        Instant emitido = Instant.now().minus(70, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(JwtService.EMISOR).subject("1").claim("rol", "admin")
                .issuedAt(emitido).expiresAt(emitido.plus(60, ChronoUnit.MINUTES)).build();
        String token = config.jwtEncoder(clave).encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        assertThatThrownBy(() -> config.jwtDecoder(clave).decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("Una clave JWT_SECRET corta impide arrancar")
    void claveCorta() {
        assertThatThrownBy(() -> config.claveJwt("corta")).isInstanceOf(IllegalStateException.class);
    }

    private static Usuario admin() {
        Usuario u = new Usuario();
        u.setId(1);
        u.setRol("admin");
        u.setCorreo("admin@pawpets.cl");
        return u;
    }
}
