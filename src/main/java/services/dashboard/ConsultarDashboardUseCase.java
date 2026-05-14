package services.dashboard;

import repositories.DashboardRepository;

public class ConsultarDashboardUseCase {

    private final DashboardRepository dashboardRepository;

    public ConsultarDashboardUseCase(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    /**
     * Consulta el dashboard gerencial con los indicadores clave del negocio:
     * ventas del día, transacciones, productos con stock crítico y top
     * de productos más vendidos.
     *
     * Nota: la actualización automática cada 5 minutos es responsabilidad
     * del Controller (Timer de JavaFX), no de este servicio.
     */
    public DashboardGerencial ejecutar() {
        return dashboardRepository.consultarDashboard();
    }
}