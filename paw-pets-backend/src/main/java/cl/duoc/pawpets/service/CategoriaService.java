package cl.duoc.pawpets.service;

// Excepciones de negocio que se convierten en 409 y 404.
import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
import cl.duoc.pawpets.model.Categoria;
// Acceso a "categorias".
import cl.duoc.pawpets.repository.CategoriaRepository;
// Se necesita para saber si una categoría tiene productos antes de borrarla.
import cl.duoc.pawpets.repository.ProductoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Lógica de negocio de las categorías. */
// Componente de la capa de negocio.
// SERVICE: reglas del CRUD de categorías. El controlador delega aquí todo.
@Service
public class CategoriaService {

    // Dependencias (inyectadas por constructor).
    private final CategoriaRepository repositorio;
    private final ProductoRepository productos;
    private final SecuenciaService secuencias;

    // Constructor con inyección de dependencias.
    public CategoriaService(CategoriaRepository repositorio, ProductoRepository productos,
            SecuenciaService secuencias) {
        this.repositorio = repositorio;
        this.productos = productos;
        this.secuencias = secuencias;
    }

    // READ (lista): devuelve todas ordenadas por id.
    public List<Categoria> listar() {
        // findAll viene de MongoRepository; Sort.by("id") ordena ascendente.
        return repositorio.findAll(Sort.by("id"));
    }

    // READ (una): busca por id.
    public Categoria obtener(Integer id) {
        return repositorio.findById(id)
                // Si no existe → excepción → HTTP 404. Optional evita devolver null.
                .orElseThrow(() -> new RecursoNoEncontradoException("La categoría " + id + " no existe."));
    }

    // CREATE.
    public Categoria crear(Categoria datos) {
        // El servidor asigna el id con el contador atómico; el id que mande el cliente se ignora.
        datos.setId(secuencias.siguiente("categorias",
                () -> repositorio.findTopByOrderByIdDesc().map(Categoria::getId).orElse(0)));
        // INSERT en MongoDB; devuelve la categoría ya con id (el frontend la agrega a su caché).
        return repositorio.save(datos);
    }

    // UPDATE.
    public Categoria actualizar(Integer id, Categoria datos) {
        // Reutiliza obtener(): si no existe lanza 404 antes de modificar.
        Categoria existente = obtener(id);
        // Solo se copian los campos editables; el id no cambia.
        existente.setNombre(datos.getNombre());
        existente.setImagen(datos.getImagen());
        // save() con un id existente hace UPDATE (upsert por _id).
        return repositorio.save(existente);
    }

    /** Regla de negocio: no se elimina una categoría que todavía tiene productos. */
    // DELETE con regla de integridad: MongoDB no impone claves foráneas, así que la regla se programa aquí.
    public void eliminar(Integer id) {
        // Verifica que exista (404 si no).
        obtener(id);
        // ¿Hay productos que dependen de ella?
        if (productos.existsByCategoriaId(id)) {
            // Sí hay → HTTP 409 y mensaje que el frontend muestra al admin.
            throw new ConflictoException("No se puede eliminar: la categoría tiene productos asociados.");
        }
        // No hay productos: borra el documento.
        repositorio.deleteById(id);
    }
}
