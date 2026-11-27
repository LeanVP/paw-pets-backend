// PAQUETE RAÍZ: Spring escanea este paquete y todos sus subpaquetes (controller, service, repository...)
// buscando clases con anotaciones (@RestController, @Service, @Component...) para crearlas automáticamente.
package cl.duoc.pawpets;

// SpringApplication: clase de Spring Boot que arranca la aplicación (servidor Tomcat embebido + contexto de Spring).
import org.springframework.boot.SpringApplication;
// @SpringBootApplication: anotación que agrupa @Configuration + @EnableAutoConfiguration + @ComponentScan.
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Punto de entrada: Spring inicia el servidor HTTP y detecta los componentes del paquete. */
// ANOTACIÓN @SpringBootApplication: activa la autoconfiguración (Mongo, Security, Web MVC según el pom.xml)
// y el escaneo de componentes. Por eso no hay un XML de configuración: todo se deduce de las dependencias.
// Clase de arranque. No tiene lógica de negocio: solo enciende el sistema.
@SpringBootApplication
public class Application {

    // main: el punto de entrada de Java; lo ejecuta "mvnw spring-boot:run" o el botón Run del IDE.
    public static void main(String[] args) {
        // Levanta Tomcat en el puerto 8080 (application.properties), conecta a MongoDB y registra todos los beans.
        SpringApplication.run(Application.class, args);
    }
}
