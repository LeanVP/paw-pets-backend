package cl.duoc.pawpets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.duoc.pawpets.dto.BoletaRequest;
import cl.duoc.pawpets.dto.ItemBoletaRequest;
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
import cl.duoc.pawpets.exception.SolicitudInvalidaException;
import cl.duoc.pawpets.model.Boleta;
import cl.duoc.pawpets.model.Producto;
import cl.duoc.pawpets.repository.BoletaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BoletaServiceTest {

    @Mock
    BoletaRepository boletas;
    @Mock
    ProductoRepository productos;
    BoletaService servicio;

    Producto conOferta;
    Producto sinOferta;

    @BeforeEach
    void preparar() {
        servicio = new BoletaService(boletas, productos, new SecuenciaEnMemoria());
        conOferta = producto(1, 10000, 8000, 10);
        sinOferta = producto(2, 5000, 0, 3);
    }

    @Test
    @DisplayName("Calcula precios en el servidor (usa la oferta), suma el total y descuenta stock")
    void compraPagada() {
        when(productos.findById(1)).thenReturn(Optional.of(conOferta));
        when(productos.findById(2)).thenReturn(Optional.of(sinOferta));
        when(productos.descontarStock(1, 2)).thenReturn(true);
        when(productos.descontarStock(2, 1)).thenReturn(true);
        when(boletas.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(boletas.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        Boleta boleta = servicio.crear(pedido("Tarjeta de débito",
                new ItemBoletaRequest(1, 2), new ItemBoletaRequest(2, 1)), 7);

        assertThat(boleta.getId()).isEqualTo(1);
        assertThat(boleta.getClienteId()).isEqualTo(7);
        assertThat(boleta.getEstado()).isEqualTo(Boleta.PAGADA);
        assertThat(boleta.getItems().get(0).getPrecio()).isEqualTo(8000);
        assertThat(boleta.getTotal()).isEqualTo(8000 * 2 + 5000);
        // El stock se descuenta en MongoDB con una operación atómica por producto
        verify(productos).descontarStock(1, 2);
        verify(productos).descontarStock(2, 1);
    }

    @Test
    @DisplayName("Junta las líneas repetidas del mismo producto")
    void juntaLineasRepetidas() {
        when(productos.findById(1)).thenReturn(Optional.of(conOferta));
        when(productos.descontarStock(1, 3)).thenReturn(true);
        when(boletas.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(boletas.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        Boleta boleta = servicio.crear(pedido("Tarjeta de débito",
                new ItemBoletaRequest(1, 2), new ItemBoletaRequest(1, 1)), 7);

        assertThat(boleta.getItems()).hasSize(1);
        assertThat(boleta.getItems().get(0).getCantidad()).isEqualTo(3);
        verify(productos).descontarStock(1, 3);
    }

    @Test
    @DisplayName("Compra simultánea: si otro cliente se llevó el stock, devuelve lo descontado y da 409")
    void compraSimultanea() {
        when(productos.findById(1)).thenReturn(Optional.of(conOferta));
        when(productos.findById(2)).thenReturn(Optional.of(sinOferta));
        when(productos.descontarStock(1, 2)).thenReturn(true);
        // Entre la validación y el descuento, otro cliente compró las últimas unidades del producto 2
        when(productos.descontarStock(2, 1)).thenReturn(false);

        assertThatThrownBy(() -> servicio.crear(pedido("Tarjeta de débito",
                new ItemBoletaRequest(1, 2), new ItemBoletaRequest(2, 1)), 7))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("otro cliente");
        verify(productos).devolverStock(1, 2);
        verify(boletas, never()).save(any());
    }

    @Test
    @DisplayName("Los números de boleta son correlativos y nunca se repiten")
    void numerosCorrelativos() {
        when(productos.findById(2)).thenReturn(Optional.of(sinOferta));
        when(boletas.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        int primera = servicio.crear(pedido(BoletaService.MEDIO_RECHAZADO, new ItemBoletaRequest(2, 1)), 7).getId();
        int segunda = servicio.crear(pedido(BoletaService.MEDIO_RECHAZADO, new ItemBoletaRequest(2, 1)), 7).getId();

        assertThat(segunda).isEqualTo(primera + 1);
    }

    @Test
    @DisplayName("Un pago rechazado guarda la boleta como Rechazada y NO descuenta stock")
    void pagoRechazado() {
        when(productos.findById(2)).thenReturn(Optional.of(sinOferta));
        when(boletas.findTopByOrderByIdDesc()).thenReturn(Optional.empty());
        when(boletas.save(any(Boleta.class))).thenAnswer(inv -> inv.getArgument(0));

        Boleta boleta = servicio.crear(pedido(BoletaService.MEDIO_RECHAZADO, new ItemBoletaRequest(2, 1)), 7);

        assertThat(boleta.getEstado()).isEqualTo(Boleta.RECHAZADA);
        verify(productos, never()).descontarStock(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("Sin stock suficiente responde conflicto y no guarda nada")
    void stockInsuficiente() {
        when(productos.findById(2)).thenReturn(Optional.of(sinOferta));

        assertThatThrownBy(() -> servicio.crear(pedido("Tarjeta de débito", new ItemBoletaRequest(2, 4)), 7))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("Stock insuficiente");
        verify(boletas, never()).save(any());
    }

    @Test
    @DisplayName("Un producto inexistente es una solicitud inválida")
    void productoInexistente() {
        when(productos.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(pedido("Tarjeta de débito", new ItemBoletaRequest(99, 1)), 7))
                .isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    @DisplayName("Un cliente solo lista sus boletas; admin y vendedor ven todas")
    void listarSegunRol() {
        servicio.listar(7, "cliente");
        verify(boletas).findByClienteIdOrderByIdAsc(7);

        servicio.listar(1, "vendedor");
        verify(boletas).findAll(any(org.springframework.data.domain.Sort.class));
    }

    @Test
    @DisplayName("Un cliente que pide una boleta ajena recibe 404; el vendedor sí la ve")
    void boletaAjena() {
        Boleta ajena = new Boleta();
        ajena.setId(5);
        ajena.setClienteId(8);
        when(boletas.findById(5)).thenReturn(Optional.of(ajena));

        assertThatThrownBy(() -> servicio.obtener(5, 7, "cliente"))
                .isInstanceOf(RecursoNoEncontradoException.class);
        assertThat(servicio.obtener(5, 3, "vendedor")).isSameAs(ajena);
    }

    private static Producto producto(int id, int precio, int oferta, int stock) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre("Producto " + id);
        p.setPrecio(precio);
        p.setPrecioOferta(oferta);
        p.setStock(stock);
        return p;
    }

    private static BoletaRequest pedido(String medioPago, ItemBoletaRequest... items) {
        return new BoletaRequest("Camila", "Rojas", "camila@correo.cl", "Los Aromos 456",
                "Región Metropolitana de Santiago", "Maipú", "", medioPago, List.of(items));
    }
}
