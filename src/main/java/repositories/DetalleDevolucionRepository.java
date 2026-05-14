package repositories;

import services.ventas.ProcesarDevolucionUseCase.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DetalleDevolucionRepository {

    void guardarDetalles(
            Connection conn,
            int idDevolucion,
            SolicitudDevolucion solicitud,
            List<DetalleVentaRetornable> disponibles
    ) throws SQLException;
}