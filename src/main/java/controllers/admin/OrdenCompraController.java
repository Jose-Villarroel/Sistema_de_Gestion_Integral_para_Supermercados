package controllers.admin;

import entities.DetalleOrdenCompra;
import entities.OrdenCompra;
import entities.Producto;
import entities.Proveedor;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import services.ordenes.CancelarOrdenCompraUseCase;
import services.ordenes.ConsultarOrdenCompraUseCase;
import services.ordenes.CrearOrdenCompraUseCase;
import services.productos.ConsultarProductoUseCase;
import services.proveedores.ConsultarProveedorUseCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrdenCompraController {

    // ── Formulario cabecera ────────────────────────────────────
    @FXML private ComboBox<Proveedor> cmbProveedor;
    @FXML private DatePicker dpFechaEntrega;

    // ── Sección agregar producto ───────────────────────────────
    @FXML private ComboBox<Producto> cmbProducto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtCostoUnitario;

    // ── Tabla de detalles (orden en construcción) ──────────────
    @FXML private TableView<DetalleOrdenCompra> tableDetalles;
    @FXML private TableColumn<DetalleOrdenCompra, Integer> colDetProducto;
    @FXML private TableColumn<DetalleOrdenCompra, Integer> colDetCantidad;
    @FXML private TableColumn<DetalleOrdenCompra, String>  colDetCosto;
    @FXML private TableColumn<DetalleOrdenCompra, String>  colDetSubtotal;

    // ── Tabla de órdenes existentes ────────────────────────────
    @FXML private TableView<OrdenCompra> tableOrdenes;
    @FXML private TableColumn<OrdenCompra, Integer> colId;
    @FXML private TableColumn<OrdenCompra, String>  colProveedor;
    @FXML private TableColumn<OrdenCompra, String>  colFechaCreacion;
    @FXML private TableColumn<OrdenCompra, String>  colFechaEntrega;
    @FXML private TableColumn<OrdenCompra, String>  colTotal;
    @FXML private TableColumn<OrdenCompra, String>  colEstado;

    // ── Labels de resumen ──────────────────────────────────────
    @FXML private Label lblTotalOrdenes;
    @FXML private Label lblOrdenesActivas;
    @FXML private Label lblTotalLabel;

    // ── Casos de uso y repositorios ───────────────────────────
    private final CrearOrdenCompraUseCase     crearOrdenUseCase;
    private final ConsultarOrdenCompraUseCase consultarOrdenUseCase;
    private final CancelarOrdenCompraUseCase  cancelarOrdenUseCase;
    private final ConsultarProveedorUseCase   consultarProveedorUseCase;
    private final ConsultarProductoUseCase    consultarProductoUseCase;

    private final ObservableList<OrdenCompra>       ordenesObservable  = FXCollections.observableArrayList();
    private final ObservableList<DetalleOrdenCompra> detallesObservable = FXCollections.observableArrayList();

    // ID de empleado fijo por sesión (empleado 1 = admin por defecto)
    private static final int ID_EMPLEADO_SESION = 1;

    public OrdenCompraController(CrearOrdenCompraUseCase crearOrdenUseCase,
                                 ConsultarOrdenCompraUseCase consultarOrdenUseCase,
                                 CancelarOrdenCompraUseCase cancelarOrdenUseCase,
                                 ConsultarProveedorUseCase consultarProveedorUseCase,
                                 ConsultarProductoUseCase consultarProductoUseCase) {
        this.crearOrdenUseCase         = crearOrdenUseCase;
        this.consultarOrdenUseCase     = consultarOrdenUseCase;
        this.cancelarOrdenUseCase      = cancelarOrdenUseCase;
        this.consultarProveedorUseCase = consultarProveedorUseCase;
        this.consultarProductoUseCase  = consultarProductoUseCase;
    }

    @FXML
    public void initialize() {
        configurarTablaOrdenes();
        configurarTablaDetalles();
        cargarCombos();
        cargarOrdenes();
        actualizarResumen();
    }

    // ── Configuración de tablas ────────────────────────────────

    private void configurarTablaOrdenes() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colProveedor.setCellValueFactory(cellData -> {
            try {
                return consultarProveedorUseCase.porId(cellData.getValue().getIdProveedor())
                        .map(p -> new SimpleStringProperty(p.getNombre()))
                        .orElse(new SimpleStringProperty("ID " + cellData.getValue().getIdProveedor()));
            } catch (Exception e) {
                return new SimpleStringProperty("-");
            }
        });

        colFechaCreacion.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getFechaCreacion().toString())
        );

        colFechaEntrega.setCellValueFactory(cellData -> {
            LocalDate fe = cellData.getValue().getFechaEntrega();
            return new SimpleStringProperty(fe != null ? fe.toString() : "-");
        });

        colTotal.setCellValueFactory(cellData ->
            new SimpleStringProperty("$" + String.format("%,.2f", cellData.getValue().getTotal()))
        );

        colEstado.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().isActiva() ? "Activa" : "Cancelada")
        );

        tableOrdenes.setItems(ordenesObservable);
    }

    private void configurarTablaDetalles() {
        colDetProducto.setCellValueFactory(cellData -> {
            try {
                Producto p = consultarProductoUseCase.buscarPorId(cellData.getValue().getIdProducto());
                return new javafx.beans.property.SimpleIntegerProperty(p.getId()).asObject();
            } catch (Exception e) {
                return new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getIdProducto()).asObject();
            }
        });

        colDetCantidad.setCellValueFactory(new PropertyValueFactory<>("cantidad"));

        colDetCosto.setCellValueFactory(cellData ->
            new SimpleStringProperty("$" + String.format("%,.2f", cellData.getValue().getCostoUnitario()))
        );

        colDetSubtotal.setCellValueFactory(cellData ->
            new SimpleStringProperty("$" + String.format("%,.2f", cellData.getValue().getSubtotal()))
        );

        tableDetalles.setItems(detallesObservable);
    }

    private void cargarCombos() {
        // Proveedores activos
        List<Proveedor> proveedores = consultarProveedorUseCase.listarActivos();
        cmbProveedor.setItems(FXCollections.observableArrayList(proveedores));
        cmbProveedor.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Proveedor p) {
                return p == null ? "" : p.getNombre();
            }
            @Override public Proveedor fromString(String s) { return null; }
        });

        // Productos disponibles
        List<Producto> productos = consultarProductoUseCase.listarTodos();
        cmbProducto.setItems(FXCollections.observableArrayList(productos));
        cmbProducto.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(Producto p) {
                return p == null ? "" : p.getNombre() + " (ID: " + p.getId() + ")";
            }
            @Override public Producto fromString(String s) { return null; }
        });
    }

    // ── Acciones ───────────────────────────────────────────────

    @FXML
    public void agregarProducto() {
        try {
            Producto producto = cmbProducto.getValue();
            if (producto == null) {
                mostrarError("Debe seleccionar un producto");
                return;
            }
            if (txtCantidad.getText().isBlank()) {
                mostrarError("La cantidad es obligatoria");
                return;
            }
            if (txtCostoUnitario.getText().isBlank()) {
                mostrarError("El costo unitario es obligatorio");
                return;
            }

            int cantidad = Integer.parseInt(txtCantidad.getText().trim());
            BigDecimal costo = new BigDecimal(txtCostoUnitario.getText().trim());

            DetalleOrdenCompra detalle = new DetalleOrdenCompra(
                    0, producto.getId(), cantidad, costo
            );

            detallesObservable.add(detalle);
            actualizarTotalParcial();

            cmbProducto.setValue(null);
            txtCantidad.clear();
            txtCostoUnitario.clear();

        } catch (NumberFormatException e) {
            mostrarError("Cantidad y costo deben ser valores numéricos válidos");
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    public void quitarProducto() {
        DetalleOrdenCompra seleccionado = tableDetalles.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarError("Seleccione un producto de la lista para quitarlo");
            return;
        }
        detallesObservable.remove(seleccionado);
        actualizarTotalParcial();
    }

    @FXML
    public void crearOrden() {
        try {
            if (cmbProveedor.getValue() == null) {
                mostrarError("Debe seleccionar un proveedor");
                return;
            }
            if (detallesObservable.isEmpty()) {
                mostrarError("Debe agregar al menos un producto a la orden");
                return;
            }

            LocalDate fechaEntrega = dpFechaEntrega.getValue();

            OrdenCompra orden = crearOrdenUseCase.ejecutar(
                    cmbProveedor.getValue().getId(),
                    ID_EMPLEADO_SESION,
                    fechaEntrega,
                    new ArrayList<>(detallesObservable)
            );

            mostrarMensaje("Orden creada exitosamente.\nCódigo: " + orden.getId()
                    + "\nTotal: $" + String.format("%,.2f", orden.getTotal()));

            limpiarFormulario();
            cargarOrdenes();
            actualizarResumen();

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al crear la orden: " + e.getMessage());
        }
    }

    @FXML
    public void cancelarOrden() {
        OrdenCompra seleccionada = tableOrdenes.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarError("Debe seleccionar una orden de la tabla para cancelarla");
            return;
        }

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar cancelación");
        confirmacion.setHeaderText(null);
        confirmacion.setContentText("¿Está seguro que desea cancelar la orden #"
                + seleccionada.getId() + "?");

        confirmacion.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    boolean cancelada = cancelarOrdenUseCase.ejecutar(seleccionada.getId());
                    if (cancelada) {
                        mostrarMensaje("Orden #" + seleccionada.getId() + " cancelada exitosamente");
                        cargarOrdenes();
                        actualizarResumen();
                    } else {
                        mostrarError("No se pudo cancelar la orden");
                    }
                } catch (IllegalStateException e) {
                    mostrarError(e.getMessage());
                } catch (Exception e) {
                    mostrarError("Error al cancelar la orden: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    public void limpiarFormulario() {
        cmbProveedor.setValue(null);
        dpFechaEntrega.setValue(null);
        cmbProducto.setValue(null);
        txtCantidad.clear();
        txtCostoUnitario.clear();
        detallesObservable.clear();
        lblTotalLabel.setText("Total: $0.00");
        tableOrdenes.getSelectionModel().clearSelection();
    }

    // ── Helpers ────────────────────────────────────────────────

    private void cargarOrdenes() {
        List<OrdenCompra> ordenes = consultarOrdenUseCase.listarTodas();
        ordenesObservable.setAll(ordenes);
    }

    private void actualizarResumen() {
        try {
            List<OrdenCompra> todas   = consultarOrdenUseCase.listarTodas();
            List<OrdenCompra> activas = consultarOrdenUseCase.listarActivas();
            lblTotalOrdenes.setText(String.valueOf(todas.size()));
            lblOrdenesActivas.setText(String.valueOf(activas.size()));
        } catch (Exception e) {
            lblTotalOrdenes.setText("0");
            lblOrdenesActivas.setText("0");
        }
    }

    private void actualizarTotalParcial() {
        BigDecimal total = detallesObservable.stream()
                .map(DetalleOrdenCompra::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalLabel.setText("Total: $" + String.format("%,.2f", total));
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