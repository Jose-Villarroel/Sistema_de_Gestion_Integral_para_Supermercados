package services.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DashboardRepository;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2DashboardRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio ConsultarDashboardUseCase (CU-013).
 *
 * Datos iniciales relevantes (cargados por DatabaseInitializer):
 *   - Ventas del día (hoy): venta 5 ($29.750 EFECTIVO) + venta 6 ($71.400 TARJETA)
 *     → Total día: $101.150, 2 transacciones
 *   - Productos con stock crítico (stock_actual < stock_minimo):
 *     * Lentejas (id=2): stock=4, mínimo=5
 *   - Top productos vendidos (todos los detalles cargados):
 *     * Producto 1 (Arroz): 5+10+4+8 = 27 unidades → líder
 *     * Producto 3 (Leche): 3+6 = 9 unidades
 *     * Producto 4 (Gaseosa): 4+2 = 6 unidades
 *     * Producto 8 (Queso): 5 unidades
 *     * Producto 11 (Agua): 3 unidades
 */
@DisplayName("Pruebas del servicio ConsultarDashboardUseCase")
class ConsultarDashboardUseCaseTest {

    private ConsultarDashboardUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        DashboardRepository repository = new H2DashboardRepository(conn);
        useCase = new ConsultarDashboardUseCase(repository);
    }

    /*
     * CP-001: Verifica que el dashboard retorne las ventas del día actual.
     * En la BD hay 2 ventas con fecha = hoy: $29.750 + $71.400 = $101.150.
     */
    @Test
    @DisplayName("CP-001: Dashboard debe calcular las ventas del día actual")
    void ejecutar_debeRetornarVentasDelDia() {
        DashboardGerencial dashboard = useCase.ejecutar();

        assertNotNull(dashboard);
        assertEquals(LocalDate.now(), dashboard.fecha());
        assertEquals(0, dashboard.ventasDelDia().compareTo(new BigDecimal("101150.00")),
                "Las ventas del día deben sumar $29.750 + $71.400");
    }

    /*
     * CP-002: Verifica el número de transacciones del día. La BD tiene
     * 2 ventas con fecha de hoy (ventas 5 y 6).
     */
    @Test
    @DisplayName("CP-002: Dashboard debe contar las transacciones del día actual")
    void ejecutar_debeContarTransaccionesDelDia() {
        DashboardGerencial dashboard = useCase.ejecutar();

        assertEquals(2, dashboard.transaccionesDelDia(),
                "Deben contarse las 2 ventas del día actual");
    }

    /*
     * CP-003: Verifica el conteo de productos con stock crítico. En la BD
     * solo el producto 2 (Lentejas: stock=4, mínimo=5) cumple la condición
     * stock_actual < stock_minimo entre los productos activos.
     */
    @Test
    @DisplayName("CP-003: Dashboard debe identificar productos con stock crítico")
    void ejecutar_debeContarProductosConStockCritico() {
        DashboardGerencial dashboard = useCase.ejecutar();

        assertEquals(1, dashboard.productosConStockCritico(),
                "Solo Lentejas (stock=4, mínimo=5) debe contar como crítico");
    }

    /*
     * CP-004: Verifica que el dashboard incluya el top de productos vendidos
     * ordenado por cantidad descendente. El producto más vendido en la BD
     * es el id=1 (Arroz) con 27 unidades.
     */
    @Test
    @DisplayName("CP-004: Dashboard debe incluir top productos ordenados por cantidad")
    void ejecutar_debeIncluirTopProductosOrdenados() {
        DashboardGerencial dashboard = useCase.ejecutar();

        assertFalse(dashboard.topProductosVendidos().isEmpty(),
                "Debe haber al menos un producto en el top");

        ProductoVendido primero = dashboard.topProductosVendidos().get(0);
        assertEquals(1, primero.idProducto(),
                "El producto más vendido debe ser el id=1 (Arroz)");
        assertEquals(27, primero.cantidadVendida(),
                "Arroz debe tener 27 unidades vendidas en total");
    }

    /*
     * CP-005: Verifica que el top no exceda los 5 productos. Aunque hubiera
     * más productos con ventas, el dashboard limita el ranking a 5 elementos.
     */
    @Test
    @DisplayName("CP-005: Top productos no debe exceder los 5 elementos")
    void ejecutar_topProductosDebeLimitarseA5() {
        DashboardGerencial dashboard = useCase.ejecutar();

        assertTrue(dashboard.topProductosVendidos().size() <= 5,
                "El top de productos debe tener máximo 5 entradas");
    }

    /*
     * CP-006: Verifica que el dashboard se construya con todos sus campos
     * inicializados (ningún null). Esto es importante porque el Controller
     * va a poblar la UI con todos los datos al mismo tiempo.
     */
    @Test
    @DisplayName("CP-006: Dashboard debe retornarse con todos los campos inicializados")
    void ejecutar_debeRetornarDashboardCompleto() {
        DashboardGerencial dashboard = useCase.ejecutar();

        assertNotNull(dashboard.fecha(), "La fecha no debe ser null");
        assertNotNull(dashboard.ventasDelDia(), "Las ventas del día no deben ser null");
        assertNotNull(dashboard.topProductosVendidos(),
                "La lista de top productos no debe ser null");
        assertTrue(dashboard.ventasDelDia().signum() >= 0,
                "Las ventas del día no pueden ser negativas");
        assertTrue(dashboard.productosConStockCritico() >= 0,
                "El conteo de stock crítico no puede ser negativo");
    }
}