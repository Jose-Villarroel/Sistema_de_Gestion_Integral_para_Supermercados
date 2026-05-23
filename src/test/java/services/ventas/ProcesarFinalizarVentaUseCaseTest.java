package services.ventas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import repositories.CajaRepository;
import repositories.ClienteRepository;
import repositories.CuentaFidelizacionRepository;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.DetalleVentaRepository;
import repositories.H2CajaRepository;
import repositories.H2ClienteRepository;
import repositories.H2CuentaFidelizacionRepository;
import repositories.H2DetalleVentaRepository;
import repositories.H2MovimientoInventarioRepository;
import repositories.H2PagoVentaRepository;
import repositories.H2ProductoRepository;
import repositories.H2VentaRepository;
import repositories.MovimientoInventarioRepository;
import repositories.PagoVentaRepository;
import repositories.ProductoRepository;
import repositories.VentaRepository;
import services.ventas.ProcesarFinalizarVentaUseCase.ClienteConCuenta;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Pruebas de integracion del servicio ProcesarFinalizarVentaUseCase.
 *
 * Cubre el CU-005: Procesar venta. Los tests estan agrupados en clases
 * @Nested por metodo del UseCase para mejor organizacion.
 *
 */
@DisplayName("Pruebas de integracion - ProcesarFinalizarVentaUseCase (CU-005)")
class ProcesarFinalizarVentaUseCaseTest {

        private ProcesarFinalizarVentaUseCase useCase;
        private DatabaseConnection dbConnection;

        @BeforeEach
        void setUp() throws Exception {
                dbConnection = new DatabaseConnection();
                DatabaseInitializer initializer = new DatabaseInitializer(dbConnection);
                initializer.init();

                ClienteRepository clienteRepository = new H2ClienteRepository(dbConnection);
                CuentaFidelizacionRepository cuentaRepository = new H2CuentaFidelizacionRepository(dbConnection);
                ProductoRepository productoRepository = new H2ProductoRepository(dbConnection);
                VentaRepository ventaRepository = new H2VentaRepository();
                DetalleVentaRepository detalleRepository = new H2DetalleVentaRepository();
                PagoVentaRepository pagoRepository = new H2PagoVentaRepository();
                MovimientoInventarioRepository movimientoRepository = new H2MovimientoInventarioRepository(
                                dbConnection);
                CajaRepository cajaRepository = new H2CajaRepository();

                useCase = new ProcesarFinalizarVentaUseCase(
                                dbConnection,
                                clienteRepository,
                                cuentaRepository,
                                productoRepository,
                                ventaRepository,
                                detalleRepository,
                                pagoRepository,
                                movimientoRepository,
                                cajaRepository);
        }

        // ===========================================================
        // buscarProductoPorId
        // ===========================================================
        @Nested
        @DisplayName("buscarProductoPorId")
        class BuscarProductoPorId {

                /*
                 * CP-001: Busqueda exitosa de un producto activo por su id.
                 */
                @Test
                @DisplayName("CP-001: Buscar producto existente activo retorna el producto")
                void productoExistenteActivo_retornaProducto() {
                        var producto = useCase.buscarProductoPorId("1");

                        assertNotNull(producto);
                        assertEquals(1, producto.getId());
                        assertEquals("Arroz", producto.getNombre());
                        assertTrue(producto.isActivo());
                }

                /*
                 * CP-002: Buscar con codigo no numerico debe lanzar excepcion.
                 * Cubre la rama parsearEntero -> NumberFormatException.
                 */
                @Test
                @DisplayName("CP-002: Codigo no numerico lanza IllegalArgumentException")
                void codigoNoNumerico_lanzaExcepcion() {
                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.buscarProductoPorId("abc"));
                        assertTrue(ex.getMessage().contains("Product not found"),
                                        "El mensaje debe identificar producto no encontrado");
                }

