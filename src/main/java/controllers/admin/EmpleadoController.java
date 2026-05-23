package controllers.admin;

import entities.Empleado;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.empleados.ConsultarEmpleadoUseCase;
import services.empleados.DesactivarEmpleadoUseCase;
import services.empleados.ModificarEmpleadoUseCase;
import services.empleados.RegistrarEmpleadoUseCase;

import java.util.List;

public class EmpleadoController {

    // Campos del formulario
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;

    // Tabla de empleados
    @FXML private TableView<Empleado> tableEmpleados;
    @FXML private TableColumn<Empleado, Integer> colId;
    @FXML private TableColumn<Empleado, String>  colNombre;
    @FXML private TableColumn<Empleado, String>  colApellido;
    @FXML private TableColumn<Empleado, String>  colCorreo;
    @FXML private TableColumn<Empleado, String>  colTelefono;
    @FXML private TableColumn<Empleado, String>  colFecha;
    @FXML private TableColumn<Empleado, String>  colActivo;

    // Campo de búsqueda
    @FXML private TextField txtBuscar;

    // Labels de resumen
    @FXML private Label lblTotalEmpleados;
    @FXML private Label lblEmpleadosActivos;
    @FXML private Label lblEmpleadosInactivos;

    // Casos de uso
    private final RegistrarEmpleadoUseCase  registrarEmpleadoUseCase;
    private final ConsultarEmpleadoUseCase  consultarEmpleadoUseCase;
    private final ModificarEmpleadoUseCase  modificarEmpleadoUseCase;
    private final DesactivarEmpleadoUseCase desactivarEmpleadoUseCase;

    private Empleado empleadoSeleccionado;
    private final ObservableList<Empleado> empleadosObservable;

