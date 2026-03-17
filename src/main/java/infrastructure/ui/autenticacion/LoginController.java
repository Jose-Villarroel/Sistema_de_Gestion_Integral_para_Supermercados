package infrastructure.ui.autenticacion;

import application.autenticacion.AutenticarEmpleadoUseCase;
import domain.model.Empleado;
import domain.model.Rol;
import infrastructure.persistence.DatabaseConnection;
import infrastructure.persistence.H2EmpleadoRepository;
import infrastructure.ui.MainApp;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    private final AutenticarEmpleadoUseCase autenticarUseCase;

    public LoginController() {
        DatabaseConnection db = new DatabaseConnection();
        H2EmpleadoRepository repo = new H2EmpleadoRepository(db);
        this.autenticarUseCase = new AutenticarEmpleadoUseCase(repo);
    }

    @FXML
    public void login() {
        lblError.setText("");

        try {
            Empleado empleado = autenticarUseCase.ejecutar(
                    txtUsuario.getText(),
                    txtPassword.getText()
            );
            redirigirSegunRol(empleado);

        } catch (Exception e) {
            lblError.setText(e.getMessage());
        }
    }

    private void redirigirSegunRol(Empleado empleado) {
        switch (empleado.getRol()) {
                case ADMINISTRADOR ->
                        MainApp.navegarA(
                                "/infrastructure/ui/admin/admin.fxml",
                                "MasterMarket - Administrador | " + empleado.getNombre(),
                                900, 650
                        );
                case CAJERO ->
                        MainApp.navegarA(
                                "/infrastructure/ui/cajero/pos.fxml",
                                "MasterMarket - Punto de Venta | " + empleado.getNombre(),
                                1100, 700
                        );
                case SUPERVISOR_INVENTARIO ->
                        MainApp.navegarA(
                                "/infrastructure/ui/supervisor/inventario.fxml",
                                "MasterMarket - Inventario | " + empleado.getNombre(),
                                900, 650
                        );
                case GERENTE ->
                        MainApp.navegarA(
                                "/infrastructure/ui/gerente/dashboard.fxml",
                                "MasterMarket - Dashboard | " + empleado.getNombre(),
                                1100, 700
                        );
                }
        }
}