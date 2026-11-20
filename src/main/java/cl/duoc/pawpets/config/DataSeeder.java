package cl.duoc.pawpets.config;

import cl.duoc.pawpets.model.Categoria;
import cl.duoc.pawpets.model.Producto;
import cl.duoc.pawpets.model.Usuario;
import cl.duoc.pawpets.repository.CategoriaRepository;
import cl.duoc.pawpets.repository.ProductoRepository;
import cl.duoc.pawpets.repository.UsuarioRepository;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Carga los datos iniciales de src/main/resources/data/*.json al arrancar,
 * SOLO si la colección está vacía (no borra ni duplica datos existentes).
 * Las contraseñas de usuarios.json vienen en texto plano y aquí se guardan cifradas con BCrypt.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final CategoriaRepository categorias;
    private final ProductoRepository productos;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder cifrador;
    private final ObjectMapper json;

    public DataSeeder(CategoriaRepository categorias, ProductoRepository productos,
                      UsuarioRepository usuarios, PasswordEncoder cifrador, ObjectMapper json) {
        this.categorias = categorias;
        this.productos = productos;
        this.usuarios = usuarios;
        this.cifrador = cifrador;
        this.json = json;
    }

    @Override
    public void run(String... args) throws IOException {
        if (categorias.count() == 0) {
            categorias.saveAll(leer("data/categorias.json", new TypeReference<List<Categoria>>() { }));
            log.info("Categorías iniciales cargadas en MongoDB");
        }
        if (productos.count() == 0) {
            productos.saveAll(leer("data/productos.json", new TypeReference<List<Producto>>() { }));
            log.info("Productos iniciales cargados en MongoDB");
        }
        if (usuarios.count() == 0) {
            List<Usuario> iniciales = leer("data/usuarios.json", new TypeReference<List<Usuario>>() { });
            iniciales.forEach(u -> u.setClave(cifrador.encode(u.getClave())));
            usuarios.saveAll(iniciales);
            log.info("Usuarios iniciales cargados en MongoDB");
        }
    }

    private <T> T leer(String ruta, TypeReference<T> tipo) throws IOException {
        try (InputStream entrada = new ClassPathResource(ruta).getInputStream()) {
            return json.readValue(entrada, tipo);
        }
    }
}
