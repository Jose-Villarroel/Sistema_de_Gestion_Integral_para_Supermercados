package controllers.autenticacion;

import controllers.MainApp;
import controllers.SesionUsuario;
import entities.Usuario;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import repositories.DatabaseConnection;
import repositories.H2UsuarioRepository;
import services.autenticacion.AutenticarEmpleadoUseCase;

public class LoginController {

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    private final AutenticarEmpleadoUseCase autenticarUseCase;

    public LoginController() {
        DatabaseConnection db = new DatabaseConnection();
        H2UsuarioRepository repo = new H2UsuarioRepository(db);
        this.autenticarUseCase = new AutenticarEmpleadoUseCase(repo);
    }

    @FXML
    public void login() {
        lblError.setText("");

        try {
            Usuario usuario = autenticarUseCase.ejecutar(
                    txtUsuario.getText(),
                    txtPassword.getText()
            );

            SesionUsuario.iniciarSesion(usuario);
            redirigirSegunRol(usuario);

        } catch (Exception e) {
            lblError.setText(e.getMessage());
        }
    }

    private void redirigirSegunRol(Usuario usuario) {
        String nombreEmpleado = usuario.getEmpleado().getNombre();

        switch (usuario.getRol().getNombreRol().toUpperCase()) {
            case "ADMINISTRADOR" ->
                    MainApp.navegarA(
                            "/infrastructure/ui/admin/admin.fxml",
                            "MasterMarket - Administrador | " + nombreEmpleado,
                            900, 650
                    );

            case "CAJERO" ->
                    MainApp.navegarA(
                            "/infrastructure/ui/cajero/pos.fxml",
                            "MasterMarket - Punto de Venta | " + nombreEmpleado,
                            1100, 700
                    );

            case "SUPERVISOR_INVENTARIO" ->
                    MainApp.navegarA(
                            "/infrastructure/ui/supervisor/inventario.fxml",
                            "MasterMarket - Inventario | " + nombreEmpleado,
                            900, 650
                    );

            case "GERENTE" ->
                    MainApp.navegarA(
                            "/infrastructure/ui/gerente/dashboard.fxml",
                            "MasterMarket - Dashboard | " + nombreEmpleado,
                            1100, 700
                    );

            default ->
                    throw new RuntimeException("Rol no reconocido: " + usuario.getRol().getNombreRol());
        }
    }
}