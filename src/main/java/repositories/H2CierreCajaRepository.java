package repositories;

import dtos.ResumenCierreCajaDTO;
import entities.CierreCaja;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;

public class H2CierreCajaRepository implements CierreCajaRepository {

    private final DatabaseConnection databaseConnection;

    public H2CierreCajaRepository(DatabaseConnection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    @Override
    public boolean existeCierre(LocalDate fecha, String turno) throws SQLException {
        String sql = "SELECT COUNT(*) FROM CIERRE_CAJA WHERE FECHA_CIERRE = ? AND TURNO = ?";

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fecha));
            ps.setString(2, turno);

            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    @Override
    public ResumenCierreCajaDTO obtenerResumenTurno(LocalDate fecha, String turno) throws SQLException {
        String sql = """
                SELECT COALESCE(SUM(c.MONTO_FINAL), 0) AS EFECTIVO_ESPERADO,
                       COUNT(DISTINCT v.ID_VENTA) AS TOTAL_TRANSACCIONES
                FROM CAJA c
                INNER JOIN VENTA v ON v.ID_VENTA = c.ID_VENTA
                WHERE v.FECHA_VENTA = ?
                  AND v.TURNO = ?
                  AND v.ESTADO_VENTA = TRUE
                  AND c.ESTADO = TRUE
                """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fecha));
            ps.setString(2, turno);

            ResultSet rs = ps.executeQuery();
            rs.next();

            BigDecimal esperado = rs.getBigDecimal("EFECTIVO_ESPERADO");
            int total = rs.getInt("TOTAL_TRANSACCIONES");

            return new ResumenCierreCajaDTO(esperado, total);
        }
    }

    @Override
    public void guardar(CierreCaja cierreCaja) throws SQLException {
        String sql = """
                INSERT INTO CIERRE_CAJA
                (NUMERO_CIERRE, FECHA_CIERRE, TURNO, ID_EMPLEADO, EFECTIVO_ESPERADO,
                 EFECTIVO_CONTADO, DIFERENCIA, ESTADO_CIERRE, TOTAL_TRANSACCIONES, OBSERVACION)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection conn = databaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cierreCaja.getNumeroCierre());
            ps.setDate(2, Date.valueOf(cierreCaja.getFechaCierre()));
            ps.setString(3, cierreCaja.getTurno());
            ps.setInt(4, cierreCaja.getIdEmpleado());
            ps.setBigDecimal(5, cierreCaja.getEfectivoEsperado());
            ps.setBigDecimal(6, cierreCaja.getEfectivoContado());
            ps.setBigDecimal(7, cierreCaja.getDiferencia());
            ps.setString(8, cierreCaja.getEstadoCierre().name());
            ps.setInt(9, cierreCaja.getTotalTransacciones());
            ps.setString(10, cierreCaja.getObservacion());

            ps.executeUpdate();
        }
    }
}
