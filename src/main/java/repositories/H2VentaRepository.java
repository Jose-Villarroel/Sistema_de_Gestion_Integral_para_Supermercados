package repositories;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;

public class H2VentaRepository implements VentaRepository {

    @Override
    public int guardar(
            Connection conn,
            int empleadoId,
            LocalDate fechaVenta,
            String turno,
            String metodoPago,
            double subtotal,
            double descuentoTotal,
            double impuestoTotal,
            double totalFinal
    ) throws SQLException {
        String sql = """
            INSERT INTO Venta
            (id_empleado, fecha_venta, turno, metodo_pago, subtotal, descuento_total,
             impuesto_total, total_final, estado_venta)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, empleadoId);
            stmt.setDate(2, Date.valueOf(fechaVenta));
            stmt.setString(3, turno);
            stmt.setString(4, metodoPago);
            stmt.setDouble(5, subtotal);
            stmt.setInt(6, (int) Math.round(descuentoTotal));
            stmt.setDouble(7, impuestoTotal);
            stmt.setDouble(8, totalFinal);
            stmt.setBoolean(9, true);
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        throw new SQLException("No fue posible registrar la venta");
    }
}
