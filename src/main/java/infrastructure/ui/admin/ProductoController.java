package infrastructure.ui.admin;

import application.productos.*;
import domain.model.Producto;
import infrastructure.persistence.DatabaseConnection;
import infrastructure.persistence.H2ProductoRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class ProductoController {

    // Campos del formulario
    @FXML private TextField txtCodigo;
    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtPrecioCompra;
    @FXML private TextField txtPrecioVenta;
    @FXML private TextField txtStockMinimo;
    @FXML private TextField txtStockMaximo;
    @FXML private ComboBox<Integer> cmbCategoria;
    @FXML private ComboBox<Integer> cmbProveedor;
    @FXML private Label lblMargen;

    // Tabla de productos
    @FXML private TableView<Producto> tableProductos;
    @FXML private TableColumn<Producto, String> colCodigo;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, Double> colPrecioVenta;
    @FXML private TableColumn<Producto, Integer> colStock;
    @FXML private TableColumn<Producto, Boolean> colActivo;

    // Campo de búsqueda
    @FXML private TextField txtBuscar;
    @FXML private CheckBox chkStockBajo;

    // Casos de uso
    private final RegistrarProductoUseCase registrarProductoUseCase;
    private final ModificarProductoUseCase modificarProductoUseCase;
    private final ConsultarProductoUseCase consultarProductoUseCase;
    private final ListarProductosStockBajoUseCase listarStockBajoUseCase;

    private Producto productoSeleccionado;
    private ObservableList<Producto> productosObservable;

    public ProductoController() {
        DatabaseConnection db = new DatabaseConnection();
        H2ProductoRepository repo = new H2ProductoRepository(db);
        
        this.registrarProductoUseCase = new RegistrarProductoUseCase(repo);
        this.modificarProductoUseCase = new ModificarProductoUseCase(repo);
        this.consultarProductoUseCase = new ConsultarProductoUseCase(repo);
        this.listarStockBajoUseCase = new ListarProductosStockBajoUseCase(repo);
        
        this.productosObservable = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarCombos();
        cargarProductos();
        configurarEventos();
    }

    private void configurarTabla() {
        colCodigo.setCellValueFactory(new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        tableProductos.setItems(productosObservable);

        // Evento de selección en la tabla
        tableProductos.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    productoSeleccionado = newSelection;
                    cargarDatosFormulario(newSelection);
                }
            }
        );
    }

    private void cargarCombos() {
        // TODO: Cargar categorías y proveedores desde BD
        // Por ahora, valores de ejemplo
        cmbCategoria.setItems(FXCollections.observableArrayList(1, 2, 3, 4, 5));
        cmbProveedor.setItems(FXCollections.observableArrayList(1, 2, 3));
    }

    private void configurarEventos() {
        // Calcular margen cuando cambien los precios
        txtPrecioCompra.textProperty().addListener((obs, old, nuevo) -> calcularMargen());
        txtPrecioVenta.textProperty().addListener((obs, old, nuevo) -> calcularMargen());

        // Búsqueda en tiempo real
        txtBuscar.textProperty().addListener((obs, old, nuevo) -> buscarProductos());
        chkStockBajo.selectedProperty().addListener((obs, old, nuevo) -> buscarProductos());
    }

    private void calcularMargen() {
        try {
            double compra = Double.parseDouble(txtPrecioCompra.getText());
            double venta = Double.parseDouble(txtPrecioVenta.getText());
            
            if (compra > 0) {
                double margen = ((venta - compra) / compra) * 100;
                lblMargen.setText(String.format("Margen: %.2f%%", margen));
            }
        } catch (NumberFormatException e) {
            lblMargen.setText("Margen: -");
        }
    }

    @FXML
    public void registrarProducto() {
        try {
            validarCampos();

            Producto producto = registrarProductoUseCase.ejecutar(
                txtCodigo.getText().trim(),
                txtNombre.getText().trim(),
                txtDescripcion.getText().trim(),
                Double.parseDouble(txtPrecioCompra.getText()),
                Double.parseDouble(txtPrecioVenta.getText()),
                Integer.parseInt(txtStockMinimo.getText()),
                Integer.parseInt(txtStockMaximo.getText()),
                cmbCategoria.getValue(),
                cmbProveedor.getValue()
            );

            mostrarMensaje("Producto registrado exitosamente: " + producto.getCodigo());
            limpiarFormulario();
            cargarProductos();

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al registrar producto: " + e.getMessage());
        }
    }

    @FXML
    public void modificarProducto() {
        if (productoSeleccionado == null) {
            mostrarError("Debe seleccionar un producto de la tabla para modificar");
            return;
        }

        try {
            validarCampos();

            boolean actualizado = modificarProductoUseCase.ejecutar(
                productoSeleccionado.getId(),
                txtNombre.getText().trim(),
                txtDescripcion.getText().trim(),
                Double.parseDouble(txtPrecioCompra.getText()),
                Double.parseDouble(txtPrecioVenta.getText()),
                Integer.parseInt(txtStockMinimo.getText()),
                Integer.parseInt(txtStockMaximo.getText()),
                cmbCategoria.getValue(),
                cmbProveedor.getValue()
            );

            if (actualizado) {
                mostrarMensaje("Producto actualizado exitosamente");
                limpiarFormulario();
                cargarProductos();
            }

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al modificar producto: " + e.getMessage());
        }
    }

    @FXML
    public void limpiarFormulario() {
        txtCodigo.clear();
        txtNombre.clear();
        txtDescripcion.clear();
        txtPrecioCompra.clear();
        txtPrecioVenta.clear();
        txtStockMinimo.clear();
        txtStockMaximo.clear();
        cmbCategoria.setValue(null);
        cmbProveedor.setValue(null);
        lblMargen.setText("Margen: -");
        
        txtCodigo.setDisable(false);
        productoSeleccionado = null;
        tableProductos.getSelectionModel().clearSelection();
    }

    private void cargarDatosFormulario(Producto producto) {
        txtCodigo.setText(producto.getCodigo());
        txtNombre.setText(producto.getNombre());
        txtDescripcion.setText(producto.getDescripcion());
        txtPrecioCompra.setText(String.valueOf(producto.getPrecioCompra()));
        txtPrecioVenta.setText(String.valueOf(producto.getPrecioVenta()));
        txtStockMinimo.setText(String.valueOf(producto.getStockMinimo()));
        txtStockMaximo.setText(String.valueOf(producto.getStockMaximo()));
        cmbCategoria.setValue(producto.getCategoriaId());
        cmbProveedor.setValue(producto.getProveedorId());
        
        txtCodigo.setDisable(true); // No se puede modificar el código
        calcularMargen();
    }

    private void cargarProductos() {
        List<Producto> productos = consultarProductoUseCase.listarActivos();
        productosObservable.setAll(productos);
    }

    private void buscarProductos() {
        try {
            List<Producto> productos;

            if (chkStockBajo.isSelected()) {
                productos = listarStockBajoUseCase.ejecutar();
            } else if (txtBuscar.getText().isBlank()) {
                productos = consultarProductoUseCase.listarActivos();
            } else {
                productos = consultarProductoUseCase.buscarPorNombre(txtBuscar.getText());
            }

            productosObservable.setAll(productos);

        } catch (Exception e) {
            mostrarError("Error en la búsqueda: " + e.getMessage());
        }
    }

    private void validarCampos() {
        if (txtCodigo.getText().isBlank()) {
            throw new IllegalArgumentException("El código es obligatorio");
        }
        if (txtNombre.getText().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (txtPrecioCompra.getText().isBlank() || txtPrecioVenta.getText().isBlank()) {
            throw new IllegalArgumentException("Los precios son obligatorios");
        }
        if (txtStockMinimo.getText().isBlank() || txtStockMaximo.getText().isBlank()) {
            throw new IllegalArgumentException("Los stocks son obligatorios");
        }
        if (cmbCategoria.getValue() == null || cmbProveedor.getValue() == null) {
            throw new IllegalArgumentException("Debe seleccionar categoría y proveedor");
        }
    }

    private void mostrarMensaje(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Éxito");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.show();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.show();
    }
}