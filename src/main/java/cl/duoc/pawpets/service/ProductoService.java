package cl.duoc.pawpets.service;

import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
import cl.duoc.pawpets.exception.SolicitudInvalidaException;
import cl.duoc.pawpets.model.Producto;
import cl.duoc.pawpets.repository.CategoriaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Lógica de negocio de los productos. */
@Service
public class ProductoService {

    private final ProductoRepository repositorio;
    private final CategoriaRepository categorias;

    public ProductoService(ProductoRepository repositorio, CategoriaRepository categorias) {
        this.repositorio = repositorio;
        this.categorias = categorias;
    }

    public List<Producto> listar() {
        return repositorio.findAll(Sort.by("id"));
    }

    public Producto obtener(Integer id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto " + id + " no existe."));
    }

    public Producto crear(Producto datos) {
        normalizar(datos, false);
        validar(datos);
        datos.setId(repositorio.findTopByOrderByIdDesc().map(p -> p.getId() + 1).orElse(1));
        return repositorio.save(datos);
    }

    public Producto actualizar(Integer id, Producto datos) {
        Producto existente = obtener(id);
        // Si el formulario no envía "destacado", se conserva el valor actual.
        normalizar(datos, existente.getDestacado());
        validar(datos);
        existente.setNombre(datos.getNombre());
        existente.setCategoriaId(datos.getCategoriaId());
        existente.setPrecio(datos.getPrecio());
        existente.setPrecioOferta(datos.getPrecioOferta());
        existente.setStock(datos.getStock());
        existente.setImagen(datos.getImagen());
        existente.setDescripcion(datos.getDescripcion());
        existente.setDestacado(datos.getDestacado());
        return repositorio.save(existente);
    }

    public void eliminar(Integer id) {
        obtener(id);
        repositorio.deleteById(id);
    }

    private static void normalizar(Producto p, Boolean destacadoPorDefecto) {
        if (p.getPrecioOferta() == null) p.setPrecioOferta(0);
        if (p.getDestacado() == null) p.setDestacado(destacadoPorDefecto);
    }

    /** Reglas de negocio: la categoría debe existir y la oferta debe ser menor que el precio normal. */
    private void validar(Producto p) {
        if (!categorias.existsById(p.getCategoriaId())) {
            throw new SolicitudInvalidaException("La categoría " + p.getCategoriaId() + " no existe.");
        }
        if (p.getPrecioOferta() > 0 && p.getPrecioOferta() >= p.getPrecio()) {
            throw new SolicitudInvalidaException("El precio de oferta debe ser menor que el precio normal.");
        }
    }
}
