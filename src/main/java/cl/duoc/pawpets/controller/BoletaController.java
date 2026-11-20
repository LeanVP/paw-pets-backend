package cl.duoc.pawpets.controller;

import cl.duoc.pawpets.dto.BoletaRequest;
import cl.duoc.pawpets.exception.ErrorRespuesta;
import cl.duoc.pawpets.model.Boleta;
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
import org.springframework.web.bind.annotation.GetMapping;
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
@RestController
@RequestMapping("/api/v1/boletas")
@Tag(name = "Boletas", description = "Compras de la tienda (BOLETA y DETALLE_BOLETA)")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Sin token o token vencido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
public class BoletaController {

    private final BoletaService servicio;

    public BoletaController(BoletaService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Listar boletas", description = "Admin y vendedor ven todas; un cliente solo las suyas.")
    @ApiResponse(responseCode = "200", description = "Lista de boletas")
    @GetMapping
    public List<Boleta> listar(@AuthenticationPrincipal Jwt jwt) {
        return servicio.listar(usuarioId(jwt), jwt.getClaimAsString("rol"));
    }

    @Operation(summary = "Obtener una boleta con su detalle", description = "Un cliente solo puede ver sus propias boletas.")
    @ApiResponse(responseCode = "200", description = "Boleta encontrada")
    @ApiResponse(responseCode = "404", description = "No existe (o es de otro cliente)", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @GetMapping("/{id}")
    public Boleta obtener(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return servicio.obtener(id, usuarioId(jwt), jwt.getClaimAsString("rol"));
    }

    @Operation(summary = "Comprar", description = "Cliente o admin. Envía solo productoId y cantidad: el servidor calcula precios, "
            + "valida y descuenta stock. Con el medio de pago 'Tarjeta sin fondos (simula error)' la boleta queda Rechazada.")
    @ApiResponse(responseCode = "201", description = "Boleta creada (Pagada o Rechazada)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o producto inexistente", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "403", description = "El vendedor no puede comprar", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Stock insuficiente", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Boleta crear(@Valid @RequestBody BoletaRequest datos, @AuthenticationPrincipal Jwt jwt) {
        return servicio.crear(datos, usuarioId(jwt));
    }

    private static Integer usuarioId(Jwt jwt) {
        return Integer.valueOf(jwt.getSubject());
    }
}
