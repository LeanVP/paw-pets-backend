package cl.duoc.pawpets.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API:
 * - Sin sesión en el servidor (stateless): cada petición trae su JWT en "Authorization: Bearer ...".
 * - El JwtDecoder verifica la firma y el vencimiento; el claim "rol" se convierte en ROLE_ADMIN, etc.
 * - Reglas de acceso por ruta y rol (Anexo 1: Administrador, Vendedor, Cliente).
 * - Sin token: 401. Con token pero sin permiso: 403. Ambos en JSON.
 */
@Configuration
public class SecurityConfig {

    private static final String API = "/api/v1";

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                // CSRF protege cookies de sesión; aquí la autenticación va en un header, no en cookies.
                .csrf(csrf -> csrf.disable())
                // Usa la configuración CORS de CorsConfig (origen del frontend).
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reglas -> reglas
                        // Documentación Swagger y página de error
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        // Preflight CORS del navegador
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Login y registro: públicos
                        .requestMatchers(HttpMethod.POST, API + "/auth/login", API + "/auth/registro").permitAll()
                        // Catálogo de la tienda: lectura pública
                        .requestMatchers(HttpMethod.GET, API + "/productos/**", API + "/categorias/**").permitAll()
                        // Crear, editar y eliminar productos/categorías, y todo lo de usuarios: solo admin
                        .requestMatchers(API + "/productos/**", API + "/categorias/**", API + "/usuarios/**").hasRole("ADMIN")
                        // Comprar: cliente o admin (el vendedor solo consulta)
                        .requestMatchers(HttpMethod.POST, API + "/boletas").hasAnyRole("CLIENTE", "ADMIN")
                        // Ver boletas: los tres roles (BoletaService filtra: el cliente ve solo las suyas)
                        .requestMatchers(HttpMethod.GET, API + "/boletas/**").hasAnyRole("ADMIN", "VENDEDOR", "CLIENTE")
                        // Todo lo demás (por ejemplo /auth/refresh) requiere un token válido
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(this::convertirToken))
                        .authenticationEntryPoint((req, res, e) ->
                                escribirError(res, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión o tu sesión expiró."))
                        .accessDeniedHandler((req, res, e) ->
                                escribirError(res, HttpStatus.FORBIDDEN, "No tienes permisos para esta acción.")))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                escribirError(res, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión o tu sesión expiró."))
                        .accessDeniedHandler((req, res, e) ->
                                escribirError(res, HttpStatus.FORBIDDEN, "No tienes permisos para esta acción.")));
        return http.build();
    }

    /** Convierte el JWT validado en un usuario autenticado con su rol (admin -> ROLE_ADMIN). */
    private AbstractAuthenticationToken convertirToken(Jwt jwt) {
        String rol = jwt.getClaimAsString("rol");
        var autoridades = rol == null
                ? List.<SimpleGrantedAuthority>of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()));
        return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
    }

    /** Clave HMAC para firmar y verificar. Debe tener al menos 32 bytes (256 bits) para HS256. */
    @Bean
    public SecretKey claveJwt(@Value("${jwt.secreto}") String secreto) {
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (revisa tu archivo .env).");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey clave) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    /** Verifica firma HS256, vencimiento (exp) y que el emisor sea este backend. */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey clave) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.EMISOR));
        return decoder;
    }

    /** Las contraseñas se guardan como hash BCrypt (con sal), nunca en texto plano. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void escribirError(HttpServletResponse res, HttpStatus status, String mensaje)
            throws java.io.IOException {
        res.setStatus(status.value());
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"estado\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"mensaje\":\"" + mensaje + "\",\"campos\":{}}");
    }
}