    public EmpleadoController(RegistrarEmpleadoUseCase registrarEmpleadoUseCase,
                              ConsultarEmpleadoUseCase consultarEmpleadoUseCase,
                              ModificarEmpleadoUseCase modificarEmpleadoUseCase,
                              DesactivarEmpleadoUseCase desactivarEmpleadoUseCase) {
        this.registrarEmpleadoUseCase  = registrarEmpleadoUseCase;
        this.consultarEmpleadoUseCase  = consultarEmpleadoUseCase;
        this.modificarEmpleadoUseCase  = modificarEmpleadoUseCase;
        this.desactivarEmpleadoUseCase = desactivarEmpleadoUseCase;
        this.empleadosObservable = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarEmpleados();
        actualizarResumen();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colApellido.setCellValueFactory(new PropertyValueFactory<>("apellido"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));

        colFecha.setCellValueFactory(cellData -> {
            String fecha = cellData.getValue().getFechaRegistro() != null
                    ? cellData.getValue().getFechaRegistro().toString()
                    : "-";
            return new SimpleStringProperty(fecha);
        });

        colActivo.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isActivo() ? "Activo" : "Inactivo")
        );

        tableEmpleados.setItems(empleadosObservable);

        tableEmpleados.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        empleadoSeleccionado = newVal;
                        cargarDatosFormulario(newVal);
                    }
                }
        );
    }

    @FXML
    public void registrarEmpleado() {
        try {
            validarCampos();

            Empleado empleado = registrarEmpleadoUseCase.ejecutar(
                    txtNombre.getText().trim(),
                    txtApellido.getText().trim(),
                    txtCorreo.getText().trim(),
                    txtTelefono.getText().trim()
            );

            mostrarMensaje("Empleado registrado exitosamente.\nCódigo: " + empleado.getId());
            limpiarFormulario();
            cargarEmpleados();
            actualizarResumen();

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al registrar empleado: " + e.getMessage());
        }
    }

    @FXML
    public void modificarEmpleado() {
        if (empleadoSeleccionado == null) {
            mostrarError("Debe seleccionar un empleado de la tabla para modificar");
            return;
        }

        try {
            validarCampos();

            boolean actualizado = modificarEmpleadoUseCase.ejecutar(
                    empleadoSeleccionado.getId(),
                    txtNombre.getText().trim(),
                    txtApellido.getText().trim(),
                    txtCorreo.getText().trim(),
                    txtTelefono.getText().trim()
            );

            if (actualizado) {
                mostrarMensaje("Empleado actualizado exitosamente");
                limpiarFormulario();
                cargarEmpleados();
                actualizarResumen();
            } else {
                mostrarError("No se pudo actualizar el empleado");
            }

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al modificar empleado: " + e.getMessage());
        }
    }

    @FXML
    public void desactivarEmpleado() {
        if (empleadoSeleccionado == null) {
            mostrarError("Debe seleccionar un empleado de la tabla para desactivar");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar desactivación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro que desea desactivar al empleado "
                + empleadoSeleccionado.getNombre() + " "
                + empleadoSeleccionado.getApellido() + "?");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean desactivado = desactivarEmpleadoUseCase
                            .ejecutar(empleadoSeleccionado.getId());
                    if (desactivado) {
                        mostrarMensaje("Empleado desactivado exitosamente");
                        limpiarFormulario();
                        cargarEmpleados();
                        actualizarResumen();
                    } else {
                        mostrarError("No se pudo desactivar el empleado");
                    }
                } catch (Exception e) {
                    mostrarError("Error al desactivar empleado: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void buscarEmpleado() {
        try {
            String texto = txtBuscar.getText().trim();
            List<Empleado> empleados;

            if (texto.isBlank()) {
                empleados = consultarEmpleadoUseCase.listarActivos();
            } else {
                empleados = consultarEmpleadoUseCase.porNombre(texto);
            }

            empleadosObservable.setAll(empleados);

        } catch (Exception e) {
            mostrarError("Error en la búsqueda: " + e.getMessage());
        }
    }

    @FXML
    public void limpiarFormulario() {
        txtNombre.clear();
        txtApellido.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtBuscar.clear();

        empleadoSeleccionado = null;
        tableEmpleados.getSelectionModel().clearSelection();
        cargarEmpleados();
    }

    private void cargarDatosFormulario(Empleado empleado) {
        txtNombre.setText(empleado.getNombre());
        txtApellido.setText(empleado.getApellido());
        txtCorreo.setText(empleado.getCorreo());
        txtTelefono.setText(empleado.getTelefono() != null ? empleado.getTelefono() : "");
    }

    private void cargarEmpleados() {
        List<Empleado> empleados = consultarEmpleadoUseCase.listarActivos();
        empleadosObservable.setAll(empleados);
    }

    private void actualizarResumen() {
        try {
            List<Empleado> todos    = consultarEmpleadoUseCase.listarTodos();
            List<Empleado> activos  = consultarEmpleadoUseCase.listarActivos();
            int inactivos = todos.size() - activos.size();

            lblTotalEmpleados.setText(String.valueOf(todos.size()));
            lblEmpleadosActivos.setText(String.valueOf(activos.size()));
            lblEmpleadosInactivos.setText(String.valueOf(Math.max(0, inactivos)));

        } catch (Exception e) {
            lblTotalEmpleados.setText("0");
            lblEmpleadosActivos.setText("0");
            lblEmpleadosInactivos.setText("0");
        }
    }

    private void validarCampos() {
        if (txtNombre.getText().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (txtApellido.getText().isBlank()) {
            throw new IllegalArgumentException("El apellido es obligatorio");
        }
        if (txtCorreo.getText().isBlank()) {
            throw new IllegalArgumentException("El correo es obligatorio");
        }
        if (!txtCorreo.getText().contains("@")) {
            throw new IllegalArgumentException("El formato del correo es inválido");
        }
        if (txtTelefono.getText().isBlank()) {
            throw new IllegalArgumentException("El teléfono es obligatorio");
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