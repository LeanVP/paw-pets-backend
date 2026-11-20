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
@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "CRUD del catálogo de productos")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Listar productos", description = "Público. Devuelve el catálogo completo ordenado por id.")
    @ApiResponse(responseCode = "200", description = "Lista de productos")
    @GetMapping
    public List<Producto> listar() {
        return servicio.listar();
    }

    @Operation(summary = "Obtener un producto", description = "Público.")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @GetMapping("/{id}")
    public Producto obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @Operation(summary = "Crear producto", description = "Solo administrador. La categoría debe existir y la oferta debe ser menor al precio.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "201", description = "Creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "401", description = "Sin token", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "Sin permiso", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@Valid @RequestBody Producto datos) {
        return servicio.crear(datos);
    }

    @Operation(summary = "Actualizar producto", description = "Solo administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PutMapping("/{id}")
    public Producto actualizar(@PathVariable Integer id, @Valid @RequestBody Producto datos) {
        return servicio.actualizar(id, datos);
    }

    @Operation(summary = "Eliminar producto", description = "Solo administrador.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }
}
