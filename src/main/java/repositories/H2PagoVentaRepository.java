package repositories;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

public class H2PagoVentaRepository implements PagoVentaRepository {

    @Override
    public void guardarPago(
            Connection conn,
            int ventaId,
            int tipoPago,
            double monto,
            LocalDate fechaPago
    ) throws SQLException {
        String sql = "INSERT INTO Pago_venta (id_venta, id_tipo_pago, monto, fecha_pago) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, ventaId);
            stmt.setInt(2, tipoPago);
            stmt.setDouble(3, monto);
            stmt.setDate(4, Date.valueOf(fechaPago));
            stmt.executeUpdate();
        }
    }
}
