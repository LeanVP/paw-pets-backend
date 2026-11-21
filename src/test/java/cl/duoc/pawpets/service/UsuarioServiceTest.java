package cl.duoc.pawpets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cl.duoc.pawpets.dto.UsuarioRequest;
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.SolicitudInvalidaException;
import cl.duoc.pawpets.model.Usuario;
import cl.duoc.pawpets.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    UsuarioRepository usuarios;

    final PasswordEncoder cifrador = new BCryptPasswordEncoder();
    UsuarioService servicio;

    @BeforeEach
    void preparar() {
        servicio = new UsuarioService(usuarios, cifrador, new SecuenciaEnMemoria());
    }

    @Test
    @DisplayName("Al editar con la clave vacía se mantiene la clave actual")
    void editarSinClaveLaMantiene() {
        Usuario existente = new Usuario();
        existente.setId(2);
        existente.setCorreo("camila@correo.cl");
        existente.setClave(cifrador.encode("Cliente123"));
        String hashOriginal = existente.getClave();
        when(usuarios.findById(2)).thenReturn(Optional.of(existente));
        when(usuarios.findByCorreoIgnoreCase("camila@correo.cl")).thenReturn(Optional.of(existente));
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        servicio.actualizar(2, datos("camila@correo.cl", "", "vendedor"));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        org.mockito.Mockito.verify(usuarios).save(guardado.capture());
        assertThat(guardado.getValue().getClave()).isEqualTo(hashOriginal);
        assertThat(guardado.getValue().getRol()).isEqualTo("vendedor");
    }

    @Test
    @DisplayName("No se puede usar el correo de otro usuario")
    void correoDeOtro() {
        Usuario yo = new Usuario();
        yo.setId(2);
        Usuario otro = new Usuario();
        otro.setId(1);
        when(usuarios.findById(2)).thenReturn(Optional.of(yo));
        when(usuarios.findByCorreoIgnoreCase("admin@pawpets.cl")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> servicio.actualizar(2, datos("admin@pawpets.cl", "", "cliente")))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("Crear exige una clave de al menos 6 caracteres y la guarda cifrada")
    void crearCifraLaClave() {
        when(usuarios.existsByCorreoIgnoreCase("nuevo@pawpets.cl")).thenReturn(false);
        when(usuarios.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(usuarios.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> servicio.crear(datos("nuevo@pawpets.cl", "123", "vendedor")))
                .isInstanceOf(SolicitudInvalidaException.class);

        servicio.crear(datos("nuevo@pawpets.cl", "Vendedor123", "vendedor"));
        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        org.mockito.Mockito.verify(usuarios).save(guardado.capture());
        assertThat(guardado.getValue().getClave()).isNotEqualTo("Vendedor123");
        assertThat(cifrador.matches("Vendedor123", guardado.getValue().getClave())).isTrue();
    }

    private static UsuarioRequest datos(String correo, String clave, String rol) {
        return new UsuarioRequest("Nombre", "Apellido", correo, clave, rol,
                "Región Metropolitana de Santiago", "Santiago", "Calle 1");
    }
}
