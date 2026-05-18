package services.inventario;

import entities.MovimientoInventario;
import entities.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2MovimientoInventarioRepository;
import repositories.H2ProductoRepository;
import repositories.MovimientoInventarioRepository;
import repositories.ProductoRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Pruebas de integracion del servicio ControlarInventarioUseCase.
 *
 * Cubre el CU-004: Controlar inventario, incluyendo:
 *   - registrarEntrada: aumenta stock y registra movimiento.
 *   - registrarSalida: disminuye stock y registra movimiento.
 *   - ajustarStock: fija un nuevo valor de stock y registra movimiento.
 *   - obtenerAlertasStockBajo: lista productos por debajo del minimo.
 *   - consultarMovimientos: historial de movimientos por producto..
 */
@DisplayName("Pruebas de integracion - ControlarInventarioUseCase (CU-004)")
class ControlarInventarioUseCaseTest {

    private static final int PRODUCTO_ARROZ_ID = 1;
    private static final int PRODUCTO_LENTEJAS_ID = 2;
    private static final int PRODUCTO_FRIJOL_ID = 5;
    private static final int PRODUCTO_INEXISTENTE_ID = 9999;
    private static final int EMPLEADO_ID = 1;
    private static final int TIPO_ENTRADA = 1;
    private static final int TIPO_SALIDA = 2;
    private static final int TIPO_AJUSTE = 3;

    private ControlarInventarioUseCase useCase;
    private ProductoRepository productoRepository;
    private MovimientoInventarioRepository movimientoRepository;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Resetear la BD a un estado conocido
        DatabaseConnection dbConnection = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(dbConnection);
        initializer.init();

        // 2. Instanciar dependencias reales
        productoRepository = new H2ProductoRepository(dbConnection);
        movimientoRepository = new H2MovimientoInventarioRepository(dbConnection);

