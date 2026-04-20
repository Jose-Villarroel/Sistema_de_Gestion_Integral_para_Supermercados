package controllers.admin;

import aggregates.Cliente;
import entities.CuentaFidelizacion;
import valueobjects.CategoriaFidelidad;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import repositories.DatabaseConnection;
import repositories.H2ClienteRepository;
import repositories.H2CuentaFidelizacionRepository;
import services.clientes.ConsultarClienteUseCase;
import services.clientes.DesactivarClienteUseCase;
import services.clientes.GestionarPuntosUseCase;
import services.clientes.ModificarClienteUseCase;
import services.clientes.RegistrarClienteUseCase;

import java.util.List;

public class ClienteController {

    // Campos del formulario
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtDireccion;

    // Tabla de clientes
    @FXML private TableView<Cliente> tableClientes;
    @FXML private TableColumn<Cliente, Integer> colId;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colCorreo;
    @FXML private TableColumn<Cliente, String> colTelefono;
    @FXML private TableColumn<Cliente, Boolean> colActivo;

    // Columnas de fidelización (se llenan manualmente)
    @FXML private TableColumn<Cliente, String> colTarjeta;
    @FXML private TableColumn<Cliente, Integer> colPuntos;

    // Campo de búsqueda
    @FXML private TextField txtBuscar;

    // Labels de resumen
    @FXML private Label lblTotalClientes;
    @FXML private Label lblPuntosAcumulados;
    @FXML private Label lblPuntosCanjeados;

    // Casos de uso
    private final RegistrarClienteUseCase registrarClienteUseCase;
    private final ConsultarClienteUseCase consultarClienteUseCase;
    private final ModificarClienteUseCase modificarClienteUseCase;
    private final DesactivarClienteUseCase desactivarClienteUseCase;
    private final GestionarPuntosUseCase gestionarPuntosUseCase;

    private Cliente clienteSeleccionado;
    private final ObservableList<Cliente> clientesObservable;

