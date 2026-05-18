package repositories;

import services.reportes.ReporteVentas;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class H2ReporteVentasRepository implements ReporteVentasRepository {

    private final DatabaseConnection dbConnection;

    public H2ReporteVentasRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public ReporteVentas generarReporte(LocalDate fechaDesde, LocalDate fechaHasta) {
        try (Connection conn = dbConnection.getConnection()) {

            // 1. Métricas generales: total, número de transacciones, descuentos, impuestos
            BigDecimal totalVendido = BigDecimal.ZERO;
            int numeroTransacciones = 0;
            BigDecimal totalDescuentos = BigDecimal.ZERO;
            BigDecimal totalImpuestos = BigDecimal.ZERO;

            String sqlAgregados = """
                SELECT COALESCE(SUM(total_final), 0) AS total_vendido,
                       COUNT(*) AS num_transacciones,
                       COALESCE(SUM(descuento_total), 0) AS total_descuentos,
                       COALESCE(SUM(impuesto_total), 0) AS total_impuestos
                FROM Venta
                WHERE estado_venta = TRUE
                  AND fecha_venta BETWEEN ? AND ?
            """;

            try (PreparedStatement stmt = conn.prepareStatement(sqlAgregados)) {
                stmt.setDate(1, Date.valueOf(fechaDesde));
                stmt.setDate(2, Date.valueOf(fechaHasta));
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    totalVendido = rs.getBigDecimal("total_vendido");
                    numeroTransacciones = rs.getInt("num_transacciones");
                    totalDescuentos = rs.getBigDecimal("total_descuentos");
                    totalImpuestos = rs.getBigDecimal("total_impuestos");
                }
            }

            // 2. Ticket promedio
            BigDecimal ticketPromedio = numeroTransacciones > 0
                    ? totalVendido.divide(BigDecimal.valueOf(numeroTransacciones), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 3. Ventas por método de pago
            Map<String, BigDecimal> ventasPorMetodo = new LinkedHashMap<>();
            String sqlPorMetodo = """
                SELECT metodo_pago, COALESCE(SUM(total_final), 0) AS total
                FROM Venta
                WHERE estado_venta = TRUE
                  AND fecha_venta BETWEEN ? AND ?
                GROUP BY metodo_pago
                ORDER BY metodo_pago
            """;

            try (PreparedStatement stmt = conn.prepareStatement(sqlPorMetodo)) {
                stmt.setDate(1, Date.valueOf(fechaDesde));
                stmt.setDate(2, Date.valueOf(fechaHasta));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    ventasPorMetodo.put(rs.getString("metodo_pago"), rs.getBigDecimal("total"));
                }
            }

            return new ReporteVentas(
                    fechaDesde,
                    fechaHasta,
                    totalVendido,
                    numeroTransacciones,
                    ticketPromedio,
                    totalDescuentos,
                    totalImpuestos,
                    ventasPorMetodo
            );

        } catch (Exception e) {
            throw new RuntimeException("Error al generar reporte de ventas", e);
        }
    }
}