package services.ventas;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import repositories.*;
import services.ventas.ProcesarDevolucionUseCase.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Pruebas de integracion del servicio ProcesarDevolucionUseCase.
 *
 * Cubre el CU-010: Procesar devolucion. Los tests estan agrupados en
 * clases @Nested por escenario.
 */
@DisplayName("Pruebas de integracion - ProcesarDevolucionUseCase (CU-010)")
class ProcesarDevolucionUseCaseTest {

        protected ProcesarDevolucionUseCase useCase;
        protected DatabaseConnection dbConnection;

        @BeforeEach
        void setUp() throws Exception {
                // 1. Resetear la BD a un estado conocido
                dbConnection = new DatabaseConnection();
                DatabaseInitializer initializer = new DatabaseInitializer(dbConnection);
                initializer.init();

                // 2. Instanciar todas las dependencias reales
                DevolucionRepository devolucionRepository = new H2DevolucionRepository();
                DetalleDevolucionRepository detalleDevolucionRepository = new H2DetalleDevolucionRepository();
                ProductoRepository productoRepository = new H2ProductoRepository(dbConnection);
                MovimientoInventarioRepository movimientoInventarioRepository = new H2MovimientoInventarioRepository(
                                dbConnection);
                CajaRepository cajaRepository = new H2CajaRepository();
                UsuarioRepository usuarioRepository = new H2UsuarioRepository(dbConnection);
                CuentaFidelizacionRepository cuentaFidelizacionRepository = new H2CuentaFidelizacionRepository(
                                dbConnection);

                // 3. Instanciar el UseCase a probar
                useCase = new ProcesarDevolucionUseCase(
                                dbConnection,
                                devolucionRepository,
                                detalleDevolucionRepository,
                                productoRepository,
                                movimientoInventarioRepository,
                                cajaRepository,
                                usuarioRepository,
                                cuentaFidelizacionRepository);
        }

        /*
         * Helper: inserta una linea de Detalle_venta para una venta del seed.
         * Se usa para preparar el escenario antes de devolver.
         */
        protected void insertarDetalleVenta(int idVenta, int idProducto, int cantidad,
                        double precioUnitario, double subtotal) throws Exception {
                try (Connection conn = dbConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement("""
                                                INSERT INTO Detalle_venta
                                                (id_venta, id_producto, cantidad, precio_unitario, descuento, subtotal)
                                                VALUES (?, ?, ?, ?, 0, ?)
                                                """)) {
                        stmt.setInt(1, idVenta);
                        stmt.setInt(2, idProducto);
                        stmt.setInt(3, cantidad);
                        stmt.setDouble(4, precioUnitario);
                        stmt.setDouble(5, subtotal);
                        stmt.executeUpdate();
                }
        }

        /*
         * Helper: inserta una venta nueva con una fecha especifica (para probar
         * el escenario de venta vieja que requiere supervisor).
         */
        protected int insertarVentaConFecha(LocalDate fecha, String turno, int idEmpleado) throws Exception {
                try (Connection conn = dbConnection.getConnection();
                                PreparedStatement stmt = conn.prepareStatement("""
                                                INSERT INTO Venta
                                                (id_empleado, fecha_venta, turno, metodo_pago, subtotal,
                                                 descuento_total, impuesto_total, total_final, estado_venta)
                                                VALUES (?, ?, ?, 'EFECTIVO', 10000, 0, 1900, 11900, TRUE)
                                                """, PreparedStatement.RETURN_GENERATED_KEYS)) {
                        stmt.setInt(1, idEmpleado);
                        stmt.setDate(2, Date.valueOf(fecha));
                        stmt.setString(3, turno);
                        stmt.executeUpdate();
                        var rs = stmt.getGeneratedKeys();
                        rs.next();
                        return rs.getInt(1);
                }
        }

        // ===========================================================
        // obtenerDetallesVenta
        // ===========================================================
        @Nested
        @DisplayName("obtenerDetallesVenta")
        class ObtenerDetallesVenta {

