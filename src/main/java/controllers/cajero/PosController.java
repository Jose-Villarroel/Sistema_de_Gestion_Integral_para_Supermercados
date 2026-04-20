package controllers.cajero;

import controllers.MainApp;
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
    public void abrirCierreCaja() {
        MainApp.navegarA(
                "/infrastructure/ui/cajero/cierre-caja-view.fxml",
                "Cierre de Caja",
                1100, 700
        );
}
}