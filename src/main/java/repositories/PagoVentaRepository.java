package repositories;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;

public interface PagoVentaRepository {

    void guardarPago(
            Connection conn,
            int ventaId,
            int tipoPago,
            double monto,
            LocalDate fechaPago
    ) throws SQLException;
}
