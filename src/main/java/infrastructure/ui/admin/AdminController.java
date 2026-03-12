package infrastructure.ui.admin;

import infrastructure.ui.MainApp;
import javafx.fxml.FXML;

public class AdminController {

    @FXML
    public void abrirEmpleados() {
        MainApp.navegarA(
                "/infrastructure/ui/admin/empleado/empleado.fxml",
                "MasterMarket - Administrar Empleados",
                1100, 650
        );
    }

    @FXML
    public void cerrarSesion() {
        MainApp.navegarA(
                "/infrastructure/ui/autenticacion/login.fxml",
                "MasterMarket - Login",
                420, 550
        );
    }
}