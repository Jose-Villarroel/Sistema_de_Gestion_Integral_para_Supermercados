package repositories;

import services.ventas.ProcesarDevolucionUseCase.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface DevolucionRepository {

    VentaInfo obtenerVenta(Connection conn, int idVenta) throws SQLException;

    List<DetalleVentaRetornable> obtenerDetallesVenta(Connection conn, int idVenta) throws SQLException;

    int guardarDevolucion(
            Connection conn,
            SolicitudDevolucion solicitud,
            double totalDevuelto,
            String numeroDevolucion
    ) throws SQLException;
}