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
 * Pruebas de integración del servicio ListarProductosStockBajoUseCase (CU-003).
 *
 * El servicio retorna los productos cuyo stock_actual está por debajo del
 * stock_minimo, lo que permite al supervisor de inventario identificar
 * productos que requieren reabastecimiento.
 *
 * En la BD inicial, el producto "Lentejas" (id=2) tiene stockActual=4 y
 * stockMinimo=5, por lo que debe aparecer en el listado.
 */
@DisplayName("Pruebas del servicio ListarProductosStockBajoUseCase")
class ListarProductosStockBajoUseCaseTest {

    private ListarProductosStockBajoUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        ProductoRepository productoRepository = new H2ProductoRepository(conn);
        useCase = new ListarProductosStockBajoUseCase(productoRepository);
    }

    /*
     * CP-015: Verifica el flujo principal del UseCase. Cuando hay al menos
     * un producto con stock por debajo del mínimo en la BD, el método debe
     * retornar una lista no vacía. La BD precarga "Lentejas" con stock=4
     * y mínimo=5, por lo que siempre debe aparecer al menos un resultado.
     */
    @Test
    @DisplayName("CP-015: Listar productos con stock bajo debe retornar resultados")
    void listarStockBajo_conProductosBajoMinimo_debeRetornarLista() {
        List<Producto> productos = useCase.ejecutar();

        assertNotNull(productos, "La lista no debe ser null");
        assertFalse(productos.isEmpty(),
                "Debe haber al menos un producto con stock bajo (Lentejas: stock=4, min=5)");
    }

    /*
     * CP-016: Verifica que todos los productos retornados realmente cumplan
     * la condición de tener stock_actual < stock_minimo. Esta prueba valida
     * la integridad del filtro aplicado por el repositorio.
     */
    @Test
    @DisplayName("CP-016: Todos los productos retornados deben tener stock < mínimo")
    void listarStockBajo_resultadosDebenCumplirCondicion() {
        List<Producto> productos = useCase.ejecutar();

        productos.forEach(p ->
                assertTrue(p.getStockActual() < p.getStockMinimo(),
                        "El producto " + p.getNombre() + " debe tener stock < mínimo " +
                        "(stock=" + p.getStockActual() + ", mínimo=" + p.getStockMinimo() + ")")
        );
    }
}