package controllers.admin;

import entities.Proveedor;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.proveedores.ConsultarProveedorUseCase;
import services.proveedores.DesactivarProveedorUseCase;
import services.proveedores.ModificarProveedorUseCase;
import services.proveedores.RegistrarProveedorUseCase;

import java.util.List;

public class ProveedorController {

    // Campos del formulario
    @FXML private TextField txtNombre;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;

    // Tabla
    @FXML private TableView<Proveedor> tableProveedores;
    @FXML private TableColumn<Proveedor, Integer> colId;
    @FXML private TableColumn<Proveedor, String>  colNombre;
    @FXML private TableColumn<Proveedor, String>  colCorreo;
    @FXML private TableColumn<Proveedor, String>  colTelefono;
    @FXML private TableColumn<Proveedor, String>  colDireccion;
    @FXML private TableColumn<Proveedor, String>  colFecha;
    @FXML private TableColumn<Proveedor, String>  colActivo;

    // Búsqueda
    @FXML private TextField txtBuscar;

    // Labels de resumen
    @FXML private Label lblTotalProveedores;
    @FXML private Label lblProveedoresActivos;
    @FXML private Label lblProveedoresInactivos;

    // Casos de uso
    private final RegistrarProveedorUseCase  registrarProveedorUseCase;
    private final ConsultarProveedorUseCase  consultarProveedorUseCase;
    private final ModificarProveedorUseCase  modificarProveedorUseCase;
    private final DesactivarProveedorUseCase desactivarProveedorUseCase;

    private Proveedor proveedorSeleccionado;
    private final ObservableList<Proveedor> proveedoresObservable;

    public ProveedorController(RegistrarProveedorUseCase registrarProveedorUseCase,
                               ConsultarProveedorUseCase consultarProveedorUseCase,
                               ModificarProveedorUseCase modificarProveedorUseCase,
                               DesactivarProveedorUseCase desactivarProveedorUseCase) {
        this.registrarProveedorUseCase  = registrarProveedorUseCase;
        this.consultarProveedorUseCase  = consultarProveedorUseCase;
        this.modificarProveedorUseCase  = modificarProveedorUseCase;
        this.desactivarProveedorUseCase = desactivarProveedorUseCase;
        this.proveedoresObservable = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarProveedores();
        actualizarResumen();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colDireccion.setCellValueFactory(new PropertyValueFactory<>("direccion"));

        colFecha.setCellValueFactory(cellData -> {
            String fecha = cellData.getValue().getFechaRegistro() != null
                    ? cellData.getValue().getFechaRegistro().toString()
                    : "-";
            return new SimpleStringProperty(fecha);
        });

        colActivo.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isActivo() ? "Activo" : "Inactivo")
        );

        tableProveedores.setItems(proveedoresObservable);

        tableProveedores.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        proveedorSeleccionado = newVal;
                        cargarDatosFormulario(newVal);
                    }
                }
        );
    }

    @FXML
    public void registrarProveedor() {
        try {
            validarCampos();

            Proveedor proveedor = registrarProveedorUseCase.ejecutar(
                    txtNombre.getText().trim(),
                    txtCorreo.getText().trim(),
                    txtTelefono.getText().trim(),
                    txtDireccion.getText().trim()
            );

            mostrarMensaje("Proveedor registrado exitosamente.\nCódigo: " + proveedor.getId());
            limpiarFormulario();
            cargarProveedores();
            actualizarResumen();

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al registrar proveedor: " + e.getMessage());
        }
    }

    @FXML
    public void modificarProveedor() {
        if (proveedorSeleccionado == null) {
            mostrarError("Debe seleccionar un proveedor de la tabla para modificar");
            return;
        }

        try {
            validarCampos();

            boolean actualizado = modificarProveedorUseCase.ejecutar(
                    proveedorSeleccionado.getId(),
                    txtNombre.getText().trim(),
                    txtCorreo.getText().trim(),
                    txtTelefono.getText().trim(),
                    txtDireccion.getText().trim()
            );

            if (actualizado) {
                mostrarMensaje("Proveedor actualizado exitosamente");
                limpiarFormulario();
                cargarProveedores();
                actualizarResumen();
            } else {
                mostrarError("No se pudo actualizar el proveedor");
            }

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al modificar proveedor: " + e.getMessage());
        }
    }

    @FXML
    public void desactivarProveedor() {
        if (proveedorSeleccionado == null) {
            mostrarError("Debe seleccionar un proveedor de la tabla para desactivar");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar desactivación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro que desea desactivar al proveedor "
                + proveedorSeleccionado.getNombre() + "?");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean desactivado = desactivarProveedorUseCase
                            .ejecutar(proveedorSeleccionado.getId());
                    if (desactivado) {
                        mostrarMensaje("Proveedor desactivado exitosamente");
                        limpiarFormulario();
                        cargarProveedores();
                        actualizarResumen();
                    } else {
                        mostrarError("No se pudo desactivar el proveedor");
                    }
                } catch (Exception e) {
                    mostrarError("Error al desactivar proveedor: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void buscarProveedor() {
        try {
            String texto = txtBuscar.getText().trim();
            List<Proveedor> proveedores;

            if (texto.isBlank()) {
                proveedores = consultarProveedorUseCase.listarActivos();
            } else {
                proveedores = consultarProveedorUseCase.porNombre(texto);
            }

            proveedoresObservable.setAll(proveedores);

        } catch (Exception e) {
            mostrarError("Error en la búsqueda: " + e.getMessage());
        }
    }

    @FXML
    public void limpiarFormulario() {
        txtNombre.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtDireccion.clear();
        txtBuscar.clear();

        proveedorSeleccionado = null;
        tableProveedores.getSelectionModel().clearSelection();
        cargarProveedores();
    }

    private void cargarDatosFormulario(Proveedor proveedor) {
        txtNombre.setText(proveedor.getNombre());
        txtCorreo.setText(proveedor.getCorreo() != null ? proveedor.getCorreo() : "");
        txtTelefono.setText(proveedor.getTelefono() != null ? proveedor.getTelefono() : "");
        txtDireccion.setText(proveedor.getDireccion() != null ? proveedor.getDireccion() : "");
    }

    private void cargarProveedores() {
        List<Proveedor> proveedores = consultarProveedorUseCase.listarActivos();
        proveedoresObservable.setAll(proveedores);
    }

    private void actualizarResumen() {
        try {
            List<Proveedor> todos   = consultarProveedorUseCase.listarTodos();
            List<Proveedor> activos = consultarProveedorUseCase.listarActivos();
            int inactivos = todos.size() - activos.size();

            lblTotalProveedores.setText(String.valueOf(todos.size()));
            lblProveedoresActivos.setText(String.valueOf(activos.size()));
            lblProveedoresInactivos.setText(String.valueOf(Math.max(0, inactivos)));

        } catch (Exception e) {
            lblTotalProveedores.setText("0");
            lblProveedoresActivos.setText("0");
            lblProveedoresInactivos.setText("0");
        }
    }

    private void validarCampos() {
        if (txtNombre.getText().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (!txtCorreo.getText().isBlank() && !txtCorreo.getText().contains("@")) {
            throw new IllegalArgumentException("El formato del correo es inválido");
        }
    }

    private void mostrarMensaje(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}