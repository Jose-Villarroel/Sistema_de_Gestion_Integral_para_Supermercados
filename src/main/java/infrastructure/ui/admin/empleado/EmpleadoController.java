package infrastructure.ui.admin.empleado;

import application.empleado.AdministrarEmpleadoUseCase;
import domain.model.Empleado;
import infrastructure.persistence.DatabaseConnection;
import infrastructure.persistence.H2EmpleadoRepository;
import infrastructure.ui.MainApp;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class EmpleadoController {

    @FXML private TextField txtBuscar;
    @FXML private TextField txtNombre;
    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtPassword;
    @FXML private ComboBox<String> cmbRol;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private Label lblTituloForm;
    @FXML private Label lblError;
    @FXML private Label lblEstado;
    @FXML private Button btnDesactivar;
    @FXML private TableView<Empleado> tablaEmpleados;
    @FXML private TableColumn<Empleado, String> colCodigo;
    @FXML private TableColumn<Empleado, String> colNombre;
    @FXML private TableColumn<Empleado, String> colUsuario;
    @FXML private TableColumn<Empleado, String> colRol;
    @FXML private TableColumn<Empleado, Boolean> colEstado;

    private final AdministrarEmpleadoUseCase useCase;
    private Empleado empleadoSeleccionado = null;
    private ObservableList<Empleado> listaEmpleados = FXCollections.observableArrayList();

    public EmpleadoController() {
        DatabaseConnection db = new DatabaseConnection();
        H2EmpleadoRepository repo = new H2EmpleadoRepository(db);
        this.useCase = new AdministrarEmpleadoUseCase(repo);
    }

    @FXML
    public void initialize() {
        // Configurar columnas
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colUsuario.setCellValueFactory(new PropertyValueFactory<>("usuario"));
        colRol.setCellValueFactory(new PropertyValueFactory<>("rol"));

        // Columna estado con texto Activo/Inactivo
        colEstado.setCellValueFactory(new PropertyValueFactory<>("activo"));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean activo, boolean empty) {
                super.updateItem(activo, empty);
                if (empty || activo == null) {
                    setText(null);
                    setStyle("");
                } else if (activo) {
                    setText("Activo");
                    setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                } else {
                    setText("Inactivo");
                    setStyle("-fx-text-fill: #e74c3c;");
                }
            }
        });

        tablaEmpleados.setItems(listaEmpleados);
        cargarEmpleados();
    }

    private void cargarEmpleados() {
        listaEmpleados.setAll(useCase.listarTodos());
    }

    @FXML
    public void buscar() {
        String filtro = txtBuscar.getText().toLowerCase().trim();
        if (filtro.isBlank()) {
            cargarEmpleados();
            return;
        }
        List<Empleado> filtrados = useCase.listarTodos().stream()
                .filter(e -> e.getNombre().toLowerCase().contains(filtro)
                        || e.getCodigo().toLowerCase().contains(filtro)
                        || e.getRol().toLowerCase().contains(filtro))
                .toList();
        listaEmpleados.setAll(filtrados);
    }

    @FXML
    public void seleccionarEmpleado() {
        Empleado seleccionado = tablaEmpleados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) return;

        empleadoSeleccionado = seleccionado;
        lblTituloForm.setText("Modificar Empleado");
        txtNombre.setText(seleccionado.getNombre());
        txtUsuario.setText(seleccionado.getUsuario());
        txtUsuario.setDisable(true); // No se puede cambiar el usuario
        txtPassword.setPromptText("Dejar vacío para no cambiar");
        txtPassword.clear();
        cmbRol.setValue(seleccionado.getRol());
        txtCorreo.setText(seleccionado.getCorreo() != null ? seleccionado.getCorreo() : "");
        txtTelefono.setText(seleccionado.getTelefono() != null ? seleccionado.getTelefono() : "");
        btnDesactivar.setVisible(seleccionado.isActivo());
        lblError.setText("");
    }

    @FXML
    public void nuevo() {
        limpiar();
        lblTituloForm.setText("Nuevo Empleado");
    }

    @FXML
    public void guardar() {
        lblError.setText("");
        try {
            if (empleadoSeleccionado == null) {
                // Crear nuevo
                String codigo = useCase.registrar(
                        txtNombre.getText(),
                        txtUsuario.getText(),
                        txtPassword.getText(),
                        cmbRol.getValue(),
                        txtCorreo.getText(),
                        txtTelefono.getText()
                );
                mostrarExito("Empleado creado exitosamente. Código: [" + codigo + "]");
            } else {
                // Actualizar existente
                useCase.actualizar(
                        empleadoSeleccionado.getId(),
                        txtNombre.getText(),
                        cmbRol.getValue(),
                        txtCorreo.getText(),
                        txtTelefono.getText()
                );
                mostrarExito("Empleado actualizado exitosamente");
            }
            cargarEmpleados();
            limpiar();
        } catch (Exception e) {
            lblError.setText(e.getMessage());
        }
    }

    @FXML
    public void desactivar() {
        if (empleadoSeleccionado == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar desactivación");
        confirm.setHeaderText("¿Desactivar empleado?");
        confirm.setContentText("Se desactivará la cuenta de "
                + empleadoSeleccionado.getNombre()
                + ". El historial se conservará.");

        confirm.showAndWait().ifPresent(respuesta -> {
            if (respuesta == ButtonType.OK) {
                try {
                    useCase.desactivar(empleadoSeleccionado.getId());
                    mostrarExito("Empleado desactivado correctamente");
                    cargarEmpleados();
                    limpiar();
                } catch (Exception e) {
                    lblError.setText(e.getMessage());
                }
            }
        });
    }

    @FXML
    public void limpiar() {
        empleadoSeleccionado = null;
        txtNombre.clear();
        txtUsuario.clear();
        txtUsuario.setDisable(false);
        txtPassword.clear();
        txtPassword.setPromptText("Contraseña temporal");
        cmbRol.setValue(null);
        txtCorreo.clear();
        txtTelefono.clear();
        btnDesactivar.setVisible(false);
        lblError.setText("");
        lblTituloForm.setText("Nuevo Empleado");
        tablaEmpleados.getSelectionModel().clearSelection();
    }

    @FXML
    public void volver() {
        MainApp.navegarA(
                "/infrastructure/ui/admin/admin.fxml",
                "MasterMarket - Administrador",
                900, 650
        );
    }

    private void mostrarExito(String mensaje) {
        lblEstado.setText("✅ " + mensaje);
        lblEstado.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 12;");
    }
}