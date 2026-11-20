package cl.duoc.pawpets.service;

import cl.duoc.pawpets.exception.ConflictoException;
import cl.duoc.pawpets.exception.RecursoNoEncontradoException;
import cl.duoc.pawpets.model.Categoria;
import cl.duoc.pawpets.repository.CategoriaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/** Lógica de negocio de las categorías. */
@Service
public class CategoriaService {

    private final CategoriaRepository repositorio;
    private final ProductoRepository productos;

    public CategoriaService(CategoriaRepository repositorio, ProductoRepository productos) {
        this.repositorio = repositorio;
        this.productos = productos;
    }

    public List<Categoria> listar() {
        return repositorio.findAll(Sort.by("id"));
    }

    public Categoria obtener(Integer id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("La categoría " + id + " no existe."));
    }

    public Categoria crear(Categoria datos) {
        datos.setId(repositorio.findTopByOrderByIdDesc().map(c -> c.getId() + 1).orElse(1));
        return repositorio.save(datos);
    }

    public Categoria actualizar(Integer id, Categoria datos) {
        Categoria existente = obtener(id);
        existente.setNombre(datos.getNombre());
        existente.setImagen(datos.getImagen());
        return repositorio.save(existente);
    }

    /** Regla de negocio: no se elimina una categoría que todavía tiene productos. */
    public void eliminar(Integer id) {
        obtener(id);
        if (productos.existsByCategoriaId(id)) {
            throw new ConflictoException("No se puede eliminar: la categoría tiene productos asociados.");
        }
        repositorio.deleteById(id);
    }
}
