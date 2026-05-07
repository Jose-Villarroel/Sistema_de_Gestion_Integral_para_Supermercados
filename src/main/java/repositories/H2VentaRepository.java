package repositories;

import dtos.DetalleVentaRetornableDTO;
import dtos.PuntosVentaDTO;
import dtos.VentaInfoDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2VentaRepository implements VentaRepository {

    private final DatabaseConnection databaseConnection;

    public H2VentaRepository(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public Optional<VentaInfoDTO> buscarVentaActivaPorId(int idVenta) {
        String sql = """
            SELECT id_venta, id_empleado, fecha_venta, turno, estado_venta
            FROM Venta
            WHERE id_venta = ?
        """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVenta);
            ResultSet rs = stmt.executeQuery();

            if (rs.next() && rs.getBoolean("estado_venta")) {
                return Optional.of(new VentaInfoDTO(
                        rs.getInt("id_venta"),
                        rs.getInt("id_empleado"),
                        rs.getDate("fecha_venta").toLocalDate(),
                        rs.getString("turno")
                ));
            }

            return Optional.empty();
        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar venta", e);
        }
    }

    @Override
    public List<DetalleVentaRetornableDTO> obtenerDetallesRetornables(int idVenta) {
        List<DetalleVentaRetornableDTO> detalles = new ArrayList<>();
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

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVenta);
            stmt.setInt(2, idVenta);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                int comprada = rs.getInt("comprada");
                double subtotal = rs.getDouble("subtotal");
                double precioUnitario = rs.getDouble("precio_unitario");
                double precioReal = comprada > 0 ? subtotal / comprada : precioUnitario;

                detalles.add(new DetalleVentaRetornableDTO(
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        comprada,
                        rs.getInt("devuelta"),
                        precioReal
                ));
            }

            return detalles;
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener detalles de la venta", e);
        }
    }

    @Override
    public List<PuntosVentaDTO> obtenerPuntosOtorgadosPorVenta(int idVenta) {
        List<PuntosVentaDTO> puntosOtorgados = new ArrayList<>();
        String sql = """
            SELECT cxm.id_cuenta_fidelizacion, mp.puntos
            FROM Movimiento_puntos mp
            JOIN CuentaXMovimiento cxm ON cxm.id_movimiento = mp.id_movimiento
            WHERE mp.id_venta = ?
              AND mp.id_tipo_movimiento_puntos = 1
        """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idVenta);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                puntosOtorgados.add(new PuntosVentaDTO(
                        rs.getInt("id_cuenta_fidelizacion"),
                        rs.getInt("puntos")
                ));
            }

            return puntosOtorgados;
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener puntos de la venta", e);
        }
    }
}
