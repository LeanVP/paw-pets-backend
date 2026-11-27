package cl.duoc.pawpets.security;

// Nimbus JOSE: librería que firma y verifica JWT. ImmutableSecret envuelve la clave secreta para el firmador.
import com.nimbusds.jose.jwk.source.ImmutableSecret;
// Objeto de respuesta HTTP; se usa para escribir a mano el JSON de los errores 401 y 403.
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
// SecretKey/SecretKeySpec: representan la clave criptográfica HMAC creada a partir de JWT_SECRET.
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
// @Value: inyecta propiedades (jwt.secreto) en parámetros o campos.
import org.springframework.beans.factory.annotation.Value;
// @Bean: el método devuelve un objeto que Spring registra y reutiliza en toda la aplicación.
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
// Enum de verbos HTTP (GET, POST...) para escribir reglas por método.
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
// Tipo base de "usuario autenticado" que Spring Security guarda durante la petición.
import org.springframework.security.authentication.AbstractAuthenticationToken;
// Customizer.withDefaults(): usa la configuración por defecto de una característica (aquí CORS).
import org.springframework.security.config.Customizer;
// HttpSecurity: constructor de la cadena de filtros de seguridad (reglas de acceso, CORS, sesión...).
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
// Representa un permiso/rol, por ejemplo "ROLE_ADMIN".
import org.springframework.security.core.authority.SimpleGrantedAuthority;
// Algoritmo BCrypt: hash lento con sal aleatoria, recomendado para contraseñas.
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
// Jwt: el token ya verificado y decodificado (con sus claims).
import org.springframework.security.oauth2.jwt.Jwt;
// JwtDecoder: verifica la firma y el vencimiento de cada token que llega.
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
// JwtValidators: validaciones extra (vencimiento, emisor).
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
// Autenticación basada en JWT: contiene el token y los roles del usuario.
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
// SecurityFilterChain: la cadena de filtros que cada petición HTTP atraviesa ANTES de llegar al controlador.
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad de la API:
 * - Sin sesión en el servidor (stateless): cada petición trae su JWT en "Authorization: Bearer ...".
 * - El JwtDecoder verifica la firma y el vencimiento; el claim "rol" se convierte en ROLE_ADMIN, etc.
 * - Reglas de acceso por ruta y rol (Anexo 1: Administrador, Vendedor, Cliente).
 * - Sin token: 401. Con token pero sin permiso: 403. Ambos en JSON.
 */
// Clase de configuración de Spring: sus métodos @Bean se ejecutan al arrancar.
// CAPA DE SEGURIDAD: decide quién puede llamar a cada endpoint. Es lo primero que toca una petición (antes del controlador).
@Configuration
public class SecurityConfig {

    // Prefijo común de todas las rutas; versionar la API (/v1) permite evolucionarla sin romper clientes.
    private static final String API = "/api/v1";