    public ClienteController() {
        DatabaseConnection db = new DatabaseConnection();
        H2ClienteRepository clienteRepo = new H2ClienteRepository(db);
        H2CuentaFidelizacionRepository cuentaRepo = new H2CuentaFidelizacionRepository(db);

        this.registrarClienteUseCase = new RegistrarClienteUseCase(clienteRepo, cuentaRepo);
        this.consultarClienteUseCase = new ConsultarClienteUseCase(clienteRepo);
        this.modificarClienteUseCase = new ModificarClienteUseCase(clienteRepo);
        this.desactivarClienteUseCase = new DesactivarClienteUseCase(clienteRepo);
        this.gestionarPuntosUseCase = new GestionarPuntosUseCase(cuentaRepo);

        this.clientesObservable = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarClientes();
        configurarEventos();
        actualizarResumen();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        // Columna tarjeta: consulta cuenta de fidelización
        colTarjeta.setCellValueFactory(cellData -> {
            try {
                CuentaFidelizacion cuenta = gestionarPuntosUseCase
                        .consultarPuntos(cellData.getValue().getId()) != -1
                        ? null : null;
                // Se muestra el id del cliente como referencia hasta tener getter de tarjeta
                return new javafx.beans.property.SimpleStringProperty(
                        "FID-" + String.format("%05d", cellData.getValue().getId())
                );
            } catch (Exception e) {
                return new javafx.beans.property.SimpleStringProperty("-");
            }
        });

        // Columna puntos: consulta puntos actuales
        colPuntos.setCellValueFactory(cellData -> {
            try {
                int puntos = gestionarPuntosUseCase.consultarPuntos(cellData.getValue().getId());
                return new javafx.beans.property.SimpleIntegerProperty(puntos).asObject();
            } catch (Exception e) {
                return new javafx.beans.property.SimpleIntegerProperty(0).asObject();
            }
        });

        tableClientes.setItems(clientesObservable);

        tableClientes.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        clienteSeleccionado = newSelection;
                        cargarDatosFormulario(newSelection);
                    }
                }
        );
    }

    private void configurarEventos() {
        txtBuscar.textProperty().addListener((obs, old, nuevo) -> buscarClientes());
    }

    @FXML
    public void registrarCliente() {
        try {
            validarCampos();

            Cliente cliente = registrarClienteUseCase.ejecutar(
                    txtNombre.getText().trim(),
                    txtApellido.getText().trim(),
                    txtCorreo.getText().trim(),
                    txtTelefono.getText().trim(),
                    txtDireccion.getText().trim()
            );

            mostrarMensaje("Cliente registrado exitosamente: "
                    + cliente.getNombre() + " " + cliente.getApellido());
            limpiarFormulario();
            cargarClientes();
            actualizarResumen();

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al registrar cliente: " + e.getMessage());
        }
    }

    @FXML
    public void modificarCliente() {
        if (clienteSeleccionado == null) {
            mostrarError("Debe seleccionar un cliente de la tabla para modificar");
            return;
        }

        try {
            validarCampos();

            boolean actualizado = modificarClienteUseCase.ejecutar(
                    clienteSeleccionado.getId(),
                    txtNombre.getText().trim(),
                    txtApellido.getText().trim(),
                    txtCorreo.getText().trim(),
                    txtTelefono.getText().trim(),
                    txtDireccion.getText().trim()
            );

            if (actualizado) {
                mostrarMensaje("Cliente actualizado exitosamente");
                limpiarFormulario();
                cargarClientes();
                actualizarResumen();
            } else {
                mostrarError("No se pudo actualizar el cliente");
            }

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al modificar cliente: " + e.getMessage());
        }
    }

    @FXML
    public void desactivarCliente() {
        if (clienteSeleccionado == null) {
            mostrarError("Debe seleccionar un cliente de la tabla para desactivar");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar desactivación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro que desea desactivar al cliente "
                + clienteSeleccionado.getNombre() + " " + clienteSeleccionado.getApellido() + "?");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean desactivado = desactivarClienteUseCase.ejecutar(clienteSeleccionado.getId());
                    if (desactivado) {
                        mostrarMensaje("Cliente desactivado exitosamente");
                        limpiarFormulario();
                        cargarClientes();
                        actualizarResumen();
                    } else {
                        mostrarError("No se pudo desactivar el cliente");
                    }
                } catch (Exception e) {
                    mostrarError("Error al desactivar cliente: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void limpiarFormulario() {
        txtNombre.clear();
        txtApellido.clear();
        txtCorreo.clear();
        txtTelefono.clear();
        txtDireccion.clear();

        clienteSeleccionado = null;
        tableClientes.getSelectionModel().clearSelection();
    }

    private void cargarDatosFormulario(Cliente cliente) {
        txtNombre.setText(cliente.getNombre());
        txtApellido.setText(cliente.getApellido());
        txtCorreo.setText(cliente.getCorreo());
        txtTelefono.setText(cliente.getTelefono());
        txtDireccion.setText(cliente.getDireccion());
    }

    private void cargarClientes() {
        List<Cliente> clientes = consultarClienteUseCase.listarActivos();
        clientesObservable.setAll(clientes);
    }

    private void buscarClientes() {
        try {
            List<Cliente> clientes;

            if (txtBuscar.getText().isBlank()) {
                clientes = consultarClienteUseCase.listarActivos();
            } else {
                clientes = consultarClienteUseCase.porNombre(txtBuscar.getText().trim());
            }

            clientesObservable.setAll(clientes);

        } catch (Exception e) {
            mostrarError("Error en la búsqueda: " + e.getMessage());
        }
    }

    private void actualizarResumen() {
        try {
            List<Cliente> todos = consultarClienteUseCase.listarActivos();
            lblTotalClientes.setText(String.valueOf(todos.size()));

            // Suma de puntos acumulados de todos los clientes activos
            int totalPuntos = todos.stream()
                    .mapToInt(c -> {
                        try {
                            return gestionarPuntosUseCase.consultarPuntos(c.getId());
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .sum();

            lblPuntosAcumulados.setText(String.format("%,d", totalPuntos));
            lblPuntosCanjeados.setText("0"); // Se actualizará cuando haya historial de canjes

        } catch (Exception e) {
            lblTotalClientes.setText("0");
            lblPuntosAcumulados.setText("0");
            lblPuntosCanjeados.setText("0");
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

        if (txtTelefono.getText().isBlank()) {
            throw new IllegalArgumentException("El teléfono es obligatorio");
        }

        if (!txtCorreo.getText().contains("@")) {
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