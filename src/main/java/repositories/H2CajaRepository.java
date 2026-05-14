package repositories;

import services.ventas.ProcesarDevolucionUseCase.MetodoReembolso;

import java.sql.*;
import java.time.LocalDate;

public class H2CajaRepository implements CajaRepository {

    @Override
    public void validarEfectivoDisponible(
            Connection conn,
            int idEmpleado,
            String turno,
            double montoDevuelto
    ) throws SQLException {

        String sql = """
            SELECT COALESCE(SUM(c.monto_final), 0) AS disponible
            FROM Caja c
            JOIN Venta v ON v.id_venta = c.id_venta
            WHERE c.id_empleado = ?
              AND v.turno = ?
              AND v.fecha_venta = ?
              AND c.estado = TRUE
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idEmpleado);
            stmt.setString(2, turno);
            stmt.setDate(3, Date.valueOf(LocalDate.now()));

            ResultSet rs = stmt.executeQuery();
            rs.next();

            double disponible = rs.getDouble("disponible");

            if (disponible < montoDevuelto) {
                throw new IllegalArgumentException(
                        "Efectivo insuficiente en caja. Ofrezca otro método de reembolso."
                );
            }
        }
    }

    @Override
    public void registrarImpactoCaja(
            Connection conn,
            int empleadoId,
            int ventaId,
            MetodoReembolso metodo,
            double montoDevuelto
    ) throws SQLException {

        if (metodo != MetodoReembolso.EFECTIVO) {
            return;
        }

        String sql = """
            INSERT INTO Caja
            (id_empleado, id_venta, fecha_apertura, fecha_cierre, monto_inicial, monto_final, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empleadoId);
            stmt.setInt(2, ventaId);
            stmt.setDate(3, Date.valueOf(LocalDate.now()));
            stmt.setDate(4, Date.valueOf(LocalDate.now()));
            stmt.setDouble(5, 0);
            stmt.setDouble(6, -montoDevuelto);
            stmt.setBoolean(7, true);

            stmt.executeUpdate();
        }
    }
}