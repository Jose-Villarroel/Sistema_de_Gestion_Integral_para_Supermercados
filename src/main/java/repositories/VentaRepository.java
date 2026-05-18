package repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public interface VentaRepository {

    int guardar(
            Connection conn,
            int empleadoId,
            LocalDate fechaVenta,
            String turno,
            String metodoPago,
            double subtotal,
            double descuentoTotal,
            double impuestoTotal,
            double totalFinal
    ) throws SQLException;
}
