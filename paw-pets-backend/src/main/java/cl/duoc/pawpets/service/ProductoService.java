package cl.duoc.pawpets.service;

import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
// Excepción que se convierte en HTTP 400 cuando se rompe una regla de negocio.
import cl.duoc.pawpets.exception.SolicitudInvalidaException;
import cl.duoc.pawpets.model.Producto;
import cl.duoc.pawpets.repository.CategoriaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Lógica de negocio de los productos. */
// Componente de la capa de negocio.
// SERVICE: CRUD de productos más sus reglas de negocio (categoría existente y oferta menor al precio).
@Service
public class ProductoService {

    // Dependencias: productos, categorías (para validar) y generador de ids.
    private final ProductoRepository repositorio;
    private final CategoriaRepository categorias;
    private final SecuenciaService secuencias;

    public ProductoService(ProductoRepository repositorio, CategoriaRepository categorias,
            SecuenciaService secuencias) {
        this.repositorio = repositorio;
        this.categorias = categorias;
        this.secuencias = secuencias;
    }

    // READ (lista): catálogo completo ordenado por id. Es público (GET sin token).
    public List<Producto> listar() {
        return repositorio.findAll(Sort.by("id"));
    }

    // READ (uno): 404 si no existe.
    public Producto obtener(Integer id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("El producto " + id + " no existe."));
    }

    // CREATE (solo admin, lo exige SecurityConfig).
    public Producto crear(Producto datos) {
        // Rellena valores omitidos (oferta 0, destacado false) para no guardar nulls.
        normalizar(datos, false);
        // Aplica las reglas de negocio; si fallan lanza 400.
        validar(datos);
        // Asigna id con el contador atómico.
        datos.setId(secuencias.siguiente("productos",
                () -> repositorio.findTopByOrderByIdDesc().map(Producto::getId).orElse(0)));
        // INSERT en MongoDB.
        return repositorio.save(datos);
    }

    // UPDATE.
    public Producto actualizar(Integer id, Producto datos) {
        // Carga el producto actual (404 si no existe).
        Producto existente = obtener(id);
        // Si el formulario no manda "destacado", se conserva el valor actual del producto existente.
        // Si el formulario no envía "destacado", se conserva el valor actual.
        normalizar(datos, existente.getDestacado());
        validar(datos);
        // Copia campo por campo los valores nuevos sobre el documento existente.
        existente.setNombre(datos.getNombre());
        existente.setCategoriaId(datos.getCategoriaId());
        existente.setPrecio(datos.getPrecio());
        existente.setPrecioOferta(datos.getPrecioOferta());
        existente.setStock(datos.getStock());
        existente.setImagen(datos.getImagen());
        existente.setDescripcion(datos.getDescripcion());
        existente.setDestacado(datos.getDestacado());
        // UPDATE en MongoDB (mismo _id).
        return repositorio.save(existente);
    }

    // DELETE.
    public void eliminar(Integer id) {
        // Comprueba que exista (404).
        obtener(id);
        // Borra el documento.
        repositorio.deleteById(id);
    }

    // Deja valores por defecto cuando el JSON no los trae.
    private static void normalizar(Producto p, Boolean destacadoPorDefecto) {
        // Sin oferta informada = 0 (sin oferta).
        if (p.getPrecioOferta() == null) p.setPrecioOferta(0);
        // Sin "destacado" informado = valor por defecto recibido.
        if (p.getDestacado() == null) p.setDestacado(destacadoPorDefecto);
    }

    /** Reglas de negocio: la categoría debe existir y la oferta debe ser menor que el precio normal. */
    // Reglas que Bean Validation no puede expresar (necesitan consultar la BD o comparar campos).
    private void validar(Producto p) {
        // Regla 1: la categoría debe existir (reemplaza a la clave foránea de SQL).
        if (!categorias.existsById(p.getCategoriaId())) {
            throw new SolicitudInvalidaException("La categoría " + p.getCategoriaId() + " no existe.");
        }
        // Regla 2: si hay oferta, debe ser menor al precio normal; si no, 400.
        if (p.getPrecioOferta() > 0 && p.getPrecioOferta() >= p.getPrecio()) {
            throw new SolicitudInvalidaException("El precio de oferta debe ser menor que el precio normal.");
        }
    }
}
