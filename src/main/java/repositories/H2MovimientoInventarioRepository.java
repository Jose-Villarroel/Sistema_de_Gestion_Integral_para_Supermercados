package repositories;

import entities.MovimientoInventario;
import entities.MovimientoInventario.TipoMovimiento;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class H2MovimientoInventarioRepository implements MovimientoInventarioRepository {

    private final DatabaseConnection dbConnection;

    public H2MovimientoInventarioRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public void guardar(MovimientoInventario movimiento) {
        String sql = "INSERT INTO movimientos_inventario " +
                     "(fecha, tipo, cantidad, motivo, producto_id, empleado_id, orden_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(movimiento.getFecha()));
            stmt.setString(2, movimiento.getTipo().name());
            stmt.setInt(3, movimiento.getCantidad());
            stmt.setString(4, movimiento.getMotivo());
            stmt.setInt(5, movimiento.getProductoId());
            stmt.setInt(6, movimiento.getEmpleadoId());
            // Si no hay orden asociada se inserta NULL en lugar de 0
            if (movimiento.getOrdenId() == 0) {
                stmt.setNull(7, java.sql.Types.INTEGER);
            } else {
                stmt.setInt(7, movimiento.getOrdenId());
            }
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<MovimientoInventario> listarPorProducto(int productoId) {
        List<MovimientoInventario> movimientos = new ArrayList<>();
        String sql = "SELECT * FROM movimientos_inventario WHERE producto_id = ? ORDER BY fecha DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, productoId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                movimientos.add(mapear(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return movimientos;
    }

    @Override
    public List<MovimientoInventario> listarTodos() {
        List<MovimientoInventario> movimientos = new ArrayList<>();
        String sql = "SELECT * FROM movimientos_inventario ORDER BY fecha DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                movimientos.add(mapear(rs));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return movimientos;
    }

    // Convierte una fila del ResultSet en un objeto MovimientoInventario
    private MovimientoInventario mapear(ResultSet rs) throws SQLException {
        return new MovimientoInventario(
                rs.getInt("id"),
                rs.getTimestamp("fecha").toLocalDateTime(),
                TipoMovimiento.valueOf(rs.getString("tipo")),
                rs.getInt("cantidad"),
                rs.getString("motivo"),
                rs.getInt("producto_id"),
                rs.getInt("empleado_id"),
                rs.getInt("orden_id")
        );
    }
}
