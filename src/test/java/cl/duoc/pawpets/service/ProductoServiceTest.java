package cl.duoc.pawpets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
import cl.duoc.pawpets.exception.SolicitudInvalidaException;
import cl.duoc.pawpets.model.Producto;
import cl.duoc.pawpets.repository.CategoriaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    ProductoRepository productos;
    @Mock
    CategoriaRepository categorias;
    @InjectMocks
    ProductoService servicio;

    @Test
    @DisplayName("Crear asigna el siguiente id y valores por defecto")
    void crearAsignaId() {
        Producto ultimo = datos(1000, 0);
        ultimo.setId(20);
        when(categorias.existsById(1)).thenReturn(true);
        when(productos.findTopByOrderByIdDesc()).thenReturn(Optional.of(ultimo));
        when(productos.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto nuevo = datos(1000, 0);
        nuevo.setPrecioOferta(null);
        nuevo.setDestacado(null);
        Producto creado = servicio.crear(nuevo);

        assertThat(creado.getId()).isEqualTo(21);
        assertThat(creado.getPrecioOferta()).isZero();
        assertThat(creado.getDestacado()).isFalse();
    }

    @Test
    @DisplayName("La categoría debe existir")
    void categoriaInexistente() {
        when(categorias.existsById(1)).thenReturn(false);

        assertThatThrownBy(() -> servicio.crear(datos(1000, 0)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("categoría");
    }

    @Test
    @DisplayName("La oferta debe ser menor que el precio normal")
    void ofertaMayorAlPrecio() {
        when(categorias.existsById(1)).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(datos(1000, 1000)))
                .isInstanceOf(SolicitudInvalidaException.class)
                .hasMessageContaining("oferta");
    }

    @Test
    @DisplayName("Actualizar conserva 'destacado' si no viene en la petición")
    void actualizarConservaDestacado() {
        Producto existente = datos(1000, 0);
        existente.setId(3);
        existente.setDestacado(true);
        when(productos.findById(3)).thenReturn(Optional.of(existente));
        when(categorias.existsById(1)).thenReturn(true);
        when(productos.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto cambios = datos(2000, 1500);
        cambios.setDestacado(null);
        Producto actualizado = servicio.actualizar(3, cambios);

        assertThat(actualizado.getPrecio()).isEqualTo(2000);
        assertThat(actualizado.getDestacado()).isTrue();
    }

    @Test
    @DisplayName("Un producto inexistente da 404")
    void noEncontrado() {
        when(productos.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtener(99)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    private static Producto datos(int precio, int oferta) {
        Producto p = new Producto();
        p.setNombre("Collar");
        p.setCategoriaId(1);
        p.setPrecio(precio);
        p.setPrecioOferta(oferta);
        p.setStock(5);
        p.setDescripcion("Collar ajustable");
        return p;
    }
}
