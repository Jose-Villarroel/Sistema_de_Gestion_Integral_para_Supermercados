package controllers.admin;

import javafx.fxml.FXML;
import controllers.MainApp;

public class AdminController {

    @FXML
    public void cerrarSesion() {
        MainApp.navegarA(
                "/infrastructure/ui/autenticacion/login.fxml",
                "MasterMarket - Login",
                420, 550
        );
    }
}
