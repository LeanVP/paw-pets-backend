package cl.duoc.pawpets.controller;

import cl.duoc.pawpets.dto.UsuarioRequest;
import cl.duoc.pawpets.dto.UsuarioResponse;
import cl.duoc.pawpets.exception.ErrorRespuesta;
import cl.duoc.pawpets.service.UsuarioService;
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

/** CRUD de usuarios. Todo el recurso es exclusivo del administrador. */
@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestión de cuentas y roles (solo administrador)")
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Sin token", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
@ApiResponse(responseCode = "403", description = "No es administrador", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
public class UsuarioController {

    private final UsuarioService servicio;

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @Operation(summary = "Listar usuarios", description = "Nunca incluye la contraseña.")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios")
    @GetMapping
    public List<UsuarioResponse> listar() {
        return servicio.listar();
    }

    @Operation(summary = "Obtener un usuario")
    @ApiResponse(responseCode = "200", description = "Usuario encontrado")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Integer id) {
        return servicio.obtener(id);
    }

    @Operation(summary = "Crear usuario", description = "Permite elegir el rol (admin, vendedor o cliente). La clave se guarda cifrada.")
    @ApiResponse(responseCode = "201", description = "Creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Correo repetido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(@Valid @RequestBody UsuarioRequest datos) {
        return servicio.crear(datos);
    }

    @Operation(summary = "Actualizar usuario", description = "Si la clave viene vacía se mantiene la actual.")
    @ApiResponse(responseCode = "200", description = "Actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @ApiResponse(responseCode = "409", description = "Correo repetido", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Integer id, @Valid @RequestBody UsuarioRequest datos) {
        return servicio.actualizar(id, datos);
    }

    @Operation(summary = "Eliminar usuario")
    @ApiResponse(responseCode = "204", description = "Eliminado")
    @ApiResponse(responseCode = "404", description = "No existe", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Integer id) {
        servicio.eliminar(id);
    }
}
