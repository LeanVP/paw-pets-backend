package cl.duoc.pawpets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cl.duoc.pawpets.dto.AuthResponse;
import cl.duoc.pawpets.dto.LoginRequest;
import cl.duoc.pawpets.dto.RegistroRequest;
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.CredencialesInvalidasException;
import cl.duoc.pawpets.model.Usuario;
import cl.duoc.pawpets.repository.UsuarioRepository;
import cl.duoc.pawpets.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    UsuarioRepository usuarios;
    @Mock
    JwtService jwt;

    final PasswordEncoder cifrador = new BCryptPasswordEncoder();
    AuthService servicio;

    @BeforeEach
    void preparar() {
        servicio = new AuthService(usuarios, new UsuarioService(usuarios, cifrador), cifrador, jwt);
        when(jwt.generarToken(any(Usuario.class))).thenReturn("token-de-prueba");
    }

    @Test
    @DisplayName("Login correcto: compara con el hash BCrypt y entrega token sin la clave")
    void loginCorrecto() {
        Usuario admin = usuario("admin@pawpets.cl", "Admin123", "admin");
        when(usuarios.findByCorreoIgnoreCase("admin@pawpets.cl")).thenReturn(Optional.of(admin));

        AuthResponse r = servicio.login(new LoginRequest("admin@pawpets.cl", "Admin123"));

        assertThat(r.token()).isEqualTo("token-de-prueba");
        assertThat(r.usuario().rol()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Clave incorrecta o correo inexistente: 401 con el mismo mensaje")
    void loginIncorrecto() {
        Usuario admin = usuario("admin@pawpets.cl", "Admin123", "admin");
        when(usuarios.findByCorreoIgnoreCase("admin@pawpets.cl")).thenReturn(Optional.of(admin));
        when(usuarios.findByCorreoIgnoreCase("nadie@pawpets.cl")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.login(new LoginRequest("admin@pawpets.cl", "otra")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos.");
        assertThatThrownBy(() -> servicio.login(new LoginRequest("nadie@pawpets.cl", "Admin123")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos.");
    }

    @Test
    @DisplayName("El registro siempre crea un cliente y guarda la clave cifrada")
    void registroCreaCliente() {
        when(usuarios.existsByCorreoIgnoreCase("nuevo@correo.cl")).thenReturn(false);
        when(usuarios.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse r = servicio.registrar(new RegistroRequest("Ana", "Pérez", " nuevo@correo.cl ",
                "Clave123", "Calle 1", "Región de Valparaíso", "Valparaíso"));

        assertThat(r.usuario().rol()).isEqualTo("cliente");
        assertThat(r.usuario().correo()).isEqualTo("nuevo@correo.cl");
        assertThat(r.usuario().id()).isEqualTo(1);
    }

    @Test
    @DisplayName("Registro con correo repetido: conflicto")
    void registroCorreoRepetido() {
        when(usuarios.existsByCorreoIgnoreCase("camila@correo.cl")).thenReturn(true);

        assertThatThrownBy(() -> servicio.registrar(new RegistroRequest("Camila", "Rojas", "camila@correo.cl",
                "Clave123", "Calle", "Región", "Comuna"))).isInstanceOf(ConflictoException.class);
    }

    private Usuario usuario(String correo, String clave, String rol) {
        Usuario u = new Usuario();
        u.setId(1);
        u.setCorreo(correo);
        u.setClave(cifrador.encode(clave));
        u.setRol(rol);
        return u;
    }
}