        // 3. Instanciar el UseCase a probar
        useCase = new ControlarInventarioUseCase(productoRepository, movimientoRepository);
    }

    // ===========================================================
    // registrarEntrada
    // ===========================================================

    /*
     * CP-001: Entrada exitosa de inventario.
     * Usa el producto Frijol (id=5) que no tiene movimientos previos
     * para poder validar que se creo exactamente 1 nuevo movimiento.
     */
    @Test
    @DisplayName("CP-001: registrarEntrada con datos validos aumenta stock y guarda movimiento")
    void registrarEntrada_datosValidos_aumentaStockYGuardaMovimiento() {
        int stockAnterior = productoRepository.buscarPorId(PRODUCTO_FRIJOL_ID).get().getStockActual();

        int stockNuevo = useCase.registrarEntrada(
                PRODUCTO_FRIJOL_ID, 10, "Recepcion mercancia", EMPLEADO_ID, TIPO_ENTRADA);

        assertEquals(stockAnterior + 10, stockNuevo,
                "El stock retornado debe ser el anterior + la cantidad ingresada");
        assertEquals(stockAnterior + 10,
                productoRepository.buscarPorId(PRODUCTO_FRIJOL_ID).get().getStockActual(),
                "El stock debe quedar actualizado en BD");

        List<MovimientoInventario> movimientos = movimientoRepository.listarPorProducto(PRODUCTO_FRIJOL_ID);
        assertEquals(1, movimientos.size(),
                "Debe quedar registrado exactamente un movimiento para Frijol (sin movimientos previos en seed)");
        assertEquals(10, movimientos.get(0).getCantidad());
        assertEquals(stockAnterior, movimientos.get(0).getStockAnterior());
        assertEquals(stockAnterior + 10, movimientos.get(0).getStockNuevo());
    }

    /*
     * CP-002: Entrada con producto inexistente.
     * Cubre la rama orElseThrow en registrarEntrada.
     */
    @Test
    @DisplayName("CP-002: registrarEntrada con producto inexistente lanza excepcion")
    void registrarEntrada_productoInexistente_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.registrarEntrada(
                        PRODUCTO_INEXISTENTE_ID, 5, "test", EMPLEADO_ID, TIPO_ENTRADA)
        );
        assertTrue(ex.getMessage().contains("Producto no encontrado"),
                "El mensaje debe indicar que el producto no fue encontrado");
    }

    /*
     * CP-003: Entrada con cantidad cero.
     * Cubre la rama de validacion cantidad <= 0.
     */
    @Test
    @DisplayName("CP-003: registrarEntrada con cantidad cero lanza excepcion")
    void registrarEntrada_cantidadCero_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.registrarEntrada(
                        PRODUCTO_ARROZ_ID, 0, "test", EMPLEADO_ID, TIPO_ENTRADA)
        );
        assertEquals("La cantidad debe ser un número positivo", ex.getMessage());
    }

    /*
     * CP-004: Entrada con cantidad negativa.
     * Cubre la misma rama de validacion con valor negativo.
     */
    @Test
    @DisplayName("CP-004: registrarEntrada con cantidad negativa lanza excepcion")
    void registrarEntrada_cantidadNegativa_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.registrarEntrada(
                        PRODUCTO_ARROZ_ID, -5, "test", EMPLEADO_ID, TIPO_ENTRADA)
        );
        assertEquals("La cantidad debe ser un número positivo", ex.getMessage());
    }

    // ===========================================================
    // registrarSalida
    // ===========================================================

    /*
     * CP-005: Salida exitosa de inventario.
     * Usa Frijol (id=5, sin movimientos seed).
     */
    @Test
    @DisplayName("CP-005: registrarSalida con datos validos disminuye stock y guarda movimiento")
    void registrarSalida_datosValidos_disminuyeStockYGuardaMovimiento() {
        int stockAnterior = productoRepository.buscarPorId(PRODUCTO_FRIJOL_ID).get().getStockActual();

        int stockNuevo = useCase.registrarSalida(
                PRODUCTO_FRIJOL_ID, 5, "Venta mostrador", EMPLEADO_ID, TIPO_SALIDA);

        assertEquals(stockAnterior - 5, stockNuevo,
                "El stock retornado debe ser el anterior - la cantidad");
        assertEquals(stockAnterior - 5,
                productoRepository.buscarPorId(PRODUCTO_FRIJOL_ID).get().getStockActual(),
                "El stock debe quedar actualizado en BD");

        List<MovimientoInventario> movimientos = movimientoRepository.listarPorProducto(PRODUCTO_FRIJOL_ID);
        assertEquals(1, movimientos.size(),
                "Frijol debe quedar con exactamente 1 movimiento (sin seed previo)");
        assertEquals(5, movimientos.get(0).getCantidad());
    }

    /*
     * CP-006: Salida con producto inexistente.
     */
    @Test
    @DisplayName("CP-006: registrarSalida con producto inexistente lanza excepcion")
    void registrarSalida_productoInexistente_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.registrarSalida(
                        PRODUCTO_INEXISTENTE_ID, 5, "test", EMPLEADO_ID, TIPO_SALIDA)
        );
        assertTrue(ex.getMessage().contains("Producto no encontrado"));
    }

    /*
     * CP-007: Salida con cantidad cero.
     */
    @Test
    @DisplayName("CP-007: registrarSalida con cantidad cero lanza excepcion")
    void registrarSalida_cantidadCero_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.registrarSalida(
                        PRODUCTO_ARROZ_ID, 0, "test", EMPLEADO_ID, TIPO_SALIDA)
        );
        assertEquals("La cantidad debe ser un número positivo", ex.getMessage());
    }

    /*
     * CP-008: Salida con cantidad mayor al stock disponible.
     * Cubre la rama de validacion de stock insuficiente.
     */
    @Test
    @DisplayName("CP-008: registrarSalida con cantidad mayor al stock lanza excepcion")
    void registrarSalida_cantidadMayorAlStock_lanzaExcepcion() {
        int stockActual = productoRepository.buscarPorId(PRODUCTO_ARROZ_ID).get().getStockActual();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.registrarSalida(
                        PRODUCTO_ARROZ_ID, stockActual + 1, "test", EMPLEADO_ID, TIPO_SALIDA)
        );
        assertTrue(ex.getMessage().contains("Stock insuficiente"),
                "El mensaje debe indicar stock insuficiente");
    }

    // ===========================================================
    // ajustarStock
    // ===========================================================

    /*
     * CP-009: Ajuste exitoso a un valor mayor.
     * Usa Frijol (sin movimientos seed) para validar conteo exacto.
     */
    @Test
    @DisplayName("CP-009: ajustarStock a valor mayor actualiza stock y registra movimiento")
    void ajustarStock_aValorMayor_actualizaYRegistraMovimiento() {
        int stockAnterior = productoRepository.buscarPorId(PRODUCTO_FRIJOL_ID).get().getStockActual();
        int nuevoStock = stockAnterior + 15;

        int resultado = useCase.ajustarStock(
                PRODUCTO_FRIJOL_ID, nuevoStock, "Ajuste por conteo", EMPLEADO_ID, TIPO_AJUSTE);

        assertEquals(nuevoStock, resultado);
        assertEquals(nuevoStock,
                productoRepository.buscarPorId(PRODUCTO_FRIJOL_ID).get().getStockActual());

        List<MovimientoInventario> movs = movimientoRepository.listarPorProducto(PRODUCTO_FRIJOL_ID);
        assertEquals(1, movs.size(),
                "Frijol debe quedar con exactamente 1 movimiento (sin seed previo)");
        assertEquals(15, movs.get(0).getCantidad(),
                "La cantidad del movimiento es el valor absoluto de la diferencia");
    }

    /*
     * CP-010: Ajuste exitoso a un valor menor.
     */
    @Test
    @DisplayName("CP-010: ajustarStock a valor menor actualiza stock y registra movimiento")
    void ajustarStock_aValorMenor_actualizaYRegistraMovimiento() {
        int stockAnterior = productoRepository.buscarPorId(PRODUCTO_FRIJOL_ID).get().getStockActual();
        int nuevoStock = stockAnterior - 5;

        int resultado = useCase.ajustarStock(
                PRODUCTO_FRIJOL_ID, nuevoStock, "Ajuste por merma", EMPLEADO_ID, TIPO_AJUSTE);

        assertEquals(nuevoStock, resultado);
        assertEquals(nuevoStock,
                productoRepository.buscarPorId(PRODUCTO_FRIJOL_ID).get().getStockActual());

        List<MovimientoInventario> movs = movimientoRepository.listarPorProducto(PRODUCTO_FRIJOL_ID);
        assertEquals(1, movs.size(),
                "Frijol debe quedar con exactamente 1 movimiento");
        assertEquals(5, movs.get(0).getCantidad(),
                "La cantidad del movimiento es el valor absoluto de la diferencia");
    }

    /*
     * CP-011: Ajuste con producto inexistente.
     */
    @Test
    @DisplayName("CP-011: ajustarStock con producto inexistente lanza excepcion")
    void ajustarStock_productoInexistente_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ajustarStock(
                        PRODUCTO_INEXISTENTE_ID, 10, "test", EMPLEADO_ID, TIPO_AJUSTE)
        );
        assertTrue(ex.getMessage().contains("Producto no encontrado"));
    }

    /*
     * CP-012: Ajuste con stock negativo.
     */
    @Test
    @DisplayName("CP-012: ajustarStock con nuevo stock negativo lanza excepcion")
    void ajustarStock_nuevoStockNegativo_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ajustarStock(
                        PRODUCTO_ARROZ_ID, -1, "test", EMPLEADO_ID, TIPO_AJUSTE)
        );
        assertEquals("El stock no puede ser negativo", ex.getMessage());
    }

    /*
     * CP-013: Ajuste con stock igual al actual (diferencia=0).
     */
    @Test
    @DisplayName("CP-013: ajustarStock con valor igual al actual lanza excepcion")
    void ajustarStock_igualAlActual_lanzaExcepcion() {
        int stockActual = productoRepository.buscarPorId(PRODUCTO_ARROZ_ID).get().getStockActual();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ajustarStock(
                        PRODUCTO_ARROZ_ID, stockActual, "test", EMPLEADO_ID, TIPO_AJUSTE)
        );
        assertEquals("El stock ingresado es igual al stock actual", ex.getMessage());
    }

    // ===========================================================
    // obtenerAlertasStockBajo
    // ===========================================================

    /*
     * CP-014: Consulta de productos con stock bajo.
     * El seed tiene a las Lentejas (id=2) con stock=4 y minimo=5,
     * asi que debe aparecer en la lista.
     */
    @Test
    @DisplayName("CP-014: obtenerAlertasStockBajo lista productos por debajo del minimo")
    void obtenerAlertasStockBajo_retornaProductosConStockBajo() {
        List<Producto> alertas = useCase.obtenerAlertasStockBajo();

        assertNotNull(alertas, "La lista no debe ser null");
        assertFalse(alertas.isEmpty(),
                "Debe haber al menos un producto con stock bajo en el seed (Lentejas)");
        assertTrue(alertas.stream().anyMatch(p -> p.getId() == PRODUCTO_LENTEJAS_ID),
                "Las Lentejas (id=2, stock=4, min=5) deben aparecer en las alertas");
        assertTrue(alertas.stream().allMatch(Producto::tieneStockBajo),
                "Todos los productos retornados deben tener stock bajo");
    }

    // ===========================================================
    // consultarMovimientos
    // ===========================================================

    /*
     * CP-015: Consulta de historial cuando no hay movimientos.
     * El producto Frijol (id=5) es el unico que no tiene movimientos
     * en el seed, por lo que la lista debe estar vacia.
     */
    @Test
    @DisplayName("CP-015: consultarMovimientos sin movimientos retorna lista vacia")
    void consultarMovimientos_sinMovimientos_retornaListaVacia() {
        List<MovimientoInventario> movimientos = useCase.consultarMovimientos(PRODUCTO_FRIJOL_ID);

        assertNotNull(movimientos, "La lista no debe ser null");
        assertTrue(movimientos.isEmpty(),
                "Frijol no tiene movimientos en el seed, la lista debe estar vacia");
    }

    /*
     * CP-016: Consulta de historial despues de registrar varias operaciones.
     * Usa Frijol (sin movimientos seed) para poder contar exactamente 3.
     */
    @Test
    @DisplayName("CP-016: consultarMovimientos retorna todos los movimientos del producto")
    void consultarMovimientos_conMovimientos_retornaTodos() {
        useCase.registrarEntrada(PRODUCTO_FRIJOL_ID, 10, "Entrada 1", EMPLEADO_ID, TIPO_ENTRADA);
        useCase.registrarSalida(PRODUCTO_FRIJOL_ID, 3, "Salida 1", EMPLEADO_ID, TIPO_SALIDA);
        useCase.registrarEntrada(PRODUCTO_FRIJOL_ID, 5, "Entrada 2", EMPLEADO_ID, TIPO_ENTRADA);

        List<MovimientoInventario> movimientos = useCase.consultarMovimientos(PRODUCTO_FRIJOL_ID);

        assertEquals(3, movimientos.size(),
                "Deben aparecer los 3 movimientos registrados en este test");
    }
}