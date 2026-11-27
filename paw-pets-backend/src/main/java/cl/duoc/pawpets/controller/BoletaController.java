package cl.duoc.pawpets.controller;

import cl.duoc.pawpets.dto.BoletaRequest;
import cl.duoc.pawpets.exception.ErrorRespuesta;
import cl.duoc.pawpets.model.Boleta;
// Servicio con la lógica de compra.
import cl.duoc.pawpets.service.BoletaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
// @GetMapping: método que atiende GET (consultas).
import org.springframework.web.bind.annotation.GetMapping;
// @PathVariable: toma un valor de la URL (/boletas/{id}).
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Compras (boletas). Se crean y se consultan; una boleta emitida no se edita ni se elimina,
 * por eso no hay PUT ni DELETE. El usuario se obtiene del JWT (no del cuerpo de la petición).
 */
// Controlador REST: devuelve JSON.
// Ruta base de las compras.
// Agrupa en Swagger.
// Todos estos endpoints requieren token (candado en Swagger).
// Documenta en Swagger el 401 común a todos los métodos de la clase.
// Compras: se crean y se consultan. NO hay PUT ni DELETE porque una boleta emitida no se modifica ni se borra (integridad contable).
@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Boletas", description = "Compras de la tienda (BOLETA y DETALLE_BOLETA)")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Sin token o token vencido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
public class BoletaController {

    // Servicio inyectado.
    private final BoletaService servicio;

    // Inyección por constructor.
    public BoletaController(BoletaService servicio) {
        this.servicio = servicio;
    }

    // Documentación Swagger.
    // Endpoint: GET /api/v1/boletas.
    // El JWT identifica al usuario que consulta.
    @Operation(summary = "Listar boletas", description = "Admin y vendedor ven todas; un cliente solo las suyas.")
    @ApiResponse(responseCode = "200", description = "Lista de boletas")
    @GetMapping
    public List<Boleta> listar(@AuthenticationPrincipal Jwt jwt) {
        // Pasa id y rol (leídos del token firmado) al servicio, que filtra: el cliente solo recibe sus boletas.
        return servicio.listar(usuarioId(jwt), jwt.getClaimAsString("rol"));
    }

    // Documentación Swagger.
    // Endpoint: GET /api/v1/boletas/3 (el {id} es variable).
    // @PathVariable lee el 3 de la URL; spring lo convierte a Integer (si no es número, GlobalExceptionHandler responde 400).
    @Operation(summary = "Obtener una boleta con su detalle", description = "Un cliente solo puede ver sus propias boletas.")
    @ApiResponse(responseCode = "200", description = "Boleta encontrada")
    @ApiResponse(responseCode = "404", description = "No existe (o es de otro cliente)", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @GetMapping("/{id}")
    public Boleta obtener(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        // El servicio verifica que un cliente solo vea una boleta propia.
        return servicio.obtener(id, usuarioId(jwt), jwt.getClaimAsString("rol"));
    }

    // Documentación Swagger.
    @Operation(summary = "Comprar", description = "Cliente o admin. Envía solo productoId y cantidad: el servidor calcula precios, "
            + "valida y descuenta stock. Con el medio de pago 'Tarjeta sin fondos (simula error)' la boleta queda Rechazada.")
    // Endpoint: POST /api/v1/boletas (comprar). SecurityConfig lo limita a CLIENTE y ADMIN.
    // Respuesta exitosa = 201 Created.
    // Recibe el checkout (BoletaRequest, ya validado) y el JWT del comprador.
    @ApiResponse(responseCode = "201", description = "Boleta creada (Pagada o Rechazada)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o producto inexistente", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "El vendedor no puede comprar", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Boleta crear(@Valid @RequestBody BoletaRequest datos, @AuthenticationPrincipal Jwt jwt) {
        // El id del cliente se toma del TOKEN, no del cuerpo: nadie puede comprar a nombre de otro.
        return servicio.crear(datos, usuarioId(jwt));
    }

    // Auxiliar que extrae el id del usuario desde el claim "sub".
    private static Integer usuarioId(Jwt jwt) {
        // El "sub" es texto; se convierte a Integer.
        return Integer.valueOf(jwt.getSubject());
    }
}
