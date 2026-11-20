package cl.duoc.pawpets.controller;

import cl.duoc.pawpets.exception.ErrorRespuesta;
import cl.duoc.pawpets.model.Categoria;
import cl.duoc.pawpets.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de categorías. Lectura pública; crear, editar y eliminar solo el administrador. */
@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "CRUD de categorías de productos")
public class CategoriaController {

    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Listar categorías", description = "Público. Devuelve todas las categorías ordenadas por id.")
    @ApiResponse(responseCode = "200", description = "Lista de categorías")
    @GetMapping
    public List<Categoria> listar() {
        return servicio.listar();
    }

    @Operation(summary = "Obtener una categoría", description = "Público.")
    @ApiResponse(responseCode = "200", description = "Categoría encontrada")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @GetMapping("/{id}")
    public Categoria obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @Operation(summary = "Crear categoría", description = "Solo administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "201", description = "Creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "Sin token", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "Sin permiso", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria crear(@Valid @RequestBody Categoria datos) {
        return servicio.crear(datos);
    }

    @Operation(summary = "Actualizar categoría", description = "Solo administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PutMapping("/{id}")
    public Categoria actualizar(@PathVariable Integer id, @Valid @RequestBody Categoria datos) {
        return servicio.actualizar(id, datos);
    }

    @Operation(summary = "Eliminar categoría", description = "Solo administrador. No se puede eliminar si tiene productos.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Eliminada")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Tiene productos asociados", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }
}
