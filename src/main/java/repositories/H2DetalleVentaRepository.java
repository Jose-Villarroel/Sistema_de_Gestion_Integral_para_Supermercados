package repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class H2DetalleVentaRepository implements DetalleVentaRepository {

    @Override
    public void guardarDetalles(Connection conn, int ventaId, List<DetalleVentaItem> items) throws SQLException {
        String sql = """
            INSERT INTO Detalle_venta
            (id_venta, id_producto, cantidad, precio_unitario, descuento, subtotal)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (DetalleVentaItem item : items) {
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
}
