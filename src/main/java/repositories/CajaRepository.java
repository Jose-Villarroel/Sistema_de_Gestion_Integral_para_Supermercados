package repositories;

import services.ventas.ProcesarDevolucionUseCase.MetodoReembolso;

import java.sql.Connection;
import java.sql.SQLException;

public interface CajaRepository {

    void validarEfectivoDisponible(
            Connection conn,
            int idEmpleado,
            String turno,
            double montoDevuelto
    ) throws SQLException;

    void registrarImpactoCaja(
            Connection conn,
            int empleadoId,
            int ventaId,
            MetodoReembolso metodo,
            double montoDevuelto
    ) throws SQLException;

    void registrarIngresoVenta(
            Connection conn,
            int empleadoId,
            int ventaId,
            double montoEfectivo
    ) throws SQLException;
}
