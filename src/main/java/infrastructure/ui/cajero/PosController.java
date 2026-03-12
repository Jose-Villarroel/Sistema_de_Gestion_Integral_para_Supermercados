package infrastructure.ui.cajero;

import infrastructure.ui.MainApp;
import javafx.fxml.FXML;

public class PosController {

    @FXML
    public void cerrarSesion() {
        MainApp.navegarA(
                "/infrastructure/ui/autenticacion/login.fxml",
                "MasterMarket - Login",
                420, 550
        );
    }
}
