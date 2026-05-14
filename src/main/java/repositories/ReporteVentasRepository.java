package repositories;

import services.reportes.ReporteVentas;

import java.time.LocalDate;

public interface ReporteVentasRepository {

    /**
     * Genera un reporte agregado de ventas para el rango de fechas indicado.
     * Las fechas son inclusivas en ambos extremos.
     */
    ReporteVentas generarReporte(LocalDate fechaDesde, LocalDate fechaHasta);
}