package services.reportes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Data Transfer Object con los resultados de un reporte de ventas (CU-012).
 *
 * Encapsula los indicadores agregados de un período de tiempo:
 *   - Métricas generales: total vendido, número de transacciones, ticket promedio
 *   - Desagregados: total descuentos, total impuestos
 *   - Distribución: ventas por método de pago
 */
public record ReporteVentas(
        LocalDate fechaDesde,
        LocalDate fechaHasta,
        BigDecimal totalVendido,
        int numeroTransacciones,
        BigDecimal ticketPromedio,
        BigDecimal totalDescuentos,
        BigDecimal totalImpuestos,
        Map<String, BigDecimal> ventasPorMetodoPago
) {
}