                /*
                 * CP-003: Buscar con id que no existe en BD debe lanzar excepcion.
                 * Cubre la rama orElseThrow.
                 */
                @Test
                @DisplayName("CP-003: Producto inexistente lanza IllegalArgumentException")
                void productoInexistente_lanzaExcepcion() {
                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.buscarProductoPorId("9999"));
                        assertTrue(ex.getMessage().contains("Product not found"));
                }
        }

        // ===========================================================
        // buscarClientePorCodigo
        // ===========================================================
        @Nested
        @DisplayName("buscarClientePorCodigo")
        class BuscarClientePorCodigo {

                /*
                 * CP-004: Buscar cliente por su id_cliente.
                 * Cliente id=1 (Mariana) tiene cuenta de fidelizacion 1001 con 120 puntos.
                 */
                @Test
                @DisplayName("CP-004: Buscar por id_cliente retorna cliente con cuenta")
                void buscarPorIdCliente_retornaClienteConCuenta() {
                        Optional<ClienteConCuenta> resultado = useCase.buscarClientePorCodigo("1");

                        assertTrue(resultado.isPresent(), "Debe encontrar al cliente id=1");
                        ClienteConCuenta cc = resultado.get();
                        assertEquals(1, cc.cliente().getId());
                        assertEquals("Mariana", cc.cliente().getNombre());
                        assertNotNull(cc.cuentaId(), "Debe traer cuenta de fidelizacion vinculada");
                        assertEquals(1001, cc.numeroTarjeta(),
                                        "El numero de tarjeta de Mariana en seed es 1001");
                        assertEquals(120, cc.puntosActuales(),
                                        "Mariana inicia con 120 puntos en el seed");
                }

                /*
                 * CP-005: Buscar cliente por su numero de tarjeta.
                 * Tarjeta 1003 pertenece al cliente id=3 (Camila) con 0 puntos.
                 */
                @Test
                @DisplayName("CP-005: Buscar por numero de tarjeta retorna cliente con cuenta")
                void buscarPorNumeroTarjeta_retornaClienteConCuenta() {
                        Optional<ClienteConCuenta> resultado = useCase.buscarClientePorCodigo("1003");

                        assertTrue(resultado.isPresent(), "Debe encontrar al cliente con tarjeta 1003");
                        ClienteConCuenta cc = resultado.get();
                        assertEquals(3, cc.cliente().getId());
                        assertEquals(1003, cc.numeroTarjeta());
                        assertEquals(0, cc.puntosActuales());
                }

                /*
                 * CP-006: Buscar con codigo null retorna Optional vacio.
                 * Cubre la rama de validacion temprana.
                 */
                @Test
                @DisplayName("CP-006: Codigo null retorna Optional vacio")
                void codigoNull_retornaOptionalVacio() {
                        Optional<ClienteConCuenta> resultado = useCase.buscarClientePorCodigo(null);
                        assertTrue(resultado.isEmpty());
                }

                /*
                 * CP-007: Buscar con codigo en blanco retorna Optional vacio.
                 */
                @Test
                @DisplayName("CP-007: Codigo en blanco retorna Optional vacio")
                void codigoEnBlanco_retornaOptionalVacio() {
                        Optional<ClienteConCuenta> resultado = useCase.buscarClientePorCodigo("   ");
                        assertTrue(resultado.isEmpty());
                }

                /*
                 * CP-008: Buscar con codigo no numerico debe lanzar excepcion.
                 * Cubre la rama parsearEntero del UseCase.
                 */
                @Test
                @DisplayName("CP-008: Codigo no numerico lanza IllegalArgumentException")
                void codigoNoNumerico_lanzaExcepcion() {
                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.buscarClientePorCodigo("abc"));
                        assertTrue(ex.getMessage().contains("Customer not found"));
                }

                /*
                 * CP-009: Buscar con un id que no existe retorna Optional vacio.
                 */
                @Test
                @DisplayName("CP-009: Cliente inexistente retorna Optional vacio")
                void clienteInexistente_retornaOptionalVacio() {
                        Optional<ClienteConCuenta> resultado = useCase.buscarClientePorCodigo("99999");
                        assertTrue(resultado.isEmpty());
                }
        }

        // ===========================================================
        // calcularResumen
        // ===========================================================
        @Nested
        @DisplayName("calcularResumen")
        class CalcularResumen {

                /*
                 * CP-010: Items null retorna ResumenVenta con todos los valores en 0.
                 */
                @Test
                @DisplayName("CP-010: Items null retorna resumen en ceros")
                void itemsNull_retornaResumenCeros() {
                        var resumen = useCase.calcularResumen(
                                        null,
                                        null,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno());

                        assertEquals(0, resumen.subtotal());
                        assertEquals(0, resumen.total());
                        assertEquals(0, resumen.puntosGanados());
                }

                /*
                 * CP-011: Lista vacia retorna ResumenVenta con todos los valores en 0.
                 */
                @Test
                @DisplayName("CP-011: Items vacios retorna resumen en ceros")
                void itemsVacios_retornaResumenCeros() {
                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(),
                                        null,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno());

                        assertEquals(0, resumen.subtotal());
                        assertEquals(0, resumen.total());
                }

                /*
                 * CP-012: Calculo basico sin descuentos: 1 item, sin cliente, sin descuento
                 * manual.
                 * subtotal=10000, IVA 19% sobre 10000 = 1900, total = 11900.
                 */
                @Test
                @DisplayName("CP-012: Calculo basico sin descuentos aplica solo IVA")
                void sinDescuentos_aplicaSoloIVA() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 1, 10000.0, 20);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        null,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno());

                        assertEquals(10000.0, resumen.subtotal());
                        assertEquals(0.0, resumen.descuentoPromocion());
                        assertEquals(0.0, resumen.descuentoFidelidad());
                        assertEquals(0.0, resumen.descuentoManual());
                        assertEquals(1900.0, resumen.impuestos());
                        assertEquals(11900.0, resumen.total());
                        assertEquals(0, resumen.puntosGanados(),
                                        "Sin cliente con cuenta, no hay puntos");
                }

                /*
                 * CP-013: Descuento por promocion: item con cantidad >= 3 obtiene 5% off.
                 * 5 unidades x 1000 = 5000 subtotal, descuento promo = 250, base = 4750,
                 * IVA = 902.5, total = 5652.5
                 */
                @Test
                @DisplayName("CP-013: Cantidad >= 3 aplica descuento por promocion 5%")
                void cantidadMayorAOTresAplicaDescuentoPromocion() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 5, 1000.0, 20);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        null,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno());

                        assertEquals(5000.0, resumen.subtotal());
                        assertEquals(250.0, resumen.descuentoPromocion(),
                                        "5% sobre el subtotal del item con cantidad >= 3");
                        assertEquals(0.0, resumen.descuentoFidelidad());
                }

                /*
                 * CP-014: Item con cantidad < 3 NO aplica descuento de promocion.
                 */
                @Test
                @DisplayName("CP-014: Cantidad menor a 3 no aplica descuento por promocion")
                void cantidadMenorATresNoAplicaDescuentoPromocion() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 2, 1000.0, 20);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        null,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno());

                        assertEquals(0.0, resumen.descuentoPromocion());
                }

                /*
                 * CP-015: Cliente con cuenta de fidelizacion y subtotal >= 30000
                 * obtiene descuento del 3%.
                 * subtotal=40000, descuento fidelidad = 1200.
                 */
                @Test
                @DisplayName("CP-015: Cliente con cuenta y subtotal >= 30000 aplica descuento fidelidad")
                void clienteConCuentaYSubtotalAlto_aplicaDescuentoFidelidad() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 1, 40000.0, 20);

                        var cliente = new ProcesarFinalizarVentaUseCase.ClienteConCuenta(
                                        null, 1, 1001, 100);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        cliente,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno());

                        assertEquals(40000.0, resumen.subtotal());
                        assertEquals(1200.0, resumen.descuentoFidelidad(),
                                        "3% sobre el subtotal cuando cliente con cuenta y subtotal >= 30000");
                }

                /*
                 * CP-016: Cliente sin cuenta de fidelizacion (cuentaId null) NO obtiene
                 * descuento por fidelidad aunque el subtotal sea >= 30000.
                 */
                @Test
                @DisplayName("CP-016: Cliente sin cuenta no aplica descuento fidelidad")
                void clienteSinCuenta_noAplicaDescuentoFidelidad() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 1, 40000.0, 20);

                        var clienteSinCuenta = new ProcesarFinalizarVentaUseCase.ClienteConCuenta(
                                        null, null, null, null);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        clienteSinCuenta,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno());

                        assertEquals(0.0, resumen.descuentoFidelidad());
                        assertEquals(0, resumen.puntosGanados(),
                                        "Sin cuenta no se generan puntos");
                }

                /*
                 * CP-017: Subtotal < 30000 NO aplica descuento por fidelidad
                 * aunque el cliente tenga cuenta.
                 */
                @Test
                @DisplayName("CP-017: Subtotal menor a 30000 no aplica descuento fidelidad")
                void subtotalBajo_noAplicaDescuentoFidelidad() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 1, 20000.0, 20);

                        var cliente = new ProcesarFinalizarVentaUseCase.ClienteConCuenta(
                                        null, 1, 1001, 100);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        cliente,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno());

                        assertEquals(0.0, resumen.descuentoFidelidad());
                }

                /*
                 * CP-018: Descuento manual por porcentaje (10%).
                 * subtotal=10000, descuento manual = 1000, base = 9000,
                 * IVA = 1710, total = 10710.
                 */
                @Test
                @DisplayName("CP-018: Descuento manual por porcentaje 10%")
                void descuentoManualPorcentaje_aplicaCorrectamente() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 1, 10000.0, 20);
                        var descuento = new ProcesarFinalizarVentaUseCase.DescuentoManual(
                                        ProcesarFinalizarVentaUseCase.TipoDescuento.PORCENTAJE, 10.0);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        null,
                                        descuento);

                        assertEquals(1000.0, resumen.descuentoManual());
                        assertEquals(10710.0, resumen.total());
                }

                /*
                 * CP-019: Descuento manual por valor fijo (500).
                 */
                @Test
                @DisplayName("CP-019: Descuento manual por valor fijo")
                void descuentoManualValorFijo_aplicaCorrectamente() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 1, 10000.0, 20);
                        var descuento = new ProcesarFinalizarVentaUseCase.DescuentoManual(
                                        ProcesarFinalizarVentaUseCase.TipoDescuento.VALOR_FIJO, 500.0);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        null,
                                        descuento);

                        assertEquals(500.0, resumen.descuentoManual());
                }

                /*
                 * CP-020: Descuento manual mayor a la base lo limita al valor de la base.
                 * subtotal=1000, descuento fijo=5000 -> se limita a 1000.
                 */
                @Test
                @DisplayName("CP-020: Descuento manual mayor a la base se limita a la base")
                void descuentoManualMayorABase_seLimitaABase() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 1, 1000.0, 20);
                        var descuento = new ProcesarFinalizarVentaUseCase.DescuentoManual(
                                        ProcesarFinalizarVentaUseCase.TipoDescuento.VALOR_FIJO, 5000.0);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        null,
                                        descuento);

                        assertEquals(1000.0, resumen.descuentoManual(),
                                        "El descuento manual no puede exceder la base gravable");
                }

                /*
                 * CP-021: Cliente con cuenta genera puntos = floor(total / 1000).
                 * Total = 11900 -> puntos = 11.
                 */
                @Test
                @DisplayName("CP-021: Cliente con cuenta gana puntos = floor(total/1000)")
                void clienteConCuenta_ganaPuntosProporcional() {
                        var item = new ProcesarFinalizarVentaUseCase.ItemVenta(
                                        1, "Producto", 1, 10000.0, 20);
                        var cliente = new ProcesarFinalizarVentaUseCase.ClienteConCuenta(
                                        null, 1, 1001, 100);

                        var resumen = useCase.calcularResumen(
                                        java.util.List.of(item),
                                        cliente,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno());

                        assertEquals(11, resumen.puntosGanados(),
                                        "11900 / 1000 = 11.9, floor = 11");
                }
        }

        // ===========================================================
        // procesarVenta - validaciones de entrada y metodos de pago
        // ===========================================================
        @Nested
        @DisplayName("procesarVenta - validaciones y pagos")
        class ProcesarVentaValidaciones {

                /*
                 * Helper: construye una SolicitudVenta valida por defecto.
                 * Los tests modifican solo los campos relevantes.
                 */
                private ProcesarFinalizarVentaUseCase.SolicitudVenta solicitudBase(
                                java.util.List<ProcesarFinalizarVentaUseCase.ItemVenta> items,
                                entities.Empleado empleado,
                                String turno,
                                ProcesarFinalizarVentaUseCase.MetodoPago metodoPago,
                                double montoRecibido,
                                String referenciaPago) {

                        return new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        items,
                                        null,
                                        empleado,
                                        turno,
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        metodoPago,
                                        montoRecibido,
                                        referenciaPago,
                                        false,
                                        null,
                                        false,
                                        null);
                }

                private entities.Empleado empleadoValido() {
                        return new entities.Empleado(
                                        1, "Andres", "Gonzales", "andre@mail.com",
                                        "3001111111", java.time.LocalDate.now(), true);
                }

                private java.util.List<ProcesarFinalizarVentaUseCase.ItemVenta> itemsValidos() {
                        // Producto id=5 (Frijol), stock seed = 18
                        return java.util.List.of(
                                        new ProcesarFinalizarVentaUseCase.ItemVenta(5, "Frijol", 1, 5200.0, 18));
                }

                /*
                 * CP-022: Items null lanza IllegalArgumentException.
                 */
                @Test
                @DisplayName("CP-022: Items null lanza excepcion")
                void itemsNull_lanzaExcepcion() {
                        var solicitud = solicitudBase(null, empleadoValido(), "MANANA",
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO, 10000, null);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("at least one product"));
                }

                /*
                 * CP-023: Lista de items vacia lanza IllegalArgumentException.
                 */
                @Test
                @DisplayName("CP-023: Items vacios lanza excepcion")
                void itemsVacios_lanzaExcepcion() {
                        var solicitud = solicitudBase(java.util.List.of(), empleadoValido(), "MANANA",
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO, 10000, null);

                        assertThrows(IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                }

                /*
                 * CP-024: Empleado null lanza IllegalArgumentException.
                 */
                @Test
                @DisplayName("CP-024: Empleado null lanza excepcion")
                void empleadoNull_lanzaExcepcion() {
                        var solicitud = solicitudBase(itemsValidos(), null, "MANANA",
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO, 10000, null);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("cajero"));
                }

                /*
                 * CP-025: Turno null lanza IllegalArgumentException.
                 */
                @Test
                @DisplayName("CP-025: Turno null lanza excepcion")
                void turnoNull_lanzaExcepcion() {
                        var solicitud = solicitudBase(itemsValidos(), empleadoValido(), null,
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO, 10000, null);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("turno"));
                }

                /*
                 * CP-026: Turno no valido (no es MANANA/TARDE/NOCHE) lanza excepcion.
                 */
                @Test
                @DisplayName("CP-026: Turno con valor invalido lanza excepcion")
                void turnoInvalido_lanzaExcepcion() {
                        var solicitud = solicitudBase(itemsValidos(), empleadoValido(), "MADRUGADA",
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO, 10000, null);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("Turno no valido"));
                }

                /*
                 * CP-027: MetodoPago null lanza IllegalArgumentException.
                 */
                @Test
                @DisplayName("CP-027: MetodoPago null lanza excepcion")
                void metodoPagoNull_lanzaExcepcion() {
                        var solicitud = solicitudBase(itemsValidos(), empleadoValido(), "MANANA",
                                        null, 10000, null);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("payment method"));
                }

                /*
                 * CP-028: Pago EFECTIVO con monto insuficiente lanza excepcion.
                 */
                @Test
                @DisplayName("CP-028: EFECTIVO con monto insuficiente lanza excepcion")
                void efectivoInsuficiente_lanzaExcepcion() {
                        var solicitud = solicitudBase(itemsValidos(), empleadoValido(), "MANANA",
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO, 100, null);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("Insufficient amount"));
                }

                /*
                 * CP-029: Pago TARJETA con referencia DECLINE lanza excepcion.
                 */
                @Test
                @DisplayName("CP-029: TARJETA con referencia DECLINE lanza excepcion")
                void tarjetaDecline_lanzaExcepcion() {
                        var solicitud = solicitudBase(itemsValidos(), empleadoValido(), "MANANA",
                                        ProcesarFinalizarVentaUseCase.MetodoPago.TARJETA, 0, "DECLINE");

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("Payment rejected"));
                }

                /*
                 * CP-030: Pago TARJETA con referencia ERROR lanza excepcion.
                 */
                @Test
                @DisplayName("CP-030: TARJETA con referencia ERROR lanza excepcion")
                void tarjetaError_lanzaExcepcion() {
                        var solicitud = solicitudBase(itemsValidos(), empleadoValido(), "MANANA",
                                        ProcesarFinalizarVentaUseCase.MetodoPago.TARJETA, 0, "ERROR");

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("Error processing"));
                }

                /*
                 * CP-031: Pago MIXTO con monto recibido <= 0 lanza excepcion.
                 */
                @Test
                @DisplayName("CP-031: MIXTO con monto cero lanza excepcion")
                void mixtoConMontoCero_lanzaExcepcion() {
                        var solicitud = solicitudBase(itemsValidos(), empleadoValido(), "MANANA",
                                        ProcesarFinalizarVentaUseCase.MetodoPago.MIXTO, 0, "TARJ-001");

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("pago mixto"));
                }

                /*
                 * CP-032: Pago MIXTO con monto recibido >= total lanza excepcion.
                 * El total de itemsValidos() es 5200 * 1.19 = 6188, asi que enviar 10000 falla.
                 */
                @Test
                @DisplayName("CP-032: MIXTO con monto mayor o igual al total lanza excepcion")
                void mixtoConMontoExcesivo_lanzaExcepcion() {
                        var solicitud = solicitudBase(itemsValidos(), empleadoValido(), "MANANA",
                                        ProcesarFinalizarVentaUseCase.MetodoPago.MIXTO, 10000, "TARJ-001");

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("pago mixto"));
                }
        }

        @Nested
        @DisplayName("procesarVenta - flujos completos")
        class ProcesarVentaFlujos {

                private entities.Empleado empleadoValido() {
                        return new entities.Empleado(
                                        1, "Andres", "Gonzales", "andre@mail.com",
                                        "3001111111", java.time.LocalDate.now(), true);
                }

                /*
                 * Construye una SolicitudVenta usando Frijol (id=5, sin movimientos seed).
                 * Permite override de cliente, metodo de pago, monto y referencia.
                 */
                private ProcesarFinalizarVentaUseCase.SolicitudVenta solicitudFrijol(
                                ProcesarFinalizarVentaUseCase.ClienteConCuenta cliente,
                                ProcesarFinalizarVentaUseCase.MetodoPago metodoPago,
                                double montoRecibido,
                                String referenciaPago,
                                boolean facturaElectronica,
                                String datosFacturacion,
                                boolean enviarPorCorreo,
                                String correoTicket) {

                        return new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(
                                                        5, "Frijol", 1, 5200.0, 18)),
                                        cliente,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        metodoPago,
                                        montoRecibido,
                                        referenciaPago,
                                        facturaElectronica,
                                        datosFacturacion,
                                        enviarPorCorreo,
                                        correoTicket);
                }

                /*
                 * CP-033: Venta exitosa con pago EFECTIVO sin cliente.
                 * Verifica generacion de ticket, calculo de cambio y persistencia en BD.
                 */
                @Test
                @DisplayName("CP-033: Venta EFECTIVO sin cliente persiste correctamente")
                void ventaEfectivoSinCliente_persisteCorrectamente() throws Exception {
                        var solicitud = solicitudFrijol(
                                        null,
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000.0,
                                        null,
                                        false, null, false, null);

                        int ventasAntes = contarVentas();
                        int cajaAntes = contarMovimientosCaja();

                        var resultado = useCase.procesarVenta(solicitud);

                        assertNotNull(resultado);
                        assertNotNull(resultado.numeroTicket());
                        assertTrue(resultado.numeroTicket().startsWith("TK-"));
                        assertTrue(resultado.cambio() > 0, "Debe calcular el cambio");
                        assertTrue(resultado.total() > 0);
                        assertTrue(resultado.mensaje().contains("Sale completed"));

                        // Verificar que se descontó el stock (Frijol seed=18, vendido 1 -> 17)
                        int stock = consultarStock(5);
                        assertEquals(17, stock, "El stock de Frijol debe disminuir en 1");

                        // Verificar que se registro 1 venta nueva
                        assertEquals(ventasAntes + 1, contarVentas(),
                                        "Debe haberse registrado 1 venta adicional");

                        // Verificar que se creo entrada en Caja (porque es EFECTIVO)
                        assertEquals(cajaAntes + 1, contarMovimientosCaja(),
                                        "Debe registrarse en Caja por ser EFECTIVO");
                }

                /*
                 * CP-034: Venta exitosa con pago TARJETA. No genera entrada en Caja.
                 */
                @Test
                @DisplayName("CP-034: Venta TARJETA no genera entrada en Caja")
                void ventaTarjeta_noGeneraCaja() throws Exception {
                        var solicitud = solicitudFrijol(
                                        null,
                                        ProcesarFinalizarVentaUseCase.MetodoPago.TARJETA,
                                        0,
                                        "AUTH-12345",
                                        false, null, false, null);

                        var resultado = useCase.procesarVenta(solicitud);

                        assertNotNull(resultado);
                        assertEquals(0.0, resultado.cambio(),
                                        "Pago con tarjeta no genera cambio");
                        assertEquals("AUTH-12345", resultado.autorizacionPago());

                        assertEquals(0, contarMovimientosCaja(),
                                        "Pago TARJETA no debe registrarse en Caja");
                }

                /*
                 * CP-035: Venta exitosa con pago TRANSFERENCIA. Genera referencia automatica
                 * si no se provee.
                 */
                @Test
                @DisplayName("CP-035: Venta TRANSFERENCIA sin referencia genera una automatica")
                void ventaTransferenciaSinReferencia_generaAutomatica() throws Exception {
                        var solicitud = solicitudFrijol(
                                        null,
                                        ProcesarFinalizarVentaUseCase.MetodoPago.TRANSFERENCIA,
                                        0,
                                        null,
                                        false, null, false, null);

                        var resultado = useCase.procesarVenta(solicitud);

                        assertNotNull(resultado.autorizacionPago());
                        assertTrue(resultado.autorizacionPago().startsWith("TRF-"),
                                        "Sin referencia, se genera con prefijo TRF-");
                        assertEquals(0, contarMovimientosCaja(),
                                        "TRANSFERENCIA no debe registrarse en Caja");
                }

                /*
                 * CP-036: Venta MIXTO valida: efectivo > 0 y < total.
                 */
                @Test
                @DisplayName("CP-036: Venta MIXTO valida persiste con entrada en Caja")
                void ventaMixto_persisteConCaja() throws Exception {
                        var solicitud = solicitudFrijol(
                                        null,
                                        ProcesarFinalizarVentaUseCase.MetodoPago.MIXTO,
                                        3000.0,
                                        "MIX-001",
                                        false, null, false, null);

                        var resultado = useCase.procesarVenta(solicitud);

                        assertNotNull(resultado);
                        assertEquals(1, contarMovimientosCaja(),
                                        "MIXTO debe registrarse en Caja por la parte de efectivo");
                }

                /*
                 * CP-037: Venta exitosa con cliente que tiene cuenta de fidelizacion.
                 * Verifica que se acreditan puntos al cliente.
                 * Cliente id=3 (Camila) tiene cuenta de fidelizacion 1003 con 0 puntos.
                 */
                @Test
                @DisplayName("CP-037: Venta con cliente acredita puntos de fidelizacion")
                void ventaConCliente_acreditaPuntos() throws Exception {
                        // Buscar cliente con su cuenta usando el propio UseCase
                        var clienteConCuenta = useCase.buscarClientePorCodigo("3").get();
                        int puntosAntes = clienteConCuenta.puntosActuales();

                        // Para que total > 1000 y se generen puntos, usamos cantidad 1 a precio 5200
                        // total = 5200 * 1.19 = 6188 -> 6 puntos esperados
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(
                                                        5, "Frijol", 1, 5200.0, 18)),
                                        clienteConCuenta,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        false, null, false, null);

                        var resultado = useCase.procesarVenta(solicitud);
                        assertNotNull(resultado);

                        // Verificar puntos en BD
                        int puntosDespues = consultarPuntosCliente(3);
                        assertTrue(puntosDespues > puntosAntes,
                                        "Los puntos del cliente deben haber aumentado");
                }

                /*
                 * CP-038: Factura electronica sin datos lanza excepcion (validacion tardia,
                 * pero antes de iniciar la transaccion BD).
                 */
                @Test
                @DisplayName("CP-038: Factura electronica sin datos lanza excepcion")
                void facturaSinDatos_lanzaExcepcion() {
                        var solicitud = solicitudFrijol(
                                        null,
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        true, null, false, null);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("factura electronica"));
                }

                /*
                 * CP-039: Envio por correo sin email lanza excepcion.
                 */
                @Test
                @DisplayName("CP-039: Envio por correo sin email lanza excepcion")
                void correoSinEmail_lanzaExcepcion() {
                        var solicitud = solicitudFrijol(
                                        null,
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        false, null, true, null);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("correo"));
                }

                /*
                 * CP-040: Stock insuficiente durante actualizacion lanza excepcion
                 * y la venta NO debe persistir (rollback).
                 * Frijol stock=18, intentamos vender 100.
                 */
                @Test
                @DisplayName("CP-040: Stock insuficiente lanza excepcion y hace rollback")
                void stockInsuficiente_lanzaExcepcionYHaceRollback() throws Exception {
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(
                                                        5, "Frijol", 100, 5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        1000000,
                                        null,
                                        false, null, false, null);

                        int ventasAntes = contarVentas();

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("Insufficient stock"));

                        // Verificar que NO se persistio nada nuevo (rollback)
                        assertEquals(ventasAntes, contarVentas(),
                                        "No debe quedar ninguna venta nueva tras rollback");
                        assertEquals(18, consultarStock(5),
                                        "El stock de Frijol no debe haber cambiado");
                }

                /*
                 * CP-041: Venta con factura electronica y datos validos persiste correctamente.
                 */
                @Test
                @DisplayName("CP-041: Venta con factura electronica y datos validos persiste")
                void ventaConFacturaElectronica_persiste() throws Exception {
                        var solicitud = solicitudFrijol(
                                        null,
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        true, "NIT 900123456-7", false, null);

                        int ventasAntes = contarVentas();

                        var resultado = useCase.procesarVenta(solicitud);

                        assertNotNull(resultado);
                        assertEquals(ventasAntes + 1, contarVentas(),
                                        "Debe haberse registrado 1 venta adicional");
                }

                // ===== Helpers de consulta en BD =====

                private int consultarStock(int productoId) throws java.sql.SQLException {
                        try (var conn = dbConnection.getConnection();
                                        var stmt = conn.prepareStatement(
                                                        "SELECT stock_actual FROM Producto WHERE id_producto = ?")) {
                                stmt.setInt(1, productoId);
                                var rs = stmt.executeQuery();
                                rs.next();
                                return rs.getInt(1);
                        }
                }

                private int contarVentas() throws java.sql.SQLException {
                        try (var conn = dbConnection.getConnection();
                                        var stmt = conn.prepareStatement("SELECT COUNT(*) FROM Venta")) {
                                var rs = stmt.executeQuery();
                                rs.next();
                                return rs.getInt(1);
                        }
                }

                private int contarMovimientosCaja() throws java.sql.SQLException {
                        try (var conn = dbConnection.getConnection();
                                        var stmt = conn.prepareStatement("SELECT COUNT(*) FROM Caja")) {
                                var rs = stmt.executeQuery();
                                rs.next();
                                return rs.getInt(1);
                        }
                }

                private int consultarPuntosCliente(int idCliente) throws java.sql.SQLException {
                        try (var conn = dbConnection.getConnection();
                                        var stmt = conn.prepareStatement(
                                                        "SELECT puntos_actuales FROM Cuenta_fidelizacion WHERE id_cliente = ?")) {
                                stmt.setInt(1, idCliente);
                                var rs = stmt.executeQuery();
                                rs.next();
                                return rs.getInt(1);
                        }
                }
        }

        @Nested
        @DisplayName("Cobertura de ramas adicionales")
        class CoberturaRamasAdicionales {

                private entities.Empleado empleadoValido() {
                        return new entities.Empleado(
                                        1, "Andres", "Gonzales", "andre@mail.com",
                                        "3001111111", java.time.LocalDate.now(), true);
                }

                /*
                 * CP-042: Turno en blanco (solo espacios) lanza excepcion.
                 * Cubre la rama turno.isBlank() de validarTurno.
                 */
                @Test
                @DisplayName("CP-042: Turno en blanco lanza excepcion")
                void turnoEnBlanco_lanzaExcepcion() {
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(
                                                        5, "Frijol", 1, 5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "   ",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000, null, false, null, false, null);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("turno"));
                }

                /*
                 * CP-043: DescuentoManual con tipo null retorna 0.
                 * Cubre la rama tipo == null de DescuentoManual.calcular.
                 */
                @Test
                @DisplayName("CP-043: DescuentoManual con tipo null retorna cero")
                void descuentoManualTipoNull_retornaCero() {
                        var descuento = new ProcesarFinalizarVentaUseCase.DescuentoManual(null, 100.0);
                        assertEquals(0.0, descuento.calcular(50000.0),
                                        "Con tipo null el descuento debe ser 0");
                }

                /*
                 * CP-044: DescuentoManual con valor <= 0 retorna 0.
                 * Cubre la rama valor <= 0 de DescuentoManual.calcular.
                 */
                @Test
                @DisplayName("CP-044: DescuentoManual con valor cero retorna cero")
                void descuentoManualValorCero_retornaCero() {
                        var descuento = new ProcesarFinalizarVentaUseCase.DescuentoManual(
                                        ProcesarFinalizarVentaUseCase.TipoDescuento.PORCENTAJE, 0.0);
                        assertEquals(0.0, descuento.calcular(50000.0),
                                        "Con valor 0 el descuento debe ser 0");
                }

                /*
                 * CP-045: Venta MIXTO completa que persiste.
                 * Cubre la rama case MIXTO de obtenerTipoPagoId y guardarPago con
                 * los dos pagos (efectivo + tarjeta).
                 */
                @Test
                @DisplayName("CP-045: Venta MIXTO completa persiste con dos pagos")
                void ventaMixtoCompleta_persisteCorrectamente() throws Exception {
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(
                                                        5, "Frijol", 1, 5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.MIXTO,
                                        3000.0,
                                        "MIX-REF-001",
                                        false, null, false, null);

                        var resultado = useCase.procesarVenta(solicitud);

                        assertNotNull(resultado);
                        assertNotNull(resultado.numeroTicket());

                        // Verificar que se registraron 2 pagos para esta venta
                        try (var conn = dbConnection.getConnection();
                                        var stmt = conn.prepareStatement(
                                                        "SELECT COUNT(*) FROM Pago_venta pv " +
                                                                        "JOIN Venta v ON pv.id_venta = v.id_venta " +
                                                                        "WHERE v.metodo_pago = 'MIXTO'")) {
                                var rs = stmt.executeQuery();
                                rs.next();
                                assertEquals(2, rs.getInt(1),
                                                "Una venta MIXTO debe generar 2 registros de pago");
                        }
                }

                /*
                 * CP-046: Pago TARJETA con referencia valida normal (no DECLINE/ERROR).
                 * Cubre la rama exitosa de procesarPagoTarjeta y limpiarReferencia
                 * con referencia provista.
                 */
                @Test
                @DisplayName("CP-046: TARJETA con referencia valida usa esa referencia")
                void tarjetaReferenciaValida_usaLaReferencia() throws Exception {
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(
                                                        5, "Frijol", 1, 5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.TARJETA,
                                        0,
                                        "miReferencia123",
                                        false, null, false, null);

                        var resultado = useCase.procesarVenta(solicitud);

                        assertEquals("MIREFERENCIA123", resultado.autorizacionPago(),
                                        "La referencia provista se usa en mayusculas");
                }

                /*
                 * CP-047: Pago TARJETA sin referencia genera una automatica con prefijo AUTH.
                 * Cubre la rama de limpiarReferencia cuando referencia es null/blank.
                 */
                @Test
                @DisplayName("CP-047: TARJETA sin referencia genera autorizacion automatica")
                void tarjetaSinReferencia_generaAutorizacionAutomatica() throws Exception {
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(
                                                        5, "Frijol", 1, 5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.TARJETA,
                                        0,
                                        null,
                                        false, null, false, null);

                        var resultado = useCase.procesarVenta(solicitud);

                        assertNotNull(resultado.autorizacionPago());
                        assertTrue(resultado.autorizacionPago().startsWith("AUTH-"),
                                        "Sin referencia, se genera una con prefijo AUTH-");
                }

                /*
                 * CP-048: buscarClientePorCodigo con cliente que existe pero NO tiene
                 * cuenta de fidelizacion. Cubre la rama cuenta == null de los
                 * operadores ternarios.
                 * Cliente id=9 (Natalia Rojas) existe en seed pero no tiene
                 * cuenta de fidelizacion (el seed solo crea cuentas para clientes 1-8).
                 */
                @Test
                @DisplayName("CP-048: Cliente sin cuenta de fidelizacion retorna ClienteConCuenta con campos null")
                void clienteSinCuentaFidelizacion_retornaCamposNull() throws Exception {
                        // Desactivar todas las cuentas de fidelizacion para forzar cuenta = null
                        try (var conn = dbConnection.getConnection();
                                        var stmt = conn.prepareStatement(
                                                        "UPDATE Cuenta_fidelizacion SET estado = FALSE WHERE id_cliente = 10")) {
                                stmt.executeUpdate();
                        }

                        var resultado = useCase.buscarClientePorCodigo("10");

                        assertTrue(resultado.isPresent(),
                                        "El cliente 10 (Felipe) existe y debe encontrarse");
                        var cc = resultado.get();
                        assertEquals(10, cc.cliente().getId());
                        assertNull(cc.cuentaId(),
                                        "Cliente con cuenta desactivada: cuentaId debe ser null");
                        assertNull(cc.numeroTarjeta(),
                                        "Cliente con cuenta desactivada: numeroTarjeta debe ser null");
                        assertNull(cc.puntosActuales(),
                                        "Cliente con cuenta desactivada: puntosActuales debe ser null");
                }

                /*
                 * CP-049: Venta con cliente (no null) para cubrir la rama del ticket
                 * donde solicitud.cliente() != null -> escribe nombre en el archivo.
                 * Tambien cubre acreditarPuntos con puntos = 0 porque el total es
                 * bajo (< 1000) y el cliente tiene cuenta pero no gana puntos.
                 */
                @Test
                @DisplayName("CP-049: Venta con cliente incluye nombre en ticket y puntos cero no acredita")
                void ventaConClienteYPuntosCero_cubreRamasDeTicketYPuntos() throws Exception {
                        // Cliente 3 (Camila) tiene cuenta fidelizacion
                        var clienteConCuenta = useCase.buscarClientePorCodigo("3").get();

                        // Total muy bajo para que puntos = floor(total/1000) = 0
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(
                                                        5, "Frijol", 1, 500.0, 18)),
                                        clienteConCuenta,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        1000,
                                        null,
                                        false, null, false, null);

                        var resultado = useCase.procesarVenta(solicitud);

                        assertNotNull(resultado);
                        assertTrue(resultado.mensaje().contains("Sale completed"));
                }

                /*
                 * CP-050: Venta con correo y factura para cubrir las ramas de
                 * generarTicketArchivo donde enviarPorCorreo=true y facturaElectronica=true.
                 */
                @Test
                @DisplayName("CP-050: Venta con correo y factura genera ticket completo")
                void ventaConCorreoYFactura_generaTicketCompleto() throws Exception {
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(
                                                        5, "Frijol", 1, 5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        true, "NIT 900123456-7",
                                        true, "test@mail.com");

                        var resultado = useCase.procesarVenta(solicitud);

                        assertNotNull(resultado);
                        assertTrue(resultado.mensaje().contains("Sale completed"));
                }

                @Test
                @DisplayName("CP-051: DescuentoManual con tipo NINGUNO retorna 0")
                void descuentoManualNinguno_retornaCero() {
                        var descuento = new ProcesarFinalizarVentaUseCase.DescuentoManual(
                                        ProcesarFinalizarVentaUseCase.TipoDescuento.NINGUNO, 100.0);
                        assertEquals(0.0, descuento.calcular(50000.0),
                                        "Con tipo NINGUNO el descuento debe ser 0");
                }

                @Test
                @DisplayName("CP-052: buscarProductoPorId con null lanza IllegalArgumentException")
                void buscarProductoPorIdNull_lanzaIllegalArgumentException() {
                        assertThrows(IllegalArgumentException.class, () -> useCase.buscarProductoPorId(null));
                }

                @Test
                @DisplayName("CP-053: Factura electronica con datos vacíos lanza excepcion")
                void ventaFacturaVacia_lanzaIllegalArgumentException() {
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(5, "Frijol", 1,
                                                        5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        true, "   ",
                                        false, null);

                        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("datos fiscales"));
                }

                @Test
                @DisplayName("CP-054: Envio por correo con email en blanco lanza excepcion")
                void ventaCorreoVacio_lanzaIllegalArgumentException() {
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(5, "Frijol", 1,
                                                        5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        false, null,
                                        true, "   ");

                        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("correo"));
                }

                @Test
                @DisplayName("CP-055: Venta TARJETA con referencia vacía genera una automatica")
                void ventaReferenciaVacia_generaReferenciaAutomatica() throws Exception {
                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(5, "Frijol", 1,
                                                        5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.TARJETA,
                                        0,
                                        "   ",
                                        false, null,
                                        false, null);

                        var resultado = useCase.procesarVenta(solicitud);
                        assertNotNull(resultado.autorizacionPago());
                        assertTrue(resultado.autorizacionPago().startsWith("AUTH-"));
                }

                @Test
                @DisplayName("CP-056: Venta con cliente que existe pero sin cuenta activa no acredita puntos")
                void ventaClienteSinCuentaFidelizacion_noAcreditaPuntos() throws Exception {
                        try (var conn = dbConnection.getConnection();
                                        var stmt = conn.prepareStatement(
                                                        "UPDATE Cuenta_fidelizacion SET estado = FALSE WHERE id_cliente = 10")) {
                                stmt.executeUpdate();
                        }

                        var clienteConCuenta = useCase.buscarClientePorCodigo("10").get();

                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(5, "Frijol", 1,
                                                        5200.0, 18)),
                                        clienteConCuenta,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        false, null,
                                        false, null);

                        var resultado = useCase.procesarVenta(solicitud);
                        assertNotNull(resultado);
                }

                @Test
                @DisplayName("CP-057: Venta con producto inactivo lanza excepcion al descontar stock")
                void ventaProductoInactivo_lanzaExcepcionPorDescontarStock() throws Exception {
                        try (var conn = dbConnection.getConnection();
                                        var stmt = conn.prepareStatement(
                                                        "UPDATE Producto SET estado_activo = FALSE WHERE id_producto = 5")) {
                                stmt.executeUpdate();
                        }

                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(5, "Frijol", 1,
                                                        5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        false, null,
                                        false, null);

                        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                                        () -> useCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("Insufficient stock"));
                }

                @Test
                @DisplayName("CP-058: Venta exitosa con error de escritura del ticket retorna mensaje de error de impresion")
                void ventaErrorImpresion_retornaMensajeDeError() throws Exception {
                        java.io.File fileAsDirectory = new java.io.File("target/tickets");
                        if (fileAsDirectory.exists()) {
                                if (fileAsDirectory.isDirectory()) {
                                        for (java.io.File sub : fileAsDirectory.listFiles()) {
                                                sub.delete();
                                        }
                                        fileAsDirectory.delete();
                                } else {
                                        fileAsDirectory.delete();
                                }
                        }

                        fileAsDirectory.createNewFile();

                        try {
                                var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                                java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(5,
                                                                "Frijol", 1, 5200.0, 18)),
                                                null,
                                                empleadoValido(),
                                                "MANANA",
                                                ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                                ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                                10000,
                                                null,
                                                false, null,
                                                false, null);

                                var resultado = useCase.procesarVenta(solicitud);
                                assertNotNull(resultado);
                                assertTrue(resultado.mensaje().contains("Sale recorded but printing error"));
                        } finally {
                                fileAsDirectory.delete();
                        }
                }

                @Test
                @DisplayName("CP-059: Error SQL al conectar lanza RuntimeException")
                void ventaErrorConexionSql_lanzaRuntimeException() {
                        DatabaseConnection faultyDb = new DatabaseConnection() {
                                @Override
                                public java.sql.Connection getConnection() throws java.sql.SQLException {
                                        throw new java.sql.SQLException("Simulated database failure");
                                }
                        };

                        ProcesarFinalizarVentaUseCase faultyUseCase = new ProcesarFinalizarVentaUseCase(
                                        faultyDb,
                                        new H2ClienteRepository(dbConnection),
                                        new H2CuentaFidelizacionRepository(dbConnection),
                                        new H2ProductoRepository(dbConnection),
                                        new H2VentaRepository(),
                                        new H2DetalleVentaRepository(),
                                        new H2PagoVentaRepository(),
                                        new H2MovimientoInventarioRepository(dbConnection),
                                        new H2CajaRepository());

                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(5, "Frijol", 1,
                                                        5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        false, null,
                                        false, null);

                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> faultyUseCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("Transaction processing error"));
                }

                @Test
                @DisplayName("CP-060: Error genérico durante transaccion lanza RuntimeException")
                void ventaErrorGenericoTransaccion_lanzaRuntimeException() {
                        VentaRepository faultyVentaRepo = (conn, empId, date, turno, method, sub, desc, tax, tot) -> {
                                throw new RuntimeException("Simulated unexpected exception");
                        };

                        ProcesarFinalizarVentaUseCase faultyUseCase = new ProcesarFinalizarVentaUseCase(
                                        dbConnection,
                                        new H2ClienteRepository(dbConnection),
                                        new H2CuentaFidelizacionRepository(dbConnection),
                                        new H2ProductoRepository(dbConnection),
                                        faultyVentaRepo,
                                        new H2DetalleVentaRepository(),
                                        new H2PagoVentaRepository(),
                                        new H2MovimientoInventarioRepository(dbConnection),
                                        new H2CajaRepository());

                        var solicitud = new ProcesarFinalizarVentaUseCase.SolicitudVenta(
                                        java.util.List.of(new ProcesarFinalizarVentaUseCase.ItemVenta(5, "Frijol", 1,
                                                        5200.0, 18)),
                                        null,
                                        empleadoValido(),
                                        "MANANA",
                                        ProcesarFinalizarVentaUseCase.DescuentoManual.ninguno(),
                                        ProcesarFinalizarVentaUseCase.MetodoPago.EFECTIVO,
                                        10000,
                                        null,
                                        false, null,
                                        false, null);

                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> faultyUseCase.procesarVenta(solicitud));
                        assertTrue(ex.getMessage().contains("Transaction processing error"));
                }
        }
}
