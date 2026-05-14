package services.reportes;

import repositories.ReporteVentasRepository;

import java.time.LocalDate;

public class GenerarReporteVentasUseCase {

    private final ReporteVentasRepository reporteRepository;

    public GenerarReporteVentasUseCase(ReporteVentasRepository reporteRepository) {
        this.reporteRepository = reporteRepository;
    }

    /**
     * Genera un reporte agregado de ventas para el rango de fechas indicado.
     * Las fechas son inclusivas en ambos extremos.
     *
     * @param fechaDesde  inicio del período (no puede ser null)
     * @param fechaHasta  fin del período (no puede ser null, ni anterior a fechaDesde)
     * @return reporte con métricas agregadas
     */
    public ReporteVentas ejecutar(LocalDate fechaDesde, LocalDate fechaHasta) {
        if (fechaDesde == null || fechaHasta == null) {
            throw new IllegalArgumentException("Las fechas del reporte son obligatorias");
        }
        if (fechaHasta.isBefore(fechaDesde)) {
            throw new IllegalArgumentException(
                    "La fecha final no puede ser anterior a la inicial");
        }

        return reporteRepository.generarReporte(fechaDesde, fechaHasta);
    }
}