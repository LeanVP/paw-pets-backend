package cl.duoc.pawpets.integracion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cl.duoc.pawpets.config.CorsConfig;
import cl.duoc.pawpets.dto.AuthResponse;
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.model.Categoria;
import cl.duoc.pawpets.model.Usuario;
import cl.duoc.pawpets.security.JwtService;
import cl.duoc.pawpets.security.SecurityConfig;
import cl.duoc.pawpets.service.AuthService;
import cl.duoc.pawpets.service.BoletaService;
import cl.duoc.pawpets.service.CategoriaService;
import cl.duoc.pawpets.service.ProductoService;
import cl.duoc.pawpets.service.UsuarioService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Pruebas de INTEGRACIÓN de la capa web: controladores + Spring Security + JWT + CORS + validación +
 * manejo de errores, trabajando juntos sobre peticiones HTTP reales (MockMvc).
 * Los servicios se simulan (@MockitoBean) para no depender de MongoDB Atlas: así la prueba
 * se centra en que cada ruta responda el código correcto según el rol del token (IE3.2.1, IE3.3.1).
 */
@WebMvcTest
// En Spring Boot 4 el slice web no activa Spring Security solo: se activa aquí con las reglas reales de SecurityConfig
@EnableWebSecurity
@Import({ SecurityConfig.class, JwtService.class, CorsConfig.class })
@TestPropertySource(properties = {
        "jwt.secreto=clave-de-pruebas-de-integracion-de-32-bytes-o-mas",
        "jwt.expiracion-minutos=60",
        "cors.origen=http://localhost:5173" })
class SeguridadApiIntegracionTest {

    private static final String API = "/api/v1";
    private static final String CATEGORIA_VALIDA = "{\"nombre\":\"Acuarios\",\"imagen\":\"acuarios.jpg\"}";

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwtService;
    @Autowired
    JwtEncoder encoder;

    @MockitoBean
    AuthService authService;
    @MockitoBean
    CategoriaService categoriaService;
    @MockitoBean
    ProductoService productoService;
    @MockitoBean
    UsuarioService usuarioService;
    @MockitoBean
    BoletaService boletaService;

