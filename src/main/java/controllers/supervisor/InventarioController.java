package controllers.supervisor;

<<<<<<< HEAD:src/main/java/infrastructure/ui/supervisor/InventarioController.java
import application.inventario.ControlarInventarioUseCase;
import domain.model.MovimientoInventario;
import domain.model.Producto;
import infrastructure.persistence.DatabaseConnection;
import infrastructure.persistence.H2MovimientoInventarioRepository;
import infrastructure.persistence.H2ProductoRepository;
import infrastructure.ui.MainApp;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
=======
import aggregates.Producto;
import controllers.MainApp;
import entities.MovimientoInventario;
>>>>>>> develop:src/main/java/controllers/supervisor/InventarioController.java
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import repositories.DatabaseConnection;
import repositories.H2MovimientoInventarioRepository;
import repositories.H2ProductoRepository;
import services.inventario.ControlarInventarioUseCase;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class InventarioController {

    @FXML private TextField txtCodigo;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtBuscar;

    // Opcionales por compatibilidad con FXML viejos
    @FXML private TextField txtOrdenId;
    @FXML private VBox panelOrden;

    @FXML private ComboBox<String> cmbTipo;
    @FXML private ComboBox<String> cmbMotivo;
    @FXML private Label lblInfoProducto;

    @FXML private TableView<MovimientoInventario> tableMovimientos;
    @FXML private TableColumn<MovimientoInventario, String> colFecha;
    @FXML private TableColumn<MovimientoInventario, String> colTipo;
    @FXML private TableColumn<MovimientoInventario, String> colProducto;
    @FXML private TableColumn<MovimientoInventario, Integer> colCantidad;
    @FXML private TableColumn<MovimientoInventario, String> colMotivo;

    private ControlarInventarioUseCase useCase;
    private H2ProductoRepository productoRepository;
    private H2MovimientoInventarioRepository movimientoRepository;

    private int empleadoId = 1;

    @FXML
    public void initialize() {
        DatabaseConnection db = new DatabaseConnection();

        productoRepository = new H2ProductoRepository(db);
        movimientoRepository = new H2MovimientoInventarioRepository(db);

        useCase = new ControlarInventarioUseCase(
                productoRepository,
                movimientoRepository
        );

        cmbTipo.setItems(FXCollections.observableArrayList(
                "ENTRADA", "SALIDA", "AJUSTE"
        ));

        configurarTabla();

        // Solo ocultar si realmente existe en el FXML
        if (panelOrden != null) {
            panelOrden.setVisible(false);
            panelOrden.setManaged(false);
        }

        cargarMovimientos();
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue()
                                .getFechaMovimiento()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                )
        );

        colTipo.setCellValueFactory(data ->
                new SimpleStringProperty(
                        obtenerNombreTipoMovimiento(data.getValue().getIdTipoMovimiento())
                )
        );

        colProducto.setCellValueFactory(data ->
                new SimpleStringProperty(
                        "ID: " + data.getValue().getIdProducto()
                )
        );

        colCantidad.setCellValueFactory(data ->
                new SimpleIntegerProperty(
                        data.getValue().getCantidad()
                ).asObject()
        );

        colMotivo.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getMotivo()
                )
        );
    }

    @FXML
    public void buscarProducto() {
        String texto = txtCodigo.getText().trim();

        if (texto.isBlank()) {
            mostrarAlerta("Ingrese el ID del producto");
            return;
        }

        try {
            int productoId = Integer.parseInt(texto);

            productoRepository.buscarPorId(productoId).ifPresentOrElse(
                    producto -> lblInfoProducto.setText(
                            producto.getNombre()
                                    + " | Stock actual: " + producto.getStockActual()
                                    + " | Stock mínimo: " + producto.getStockMinimo()
                    ),
                    () -> lblInfoProducto.setText("Producto no encontrado")
            );

        } catch (NumberFormatException e) {
            mostrarAlerta("El ID del producto debe ser un número entero");
        }
    }

    @FXML
    public void onTipoChanged() {
        String tipo = cmbTipo.getValue();

        if (tipo == null) {
            return;
        }

        if (tipo.equals("ENTRADA")) {
            cmbMotivo.setItems(FXCollections.observableArrayList(
                    "Compra a proveedor",
                    "Devolucion de cliente",
                    "Ajuste por conteo"
            ));
        } else if (tipo.equals("SALIDA")) {
            cmbMotivo.setItems(FXCollections.observableArrayList(
                    "Merma",
                    "Robo",
                    "Vencimiento",
                    "Ajuste por conteo"
            ));
        } else {
            cmbMotivo.setItems(FXCollections.observableArrayList(
                    "Correccion manual",
                    "Conteo fisico"
            ));
        }

        cmbMotivo.setValue(null);

        if (panelOrden != null) {
            panelOrden.setVisible(false);
            panelOrden.setManaged(false);
        }
    }

    @FXML
    public void registrarMovimiento() {
        try {
            String textoProducto = txtCodigo.getText().trim();
            String tipoSeleccionado = cmbTipo.getValue();
            String motivo = cmbMotivo.getValue();
            String textoCantidad = txtCantidad.getText().trim();

            if (textoProducto.isBlank()) {
                throw new IllegalArgumentException("Debe ingresar el ID del producto");
            }

            if (tipoSeleccionado == null || tipoSeleccionado.isBlank()) {
                throw new IllegalArgumentException("Debe seleccionar el tipo de movimiento");
            }

            if (motivo == null || motivo.isBlank()) {
                throw new IllegalArgumentException("Debe seleccionar un motivo");
            }

            if (textoCantidad.isBlank()) {
                throw new IllegalArgumentException("Debe ingresar una cantidad");
            }

            int productoId = Integer.parseInt(textoProducto);
            int cantidad = Integer.parseInt(textoCantidad);

            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
            }

            int tipoMovimientoId = obtenerTipoMovimientoId(tipoSeleccionado);
            int nuevoStock;

            if (tipoSeleccionado.equals("ENTRADA")) {
                nuevoStock = useCase.registrarEntrada(
                        productoId,
                        cantidad,
                        motivo,
                        empleadoId,
                        tipoMovimientoId
                );
            } else if (tipoSeleccionado.equals("SALIDA")) {
                nuevoStock = useCase.registrarSalida(
                        productoId,
                        cantidad,
                        motivo,
                        empleadoId,
                        tipoMovimientoId
                );
            } else {
                nuevoStock = useCase.ajustarStock(
                        productoId,
                        cantidad,
                        motivo,
                        empleadoId,
                        tipoMovimientoId
                );
            }

            mostrarInfo("Movimiento registrado. Stock actual: " + nuevoStock);
            limpiarFormulario();
            cargarMovimientos();

        } catch (NumberFormatException e) {
            mostrarAlerta("El ID del producto y la cantidad deben ser números enteros");
        } catch (IllegalArgumentException e) {
            mostrarAlerta(e.getMessage());
        } catch (Exception e) {
            mostrarAlerta("Ocurrió un error al registrar el movimiento");
            e.printStackTrace();
        }
    }

    @FXML
    public void verAlertasStock() {
        List<Producto> alertas = useCase.obtenerAlertasStockBajo();

        if (alertas.isEmpty()) {
            mostrarInfo("No hay productos con stock bajo");
            return;
        }

        StringBuilder mensaje = new StringBuilder("Productos con stock bajo:\n\n");

        for (Producto producto : alertas) {
            mensaje.append("- ")
                    .append(producto.getNombre())
                    .append(" | Stock: ").append(producto.getStockActual())
                    .append(" | Mínimo: ").append(producto.getStockMinimo())
                    .append("\n");
        }

        mostrarInfo(mensaje.toString());
    }

    @FXML
    public void limpiarFormulario() {
        txtCodigo.clear();
        txtCantidad.clear();
        txtBuscar.clear();

        if (txtOrdenId != null) {
            txtOrdenId.clear();
        }

        cmbTipo.setValue(null);
        cmbMotivo.setValue(null);
        lblInfoProducto.setText("");

        if (panelOrden != null) {
            panelOrden.setVisible(false);
            panelOrden.setManaged(false);
        }
    }

    @FXML
    public void cerrarSesion() {
        MainApp.navegarA(
                "/infrastructure/ui/autenticacion/login.fxml",
                "MasterMarket - Login",
                420,
                550
        );
    }

    private void cargarMovimientos() {
        try {
            ObservableList<MovimientoInventario> lista =
                    FXCollections.observableArrayList(movimientoRepository.listarTodos());

            tableMovimientos.setItems(lista);
        } catch (Exception e) {
            tableMovimientos.setItems(FXCollections.observableArrayList());
            System.out.println("No se pudieron cargar movimientos: " + e.getMessage());
        }
    }

    private int obtenerTipoMovimientoId(String tipo) {
        return switch (tipo.toUpperCase()) {
            case "ENTRADA" -> 1;
            case "SALIDA" -> 2;
            case "AJUSTE" -> 3;
            default -> throw new IllegalArgumentException("Tipo de movimiento no válido");
        };
    }

    private String obtenerNombreTipoMovimiento(int idTipoMovimiento) {
        return switch (idTipoMovimiento) {
            case 1 -> "ENTRADA";
            case 2 -> "SALIDA";
            case 3 -> "AJUSTE";
            default -> "TIPO " + idTipoMovimiento;
        };
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
