package repositories;

import entities.MovimientoInventario;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class H2MovimientoInventarioRepository implements MovimientoInventarioRepository {

    private final DatabaseConnection dbConnection;

    public H2MovimientoInventarioRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public void guardar(MovimientoInventario movimiento) {

        String sql = "INSERT INTO Movimiento_inventario " +
                "(id_empleado, id_tipo_movimiento, id_producto, cantidad, " +
                "stock_anterior, stock_nuevo, motivo, fecha_movimiento) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, movimiento.getIdEmpleado());
            stmt.setInt(2, movimiento.getIdTipoMovimiento());
            stmt.setInt(3, movimiento.getIdProducto());
            stmt.setInt(4, movimiento.getCantidad());
            stmt.setInt(5, movimiento.getStockAnterior());
            stmt.setInt(6, movimiento.getStockNuevo());
            stmt.setString(7, movimiento.getMotivo());
            stmt.setDate(8, Date.valueOf(movimiento.getFechaMovimiento()));

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<MovimientoInventario> listarPorProducto(int productoId) {

        List<MovimientoInventario> movimientos = new ArrayList<>();

        String sql = "SELECT * FROM Movimiento_inventario " +
                "WHERE id_producto = ? ORDER BY fecha_movimiento DESC";

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

        String sql = "SELECT * FROM Movimiento_inventario ORDER BY fecha_movimiento DESC";

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

    private MovimientoInventario mapear(ResultSet rs) throws SQLException {

        return new MovimientoInventario(
                rs.getInt("id_movimiento"),
                rs.getInt("id_empleado"),
                rs.getInt("id_tipo_movimiento"),
                rs.getInt("id_producto"),
                rs.getInt("cantidad"),
                rs.getInt("stock_anterior"),
                rs.getInt("stock_nuevo"),
                rs.getString("motivo"),
                rs.getDate("fecha_movimiento").toLocalDate()
        );
    }
}