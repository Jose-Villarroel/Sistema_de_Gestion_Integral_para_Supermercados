package services.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object con los indicadores del dashboard gerencial (CU-013).
 *
 * Encapsula las métricas clave que el Gerente consulta para monitorear
 * el negocio en tiempo real:
 *   - Ventas del día (monetario y número de transacciones)
 *   - Productos con stock crítico (que requieren reabastecimiento)
 *   - Top productos más vendidos
 */
public record DashboardGerencial(
        LocalDate fecha,
        BigDecimal ventasDelDia,
        int transaccionesDelDia,
        int productosConStockCritico,
        List<ProductoVendido> topProductosVendidos
) {
}