    private String token(int id, String rol) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setRol(rol);
        u.setCorreo(rol + "@pawpets.cl");
        return "Bearer " + jwtService.generarToken(u);
    }

    private String admin() {
        return token(1, Usuario.ADMIN);
    }

    private String vendedor() {
        return token(2, Usuario.VENDEDOR);
    }

    private String cliente() {
        return token(3, Usuario.CLIENTE);
    }

    @Nested
    @DisplayName("Rutas públicas")
    class Publicas {

        @Test
        @DisplayName("Cualquiera puede listar categorías y productos sin token")
        void catalogoPublico() throws Exception {
            when(categoriaService.listar()).thenReturn(List.of(new Categoria()));
            mvc.perform(get(API + "/categorias")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
            mvc.perform(get(API + "/productos")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("Login con datos vacíos responde 400 con los campos inválidos")
        void loginValidacion() throws Exception {
            mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.correo").exists())
                    .andExpect(jsonPath("$.campos.clave").exists());
        }
    }

    @Nested
    @DisplayName("Roles sobre el CRUD de categorías")
    class CrudPorRol {

        @Test
        @DisplayName("Sin token no se puede crear: 401 en JSON")
        void sinToken() throws Exception {
            mvc.perform(post(API + "/categorias").contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_VALIDA))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.estado").value(401));
            verify(categoriaService, never()).crear(any());
        }

        @Test
        @DisplayName("Cliente y vendedor no pueden crear: 403")
        void sinPermiso() throws Exception {
            for (String t : List.of(cliente(), vendedor())) {
                mvc.perform(post(API + "/categorias").header("Authorization", t)
                        .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_VALIDA))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.estado").value(403));
            }
            verify(categoriaService, never()).crear(any());
        }

        @Test
        @DisplayName("El administrador crea (201), y si faltan datos recibe 400")
        void adminCrea() throws Exception {
            when(categoriaService.crear(any())).thenAnswer(i -> i.getArgument(0));
            mvc.perform(post(API + "/categorias").header("Authorization", admin())
                    .contentType(MediaType.APPLICATION_JSON).content(CATEGORIA_VALIDA))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.nombre").value("Acuarios"));
            mvc.perform(post(API + "/categorias").header("Authorization", admin())
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.nombre").exists());
        }

        @Test
        @DisplayName("Eliminar una categoría con productos responde 409 (regla de negocio)")
        void eliminarConflicto() throws Exception {
            doThrow(new ConflictoException("La categoría tiene productos.")).when(categoriaService).eliminar(1);
            mvc.perform(delete(API + "/categorias/1").header("Authorization", admin()))
                    .andExpect(status().isConflict());
            mvc.perform(delete(API + "/categorias/4").header("Authorization", admin()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Permisos del Anexo 1 por rol")
    class PermisosAnexo1 {

        @Test
        @DisplayName("Usuarios: solo el administrador")
        void usuariosSoloAdmin() throws Exception {
            mvc.perform(get(API + "/usuarios").header("Authorization", admin())).andExpect(status().isOk());
            mvc.perform(get(API + "/usuarios").header("Authorization", vendedor())).andExpect(status().isForbidden());
            mvc.perform(get(API + "/usuarios").header("Authorization", cliente())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Órdenes: el vendedor las ve pero no puede comprar; el cliente sí compra")
        void ordenes() throws Exception {
            mvc.perform(get(API + "/boletas").header("Authorization", vendedor())).andExpect(status().isOk());
            verify(boletaService).listar(2, Usuario.VENDEDOR);

            String compra = "{\"nombre\":\"Camila\",\"apellidos\":\"Rojas\",\"correo\":\"camila@correo.cl\","
                    + "\"calle\":\"Los Aromos 456\",\"region\":\"RM\",\"comuna\":\"Maipú\",\"medioPago\":\"debito\","
                    + "\"items\":[{\"productoId\":1,\"cantidad\":1}]}";
            mvc.perform(post(API + "/boletas").header("Authorization", vendedor())
                    .contentType(MediaType.APPLICATION_JSON).content(compra))
                    .andExpect(status().isForbidden());
            mvc.perform(post(API + "/boletas").header("Authorization", cliente())
                    .contentType(MediaType.APPLICATION_JSON).content(compra))
                    .andExpect(status().isCreated());
            verify(boletaService).crear(any(), org.mockito.ArgumentMatchers.eq(3));
            mvc.perform(post(API + "/boletas").header("Authorization", cliente())
                    .contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campos.items").exists());
            mvc.perform(get(API + "/boletas")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("El vendedor no puede modificar ni borrar productos")
        void vendedorSoloLectura() throws Exception {
            mvc.perform(get(API + "/productos/1").header("Authorization", vendedor())).andExpect(status().isOk());
            mvc.perform(delete(API + "/productos/1").header("Authorization", vendedor())).andExpect(status().isForbidden());
            verify(productoService, never()).eliminar(any());
        }
    }

    @Nested
    @DisplayName("Tokens JWT")
    class Tokens {

        @Test
        @DisplayName("Renovación: con un token válido /auth/refresh entrega uno nuevo")
        void renovar() throws Exception {
            when(authService.refrescar(3)).thenReturn(new AuthResponse("token-nuevo", null));
            mvc.perform(post(API + "/auth/refresh").header("Authorization", cliente()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("token-nuevo"));
            mvc.perform(post(API + "/auth/refresh")).andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token vencido: 401")
        void vencido() throws Exception {
            Instant antes = Instant.now().minus(2, ChronoUnit.HOURS);
            JwtClaimsSet claims = JwtClaimsSet.builder().issuer(JwtService.EMISOR).subject("1")
                    .issuedAt(antes.minus(1, ChronoUnit.HOURS)).expiresAt(antes).claim("rol", "admin").build();
            String vencido = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                    .getTokenValue();
            mvc.perform(get(API + "/usuarios").header("Authorization", "Bearer " + vencido))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Token firmado con otra clave: 401")
        void otraClave() throws Exception {
            byte[] otra = "una-clave-distinta-que-no-es-la-del-backend!!".getBytes(StandardCharsets.UTF_8);
            JwtEncoder falso = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otra, "HmacSHA256")));
            JwtClaimsSet claims = JwtClaimsSet.builder().issuer(JwtService.EMISOR).subject("1")
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plus(1, ChronoUnit.HOURS)).claim("rol", "admin").build();
            String token = falso.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                    .getTokenValue();
            mvc.perform(get(API + "/usuarios").header("Authorization", "Bearer " + token))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Cambiar el rol dentro del token (cliente -> admin) invalida la firma: 401")
        void tokenManipulado() throws Exception {
            String[] partes = cliente().substring("Bearer ".length()).split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8)
                    .replace("\"cliente\"", "\"admin\"");
            String manipulado = partes[0] + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                    + "." + partes[2];
            mvc.perform(get(API + "/usuarios").header("Authorization", "Bearer " + manipulado))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CRUD completo con el administrador (IE3.2.1)")
    class CrudCompleto {

        private void crud(String ruta, String cuerpo) throws Exception {
            mvc.perform(get(API + ruta).header("Authorization", admin())).andExpect(status().isOk());
            mvc.perform(get(API + ruta + "/1").header("Authorization", admin())).andExpect(status().isOk());
            mvc.perform(post(API + ruta).header("Authorization", admin())
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isCreated());
            mvc.perform(put(API + ruta + "/1").header("Authorization", admin())
                    .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andExpect(status().isOk());
            mvc.perform(delete(API + ruta + "/1").header("Authorization", admin())).andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Productos: listar, obtener, crear, actualizar y eliminar")
        void productos() throws Exception {
            crud("/productos", "{\"nombre\":\"Collar\",\"categoriaId\":3,\"precio\":5990,\"precioOferta\":0,"
                    + "\"stock\":10,\"imagen\":\"collar.png\",\"descripcion\":\"Collar ajustable\",\"destacado\":false}");
            verify(productoService).eliminar(1);
        }

        @Test
        @DisplayName("Categorías: listar, obtener, crear, actualizar y eliminar")
        void categorias() throws Exception {
            crud("/categorias", CATEGORIA_VALIDA);
            verify(categoriaService).eliminar(1);
        }

        @Test
        @DisplayName("Usuarios: listar, obtener, crear, actualizar y eliminar")
        void usuarios() throws Exception {
            crud("/usuarios", "{\"nombre\":\"Pedro\",\"apellidos\":\"Soto\",\"correo\":\"pedro@pawpets.cl\","
                    + "\"clave\":\"Clave123\",\"rol\":\"vendedor\"}");
            verify(usuarioService).eliminar(1);
        }
    }

    @Nested
    @DisplayName("CORS")
    class Cors {

        @Test
        @DisplayName("El frontend (localhost:5173) puede llamar a la API; otro origen no")
        void origenes() throws Exception {
            mvc.perform(options(API + "/productos").header("Origin", "http://localhost:5173")
                    .header("Access-Control-Request-Method", "POST"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
            mvc.perform(get(API + "/productos").header("Origin", "http://sitio-malo.com"))
                    .andExpect(status().isForbidden());
        }
    }
}