                /*
                 * CP-001: Venta sin detalles retorna lista vacia.
                 * Las ventas seed no tienen Detalle_venta cargado.
                 */
                @Test
                @DisplayName("CP-001: Venta sin detalles retorna lista vacia")
                void ventaSinDetalles_retornaListaVacia() {
                        List<DetalleVentaRetornable> detalles = useCase.obtenerDetallesVenta(5);

                        assertNotNull(detalles);
                        assertTrue(detalles.isEmpty(),
                                        "Sin detalles cargados la lista debe estar vacia");
                }

                /*
                 * CP-002: Venta con un detalle retorna ese item con cantidad disponible
                 * igual a la comprada (sin devoluciones previas).
                 */
                @Test
                @DisplayName("CP-002: Venta con detalle retorna item con cantidad disponible")
                void ventaConDetalle_retornaItem() throws Exception {
                        // Venta seed id=5, agregamos 1 detalle de Arroz (id=1)
                        insertarDetalleVenta(5, 1, 4, 4500.0, 18000.0);

                        List<DetalleVentaRetornable> detalles = useCase.obtenerDetallesVenta(5);

                        assertEquals(1, detalles.size());
                        DetalleVentaRetornable item = detalles.get(0);
                        assertEquals(1, item.idProducto());
                        assertEquals("Arroz", item.nombreProducto());
                        assertEquals(4, item.comprada());
                        assertEquals(0, item.devuelta(),
                                        "Sin devoluciones previas, devuelta debe ser 0");
                        assertEquals(4, item.cantidadDisponible(),
                                        "Disponible = comprada - devuelta = 4 - 0 = 4");
                        assertEquals(4500.0, item.precioRealUnitario(),
                                        "Precio real = subtotal / cantidad");
                }

                /*
                 * CP-003: Venta con multiples detalles retorna todos los items.
                 */
                @Test
                @DisplayName("CP-003: Venta con varios detalles retorna todos los items")
                void ventaConVariosDetalles_retornaTodos() throws Exception {
                        insertarDetalleVenta(5, 1, 2, 4500.0, 9000.0);
                        insertarDetalleVenta(5, 2, 3, 3200.0, 9600.0);
                        insertarDetalleVenta(5, 3, 1, 3800.0, 3800.0);

                        List<DetalleVentaRetornable> detalles = useCase.obtenerDetallesVenta(5);

                        assertEquals(3, detalles.size(),
                                        "Deben retornarse los 3 detalles insertados");
                }

                /*
                 * CP-004: Venta inexistente retorna lista vacia (no excepcion).
                 */
                @Test
                @DisplayName("CP-004: Venta inexistente retorna lista vacia")
                void ventaInexistente_retornaListaVacia() {
                        List<DetalleVentaRetornable> detalles = useCase.obtenerDetallesVenta(99999);

                        assertNotNull(detalles);
                        assertTrue(detalles.isEmpty());
                }
        }

        // ===========================================================
        // procesarDevolucion - validaciones de negocio
        // ===========================================================
        @Nested
        @DisplayName("procesarDevolucion - validaciones")
        class ProcesarDevolucionValidaciones {

                /*
                 * Helper: construye una SolicitudDevolucion con items.
                 */
                private SolicitudDevolucion solicitudBase(int idVenta, List<ItemDevolucion> items,
                                MetodoReembolso metodo) {
                        return new SolicitudDevolucion(
                                        idVenta,
                                        1,
                                        "Motivo general",
                                        metodo,
                                        "admin",
                                        "admin",
                                        items);
                }

                /*
                 * CP-005: Items null lanza excepcion antes de tocar BD.
                 * Como esta validacion esta antes del try, viene como IllegalArgumentException
                 * directa.
                 */
                @Test
                @DisplayName("CP-005: Items null lanza IllegalArgumentException")
                void itemsNull_lanzaExcepcion() {
                        var solicitud = solicitudBase(5, null, MetodoReembolso.EFECTIVO);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarDevolucion(solicitud));
                        assertTrue(ex.getMessage().contains("al menos un producto"));
                }

