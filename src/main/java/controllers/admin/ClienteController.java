package controllers.admin;

import entities.Cliente;
import entities.CuentaFidelizacion;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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
import java.util.Optional;

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
    @FXML private TableColumn<Cliente, String> colTarjeta;
    @FXML private TableColumn<Cliente, Integer> colPuntos;
    @FXML private TableColumn<Cliente, String> colActivo;

    // Campo de búsqueda
    @FXML private TextField txtBuscar;

    // Labels de resumen
    @FXML private Label lblTotalClientes;
    @FXML private Label lblPuntosAcumulados;
    @FXML private Label lblPuntosCanjeados;

    // Repositorios y casos de uso
    private final H2CuentaFidelizacionRepository cuentaRepo;
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
        this.cuentaRepo = new H2CuentaFidelizacionRepository(db);

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
        actualizarResumen();
    }

    private void configurarTabla() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colCorreo.setCellValueFactory(new PropertyValueFactory<>("correo"));
        colTelefono.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));
        colActivo.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isActivo() ? "Activo" : "Inactivo")
        );

        // Columna tarjeta: consulta la cuenta real de fidelización
        colTarjeta.setCellValueFactory(cellData -> {
            try {
                Optional<CuentaFidelizacion> cuenta = cuentaRepo
                        .buscarPorCliente(cellData.getValue().getId());
                return cuenta.map(c -> new SimpleStringProperty(
                        String.valueOf(c.getNumeroTarjeta())))
                        .orElse(new SimpleStringProperty("-"));
            } catch (Exception e) {
                return new SimpleStringProperty("-");
            }
        });

        // Columna puntos: consulta puntos actuales
        colPuntos.setCellValueFactory(cellData -> {
            try {
                int puntos = gestionarPuntosUseCase.consultarPuntos(
                        cellData.getValue().getId());
                return new SimpleIntegerProperty(puntos).asObject();
            } catch (Exception e) {
                return new SimpleIntegerProperty(0).asObject();
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

            // Obtener número de tarjeta generado automáticamente
            String numeroTarjeta = cuentaRepo.buscarPorCliente(cliente.getId())
                    .map(c -> String.valueOf(c.getNumeroTarjeta()))
                    .orElse("N/A");

            mostrarMensaje(
                    "Cliente registrado exitosamente.\n" +
                    "Código: " + cliente.getId() + "\n" +
                    "Tarjeta: " + numeroTarjeta
            );

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
                + clienteSeleccionado.getNombre() + " "
                + clienteSeleccionado.getApellido() + "?");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean desactivado = desactivarClienteUseCase
                            .ejecutar(clienteSeleccionado.getId());
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
    public void buscarCliente() {
        try {
            List<Cliente> clientes;
            String texto = txtBuscar.getText().trim();

            if (texto.isBlank()) {
                clientes = consultarClienteUseCase.listarActivos();
            } else {
                clientes = consultarClienteUseCase.porNombre(texto);
            }

            clientesObservable.setAll(clientes);

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
        txtDireccion.clear();
        txtBuscar.clear();

        clienteSeleccionado = null;
        tableClientes.getSelectionModel().clearSelection();
        cargarClientes();
    }

    private void cargarDatosFormulario(Cliente cliente) {
        txtNombre.setText(cliente.getNombre());
        txtApellido.setText(cliente.getApellido());
        txtCorreo.setText(cliente.getCorreo());
        txtTelefono.setText(cliente.getTelefono());
        txtDireccion.setText(cliente.getDireccion() != null ? cliente.getDireccion() : "");
    }

    private void cargarClientes() {
        List<Cliente> clientes = consultarClienteUseCase.listarActivos();
        clientesObservable.setAll(clientes);
    }

    private void actualizarResumen() {
        try {
            List<Cliente> todos = consultarClienteUseCase.listarActivos();
            lblTotalClientes.setText(String.valueOf(todos.size()));

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
            lblPuntosCanjeados.setText("0");

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