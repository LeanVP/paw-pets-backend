package cl.duoc.pawpets.controller;

import cl.duoc.pawpets.exception.ErrorRespuesta;
import cl.duoc.pawpets.model.Producto;
import cl.duoc.pawpets.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
// Anotaciones que enlazan cada método con su verbo HTTP (DELETE, GET, POST, PUT) y leen datos de la URL/cuerpo.
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CRUD de productos. Lectura pública (tienda); crear, editar y eliminar solo el administrador. */
// Controlador REST (responde JSON).
// Ruta base: /api/v1/productos.
// CRUD REST de productos. El controlador es "delgado": recibe, valida y delega; la lógica está en ProductoService.
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "CRUD del catálogo de productos")
public class ProductoController {

    // Servicio inyectado por constructor.
    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    // GET /api/v1/productos: lista pública del catálogo (la usa la tienda sin sesión).
    // Devuelve todos los productos como arreglo JSON.
    @Operation(summary = "Listar productos", description = "Público. Devuelve el catálogo completo ordenado por id.")
    @ApiResponse(responseCode = "200", description = "Lista de productos")
    @GetMapping
    public List<Producto> listar() {
        // Delega en el servicio.
        return servicio.listar();
    }

    // GET /api/v1/productos/5: detalle de un producto.
    // El 5 de la URL llega en id.
    @Operation(summary = "Obtener un producto", description = "Público.")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Integer id) {
        // 404 si no existe.
        return servicio.obtener(id);
    }

    // POST /api/v1/productos: crear (solo ADMIN).
    // 201 Created.
    // JSON → Producto, validado con las anotaciones del modelo (@NotBlank, @Min...). Si falla, 400.
    @Operation(summary = "Crear producto", description = "Solo administrador. La categoría debe existir y la oferta debe ser menor al precio.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "201", description = "Creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "Sin token", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "Sin permiso", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@Valid @RequestBody Producto datos) {
        // El servicio aplica reglas de negocio, asigna id y guarda.
        return servicio.crear(datos);
    }

    // PUT /api/v1/productos/5: actualizar (solo ADMIN).
    // id de la URL + cuerpo validado.
    @Operation(summary = "Actualizar producto", description = "Solo administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Integer id, @Valid @RequestBody Producto datos) {
        // Delega la actualización.
        return servicio.actualizar(id, datos);
    }

    // DELETE /api/v1/productos/5: eliminar (solo ADMIN).
    // 204 sin cuerpo.
    // No devuelve nada.
    @Operation(summary = "Eliminar producto", description = "Solo administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        // Borra el producto (404 si no existe).
        servicio.eliminar(id);
    }
}
