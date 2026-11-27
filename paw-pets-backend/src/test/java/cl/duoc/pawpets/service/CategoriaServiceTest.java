package cl.duoc.pawpets.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
import cl.duoc.pawpets.model.Categoria;
import cl.duoc.pawpets.repository.CategoriaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    CategoriaRepository categorias;
    @Mock
    ProductoRepository productos;
    CategoriaService servicio;

    @org.junit.jupiter.api.BeforeEach
    void preparar() {
        servicio = new CategoriaService(categorias, productos, new SecuenciaEnMemoria());
    }

    @Test
    @DisplayName("No se puede eliminar una categoría con productos")
    void eliminarConProductos() {
        when(categorias.findById(1)).thenReturn(Optional.of(new Categoria()));
        when(productos.existsByCategoriaId(1)).thenReturn(true);

        assertThatThrownBy(() -> servicio.eliminar(1)).isInstanceOf(ConflictoException.class);
        verify(categorias, never()).deleteById(1);
    }

    @Test
    @DisplayName("Una categoría sin productos se elimina")
    void eliminarSinProductos() {
        when(categorias.findById(4)).thenReturn(Optional.of(new Categoria()));
        when(productos.existsByCategoriaId(4)).thenReturn(false);

        servicio.eliminar(4);

        verify(categorias).deleteById(4);
    }

    @Test
    @DisplayName("Una categoría inexistente da 404")
    void noEncontrada() {
        when(categorias.findById(9)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(9)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
