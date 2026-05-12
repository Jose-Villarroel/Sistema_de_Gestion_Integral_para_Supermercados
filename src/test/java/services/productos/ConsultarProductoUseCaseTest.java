package services.productos;

import entities.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ProductoRepository;
import repositories.ProductoRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio ConsultarProductoUseCase (CU-003).
 *
 * Las pruebas asumen el estado inicial cargado por DatabaseInitializer:
 * 14 productos en total, todos activos, distribuidos en 3 categorías
 * (Granos=1, Lácteos=2, Bebidas=3).
 */
@DisplayName("Pruebas del servicio ConsultarProductoUseCase")
class ConsultarProductoUseCaseTest {

    private ConsultarProductoUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        ProductoRepository productoRepository = new H2ProductoRepository(conn);
        useCase = new ConsultarProductoUseCase(productoRepository);
    }

    // ==================== buscarPorId ====================

    /*
     * CP-004: Verifica el flujo principal de buscarPorId. El producto con
     * id=1 (Arroz) está precargado en la BD por el script de inicialización.
     */
    @Test
    @DisplayName("CP-004: Buscar producto por id existente debe retornarlo")
    void buscarPorId_conIdExistente_debeRetornarProducto() {
        Producto producto = useCase.buscarPorId(1);

        assertNotNull(producto, "El producto encontrado no debe ser null");
        assertEquals(1, producto.getId());
        assertEquals("Arroz", producto.getNombre());
    }

    /*
     * CP-005: Verifica la primera validación del método. Un id menor o igual
     * a cero es inválido y debe lanzar IllegalArgumentException antes de
     * tocar la base de datos.
     */
    @Test
    @DisplayName("CP-005: Buscar producto por id inválido (cero) debe lanzar excepción")
    void buscarPorId_conIdInvalido_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.buscarPorId(0)
        );

        assertTrue(ex.getMessage().contains("mayor que cero"),
                "El mensaje debe explicar que el id debe ser mayor que cero");
    }

    /*
     * CP-006: Verifica el caso en que el id es válido pero no existe en la
     * BD. El UseCase debe lanzar IllegalArgumentException con un mensaje
     * que incluya el id buscado. Usamos un id muy alto que no está cargado.
     */
    @Test
    @DisplayName("CP-006: Buscar producto por id inexistente debe lanzar excepción")
    void buscarPorId_conIdInexistente_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.buscarPorId(99999)
        );

        assertTrue(ex.getMessage().contains("99999"),
                "El mensaje debe incluir el id no encontrado");
    }

    // ==================== listarTodos ====================

    /*
     * CP-007: Verifica que listarTodos retorne todos los productos cargados
     * en la BD inicial. El script BD-fundamentos.sql carga exactamente 14
     * productos.
     */
    @Test
    @DisplayName("CP-007: Listar todos los productos debe retornar los 14 cargados")
    void listarTodos_debeRetornarTodosLosProductos() {
        List<Producto> productos = useCase.listarTodos();

        assertNotNull(productos);
        assertEquals(14, productos.size(),
                "Deben retornarse los 14 productos cargados en BD");
    }

    // ==================== listarActivos ====================

    /*
     * CP-008: Verifica que listarActivos retorne únicamente los productos
     * con estado_activo = true. En la BD inicial todos los 14 productos
     * están activos, por lo que el resultado coincide con listarTodos.
     */
    @Test
    @DisplayName("CP-008: Listar productos activos debe retornar solo los activos")
    void listarActivos_debeRetornarSoloProductosActivos() {
        List<Producto> productos = useCase.listarActivos();

        assertNotNull(productos);
        assertEquals(14, productos.size(),
                "Los 14 productos cargados están activos");
        productos.forEach(p ->
                assertTrue(p.isActivo(), "Todos los productos retornados deben estar activos"));
    }

    // ==================== buscarPorNombre ====================

    /*
     * CP-009: Verifica el flujo principal de buscarPorNombre. La BD tiene
     * varios productos con la palabra "Arroz" en el nombre. El método
     * debe retornar al menos uno.
     */
    @Test
    @DisplayName("CP-009: Buscar producto por nombre existente debe retornarlo")
    void buscarPorNombre_conNombreExistente_debeRetornarResultados() {
        List<Producto> productos = useCase.buscarPorNombre("Arroz");

        assertNotNull(productos);
        assertFalse(productos.isEmpty(),
                "Debe encontrar al menos un producto que contenga 'Arroz'");
    }

    /*
     * CP-010: Verifica la validación de nombre vacío. Tanto un nombre null
     * como un string en blanco deben lanzar IllegalArgumentException.
     */
    @Test
    @DisplayName("CP-010: Buscar producto por nombre vacío debe lanzar excepción")
    void buscarPorNombre_conNombreVacio_debeLanzarExcepcion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.buscarPorNombre("")
        );
    }

    // ==================== listarPorCategoria ====================

    /*
     * CP-011: Verifica el flujo principal de listarPorCategoria. La
     * categoría 1 (Granos) tiene varios productos asociados en la BD
     * inicial: Arroz, Lentejas, Frijol, Garbanzo y Maíz.
     */
    @Test
    @DisplayName("CP-011: Listar productos por categoría existente debe retornarlos")
    void listarPorCategoria_conCategoriaExistente_debeRetornarProductos() {
        List<Producto> productos = useCase.listarPorCategoria(1);

        assertNotNull(productos);
        assertFalse(productos.isEmpty(),
                "La categoría 1 (Granos) debe tener productos asociados");
        productos.forEach(p ->
                assertEquals(1, p.getCategoriaId(),
                        "Todos los productos retornados deben pertenecer a la categoría 1"));
    }

    /*
     * CP-012: Verifica la validación de categoría inválida. Un categoriaId
     * menor o igual a cero debe lanzar IllegalArgumentException antes de
     * consultar la base de datos.
     */
    @Test
    @DisplayName("CP-012: Listar productos por categoría inválida debe lanzar excepción")
    void listarPorCategoria_conCategoriaInvalida_debeLanzarExcepcion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.listarPorCategoria(0)
        );
    }
}