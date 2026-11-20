package cl.duoc.pawpets.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS (material 3.2.2): el navegador bloquea las llamadas entre orígenes distintos
 * (frontend en :5173 y backend en :8080) salvo que el backend las autorice.
 * El origen permitido viene de la propiedad cors.origen (CORS_ORIGIN en .env).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String origen;

    public CorsConfig(@Value("${cors.origen}") String origen) {
        this.origen = origen;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(origen)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
