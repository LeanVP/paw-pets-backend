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
// @DeleteMapping / @GetMapping / @PostMapping / @PutMapping: asocian cada método con el verbo HTTP DELETE, GET, POST y PUT.
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
// @PathVariable: lee {id} de la URL.
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de categorías. Lectura pública; crear, editar y eliminar solo el administrador. */
// Controlador REST (responde JSON).
// Ruta base: /api/v1/categorias.
// CRUD REST completo: Create (POST), Read (GET), Update (PUT) y Delete (DELETE). Los permisos los define SecurityConfig.
@RestController
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "CRUD de categorías de productos")
public class CategoriaController {

    // Servicio inyectado por constructor.
    private final CategoriaService servicio;

    public CategoriaController(CategoriaService servicio) {
        this.servicio = servicio;
    }

    // Documentación Swagger (y las respuestas posibles).
    // GET /api/v1/categorias: público (cualquier visitante ve las categorías).
    // Devuelve la lista; Spring la serializa a un arreglo JSON.
    @Operation(summary = "Listar categorías", description = "Público. Devuelve todas las categorías ordenadas por id.")
    @ApiResponse(responseCode = "200", description = "Lista de categorías")
    @GetMapping
    public List<Categoria> listar() {
        // Delega en el servicio.
        return servicio.listar();
    }

    // GET /api/v1/categorias/2: una categoría.
    // @PathVariable: el 2 de la URL llega como parámetro id.
    @Operation(summary = "Obtener una categoría", description = "Público.")
    @ApiResponse(responseCode = "200", description = "Categoría encontrada")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @GetMapping("/{id}")
    public Categoria obtener(@PathVariable Integer id) {
        // 404 si no existe (lo lanza el servicio).
        return servicio.obtener(id);
    }

    // Swagger: este endpoint pide token.
    // POST /api/v1/categorias: crear (solo ADMIN).
    // Código 201 al crear.
    // El JSON se convierte en Categoria y @Valid aplica @NotBlank de nombre e imagen.
    @Operation(summary = "Crear categoría", description = "Solo administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "201", description = "Creada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "Sin token", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "Sin permiso", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria crear(@Valid @RequestBody Categoria datos) {
        // El servicio asigna el id y guarda.
        return servicio.crear(datos);
    }

    // PUT /api/v1/categorias/2: actualizar (solo ADMIN).
    // Combina el id de la URL con el JSON validado del cuerpo.
    @Operation(summary = "Actualizar categoría", description = "Solo administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PutMapping("/{id}")
    public Categoria actualizar(@PathVariable Integer id, @Valid @RequestBody Categoria datos) {
        // Delega la actualización.
        return servicio.actualizar(id, datos);
    }

    // DELETE /api/v1/categorias/2: eliminar (solo ADMIN).
    // Código 204: éxito sin cuerpo. El frontend (api.js) lo interpreta devolviendo null.
    // void porque no hay nada que devolver.
    @Operation(summary = "Eliminar categoría", description = "Solo administrador. No se puede eliminar si tiene productos.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Eliminada")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Tiene productos asociados", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        // 409 si la categoría aún tiene productos.
        servicio.eliminar(id);
    }
}
