package cl.duoc.pawpets.config;

// Modelos: tipos de los datos que se leerán desde los JSON iniciales.
import cl.duoc.pawpets.model.Categoria;
import cl.duoc.pawpets.model.Producto;
import cl.duoc.pawpets.model.Usuario;
// Repositorios: se usan para contar (count) y guardar (saveAll) en MongoDB.
import cl.duoc.pawpets.repository.CategoriaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
import cl.duoc.pawpets.repository.UsuarioRepository;
// Excepción que puede lanzar la lectura de archivos.
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
// SLF4J: sistema de logs. Permite escribir mensajes informativos en la consola del servidor.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// CommandLineRunner: interfaz de Spring Boot cuyo método run() se ejecuta UNA VEZ justo después de arrancar.
import org.springframework.boot.CommandLineRunner;
// ClassPathResource: lee archivos que están dentro del proyecto (carpeta src/main/resources).
import org.springframework.core.io.ClassPathResource;
// PasswordEncoder: interfaz que cifra contraseñas (aquí la implementa BCrypt, definido en SecurityConfig).
import org.springframework.security.crypto.password.PasswordEncoder;
// @Component: Spring crea y administra esta clase automáticamente.
import org.springframework.stereotype.Component;
// TypeReference: le dice a Jackson qué tipo genérico construir (List<Categoria>) al leer el JSON.
import tools.jackson.core.type.TypeReference;
// ObjectMapper (Jackson): convierte JSON ↔ objetos Java. Es el mismo que usa Spring para las respuestas de la API.
import tools.jackson.databind.ObjectMapper;

/**
 * Carga los datos iniciales de src/main/resources/data/*.json al arrancar,
 * SOLO si la colección está vacía (no borra ni duplica datos existentes).
 * Las contraseñas de usuarios.json vienen en texto plano y aquí se guardan cifradas con BCrypt.
 */
// Spring instancia esta clase al iniciar y, como es un CommandLineRunner, ejecuta su run().
// "Seeder" = sembrador de datos iniciales: deja la base lista con categorías, productos y usuarios de ejemplo.
@Component
public class DataSeeder implements CommandLineRunner {

    // Logger para registrar en consola qué se cargó.
    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    // Dependencias que necesita (todas se las entrega Spring por el constructor = inyección de dependencias).
    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    // Para cifrar con BCrypt las contraseñas del JSON antes de guardarlas.
    private final PasswordEncoder cifrador;
    private final ObjectMapper json;

    // CONSTRUCTOR con inyección de dependencias: Spring busca cada bean pedido y lo entrega aquí.
    public DataSeeder(CategoriaRepository categorias, ProductoRepository productos,
                      UsuarioRepository usuarios, PasswordEncoder cifrador, ObjectMapper json) {
        // Asigna las dependencias recibidas a los campos de la clase.
        this.categorias = categorias;
        this.productos = productos;
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.json = json;
    }

    // Se ejecuta automáticamente al terminar de arrancar la aplicación.
    @Override
    public void run(String... args) throws IOException {
        // Regla de idempotencia: solo carga si la colección está VACÍA, así nunca duplica ni pisa datos existentes.
        if (categorias.count() == 0) {
            // Lee el JSON, lo convierte en List<Categoria> y lo inserta de una vez en MongoDB.
            categorias.saveAll(leer("data/categorias.json", new TypeReference<List<Categoria>>() { }));
            // Deja constancia en la consola.
            log.info("Categorías iniciales cargadas en MongoDB");
        }
        // Mismo criterio para los productos.
        if (productos.count() == 0) {
            productos.saveAll(leer("data/productos.json", new TypeReference<List<Producto>>() { }));
            log.info("Productos iniciales cargados en MongoDB");
        }
        // Mismo criterio para los usuarios.
        if (usuarios.count() == 0) {
            // Lee los usuarios; sus contraseñas vienen en texto plano en el JSON.
            List<Usuario> iniciales = leer("data/usuarios.json", new TypeReference<List<Usuario>>() { });
            // Antes de guardar, reemplaza cada contraseña por su hash BCrypt (con sal): en la BD nunca queda texto plano.
            iniciales.forEach(u -> u.setClave(cifrador.encode(u.getClave())));
            // Inserta los usuarios ya con la clave cifrada.
            usuarios.saveAll(iniciales);
            log.info("Usuarios iniciales cargados en MongoDB");
        }
    }

    // Método genérico auxiliar: lee un JSON del classpath y lo convierte al tipo T pedido.
    private <T> T leer(String ruta, TypeReference<T> tipo) throws IOException {
        // try-with-resources: abre el archivo y lo cierra solo al terminar (aunque haya error).
        try (InputStream entrada = new ClassPathResource(ruta).getInputStream()) {
            // Jackson transforma el JSON en objetos Java (deserialización).
            return json.readValue(entrada, tipo);
        }
    }
}
