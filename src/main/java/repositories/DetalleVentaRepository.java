package repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DetalleVentaRepository {

    void guardarDetalles(Connection conn, int ventaId, List<DetalleVentaItem> items) throws SQLException;

    record DetalleVentaItem(int productoId, int cantidad, double precioUnitario, double subtotal) {
    }
}
