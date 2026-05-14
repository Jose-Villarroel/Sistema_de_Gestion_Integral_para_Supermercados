package repositories;

import services.dashboard.DashboardGerencial;

public interface DashboardRepository {

    /**
     * Genera el dashboard gerencial con indicadores del día actual.
     */
    DashboardGerencial consultarDashboard();
}