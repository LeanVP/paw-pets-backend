package cl.duoc.pawpets.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger (springdoc-openapi, material 3.1.1):
 * título de la documentación y el esquema "bearerAuth", que agrega el botón
 * "Authorize" para pegar el JWT y probar los endpoints protegidos.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API PawPets",
        description = "API REST de la tienda PawPets: catálogo, usuarios, compras y autenticación con JWT. "
                + "Para probar los endpoints protegidos: POST /api/v1/auth/login, copia el token y pégalo en Authorize.",
        version = "v1"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
