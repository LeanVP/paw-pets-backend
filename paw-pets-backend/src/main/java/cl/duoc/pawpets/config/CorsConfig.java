package cl.duoc.pawpets.config;

// @Value: inyecta en una variable el valor de una propiedad (application.properties / .env).
import org.springframework.beans.factory.annotation.Value;
// @Configuration: clase de configuración que Spring lee al arrancar.
import org.springframework.context.annotation.Configuration;
// CorsRegistry: el "registro" donde se declara qué rutas aceptan llamadas desde otros orígenes.
import org.springframework.web.servlet.config.annotation.CorsRegistry;
// WebMvcConfigurer: interfaz que permite personalizar Spring MVC (aquí, solo CORS).
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS (material 3.2.2): el navegador bloquea las llamadas entre orígenes distintos
 * (frontend en :5173 y backend en :8080) salvo que el backend las autorice.
 * El origen permitido viene de la propiedad cors.origen (CORS_ORIGIN en .env).
 */
// Spring ejecuta esta clase al iniciar y aplica su configuración a toda la API.
// Implementa WebMvcConfigurer para poder sobrescribir addCorsMappings.
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Guarda el origen permitido (por defecto http://localhost:5173, donde corre el frontend Vite).
    private final String origen;

    // INYECCIÓN POR CONSTRUCTOR: Spring le pasa el valor de cors.origen (application.properties → variable CORS_ORIGIN del .env).
    public CorsConfig(@Value("${cors.origen}") String origen) {
        // Se guarda el valor para usarlo en addCorsMappings.
        this.origen = origen;
    }

    // Sobrescribe el método de la interfaz.
    // Aquí se define la política CORS. Sin esto, el navegador bloquea las llamadas del puerto 5173 al 8080.
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Aplica la regla a TODAS las rutas que empiezan con /api/ (toda la API REST).
        registry.addMapping("/api/**")
                // Solo ese origen (el frontend) puede llamar a la API; cualquier otra web es rechazada por el navegador.
                .allowedOrigins(origen)
                // Verbos HTTP permitidos. OPTIONS es el "preflight": una consulta previa automática que hace el navegador.
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // Permite cualquier cabecera, en particular "Authorization" (el JWT) y "Content-Type".
                .allowedHeaders("*");
    }
}
