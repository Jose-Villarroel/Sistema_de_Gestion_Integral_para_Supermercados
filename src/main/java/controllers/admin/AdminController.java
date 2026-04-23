package controllers.admin;

import controllers.MainApp;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Button;

public class AdminController {

    @FXML private StackPane contenidoCentral;

    @FXML private Button btnEmpleados;
    @FXML private Button btnProductos;
    @FXML private Button btnInventario;
    @FXML private Button btnClientes;
    @FXML private Button btnReportes;

    private Button botonActivo;

    @FXML
    public void mostrarEmpleados() {
        cargarVista("/infrastructure/ui/admin/admin_empleados.fxml");
        marcarActivo(btnEmpleados);
    }

    @FXML
    public void mostrarProductos() {
        cargarVista("/infrastructure/ui/admin/productos.fxml");
        marcarActivo(btnProductos);
    }

    @FXML
    public void mostrarInventario() {
        cargarVista("/infrastructure/ui/supervisor/inventario.fxml");
        marcarActivo(btnInventario);
    }

    @FXML
    public void mostrarClientes() {
        cargarVista("/infrastructure/ui/admin/cliente.fxml");
        marcarActivo(btnClientes);
    }

    @FXML
    public void mostrarReportes() {
        cargarVista("/infrastructure/ui/gerente/dashboard.fxml");
        marcarActivo(btnReportes);
    }

    @FXML
    public void cerrarSesion() {
        MainApp.navegarA(
                "/infrastructure/ui/autenticacion/login.fxml",
                "MasterMarket - Login",
                420, 550
        );
    }

    private void cargarVista(String ruta) {
        try {
            FXMLLoader loader = MainApp.crearLoader(ruta);
            Node vista = loader.load();
            contenidoCentral.getChildren().setAll(vista);
        } catch (Exception e) {
            System.err.println("Error al cargar vista: " + ruta + " — " + e.getMessage());
        }
    }

    private void marcarActivo(Button boton) {
        if (botonActivo != null) {
            botonActivo.getStyleClass().remove("sidebar-btn-active");
        }

        if (!boton.getStyleClass().contains("sidebar-btn-active")) {
            boton.getStyleClass().add("sidebar-btn-active");
        }

        botonActivo = boton;
    }
}