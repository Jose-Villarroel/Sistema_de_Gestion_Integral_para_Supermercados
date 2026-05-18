package services.ventas;

import repositories.DatabaseConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProcesarDevolucionUseCase {

    private static final int DIAS_MAXIMOS_DEVOLUCION = 30;

    private final DatabaseConnection databaseConnection;

    public ProcesarDevolucionUseCase(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<DetalleVentaRetornable> obtenerDetallesVenta(int idVenta) {
        try (Connection conn = databaseConnection.getConnection()) {
            return obtenerDetallesVenta(conn, idVenta);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener detalles de la venta", e);
        }
    }

    private List<DetalleVentaRetornable> obtenerDetallesVenta(Connection conn, int idVenta) throws SQLException {
        List<DetalleVentaRetornable> detalles = new ArrayList<>();
        String sql = """
            SELECT dv.id_producto, p.nombre, dv.cantidad AS comprada, dv.precio_unitario, dv.subtotal,
                   COALESCE((
                       SELECT SUM(dd.cantidad)
                       FROM Detalle_devolucion dd
                       JOIN Devoluciones d ON dd.id_devoluciones = d.id_devoluciones
                       WHERE d.id_venta = ? AND dd.id_producto = dv.id_producto
                   ), 0) AS devuelta
            FROM Detalle_venta dv
            JOIN Producto p ON dv.id_producto = p.id_producto
            WHERE dv.id_venta = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVenta);
            stmt.setInt(2, idVenta);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int comprada = rs.getInt("comprada");
                double subtotal = rs.getDouble("subtotal");
                double precioUnitario = rs.getDouble("precio_unitario");
                double precioReal = comprada > 0 ? subtotal / comprada : precioUnitario;

                detalles.add(new DetalleVentaRetornable(
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        comprada,
                        rs.getInt("devuelta"),
                        precioReal
                ));
            }
        }

        return detalles;
    }

    public ResultadoDevolucion procesarDevolucion(SolicitudDevolucion solicitud) {
        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un producto para devolver.");
        }

        try (Connection conn = databaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                VentaInfo venta = obtenerVenta(conn, solicitud.idVenta());
                if (venta == null) {
                    throw new IllegalArgumentException("Ticket no encontrado o inválido.");
                }

                validarPeriodoDevolucion(
                        venta.fechaVenta(),
                        solicitud.supervisorUsuario(),
                        solicitud.supervisorPassword(),
                        conn
                );

                double totalDevuelto = 0;
                List<DetalleVentaRetornable> disponibles = obtenerDetallesVenta(conn, solicitud.idVenta());

                boolean requiereSupervisor = false;

                for (ItemDevolucion item : solicitud.items()) {
                    if (item.cantidad() <= 0) {
                        continue;
                    }

                    DetalleVentaRetornable detalle = disponibles.stream()
                            .filter(d -> d.idProducto() == item.idProducto())
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "El producto " + item.idProducto() + " no pertenece a la compra original."
                            ));

                    if (item.cantidad() > (detalle.comprada() - detalle.devuelta())) {
                        throw new IllegalArgumentException("Cantidad a devolver excede cantidad comprada.");
                    }

                    if (item.estadoProducto() == EstadoProductoDevolucion.ABIERTO) {
                        requiereSupervisor = true;
                    }

                    totalDevuelto += item.cantidad() * detalle.precioRealUnitario();
                }

                if (requiereSupervisor) {
                    validarSupervisor(conn, solicitud.supervisorUsuario(), solicitud.supervisorPassword());
                }

                if (totalDevuelto <= 0) {
                    throw new IllegalArgumentException("El total a devolver debe ser mayor a cero.");
                }

                if (solicitud.metodoReembolso() == MetodoReembolso.EFECTIVO) {
                    validarEfectivoDisponible(conn, venta.idEmpleado(), venta.turno(), totalDevuelto);
                }

                String numeroDevolucion = "DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                int idDevolucion = guardarDevolucion(conn, solicitud, totalDevuelto, numeroDevolucion);
                guardarDetalles(conn, idDevolucion, solicitud, disponibles);
                actualizarStockYMovimiento(conn, solicitud);
                registrarImpactoCaja(conn, solicitud.idEmpleado(), solicitud.idVenta(), solicitud.metodoReembolso(), totalDevuelto);
                descontarPuntosSiAplica(conn, solicitud.idVenta());
                generarComprobante(numeroDevolucion, solicitud.idVenta(), totalDevuelto, solicitud.metodoReembolso());

                conn.commit();
                return new ResultadoDevolucion(numeroDevolucion, totalDevuelto);

            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error procesando la devolución: " + e.getMessage(), e);
        }
    }

    private VentaInfo obtenerVenta(Connection conn, int idVenta) throws SQLException {
        String sql = """
            SELECT id_venta, id_empleado, fecha_venta, turno, estado_venta
            FROM Venta
            WHERE id_venta = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVenta);
            ResultSet rs = stmt.executeQuery();

            if (rs.next() && rs.getBoolean("estado_venta")) {
                return new VentaInfo(
                        rs.getInt("id_venta"),
                        rs.getInt("id_empleado"),
                        rs.getDate("fecha_venta").toLocalDate(),
                        rs.getString("turno")
                );
            }
            return null;
        }
    }

    private void validarPeriodoDevolucion(
            LocalDate fechaVenta,
            String supervisorUsuario,
            String supervisorPassword,
            Connection conn
    ) throws SQLException {
        long dias = ChronoUnit.DAYS.between(fechaVenta, LocalDate.now());

        if (dias > DIAS_MAXIMOS_DEVOLUCION) {
            validarSupervisor(conn, supervisorUsuario, supervisorPassword);
        }
    }

    private void validarSupervisor(Connection conn, String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Se requiere autorización del supervisor.");
        }

        String sql = """
            SELECT u.password_hash, r.nombre_rol
            FROM Usuario u
            JOIN Rol r ON u.id_rol = r.id_rol
            WHERE u.username = ? AND u.estado_usuario = TRUE
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                throw new IllegalArgumentException("Supervisor no encontrado.");
            }

            String hashBd = rs.getString("password_hash");
            String rol = rs.getString("nombre_rol");

            if (!"SUPERVISOR_INVENTARIO".equalsIgnoreCase(rol)) {
                throw new IllegalArgumentException("El usuario ingresado no tiene rol de supervisor.");
            }

            if (!hashBd.equals(String.valueOf(password.hashCode()))) {
                throw new IllegalArgumentException("Credenciales de supervisor inválidas.");
            }
        }
    }

    private void validarEfectivoDisponible(Connection conn, int idEmpleado, String turno, double montoDevuelto) throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(c.monto_final), 0) AS disponible
            FROM Caja c
            JOIN Venta v ON v.id_venta = c.id_venta
            WHERE c.id_empleado = ?
              AND v.turno = ?
              AND v.fecha_venta = ?
              AND c.estado = TRUE
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEmpleado);
            stmt.setString(2, turno);
            stmt.setDate(3, Date.valueOf(LocalDate.now()));

            ResultSet rs = stmt.executeQuery();
            rs.next();

            double disponible = rs.getDouble("disponible");
            if (disponible < montoDevuelto) {
                throw new IllegalArgumentException("Efectivo insuficiente en caja. Ofrezca otro método de reembolso.");
            }
        }
    }

    private int guardarDevolucion(
            Connection conn,
            SolicitudDevolucion solicitud,
            double totalDevuelto,
            String numeroDevolucion
    ) throws SQLException {
        String sql = """
            INSERT INTO Devoluciones
            (id_empleado, id_venta, fecha_devolucion, motivo, total_devuelto, estado, metodo_reembolso, numero_devolucion)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, solicitud.idEmpleado());
            stmt.setInt(2, solicitud.idVenta());
            stmt.setDate(3, Date.valueOf(LocalDate.now()));
            stmt.setString(4, solicitud.motivoGeneral());
            stmt.setDouble(5, totalDevuelto);
            stmt.setBoolean(6, true);
            stmt.setString(7, solicitud.metodoReembolso().name());
            stmt.setString(8, numeroDevolucion);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new SQLException("No se pudo registrar la devolución principal.");
        }
    }

    private void guardarDetalles(
            Connection conn,
            int idDevolucion,
            SolicitudDevolucion solicitud,
            List<DetalleVentaRetornable> disponibles
    ) throws SQLException {
        String sql = """
            INSERT INTO Detalle_devolucion
            (id_devoluciones, id_producto, cantidad, subtotal_devuelto, motivo, estado_producto, reintegra_inventario)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ItemDevolucion item : solicitud.items()) {
                if (item.cantidad() <= 0) {
                    continue;
                }

                DetalleVentaRetornable detalle = disponibles.stream()
                        .filter(d -> d.idProducto() == item.idProducto())
                        .findFirst()
                        .orElseThrow();

                boolean reintegra = item.estadoProducto() != EstadoProductoDevolucion.DEFECTUOSO
                        && item.estadoProducto() != EstadoProductoDevolucion.VENCIDO;

                stmt.setInt(1, idDevolucion);
                stmt.setInt(2, item.idProducto());
                stmt.setInt(3, item.cantidad());
                stmt.setDouble(4, item.cantidad() * detalle.precioRealUnitario());
                stmt.setString(5, item.motivo());
                stmt.setString(6, item.estadoProducto().name());
                stmt.setBoolean(7, reintegra);
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    private void actualizarStockYMovimiento(Connection conn, SolicitudDevolucion solicitud) throws SQLException {
        String sqlStock = "UPDATE Producto SET stock_actual = stock_actual + ? WHERE id_producto = ?";
        String sqlMovimiento = """
            INSERT INTO Movimiento_inventario
            (id_empleado, id_tipo_movimiento, id_producto, cantidad, stock_anterior, stock_nuevo, motivo, fecha_movimiento)
            VALUES (?, 1, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmtStock = conn.prepareStatement(sqlStock);
             PreparedStatement stmtMov = conn.prepareStatement(sqlMovimiento)) {

            for (ItemDevolucion item : solicitud.items()) {
                if (item.cantidad() <= 0) {
                    continue;
                }

                if (item.estadoProducto() == EstadoProductoDevolucion.DEFECTUOSO
                        || item.estadoProducto() == EstadoProductoDevolucion.VENCIDO) {
                    continue;
                }

                int stockAnterior = obtenerStockActual(conn, item.idProducto());

                stmtStock.setInt(1, item.cantidad());
                stmtStock.setInt(2, item.idProducto());
                stmtStock.addBatch();

                stmtMov.setInt(1, solicitud.idEmpleado());
                stmtMov.setInt(2, item.idProducto());
                stmtMov.setInt(3, item.cantidad());
                stmtMov.setInt(4, stockAnterior);
                stmtMov.setInt(5, stockAnterior + item.cantidad());
                stmtMov.setString(6, "Devolución de venta " + solicitud.idVenta());
                stmtMov.setDate(7, Date.valueOf(LocalDate.now()));
                stmtMov.addBatch();
            }

            stmtStock.executeBatch();
            stmtMov.executeBatch();
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

    private void registrarImpactoCaja(
            Connection conn,
            int empleadoId,
            int ventaId,
            MetodoReembolso metodo,
            double montoDevuelto
    ) throws SQLException {
        if (metodo != MetodoReembolso.EFECTIVO) {
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
            stmt.setDouble(6, -montoDevuelto);
            stmt.setBoolean(7, true);
            stmt.executeUpdate();
        }
    }

    private void descontarPuntosSiAplica(Connection conn, int idVenta) throws SQLException {
        String sqlBuscar = """
            SELECT cxm.id_cuenta_fidelizacion, mp.puntos
            FROM Movimiento_puntos mp
            JOIN CuentaXMovimiento cxm ON cxm.id_movimiento = mp.id_movimiento
            WHERE mp.id_venta = ?
              AND mp.id_tipo_movimiento_puntos = 1
        """;

        String sqlActualizar = """
            UPDATE Cuenta_fidelizacion
            SET puntos_actuales = CASE
                WHEN puntos_actuales >= ? THEN puntos_actuales - ?
                ELSE 0
            END
            WHERE id_fidelizacion = ?
        """;

        try (PreparedStatement stmtBuscar = conn.prepareStatement(sqlBuscar)) {
            stmtBuscar.setInt(1, idVenta);
            ResultSet rs = stmtBuscar.executeQuery();

            while (rs.next()) {
                int idCuenta = rs.getInt("id_cuenta_fidelizacion");
                int puntos = rs.getInt("puntos");

                try (PreparedStatement stmtActualizar = conn.prepareStatement(sqlActualizar)) {
                    stmtActualizar.setInt(1, puntos);
                    stmtActualizar.setInt(2, puntos);
                    stmtActualizar.setInt(3, idCuenta);
                    stmtActualizar.executeUpdate();
                }
            }
        }
    }

    private void generarComprobante(
            String numeroDevolucion,
            int idVenta,
            double totalDevuelto,
            MetodoReembolso metodo
    ) throws IOException {
        Path directorio = Path.of("target", "devoluciones");
        Files.createDirectories(directorio);

        String contenido = """
                MasterMarket
                Comprobante de devolución
                Número: %s
                Venta original: %d
                Fecha: %s
                Método: %s
                Total devuelto: %s
                """.formatted(
                numeroDevolucion,
                idVenta,
                LocalDate.now(),
                metodo.name(),
                ProcesarFinalizarVentaUseCase.formatearMoneda(totalDevuelto)
        );

        Files.writeString(directorio.resolve("devolucion-" + numeroDevolucion + ".txt"), contenido);
    }

    public record DetalleVentaRetornable(
            int idProducto,
            String nombreProducto,
            int comprada,
            int devuelta,
            double precioRealUnitario
    ) {
        public int cantidadDisponible() {
            return comprada - devuelta;
        }
    }

    public record SolicitudDevolucion(
            int idVenta,
            int idEmpleado,
            String motivoGeneral,
            MetodoReembolso metodoReembolso,
            String supervisorUsuario,
            String supervisorPassword,
            List<ItemDevolucion> items
    ) {}

    public record ItemDevolucion(
            int idProducto,
            int cantidad,
            String motivo,
            EstadoProductoDevolucion estadoProducto
    ) {}

    public record ResultadoDevolucion(String numeroDevolucion, double totalDevuelto) {}

    public enum MetodoReembolso {
        EFECTIVO,
        NOTA_CREDITO,
        CAMBIO_PRODUCTO
    }

    public enum EstadoProductoDevolucion {
        CERRADO,
        ABIERTO,
        DEFECTUOSO,
        VENCIDO
    }

    private record VentaInfo(int idVenta, int idEmpleado, LocalDate fechaVenta, String turno) {}
}