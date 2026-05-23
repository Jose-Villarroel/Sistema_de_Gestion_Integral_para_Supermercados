package controllers.gerente;

import controllers.MainApp;
import javafx.fxml.FXML;
import services.reportes.GenerarReporteVentasUseCase;

public class DashboardController {

    private final GenerarReporteVentasUseCase generarReporteVentasUseCase;

    public DashboardController(GenerarReporteVentasUseCase generarReporteVentasUseCase) {
        this.generarReporteVentasUseCase = generarReporteVentasUseCase;
    }

    @FXML
    public void cerrarSesion() {
        MainApp.navegarA(
                "/infrastructure/ui/autenticacion/login.fxml",
                "MasterMarket - Login",
                420, 550);
    }
}
