package infrastructure.ui.autenticacion;

import application.autenticacion.AutenticarEmpleadoUseCase;
import domain.model.Empleado;
import infrastructure.persistence.DatabaseConnection;
import infrastructure.persistence.H2EmpleadoRepository;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;

    private final AutenticarEmpleadoUseCase autenticarUseCase;

    public LoginController() {
        DatabaseConnection db = new DatabaseConnection();
        H2EmpleadoRepository repo = new H2EmpleadoRepository(db);
        this.autenticarUseCase = new AutenticarEmpleadoUseCase(repo);
    }

    @FXML
    public void login() {
        try {
            Empleado empleado = autenticarUseCase.ejecutar(
                    txtUsuario.getText(),
                    txtPassword.getText()
            );
            mostrarMensaje("Bienvenido, " + empleado.getUsuario() +
                    " | Rol: " + empleado.getRol());
            // aquí luego navegas al menú según el rol

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    private void mostrarMensaje(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText(msg);
        alert.show();
    }

    private void mostrarError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setContentText(msg);
        alert.show();
    }
}