                /*
                 * CP-006: Items vacios lanza excepcion antes de tocar BD.
                 */
                @Test
                @DisplayName("CP-006: Items vacios lanza IllegalArgumentException")
                void itemsVacios_lanzaExcepcion() {
                        var solicitud = solicitudBase(5, List.of(), MetodoReembolso.EFECTIVO);

                        IllegalArgumentException ex = assertThrows(
                                        IllegalArgumentException.class,
                                        () -> useCase.procesarDevolucion(solicitud));
                        assertTrue(ex.getMessage().contains("al menos un producto"));
                }

                /*
                 * CP-007: Venta inexistente lanza excepcion envuelta en RuntimeException.
                 * Se da despues de abrir conexion, asi que viene envuelta.
                 */
                @Test
                @DisplayName("CP-007: Venta inexistente lanza excepcion (envuelta)")
                void ventaInexistente_lanzaExcepcion() {
                        var solicitud = solicitudBase(
                                        99999,
                                        List.of(new ItemDevolucion(1, 1, "motivo", EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.EFECTIVO);

                        RuntimeException ex = assertThrows(
                                        RuntimeException.class,
                                        () -> useCase.procesarDevolucion(solicitud));
                        assertTrue(ex.getMessage().contains("Ticket no encontrado"),
                                        "El mensaje debe identificar venta no encontrada");
                }

                /*
                 * CP-008: Producto que no pertenece a la venta original lanza excepcion.
                 * Venta 5 solo tiene Arroz (id=1), intentamos devolver Lentejas (id=2).
                 */
                @Test
                @DisplayName("CP-008: Producto no comprado en la venta lanza excepcion")
                void productoNoEnVenta_lanzaExcepcion() throws Exception {
                        insertarDetalleVenta(5, 1, 4, 4500.0, 18000.0);

                        var solicitud = solicitudBase(5,
                                        List.of(new ItemDevolucion(2, 1, "motivo", EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.EFECTIVO);

                        RuntimeException ex = assertThrows(
                                        RuntimeException.class,
                                        () -> useCase.procesarDevolucion(solicitud));
                        assertTrue(ex.getMessage().contains("no pertenece a la compra original"));
                }

                /*
                 * CP-009: Cantidad a devolver mayor a la comprada lanza excepcion.
                 * Compraron 2 Arroces, intentamos devolver 5.
                 */
                @Test
                @DisplayName("CP-009: Cantidad a devolver mayor a comprada lanza excepcion")
                void cantidadExcedeComprada_lanzaExcepcion() throws Exception {
                        insertarDetalleVenta(5, 1, 2, 4500.0, 9000.0);

                        var solicitud = solicitudBase(5,
                                        List.of(new ItemDevolucion(1, 5, "motivo", EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.EFECTIVO);

                        RuntimeException ex = assertThrows(
                                        RuntimeException.class,
                                        () -> useCase.procesarDevolucion(solicitud));
                        assertTrue(ex.getMessage().contains("excede cantidad comprada"));
                }

                /*
                 * CP-010: Todos los items con cantidad <= 0 -> totalDevuelto = 0 -> excepcion.
                 * Los items con cantidad <= 0 se ignoran, asi que el total queda en 0.
                 */
                @Test
                @DisplayName("CP-010: Items con cantidad cero -> total cero -> excepcion")
                void totalDevueltoCero_lanzaExcepcion() throws Exception {
                        insertarDetalleVenta(5, 1, 2, 4500.0, 9000.0);

                        var solicitud = solicitudBase(5,
                                        List.of(new ItemDevolucion(1, 0, "motivo", EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.EFECTIVO);

                        RuntimeException ex = assertThrows(
                                        RuntimeException.class,
                                        () -> useCase.procesarDevolucion(solicitud));
                        assertTrue(ex.getMessage().contains("mayor a cero"));
                }

                /*
                 * CP-011: Venta con mas de 30 dias requiere supervisor.
                 * Si las credenciales son invalidas, falla.
                 */
                @Test
                @DisplayName("CP-011: Venta vieja con supervisor invalido lanza excepcion")
                void ventaVieja_supervisorInvalido_lanzaExcepcion() throws Exception {
                        // Insertar venta de hace 45 dias
                        int idVentaVieja = insertarVentaConFecha(
                                        LocalDate.now().minusDays(45), "MANANA", 3);
                        insertarDetalleVenta(idVentaVieja, 1, 1, 4500.0, 4500.0);

                        var solicitud = new SolicitudDevolucion(
                                        idVentaVieja, 1, "motivo", MetodoReembolso.EFECTIVO,
                                        "no_existe", "incorrecta",
                                        List.of(new ItemDevolucion(1, 1, "motivo", EstadoProductoDevolucion.CERRADO)));

                        RuntimeException ex = assertThrows(
                                        RuntimeException.class,
                                        () -> useCase.procesarDevolucion(solicitud));
                        assertNotNull(ex.getMessage(),
                                        "Debe lanzarse excepcion por supervisor invalido o no encontrado");
                }

                /*
                 * CP-012: Producto en estado ABIERTO requiere supervisor.
                 * Con credenciales invalidas, falla.
                 */
                @Test
                @DisplayName("CP-012: Producto ABIERTO con supervisor invalido lanza excepcion")
                void productoAbierto_supervisorInvalido_lanzaExcepcion() throws Exception {
                        insertarDetalleVenta(5, 1, 2, 4500.0, 9000.0);

                        var solicitud = new SolicitudDevolucion(
                                        5, 1, "motivo", MetodoReembolso.EFECTIVO,
                                        "no_existe", "incorrecta",
                                        List.of(new ItemDevolucion(1, 1, "motivo", EstadoProductoDevolucion.ABIERTO)));

                        RuntimeException ex = assertThrows(
                                        RuntimeException.class,
                                        () -> useCase.procesarDevolucion(solicitud));
                        assertNotNull(ex.getMessage(),
                                        "Debe lanzarse excepcion por supervisor invalido");
                }
        }

        // ===========================================================
        // procesarDevolucion - flujos exitosos
        // ===========================================================
        @Nested
        @DisplayName("procesarDevolucion - flujos exitosos")
        class ProcesarDevolucionFlujos {

                private SolicitudDevolucion solicitudExitosa(
                                int idVenta,
                                List<ItemDevolucion> items,
                                MetodoReembolso metodo) {
                        return new SolicitudDevolucion(
                                        idVenta,
                                        1,
                                        "Devolucion de prueba",
                                        metodo,
                                        null,
                                        null,
                                        items);
                }

                /*
                 * CP-013: Devolucion exitosa con NOTA_CREDITO no requiere caja.
                 * Cubre el flujo completo: persistencia, reintegro de stock, comprobante.
                 * Producto 5 (Frijol) stock seed=18. Devolver 2 -> stock final 20.
                 */
                @Test
                @DisplayName("CP-013: Devolucion exitosa con NOTA_CREDITO")
                void devolucionExitosaNotaCredito_persisteYReintegrarStock() throws Exception {
                        insertarDetalleVenta(5, 5, 4, 5200.0, 20800.0);
                        int stockAntes = consultarStock(5);

                        var solicitud = solicitudExitosa(5,
                                        List.of(new ItemDevolucion(5, 2, "Producto sin abrir",
                                                        EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.NOTA_CREDITO);

                        var resultado = useCase.procesarDevolucion(solicitud);

                        assertNotNull(resultado);
                        assertNotNull(resultado.numeroDevolucion());
                        assertTrue(resultado.numeroDevolucion().startsWith("DEV-"));
                        assertEquals(10400.0, resultado.totalDevuelto(),
                                        "2 unidades * 5200 (precio real) = 10400");

                        // Verificar reintegro de stock
                        assertEquals(stockAntes + 2, consultarStock(5),
                                        "El stock de Frijol debe haber aumentado en 2");

                        // Verificar que se persistio la devolucion
                        assertEquals(1, contarDevoluciones(),
                                        "Debe haberse registrado 1 devolucion");
                }

                /*
                 * CP-014: Devolucion con CAMBIO_PRODUCTO no toca caja.
                 */
                @Test
                @DisplayName("CP-014: Devolucion exitosa con CAMBIO_PRODUCTO")
                void devolucionExitosaCambioProducto_persisteCorrectamente() throws Exception {
                        insertarDetalleVenta(5, 5, 3, 5200.0, 15600.0);

                        var solicitud = solicitudExitosa(5,
                                        List.of(new ItemDevolucion(5, 1, "Cambio por otro",
                                                        EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.CAMBIO_PRODUCTO);

                        var resultado = useCase.procesarDevolucion(solicitud);

                        assertNotNull(resultado);
                        assertEquals(5200.0, resultado.totalDevuelto());
                }

                /*
                 * CP-015: Producto en estado DEFECTUOSO no reintegra stock.
                 * El stock NO debe cambiar (continue en actualizarStockYMovimiento).
                 */
                @Test
                @DisplayName("CP-015: Producto DEFECTUOSO no reintegra stock")
                void productoDefectuoso_noReintegraStock() throws Exception {
                        insertarDetalleVenta(5, 5, 2, 5200.0, 10400.0);
                        int stockAntes = consultarStock(5);

                        var solicitud = solicitudExitosa(5,
                                        List.of(new ItemDevolucion(5, 1, "Producto roto",
                                                        EstadoProductoDevolucion.DEFECTUOSO)),
                                        MetodoReembolso.NOTA_CREDITO);

                        var resultado = useCase.procesarDevolucion(solicitud);

                        assertNotNull(resultado);
                        assertEquals(stockAntes, consultarStock(5),
                                        "El stock NO debe cambiar para producto DEFECTUOSO");
                        assertEquals(5200.0, resultado.totalDevuelto(),
                                        "Aunque no reintegre stock, si se devuelve el dinero");
                }

                /*
                 * CP-016: Producto en estado VENCIDO tampoco reintegra stock.
                 */
                @Test
                @DisplayName("CP-016: Producto VENCIDO no reintegra stock")
                void productoVencido_noReintegraStock() throws Exception {
                        insertarDetalleVenta(5, 5, 2, 5200.0, 10400.0);
                        int stockAntes = consultarStock(5);

                        var solicitud = solicitudExitosa(5,
                                        List.of(new ItemDevolucion(5, 1, "Vencido",
                                                        EstadoProductoDevolucion.VENCIDO)),
                                        MetodoReembolso.NOTA_CREDITO);

                        var resultado = useCase.procesarDevolucion(solicitud);

                        assertNotNull(resultado);
                        assertEquals(stockAntes, consultarStock(5),
                                        "El stock NO debe cambiar para producto VENCIDO");
                }

                /*
                 * CP-017: Items con cantidad <= 0 se ignoran (rama del continue en
                 * calcularTotalYValidarItems). Solo se procesan los con cantidad > 0.
                 */
                @Test
                @DisplayName("CP-017: Item con cantidad cero se ignora, item valido se procesa")
                void itemConCantidadCero_seIgnora() throws Exception {
                        insertarDetalleVenta(5, 5, 3, 5200.0, 15600.0);

                        var solicitud = solicitudExitosa(5,
                                        List.of(
                                                        new ItemDevolucion(5, 0, "ignorar",
                                                                        EstadoProductoDevolucion.CERRADO),
                                                        new ItemDevolucion(5, 1, "valido",
                                                                        EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.NOTA_CREDITO);

                        var resultado = useCase.procesarDevolucion(solicitud);

                        assertNotNull(resultado);
                        assertEquals(5200.0, resultado.totalDevuelto(),
                                        "Solo se devuelve 1 unidad (el item con cantidad 0 se ignora)");
                }

                /*
                 * CP-018: Devolucion parcial.
                 * Comprados 5, se devuelven 2 -> quedan 3 disponibles para futura devolucion.
                 */
                @Test
                @DisplayName("CP-018: Devolucion parcial actualiza cantidad disponible")
                void devolucionParcial_actualizaDisponible() throws Exception {
                        insertarDetalleVenta(5, 5, 5, 5200.0, 26000.0);

                        var solicitud = solicitudExitosa(5,
                                        List.of(new ItemDevolucion(5, 2, "parcial",
                                                        EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.NOTA_CREDITO);

                        useCase.procesarDevolucion(solicitud);

                        // Despues de la devolucion, cantidad disponible = 5 - 2 = 3
                        List<DetalleVentaRetornable> detalles = useCase.obtenerDetallesVenta(5);
                        assertEquals(1, detalles.size());
                        assertEquals(5, detalles.get(0).comprada());
                        assertEquals(2, detalles.get(0).devuelta(),
                                        "Debe registrar 2 unidades devueltas");
                        assertEquals(3, detalles.get(0).cantidadDisponible(),
                                        "Quedan 3 unidades disponibles para devolver");
                }

                /*
                 * CP-019: Devolucion de multiples productos en una sola transaccion.
                 */
                @Test
                @DisplayName("CP-019: Devolucion de varios productos en una sola operacion")
                void devolucionMultiplesProductos_funcionaCorrectamente() throws Exception {
                        insertarDetalleVenta(5, 1, 2, 4500.0, 9000.0); // Arroz
                        insertarDetalleVenta(5, 5, 3, 5200.0, 15600.0); // Frijol

                        var solicitud = solicitudExitosa(5,
                                        List.of(
                                                        new ItemDevolucion(1, 1, "motivo",
                                                                        EstadoProductoDevolucion.CERRADO),
                                                        new ItemDevolucion(5, 2, "motivo",
                                                                        EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.NOTA_CREDITO);

                        var resultado = useCase.procesarDevolucion(solicitud);

                        assertNotNull(resultado);
                        double esperado = 1 * 4500.0 + 2 * 5200.0;
                        assertEquals(esperado, resultado.totalDevuelto(),
                                        "Total = 1*4500 + 2*5200 = 14900");
                }

                /*
                 * CP-020: La devolucion exitosa genera el archivo de comprobante.
                 */
                @Test
                @DisplayName("CP-020: La devolucion exitosa genera archivo de comprobante")
                void devolucionExitosa_generaArchivoComprobante() throws Exception {
                        insertarDetalleVenta(5, 5, 2, 5200.0, 10400.0);

                        var solicitud = solicitudExitosa(5,
                                        List.of(new ItemDevolucion(5, 1, "motivo",
                                                        EstadoProductoDevolucion.CERRADO)),
                                        MetodoReembolso.NOTA_CREDITO);

                        var resultado = useCase.procesarDevolucion(solicitud);

                        java.nio.file.Path archivo = java.nio.file.Path.of(
                                        "target", "devoluciones",
                                        "devolucion-" + resultado.numeroDevolucion() + ".txt");
                        assertTrue(java.nio.file.Files.exists(archivo),
                                        "Debe generarse el archivo de comprobante en target/devoluciones/");
                }

                // ===== Helpers =====

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

                private int contarDevoluciones() throws java.sql.SQLException {
                        try (var conn = dbConnection.getConnection();
                                        var stmt = conn.prepareStatement("SELECT COUNT(*) FROM Devoluciones")) {
                                var rs = stmt.executeQuery();
                                rs.next();
                                return rs.getInt(1);
                        }
                }
        }

        // ===========================================================
        // procesarDevolucion - venta vieja con supervisor valido
        // ===========================================================
        @Nested
        @DisplayName("procesarDevolucion - autorizacion supervisor")
        class ProcesarDevolucionSupervisor {

                /*
                 * CP-021: Venta con mas de 30 dias, pero con supervisor VALIDO.
                 * Cubre la rama de validarPeriodoDevolucion donde el supervisor
                 * autoriza correctamente y la devolucion procede.
                 * Supervisor seed: usuario 'inventario', password '1234',
                 * rol SUPERVISOR_INVENTARIO.
                 */
                @Test
                @DisplayName("CP-021: Venta vieja con supervisor valido procesa la devolucion")
                void ventaVieja_supervisorValido_procesaDevolucion() throws Exception {
                        // Insertar venta de hace 45 dias
                        int idVentaVieja = insertarVentaConFecha(
                                        LocalDate.now().minusDays(45), "MANANA", 3);
                        insertarDetalleVenta(idVentaVieja, 5, 3, 5200.0, 15600.0);

                        var solicitud = new SolicitudDevolucion(
                                        idVentaVieja, 1, "Devolucion tardia autorizada",
                                        MetodoReembolso.NOTA_CREDITO,
                                        "inventario", "1234",
                                        List.of(new ItemDevolucion(5, 1, "motivo",
                                                        EstadoProductoDevolucion.CERRADO)));

                        var resultado = useCase.procesarDevolucion(solicitud);

                        assertNotNull(resultado,
                                        "Con supervisor valido la devolucion debe procesarse aunque la venta sea vieja");
                        assertEquals(5200.0, resultado.totalDevuelto());
                }

                /*
                 * CP-022: Producto ABIERTO con supervisor VALIDO.
                 * Cubre la rama requiereSupervisor=true con validacion exitosa.
                 */
                @Test
                @DisplayName("CP-022: Producto ABIERTO con supervisor valido procesa la devolucion")
                void productoAbierto_supervisorValido_procesaDevolucion() throws Exception {
                        insertarDetalleVenta(5, 5, 3, 5200.0, 15600.0);

                        var solicitud = new SolicitudDevolucion(
                                        5, 1, "Producto abierto autorizado",
                                        MetodoReembolso.NOTA_CREDITO,
                                        "inventario", "1234",
                                        List.of(new ItemDevolucion(5, 1, "motivo",
                                                        EstadoProductoDevolucion.ABIERTO)));

                        var resultado = useCase.procesarDevolucion(solicitud);

                        assertNotNull(resultado,
                                        "Con supervisor valido, un producto ABIERTO debe poder devolverse");
                }
        }

        @Nested
        @DisplayName("Cobertura de ramas adicionales - Devolucion")
        class CoberturaRamasAdicionalesDevolucion {

                private void insertarSaldoCaja(int idEmpleado, int idVenta, double monto) throws Exception {
                        try (Connection conn = dbConnection.getConnection();
                                        PreparedStatement stmt = conn.prepareStatement(
                                                        """
                                                                        INSERT INTO Caja
                                                                        (id_empleado, id_venta, fecha_apertura, fecha_cierre, monto_inicial, monto_final, estado)
                                                                        VALUES (?, ?, CURRENT_DATE, CURRENT_DATE, 0, ?, TRUE)
                                                                        """)) {
                                stmt.setInt(1, idEmpleado);
                                stmt.setInt(2, idVenta);
                                stmt.setDouble(3, monto);
                                stmt.executeUpdate();
                        }
                }

                @Test
                @DisplayName("CP-023: Devolucion con EFECTIVO y saldo de caja insuficiente lanza IllegalArgumentException")
                void devolucionEfectivoCajaInsuficiente_lanzaIllegalArgumentException() throws Exception {
                        insertarDetalleVenta(5, 5, 2, 5200.0, 10400.0);

                        var solicitud = new SolicitudDevolucion(
                                        5, 3, "Reembolso en efectivo sin caja",
                                        MetodoReembolso.EFECTIVO,
                                        null, null,
                                        List.of(new ItemDevolucion(5, 1, "Cerrado", EstadoProductoDevolucion.CERRADO)));

                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> useCase.procesarDevolucion(solicitud));
                        assertTrue(ex.getMessage().contains("Efectivo insuficiente en caja"));
                }

                @Test
                @DisplayName("CP-024: Devolucion con EFECTIVO y saldo de caja suficiente se procesa correctamente")
                void devolucionEfectivoCajaSuficiente_procesaCorrectamente() throws Exception {
                        int idVenta = insertarVentaConFecha(LocalDate.now(), "MANANA", 3);

                        insertarDetalleVenta(idVenta, 5, 2, 5200.0, 10400.0);
                        insertarSaldoCaja(3, idVenta, 50000.0);

                        var solicitud = new SolicitudDevolucion(
                                idVenta,
                                3,
                                "Reembolso en efectivo con caja",
                                MetodoReembolso.EFECTIVO,
                                null,
                                null,
                                List.of(new ItemDevolucion(5, 1, "Cerrado", EstadoProductoDevolucion.CERRADO))
                        );

                        var resultado = useCase.procesarDevolucion(solicitud);

                        assertNotNull(resultado);
                        assertEquals(5200.0, resultado.totalDevuelto());
                }

                @Test
                @DisplayName("CP-025: obtenerDetallesVenta con SQLException en conexion lanza RuntimeException")
                void obtenerDetallesVentaErrorSql_lanzaRuntimeException() {
                        DatabaseConnection faultyDb = new DatabaseConnection() {
                                @Override
                                public Connection getConnection() throws java.sql.SQLException {
                                        throw new java.sql.SQLException("Simulated database failure");
                                }
                        };

                        ProcesarDevolucionUseCase faultyUseCase = new ProcesarDevolucionUseCase(
                                        faultyDb,
                                        new H2DevolucionRepository(),
                                        new H2DetalleDevolucionRepository(),
                                        new H2ProductoRepository(dbConnection),
                                        new H2MovimientoInventarioRepository(dbConnection),
                                        new H2CajaRepository(),
                                        new H2UsuarioRepository(dbConnection),
                                        new H2CuentaFidelizacionRepository(dbConnection));

                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> faultyUseCase.obtenerDetallesVenta(5));
                        assertTrue(ex.getMessage().contains("Error al obtener detalles"));
                }

                @Test
                @DisplayName("CP-026: procesarDevolucion con SQLException en conexion lanza RuntimeException")
                void devolucionErrorSql_lanzaRuntimeException() {
                        DatabaseConnection faultyDb = new DatabaseConnection() {
                                @Override
                                public Connection getConnection() throws java.sql.SQLException {
                                        throw new java.sql.SQLException("Simulated database failure");
                                }
                        };

                        ProcesarDevolucionUseCase faultyUseCase = new ProcesarDevolucionUseCase(
                                        faultyDb,
                                        new H2DevolucionRepository(),
                                        new H2DetalleDevolucionRepository(),
                                        new H2ProductoRepository(dbConnection),
                                        new H2MovimientoInventarioRepository(dbConnection),
                                        new H2CajaRepository(),
                                        new H2UsuarioRepository(dbConnection),
                                        new H2CuentaFidelizacionRepository(dbConnection));

                        var solicitud = new SolicitudDevolucion(
                                        5, 3, "Error de conexion",
                                        MetodoReembolso.NOTA_CREDITO,
                                        null, null,
                                        List.of(new ItemDevolucion(5, 1, "Cerrado", EstadoProductoDevolucion.CERRADO)));

                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> faultyUseCase.procesarDevolucion(solicitud));
                        assertTrue(ex.getMessage().contains("Error procesando la devolución"));
                }

                @Test
                @DisplayName("CP-027: procesarDevolucion con error inesperado en repositorio lanza RuntimeException")
                void devolucionErrorGenerico_lanzaRuntimeException() {
                        DevolucionRepository faultyDevolucionRepo = new DevolucionRepository() {
                                @Override
                                public VentaInfo obtenerVenta(Connection conn, int idVenta)
                                                throws java.sql.SQLException {
                                        throw new RuntimeException("Simulated unexpected exception");
                                }

                                @Override
                                public List<DetalleVentaRetornable> obtenerDetallesVenta(Connection conn, int idVenta)
                                                throws java.sql.SQLException {
                                        return List.of();
                                }

                                @Override
                                public int guardarDevolucion(Connection conn, SolicitudDevolucion solicitud,
                                                double totalDevuelto, String numeroDevolucion)
                                                throws java.sql.SQLException {
                                        return 0;
                                }
                        };

                        ProcesarDevolucionUseCase faultyUseCase = new ProcesarDevolucionUseCase(
                                        dbConnection,
                                        faultyDevolucionRepo,
                                        new H2DetalleDevolucionRepository(),
                                        new H2ProductoRepository(dbConnection),
                                        new H2MovimientoInventarioRepository(dbConnection),
                                        new H2CajaRepository(),
                                        new H2UsuarioRepository(dbConnection),
                                        new H2CuentaFidelizacionRepository(dbConnection));

                        var solicitud = new SolicitudDevolucion(
                                        5, 3, "Error en transaccion",
                                        MetodoReembolso.NOTA_CREDITO,
                                        null, null,
                                        List.of(new ItemDevolucion(5, 1, "Cerrado", EstadoProductoDevolucion.CERRADO)));

                        RuntimeException ex = assertThrows(RuntimeException.class,
                                        () -> faultyUseCase.procesarDevolucion(solicitud));
                        assertTrue(ex.getMessage().contains("Simulated unexpected exception"));
                }
        }
}