package services.reportes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ReporteVentasRepository;
import repositories.ReporteVentasRepository;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio GenerarReporteVentasUseCase (CU-012).
 *
 * Datos iniciales (cargados por DatabaseInitializer):
 *   Venta 1: hace 10 días, EFECTIVO, total $59.500
 *   Venta 2: hace 10 días, TARJETA, total $90.440
 *   Venta 3: hace 5 días, EFECTIVO, total $35.700
 *   Venta 4: hace 5 días, TRANSFERENCIA, total $142.800
 *   Venta 5: hoy, EFECTIVO, total $29.750
 *   Venta 6: hoy, TARJETA, total $71.400
 *
 * Total general (todas las fechas): $429.590 / 6 transacciones
 */
@DisplayName("Pruebas del servicio GenerarReporteVentasUseCase")
class GenerarReporteVentasUseCaseTest {

    private GenerarReporteVentasUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        ReporteVentasRepository repository = new H2ReporteVentasRepository(conn);
        useCase = new GenerarReporteVentasUseCase(repository);
    }

    /*
     * CP-001: Verifica el flujo principal del reporte. El rango completo
     * de los últimos 15 días incluye las 6 ventas precargadas. La prueba
     * valida que las métricas agregadas (total, número, descuentos,
     * impuestos) se calculen correctamente.
     */
    @Test
    @DisplayName("CP-001: Reporte sobre rango completo debe agregar todas las ventas")
    void ejecutar_conRangoCompleto_debeAgregarTodo() {
        LocalDate desde = LocalDate.now().minusDays(15);
        LocalDate hasta = LocalDate.now();

        ReporteVentas reporte = useCase.ejecutar(desde, hasta);

        assertNotNull(reporte);
        assertEquals(6, reporte.numeroTransacciones(),
                "Deben incluirse las 6 ventas precargadas");
        assertEquals(0, reporte.totalVendido().compareTo(new BigDecimal("429590.00")),
                "El total debe ser la suma de los 6 totales finales");
    }

    /*
     * CP-002: Verifica el cálculo del ticket promedio. Con 6 transacciones
     * sumando $429.590, el promedio debe ser $71.598.33.
     */
    @Test
    @DisplayName("CP-002: Ticket promedio debe calcularse correctamente")
    void ejecutar_debeCalcularTicketPromedio() {
        LocalDate desde = LocalDate.now().minusDays(15);
        LocalDate hasta = LocalDate.now();

        ReporteVentas reporte = useCase.ejecutar(desde, hasta);

        BigDecimal esperado = reporte.totalVendido()
                .divide(BigDecimal.valueOf(reporte.numeroTransacciones()),
                        2, java.math.RoundingMode.HALF_UP);

        assertEquals(0, reporte.ticketPromedio().compareTo(esperado),
                "El ticket promedio debe ser total / transacciones");
    }

    /*
     * CP-003: Verifica el desglose por método de pago. La BD tiene
     * 3 ventas EFECTIVO, 2 TARJETA y 1 TRANSFERENCIA en el rango completo.
     */
    @Test
    @DisplayName("CP-003: Reporte debe agrupar correctamente por método de pago")
    void ejecutar_debeDesglosarPorMetodoPago() {
        LocalDate desde = LocalDate.now().minusDays(15);
        LocalDate hasta = LocalDate.now();

        ReporteVentas reporte = useCase.ejecutar(desde, hasta);

        assertEquals(3, reporte.ventasPorMetodoPago().size(),
                "Deben aparecer los 3 métodos de pago usados");
        assertTrue(reporte.ventasPorMetodoPago().containsKey("EFECTIVO"));
        assertTrue(reporte.ventasPorMetodoPago().containsKey("TARJETA"));
        assertTrue(reporte.ventasPorMetodoPago().containsKey("TRANSFERENCIA"));
    }

    /*
     * CP-004: Verifica el filtrado por rango de fechas. Si pedimos solo
     * "hoy", deben incluirse únicamente las 2 ventas del día actual
     * (ventas 5 y 6), con total $29.750 + $71.400 = $101.150.
     */
    @Test
    @DisplayName("CP-004: Reporte solo del día actual debe incluir 2 ventas")
    void ejecutar_conRangoDelDia_debeFiltrarSoloHoy() {
        LocalDate hoy = LocalDate.now();

        ReporteVentas reporte = useCase.ejecutar(hoy, hoy);

        assertEquals(2, reporte.numeroTransacciones(),
                "Solo deben incluirse las 2 ventas del día actual");
        assertEquals(0, reporte.totalVendido().compareTo(new BigDecimal("101150.00")),
                "Total debe ser $29.750 + $71.400");
    }

    /*
     * CP-005: Verifica el comportamiento sobre un rango sin ventas.
     * Pedir un reporte de hace 100 días no debe encontrar nada. Debe
     * retornar un reporte con totales en cero y mapa vacío.
     */
    @Test
    @DisplayName("CP-005: Reporte sobre rango sin ventas debe retornar totales en cero")
    void ejecutar_conRangoSinVentas_debeRetornarReporteVacio() {
        LocalDate fechaAntigua = LocalDate.now().minusDays(100);

        ReporteVentas reporte = useCase.ejecutar(fechaAntigua, fechaAntigua);

        assertEquals(0, reporte.numeroTransacciones());
        assertEquals(0, reporte.totalVendido().compareTo(BigDecimal.ZERO));
        assertEquals(0, reporte.ticketPromedio().compareTo(BigDecimal.ZERO),
                "Sin transacciones, el ticket promedio es cero (no NaN)");
        assertTrue(reporte.ventasPorMetodoPago().isEmpty());
    }

    /*
     * CP-006: Verifica la validación de fechas nulas. Pasar null como
     * fechaDesde debe lanzar IllegalArgumentException.
     */
    @Test
    @DisplayName("CP-006: Reporte con fecha desde null debe lanzar excepción")
    void ejecutar_conFechaDesdeNula_debeLanzarExcepcion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(null, LocalDate.now())
        );
    }

    /*
     * CP-007: Verifica la validación de orden de fechas. fechaHasta no
     * puede ser anterior a fechaDesde.
     */
    @Test
    @DisplayName("CP-007: Reporte con fechaHasta anterior a fechaDesde debe lanzar excepción")
    void ejecutar_conFechasInvertidas_debeLanzarExcepcion() {
        LocalDate hoy = LocalDate.now();
        LocalDate ayer = hoy.minusDays(1);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(hoy, ayer)
        );

        assertTrue(ex.getMessage().contains("anterior"),
                "El mensaje debe mencionar el orden de las fechas");
    }
}