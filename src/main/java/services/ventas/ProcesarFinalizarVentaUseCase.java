package services.ventas;

import aggregates.Cliente;
import aggregates.Empleado;
import aggregates.Producto;
import repositories.DatabaseConnection;
import repositories.ProductoRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class ProcesarFinalizarVentaUseCase {
    private static final double IVA = 0.19;
    private static final Locale LOCALE_CO = Locale.of("es", "CO");

    private final DatabaseConnection databaseConnection;
    private final ProductoRepository productoRepository;

    public ProcesarFinalizarVentaUseCase(DatabaseConnection databaseConnection,
                                         ProductoRepository productoRepository) {
        this.databaseConnection = databaseConnection;
        this.productoRepository = productoRepository;
    }

    public Producto buscarProductoPorId(String codigoEscaneado) {
        int idProducto = parsearEntero(codigoEscaneado, "Product not found. Code: " + codigoEscaneado);
        return productoRepository.buscarPorId(idProducto)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("Product not found. Code: " + codigoEscaneado));
    }

    public Optional<ClienteConCuenta> buscarClientePorCodigo(String codigoCliente) {
        if (codigoCliente == null || codigoCliente.isBlank()) {
            return Optional.empty();
        }

        int codigo = parsearEntero(codigoCliente, "Customer not found");
        String sql = """
            SELECT c.id_cliente, c.nombre, c.apellido, c.correo, c.telefono, c.direccion,
                   c.fecha_registro, c.estado_activo,
                   cf.id_fidelizacion, cf.numero_tarjeta, cf.puntos_actuales,
                   cf.fecha_creacion, cf.estado
            FROM Cliente c
            LEFT JOIN Cuenta_fidelizacion cf ON c.id_cliente = cf.id_cliente AND cf.estado = TRUE
            WHERE c.estado_activo = TRUE
              AND (c.id_cliente = ? OR cf.numero_tarjeta = ?)
            ORDER BY cf.id_fidelizacion DESC
            LIMIT 1
        """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, codigo);
            stmt.setInt(2, codigo);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Cliente cliente = new Cliente(
                        rs.getInt("id_cliente"),
                        rs.getString("nombre"),
                        rs.getString("apellido"),
                        rs.getString("correo"),
                        rs.getString("telefono"),
                        rs.getString("direccion"),
                        rs.getDate("fecha_registro").toLocalDate(),
                        rs.getBoolean("estado_activo")
                );

                int idCuenta = rs.getInt("id_fidelizacion");
                Integer cuentaId = rs.wasNull() ? null : idCuenta;
                Integer numeroTarjeta = cuentaId == null ? null : rs.getInt("numero_tarjeta");
                Integer puntos = cuentaId == null ? null : rs.getInt("puntos_actuales");

                return Optional.of(new ClienteConCuenta(cliente, cuentaId, numeroTarjeta, puntos));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar cliente", e);
        }

        return Optional.empty();
    }

    public ResumenVenta calcularResumen(List<ItemVenta> items,
                                        ClienteConCuenta cliente,
                                        DescuentoManual descuentoManual) {
        if (items == null || items.isEmpty()) {
            return new ResumenVenta(0, 0, 0, 0, 0, 0, 0, 0);
        }

        double subtotal = items.stream().mapToDouble(ItemVenta::subtotal).sum();
        double descuentoPromocion = items.stream()
                .filter(item -> item.cantidad() >= 3)
                .mapToDouble(item -> item.subtotal() * 0.05)
                .sum();
        double descuentoFidelidad = cliente != null && cliente.cuentaId() != null && subtotal >= 30000
                ? subtotal * 0.03
                : 0;
        double descuentoAutomatico = descuentoPromocion + descuentoFidelidad;
        double descuentoManualValor = descuentoManual.calcular(subtotal - descuentoAutomatico);
        double baseGravable = Math.max(subtotal - descuentoAutomatico - descuentoManualValor, 0);
        double impuestos = baseGravable * IVA;
        double total = baseGravable + impuestos;
        int puntosGanados = cliente == null || cliente.cuentaId() == null ? 0 : (int) Math.floor(total / 1000);

        return new ResumenVenta(
                redondear(subtotal),
                redondear(descuentoPromocion),
                redondear(descuentoFidelidad),
                redondear(descuentoManualValor),
                redondear(descuentoAutomatico + descuentoManualValor),
                redondear(impuestos),
                redondear(total),
                puntosGanados
        );
    }

    public ResultadoVenta procesarVenta(SolicitudVenta solicitud) {
        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new IllegalArgumentException("You must add at least one product to the sale");
        }
        if (solicitud.empleado() == null) {
            throw new IllegalArgumentException("No hay un cajero autenticado. Inicie sesion nuevamente.");
        }

        ResumenVenta resumen = calcularResumen(solicitud.items(), solicitud.cliente(), solicitud.descuentoManual());
        PagoProcesado pago = procesarPago(solicitud.metodoPago(), resumen.total(), solicitud.montoRecibido(),
                solicitud.referenciaPago());

        if (solicitud.facturaElectronica() &&
                (solicitud.datosFacturacion() == null || solicitud.datosFacturacion().isBlank())) {
            throw new IllegalArgumentException("Debe ingresar los datos fiscales para la factura electronica");
        }
        if (solicitud.enviarPorCorreo() &&
                (solicitud.correoTicket() == null || solicitud.correoTicket().isBlank())) {
            throw new IllegalArgumentException("Debe ingresar un correo para enviar el ticket");
        }

        String numeroTicket = generarTicket();

        try (Connection conn = databaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int ventaId = guardarVenta(conn, solicitud, resumen);
                guardarDetalles(conn, ventaId, solicitud.items());
                guardarPago(conn, ventaId, solicitud.metodoPago(), resumen.total(), pago);
                actualizarInventario(conn, solicitud.items(), solicitud.empleado().getId(), ventaId);
                actualizarCaja(conn, solicitud.empleado().getId(), ventaId, solicitud.metodoPago(),
                        calcularIncrementoCaja(solicitud.metodoPago(), resumen.total(), pago));
                acreditarPuntos(conn, solicitud.cliente(), ventaId, resumen.puntosGanados());
                conn.commit();

                try {
                    generarTicketArchivo(numeroTicket, ventaId, solicitud, resumen, pago);
                } catch (IOException e) {
                    return new ResultadoVenta(numeroTicket, pago.cambio(),
                            "Sale recorded but printing error. Number: " + numeroTicket,
                            resumen.total(), pago.autorizacion());
                }

                return new ResultadoVenta(numeroTicket, pago.cambio(),
                        "Sale completed. Change: " + formatearMoneda(pago.cambio()) + ". Ticket: " + numeroTicket,
                        resumen.total(), pago.autorizacion());
            } catch (IllegalArgumentException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Transaction processing error", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Transaction processing error", e);
        }
    }

    private int guardarVenta(Connection conn, SolicitudVenta solicitud, ResumenVenta resumen) throws SQLException {
        String sql = """
            INSERT INTO Venta
            (id_empleado, fecha_venta, subtotal, descuento_total, impuesto_total, total_final, estado_venta)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, solicitud.empleado().getId());
            stmt.setDate(2, Date.valueOf(LocalDate.now()));
            stmt.setDouble(3, resumen.subtotal());
            stmt.setInt(4, (int) Math.round(resumen.descuentoTotal()));
            stmt.setDouble(5, resumen.impuestos());
            stmt.setDouble(6, resumen.total());
            stmt.setBoolean(7, true);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("No fue posible registrar la venta");
        }
    }

    private void guardarDetalles(Connection conn, int ventaId, List<ItemVenta> items) throws SQLException {
        String sql = """
            INSERT INTO Detalle_venta
            (id_venta, id_producto, cantidad, precio_unitario, descuento, subtotal)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ItemVenta item : items) {
                stmt.setInt(1, ventaId);
                stmt.setInt(2, item.productoId());
                stmt.setInt(3, item.cantidad());
                stmt.setDouble(4, item.precioUnitario());
                stmt.setInt(5, 0);
                stmt.setDouble(6, item.subtotal());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    private void guardarPago(Connection conn, int ventaId, MetodoPago metodoPago, double total,
                             PagoProcesado pago) throws SQLException {
        String sql = "INSERT INTO Pago_venta (id_venta, id_tipo_pago, monto, fecha_pago) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (metodoPago == MetodoPago.MIXTO) {
                agregarPagoBatch(stmt, ventaId, 1, pago.montoEfectivo());
                agregarPagoBatch(stmt, ventaId, 2, total - pago.montoEfectivo());
                stmt.executeBatch();
                return;
            }

            agregarPagoBatch(stmt, ventaId, obtenerTipoPagoId(metodoPago), total);
            stmt.executeBatch();
        }
    }

    private void agregarPagoBatch(PreparedStatement stmt, int ventaId, int tipoPago, double monto)
            throws SQLException {
        stmt.setInt(1, ventaId);
        stmt.setInt(2, tipoPago);
        stmt.setDouble(3, redondear(monto));
        stmt.setDate(4, Date.valueOf(LocalDate.now()));
        stmt.addBatch();
    }

    private void actualizarInventario(Connection conn, List<ItemVenta> items, int empleadoId, int ventaId)
            throws SQLException {
        String sqlStock = """
            UPDATE Producto
            SET stock_actual = stock_actual - ?
            WHERE id_producto = ? AND stock_actual >= ? AND estado_activo = TRUE
        """;
        String sqlMovimiento = """
            INSERT INTO Movimiento_inventario
            (id_empleado, id_tipo_movimiento, id_producto, cantidad, stock_anterior,
             stock_nuevo, motivo, fecha_movimiento)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmtStock = conn.prepareStatement(sqlStock);
             PreparedStatement stmtMovimiento = conn.prepareStatement(sqlMovimiento)) {

            for (ItemVenta item : items) {
                int stockActual = obtenerStockActual(conn, item.productoId());
                if (stockActual < item.cantidad()) {
                    throw new IllegalArgumentException("Insufficient stock. Available: " + stockActual + " units");
                }

                stmtStock.setInt(1, item.cantidad());
                stmtStock.setInt(2, item.productoId());
                stmtStock.setInt(3, item.cantidad());
                if (stmtStock.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Insufficient stock. Available: "
                            + obtenerStockActual(conn, item.productoId()) + " units");
                }

                stmtMovimiento.setInt(1, empleadoId);
                stmtMovimiento.setInt(2, 2);
                stmtMovimiento.setInt(3, item.productoId());
                stmtMovimiento.setInt(4, item.cantidad());
                stmtMovimiento.setInt(5, stockActual);
                stmtMovimiento.setInt(6, stockActual - item.cantidad());
                stmtMovimiento.setString(7, "Venta " + ventaId);
                stmtMovimiento.setDate(8, Date.valueOf(LocalDate.now()));
                stmtMovimiento.addBatch();
            }

            stmtMovimiento.executeBatch();
        }
    }

    private int obtenerStockActual(Connection conn, int productoId) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT stock_actual FROM Producto WHERE id_producto = ?")) {
            stmt.setInt(1, productoId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("stock_actual");
            }
            return 0;
        }
    }

    private void actualizarCaja(Connection conn, int empleadoId, int ventaId, MetodoPago metodoPago, double montoEfectivo)
            throws SQLException {
        if (metodoPago != MetodoPago.EFECTIVO && metodoPago != MetodoPago.MIXTO) {
            return;
        }

        String sql = """
            INSERT INTO Caja
            (id_empleado, id_venta, fecha_apertura, fecha_cierre, monto_inicial, monto_final, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empleadoId);
            stmt.setInt(2, ventaId);
            stmt.setDate(3, Date.valueOf(LocalDate.now()));
            stmt.setDate(4, Date.valueOf(LocalDate.now()));
            stmt.setDouble(5, 0);
            stmt.setDouble(6, montoEfectivo);
            stmt.setBoolean(7, true);
            stmt.executeUpdate();
        }
    }

    private void acreditarPuntos(Connection conn, ClienteConCuenta cliente, int ventaId, int puntos) throws SQLException {
        if (cliente == null || cliente.cuentaId() == null || puntos <= 0) {
            return;
        }

        String sqlCuenta = """
            UPDATE Cuenta_fidelizacion
            SET puntos_actuales = puntos_actuales + ?
            WHERE id_fidelizacion = ?
        """;
        String sqlMovimiento = """
            INSERT INTO Movimiento_puntos
            (id_tipo_movimiento_puntos, id_venta, puntos, fecha_movimiento)
            VALUES (?, ?, ?, ?)
        """;
        String sqlRelacion = """
            INSERT INTO CuentaXMovimiento
            (id_movimiento, id_cuenta_fidelizacion)
            VALUES (?, ?)
        """;

        try (PreparedStatement stmtCuenta = conn.prepareStatement(sqlCuenta);
             PreparedStatement stmtMovimiento = conn.prepareStatement(sqlMovimiento, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtRelacion = conn.prepareStatement(sqlRelacion)) {

            stmtCuenta.setInt(1, puntos);
            stmtCuenta.setInt(2, cliente.cuentaId());
            stmtCuenta.executeUpdate();

            stmtMovimiento.setInt(1, 1);
            stmtMovimiento.setInt(2, ventaId);
            stmtMovimiento.setInt(3, puntos);
            stmtMovimiento.setDate(4, Date.valueOf(LocalDate.now()));
            stmtMovimiento.executeUpdate();

            ResultSet rs = stmtMovimiento.getGeneratedKeys();
            if (rs.next()) {
                stmtRelacion.setInt(1, rs.getInt(1));
                stmtRelacion.setInt(2, cliente.cuentaId());
                stmtRelacion.executeUpdate();
            }
        }
    }

    private PagoProcesado procesarPago(MetodoPago metodoPago, double total, double montoRecibido, String referenciaPago) {
        if (metodoPago == null) {
            throw new IllegalArgumentException("You must select a payment method");
        }

        return switch (metodoPago) {
            case EFECTIVO -> procesarPagoEfectivo(total, montoRecibido);
            case TARJETA -> procesarPagoTarjeta(referenciaPago, "AUTH");
            case TRANSFERENCIA -> new PagoProcesado(0, 0, limpiarReferencia(referenciaPago, "TRF"));
            case MIXTO -> procesarPagoMixto(total, montoRecibido, referenciaPago);
        };
    }

    private PagoProcesado procesarPagoEfectivo(double total, double montoRecibido) {
        if (montoRecibido < total) {
            throw new IllegalArgumentException("Insufficient amount. Missing: " + formatearMoneda(total - montoRecibido));
        }
        return new PagoProcesado(redondear(montoRecibido - total), montoRecibido, null);
    }

    private PagoProcesado procesarPagoTarjeta(String referenciaPago, String prefijo) {
        String referencia = referenciaPago == null ? "" : referenciaPago.trim().toUpperCase();
        if ("DECLINE".equals(referencia)) {
            throw new IllegalArgumentException("Payment rejected");
        }
        if ("ERROR".equals(referencia)) {
            throw new IllegalArgumentException("Error processing card payment. Try another method or check the connection");
        }
        return new PagoProcesado(0, 0, limpiarReferencia(referenciaPago, prefijo));
    }

    private PagoProcesado procesarPagoMixto(double total, double montoRecibido, String referenciaPago) {
        if (montoRecibido <= 0 || montoRecibido >= total) {
            throw new IllegalArgumentException("Para pago mixto el efectivo debe ser mayor a cero y menor al total");
        }
        PagoProcesado tarjeta = procesarPagoTarjeta(referenciaPago, "MIX");
        return new PagoProcesado(tarjeta.cambio(), montoRecibido, tarjeta.autorizacion());
    }

    private String limpiarReferencia(String referenciaPago, String prefijo) {
        if (referenciaPago != null && !referenciaPago.isBlank()) {
            return referenciaPago.trim().toUpperCase();
        }
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private int obtenerTipoPagoId(MetodoPago metodoPago) {
        return switch (metodoPago) {
            case EFECTIVO -> 1;
            case TARJETA -> 2;
            case TRANSFERENCIA -> 3;
            case MIXTO -> 4;
        };
    }

    private double calcularIncrementoCaja(MetodoPago metodoPago, double total, PagoProcesado pago) {
        return switch (metodoPago) {
            case EFECTIVO -> total;
            case MIXTO -> pago.montoEfectivo();
            case TARJETA, TRANSFERENCIA -> 0;
        };
    }

    private void generarTicketArchivo(String numeroTicket, int ventaId, SolicitudVenta solicitud,
                                      ResumenVenta resumen, PagoProcesado pago) throws IOException {
        Path directorio = Path.of("target", "tickets");
        Files.createDirectories(directorio);

        StringBuilder contenido = new StringBuilder();
        contenido.append("MasterMarket").append(System.lineSeparator());
        contenido.append("Ticket: ").append(numeroTicket).append(System.lineSeparator());
        contenido.append("Venta BD: ").append(ventaId).append(System.lineSeparator());
        contenido.append("Cajero: ").append(solicitud.empleado().getNombre()).append(" ")
                .append(solicitud.empleado().getApellido()).append(System.lineSeparator());
        if (solicitud.cliente() != null) {
            contenido.append("Cliente: ").append(solicitud.cliente().cliente().getNombreCompleto()).append(System.lineSeparator());
        }
        contenido.append(System.lineSeparator());

        for (ItemVenta item : solicitud.items()) {
            contenido.append("#").append(item.productoId())
                    .append(" ").append(item.nombreProducto())
                    .append(" x").append(item.cantidad())
                    .append(" -> ").append(formatearMoneda(item.subtotal()))
                    .append(System.lineSeparator());
        }

        contenido.append(System.lineSeparator());
        contenido.append("Subtotal: ").append(formatearMoneda(resumen.subtotal())).append(System.lineSeparator());
        contenido.append("Descuentos: ").append(formatearMoneda(resumen.descuentoTotal())).append(System.lineSeparator());
        contenido.append("Impuestos: ").append(formatearMoneda(resumen.impuestos())).append(System.lineSeparator());
        contenido.append("Total: ").append(formatearMoneda(resumen.total())).append(System.lineSeparator());
        contenido.append("Pago: ").append(solicitud.metodoPago().name()).append(System.lineSeparator());
        if (pago.autorizacion() != null) {
            contenido.append("Autorizacion: ").append(pago.autorizacion()).append(System.lineSeparator());
        }
        if (solicitud.enviarPorCorreo()) {
            contenido.append("Enviar por correo a: ").append(solicitud.correoTicket()).append(System.lineSeparator());
        }
        if (solicitud.facturaElectronica()) {
            contenido.append("Factura electronica: ").append(solicitud.datosFacturacion()).append(System.lineSeparator());
        }

        Files.writeString(directorio.resolve("ticket-" + numeroTicket + ".txt"), contenido.toString());
    }

    private int parsearEntero(String valor, String mensajeError) {
        try {
            return Integer.parseInt(valor == null ? "" : valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(mensajeError);
        }
    }

    private String generarTicket() {
        return "TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static String formatearMoneda(double valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_CO).format(valor);
    }

    public enum MetodoPago {
        EFECTIVO,
        TARJETA,
        TRANSFERENCIA,
        MIXTO
    }

    public enum TipoDescuento {
        NINGUNO,
        PORCENTAJE,
        VALOR_FIJO
    }

    public record ClienteConCuenta(Cliente cliente, Integer cuentaId, Integer numeroTarjeta, Integer puntosActuales) {
    }

    public record ItemVenta(int productoId, String nombreProducto, int cantidad,
                            double precioUnitario, int stockDisponible) {
        public double subtotal() {
            return redondear(cantidad * precioUnitario);
        }
    }

    public record DescuentoManual(TipoDescuento tipo, double valor) {
        public static DescuentoManual ninguno() {
            return new DescuentoManual(TipoDescuento.NINGUNO, 0);
        }

        public double calcular(double base) {
            if (tipo == null || tipo == TipoDescuento.NINGUNO || valor <= 0) {
                return 0;
            }
            return switch (tipo) {
                case PORCENTAJE -> redondear(Math.min(base * (valor / 100), base));
                case VALOR_FIJO -> redondear(Math.min(valor, base));
                case NINGUNO -> 0;
            };
        }
    }

    public record ResumenVenta(double subtotal, double descuentoPromocion, double descuentoFidelidad,
                               double descuentoManual, double descuentoTotal, double impuestos,
                               double total, int puntosGanados) {
    }

    public record SolicitudVenta(List<ItemVenta> items, ClienteConCuenta cliente, Empleado empleado,
                                 DescuentoManual descuentoManual, MetodoPago metodoPago, double montoRecibido,
                                 String referenciaPago, boolean facturaElectronica, String datosFacturacion,
                                 boolean enviarPorCorreo, String correoTicket) {
    }

    public record ResultadoVenta(String numeroTicket, double cambio, String mensaje,
                                 double total, String autorizacionPago) {
    }

    private record PagoProcesado(double cambio, double montoEfectivo, String autorizacion) {
    }
}
