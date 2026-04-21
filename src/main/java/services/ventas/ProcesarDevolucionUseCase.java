package services.ventas;

import repositories.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProcesarDevolucionUseCase {

    private final DatabaseConnection databaseConnection;

    public ProcesarDevolucionUseCase(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    public List<DetalleVentaRetornable> obtenerDetallesVenta(int idVenta) {
        List<DetalleVentaRetornable> detalles = new ArrayList<>();
        String sql = """
            SELECT dv.id_producto, p.nombre, dv.cantidad as comprada, dv.precio_unitario, dv.subtotal,
                   COALESCE((SELECT SUM(dd.cantidad) 
                             FROM Detalle_devolucion dd 
                             JOIN Devoluciones d ON dd.id_devoluciones = d.id_devoluciones 
                             WHERE d.id_venta = ? AND dd.id_producto = dv.id_producto), 0) as devuelta
            FROM Detalle_venta dv
            JOIN Producto p ON dv.id_producto = p.id_producto
            WHERE dv.id_venta = ?
        """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idVenta);
            stmt.setInt(2, idVenta);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                int idProducto = rs.getInt("id_producto");
                String nombre = rs.getString("nombre");
                int comprada = rs.getInt("comprada");
                int devuelta = rs.getInt("devuelta");
                double precioUnitario = rs.getDouble("precio_unitario");
                double subtotal = rs.getDouble("subtotal");
                
                double precioPagadoRealPorUnidad = comprada > 0 ? (subtotal / comprada) : precioUnitario;
                
                detalles.add(new DetalleVentaRetornable(idProducto, nombre, comprada, devuelta, precioPagadoRealPorUnidad));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener detalles de la venta", e);
        }
        return detalles;
    }

    public void procesarDevolucion(SolicitudDevolucion solicitud) {
        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un producto para devolver.");
        }

        try (Connection conn = databaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Validar venta existe
                if (!ventaExiste(conn, solicitud.idVenta())) {
                    throw new IllegalArgumentException("La venta no existe.");
                }

                // 2. Calcular total a devolver y validar cantidades
                double totalDevuelto = 0;
                List<DetalleVentaRetornable> disponibles = obtenerDetallesVenta(solicitud.idVenta());
                
                for (ItemDevolucion item : solicitud.items()) {
                    if (item.cantidad() <= 0) continue;
                    
                    DetalleVentaRetornable detalle = disponibles.stream()
                        .filter(d -> d.idProducto() == item.idProducto())
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("El producto " + item.idProducto() + " no pertenece a la venta."));
                        
                    if (item.cantidad() > (detalle.comprada() - detalle.devuelta())) {
                        throw new IllegalArgumentException("La cantidad a devolver del producto " + item.idProducto() + " supera lo permitido.");
                    }
                    
                    totalDevuelto += (item.cantidad() * detalle.precioRealUnitario());
                }
                
                if (totalDevuelto <= 0) {
                    throw new IllegalArgumentException("El total a devolver debe ser mayor a cero.");
                }

                // 3. Insertar Devolucion
                int idDevolucion = guardarDevolucion(conn, solicitud, totalDevuelto);

                // 4. Insertar Detalles de devolucion, actualizar stock y caja
                guardarDetallesDevolucionYStock(conn, idDevolucion, solicitud, disponibles);
                actualizarCaja(conn, solicitud.idEmpleado(), solicitud.idVenta(), totalDevuelto);

                conn.commit();
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

    private boolean ventaExiste(Connection conn, int idVenta) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM Venta WHERE id_venta = ?")) {
            stmt.setInt(1, idVenta);
            return stmt.executeQuery().next();
        }
    }

    private int guardarDevolucion(Connection conn, SolicitudDevolucion solicitud, double totalDevuelto) throws SQLException {
        String sql = """
            INSERT INTO Devoluciones (id_empleado, id_venta, fecha_devolucion, motivo, total_devuelto, estado)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, solicitud.idEmpleado());
            stmt.setInt(2, solicitud.idVenta());
            stmt.setDate(3, Date.valueOf(LocalDate.now()));
            stmt.setString(4, solicitud.motivoGeneral());
            stmt.setDouble(5, totalDevuelto);
            stmt.setBoolean(6, true);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("No se pudo registrar la devolución principal.");
        }
    }

    private void guardarDetallesDevolucionYStock(Connection conn, int idDevolucion, SolicitudDevolucion solicitud, List<DetalleVentaRetornable> disponibles) throws SQLException {
        String sqlDetalle = """
            INSERT INTO Detalle_devolucion (id_devoluciones, id_producto, cantidad, subtotal_devuelto, motivo)
            VALUES (?, ?, ?, ?, ?)
        """;
        String sqlStock = "UPDATE Producto SET stock_actual = stock_actual + ? WHERE id_producto = ?";
        String sqlMovimiento = """
            INSERT INTO Movimiento_inventario (id_empleado, id_tipo_movimiento, id_producto, cantidad, stock_anterior, stock_nuevo, motivo, fecha_movimiento)
            VALUES (?, 1, ?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmtDet = conn.prepareStatement(sqlDetalle);
             PreparedStatement stmtStock = conn.prepareStatement(sqlStock);
             PreparedStatement stmtMov = conn.prepareStatement(sqlMovimiento)) {
             
            for (ItemDevolucion item : solicitud.items()) {
                if (item.cantidad() <= 0) continue;
                
                DetalleVentaRetornable detalle = disponibles.stream()
                        .filter(d -> d.idProducto() == item.idProducto())
                        .findFirst().get();
                
                double subtotalItem = item.cantidad() * detalle.precioRealUnitario();
                
                stmtDet.setInt(1, idDevolucion);
                stmtDet.setInt(2, item.idProducto());
                stmtDet.setInt(3, item.cantidad());
                stmtDet.setDouble(4, subtotalItem);
                stmtDet.setString(5, item.motivo());
                stmtDet.addBatch();
                
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
            
            stmtDet.executeBatch();
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

    private void actualizarCaja(Connection conn, int empleadoId, int ventaId, double montoDevuelto) throws SQLException {
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

    public record DetalleVentaRetornable(int idProducto, String nombreProducto, int comprada, int devuelta, double precioRealUnitario) {
        public int cantidadDisponible() {
            return comprada - devuelta;
        }
    }

    public record SolicitudDevolucion(int idVenta, int idEmpleado, String motivoGeneral, List<ItemDevolucion> items) {}

    public record ItemDevolucion(int idProducto, int cantidad, String motivo) {}
}
