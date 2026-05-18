package repositories;

import services.ventas.ProcesarDevolucionUseCase.*;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class H2DevolucionRepository implements DevolucionRepository {

    @Override
    public VentaInfo obtenerVenta(Connection conn, int idVenta) throws SQLException {
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

    @Override
    public List<DetalleVentaRetornable> obtenerDetallesVenta(Connection conn, int idVenta) throws SQLException {
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

    @Override
    public int guardarDevolucion(
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

            throw new SQLException("No se pudo registrar la devolución.");
        }
    }
}