    // Este método construye la cadena de filtros y Spring la instala en el servidor.
    // Recibe el configurador HttpSecurity y devuelve la cadena de seguridad terminada.
    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                // CSRF protege cookies de sesión; aquí la autenticación va en un header, no en cookies.
                // CSRF solo afecta a autenticación por cookies; aquí el JWT va en un header, por eso se desactiva sin riesgo.
                .csrf(csrf -> csrf.disable())
                // Usa la configuración CORS de CorsConfig (origen del frontend).
                // Activa CORS usando la configuración de CorsConfig.
                .cors(Customizer.withDefaults())
                // STATELESS = el servidor NO guarda sesión. Cada petición debe traer su propio JWT. Permite escalar sin memoria de sesión.
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // REGLAS DE ACCESO. Se evalúan en orden: la primera que coincide gana, por eso las específicas van antes que las generales.
                .authorizeHttpRequests(reglas -> reglas
                        // Documentación Swagger y página de error
                        // Swagger y la ruta /error son públicos (documentación y errores).
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        // Preflight CORS del navegador
                        // Deja pasar el "preflight" CORS del navegador (no lleva token).
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Login y registro: públicos
                        // Login y registro: públicos (aún no hay token).
                        .requestMatchers(HttpMethod.POST, API + "/auth/login", API + "/auth/registro").permitAll()
                        // Catálogo de la tienda: lectura pública
                        // Ver el catálogo (GET productos y categorías) es público: lo ve cualquier visitante.
                        .requestMatchers(HttpMethod.GET, API + "/productos/**", API + "/categorias/**").permitAll()
                        // Crear, editar y eliminar productos/categorías, y todo lo de usuarios: solo admin
                        // Cualquier OTRO método (POST/PUT/DELETE) sobre productos/categorías, y todo /usuarios, solo ADMIN. Si no, responde 403.
                        .requestMatchers(API + "/productos/**", API + "/categorias/**", API + "/usuarios/**").hasRole("ADMIN")
                        // Comprar: cliente o admin (el vendedor solo consulta)
                        // Comprar (crear boleta): solo CLIENTE o ADMIN. El vendedor solo consulta.
                        .requestMatchers(HttpMethod.POST, API + "/boletas").hasAnyRole("CLIENTE", "ADMIN")
                        // Ver boletas: los tres roles (BoletaService filtra: el cliente ve solo las suyas)
                        // Ver boletas: los tres roles; BoletaService filtra para que el cliente vea solo las suyas.
                        .requestMatchers(HttpMethod.GET, API + "/boletas/**").hasAnyRole("ADMIN", "VENDEDOR", "CLIENTE")
                        // Todo lo demás (por ejemplo /auth/refresh) requiere un token válido
                        // Regla final: todo lo demás (ej. /auth/refresh) exige token válido. Es la política por defecto "cerrado".
                        .anyRequest().authenticated())
                // Convierte la app en "Resource Server": lee el header "Authorization: Bearer <token>" y lo valida con el JwtDecoder.
                .oauth2ResourceServer(rs -> rs
                        // Cuando el token es válido, usa convertirToken para transformar el claim "rol" en un permiso de Spring.
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(this::convertirToken))
                        // Qué responder cuando NO hay token o es inválido: 401 con un JSON propio.
                        .authenticationEntryPoint((req, res, e) ->
                                escribirError(res, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión o tu sesión expiró."))
                        // Qué responder cuando hay token válido pero el rol no alcanza: 403 con un JSON propio.
                        .accessDeniedHandler((req, res, e) ->
                                escribirError(res, HttpStatus.FORBIDDEN, "No tienes permisos para esta acción.")))
                // Lo mismo para errores de seguridad que no pasan por el Resource Server (cubre ambos caminos).
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) ->
                                escribirError(res, HttpStatus.UNAUTHORIZED, "Debes iniciar sesión o tu sesión expiró."))
                        .accessDeniedHandler((req, res, e) ->
                                escribirError(res, HttpStatus.FORBIDDEN, "No tienes permisos para esta acción.")));
        // Construye y entrega la cadena de filtros terminada.
        return http.build();
    }

    /** Convierte el JWT validado en un usuario autenticado con su rol (admin -> ROLE_ADMIN). */
    // Traduce el JWT verificado a un usuario de Spring Security con sus autoridades (roles).
    private AbstractAuthenticationToken convertirToken(Jwt jwt) {
        // Lee el claim "rol" del token (admin, vendedor o cliente).
        String rol = jwt.getClaimAsString("rol");
        // Si el token no trae rol, el usuario queda sin permisos.
        var autoridades = rol == null
                ? List.<SimpleGrantedAuthority>of()
                // Spring exige el prefijo "ROLE_" y mayúsculas: "admin" → "ROLE_ADMIN", lo que luego usa hasRole("ADMIN").
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol.toUpperCase()));
        // Devuelve el usuario autenticado: guarda el JWT, los roles y el id (sub) como nombre. Los controladores lo reciben con @AuthenticationPrincipal.
        return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
    }

    /** Clave HMAC para firmar y verificar. Debe tener al menos 32 bytes (256 bits) para HS256. */
    // Construye la clave secreta HMAC a partir de JWT_SECRET (.env). Con ella se firman y verifican TODOS los tokens.
    @Bean
    public SecretKey claveJwt(@Value("${jwt.secreto}") String secreto) {
        // Convierte el texto secreto a bytes.
        byte[] bytes = secreto.getBytes(StandardCharsets.UTF_8);
        // HS256 exige al menos 256 bits = 32 bytes. Si el secreto es más corto, la app NO arranca (falla rápido y avisa).
        if (bytes.length < 32) {
            // Detiene el arranque con un mensaje claro.
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres (revisa tu archivo .env).");
        }
        // Clave HMAC-SHA256 lista para usar.
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    // Bean que FIRMA tokens (lo usa JwtService). Recibe la clave del bean anterior.
    @Bean
    public JwtEncoder jwtEncoder(SecretKey clave) {
        // Implementación de Nimbus con la clave simétrica.
        return new NimbusJwtEncoder(new ImmutableSecret<>(clave));
    }

    /** Verifica firma HS256, vencimiento (exp) y que el emisor sea este backend. */
    // Bean que VERIFICA tokens en cada petición: la misma clave que firma es la que valida (criptografía simétrica).
    @Bean
    public JwtDecoder jwtDecoder(SecretKey clave) {
        // Crea el decoder con la clave y el algoritmo HS256.
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(clave).macAlgorithm(MacAlgorithm.HS256).build();
        // Validaciones adicionales: vencimiento (exp) y que el emisor (iss) sea "paw-pets-backend".
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(JwtService.EMISOR));
        // Entrega el decoder configurado.
        return decoder;
    }

    /** Las contraseñas se guardan como hash BCrypt (con sal), nunca en texto plano. */
    // Bean del cifrador de contraseñas. AuthService, UsuarioService y DataSeeder lo reciben por inyección.
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt agrega una sal aleatoria: dos usuarios con la misma clave tienen hashes distintos.
        return new BCryptPasswordEncoder();
    }

    // Auxiliar que escribe a mano el JSON de error de seguridad con el MISMO formato que ErrorRespuesta (estado, error, mensaje, campos).
    private static void escribirError(HttpServletResponse res, HttpStatus status, String mensaje)
            throws java.io.IOException {
        // Pone el código HTTP (401 o 403).
        res.setStatus(status.value());
        // Indica que la respuesta es JSON en UTF-8 (para tildes y ñ).
        res.setContentType("application/json;charset=UTF-8");
        // Escribe el cuerpo JSON. Así el frontend siempre recibe errores con la misma estructura y lee "mensaje".
        res.getWriter().write("{\"estado\":" + status.value() + ",\"error\":\"" + status.getReasonPhrase()
                + "\",\"mensaje\":\"" + mensaje + "\",\"campos\":{}}");
    }
}
