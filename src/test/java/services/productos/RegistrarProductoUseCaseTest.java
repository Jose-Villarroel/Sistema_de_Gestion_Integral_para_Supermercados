package services.productos;

import entities.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ProductoRepository;
import repositories.ProductoRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio RegistrarProductoUseCase (CU-003).
 *
 * Estas pruebas usan la base de datos H2 real (no mocks). Cada prueba parte
 * de un estado conocido cargado por DatabaseInitializer desde el script
 * BD-fundamentos.sql.
 */
@DisplayName("Pruebas del servicio RegistrarProductoUseCase")
class RegistrarProductoUseCaseTest {

    private RegistrarProductoUseCase useCase;
    private ProductoRepository productoRepository;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Resetear la base de datos al estado conocido
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        // 2. Inyectar dependencias reales
        productoRepository = new H2ProductoRepository(conn);
        useCase = new RegistrarProductoUseCase(productoRepository);
    }

    /*
     * CP-001: Verifica el flujo principal del CU-003. Cuando se reciben datos
     * válidos para un producto que no existe previamente, el servicio debe
     * crear el producto y persistirlo en la base de datos asignándole un id.
     */
    @Test
    @DisplayName("CP-001: Registrar producto con datos válidos debe persistirlo")
    void registrarProducto_conDatosValidos_debePersistirEnBD() {
        Producto resultado = useCase.ejecutar(
                "Producto Nuevo Prueba",
                "Descripción de prueba",
                "Marca Test",
                2000.0,
                3500.0,
                10,
                3,
                1,
                true
        );

        assertNotNull(resultado, "El producto registrado no debe ser null");
        assertTrue(resultado.getId() > 0, "El producto debe tener un id asignado");
        assertEquals("Producto Nuevo Prueba", resultado.getNombre());
        assertTrue(productoRepository.existeNombre("Producto Nuevo Prueba"),
                "El producto debe quedar persistido en la BD");
    }

    /*
     * CP-002: Verifica la primera validación del UseCase. Cuando el nombre es
     * null, debe lanzar IllegalArgumentException con un mensaje específico,
     * sin tocar la base de datos.
     */
    @Test
    @DisplayName("CP-002: Registrar producto con nombre null debe lanzar excepción")
    void registrarProducto_conNombreNull_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(
                        null,
                        "Descripción",
                        "Marca",
                        2000.0,
                        3500.0,
                        10,
                        3,
                        1,
                        true
                )
        );

        assertTrue(ex.getMessage().contains("nombre"),
                "El mensaje debe mencionar que el nombre es obligatorio");
    }

    /*
     * CP-003: Verifica la segunda validación del UseCase. Cuando se intenta
     * registrar un producto cuyo nombre ya existe en la BD, debe lanzar
     * IllegalArgumentException sin crear duplicados. La BD precarga "Arroz"
     * desde BD-fundamentos.sql, lo usamos como caso de colisión.
     */
    @Test
    @DisplayName("CP-003: Registrar producto con nombre duplicado debe lanzar excepción")
    void registrarProducto_conNombreDuplicado_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(
                        "Arroz",
                        "Otro arroz cualquiera",
                        "Otra marca",
                        2500.0,
                        4000.0,
                        5,
                        2,
                        1,
                        true
                )
        );

        assertTrue(ex.getMessage().contains("Arroz"),
                "El mensaje debe mencionar el nombre duplicado");
    }
}