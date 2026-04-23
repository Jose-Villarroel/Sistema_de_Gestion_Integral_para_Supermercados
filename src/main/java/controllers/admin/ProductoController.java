package controllers.admin;

import entities.Producto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import repositories.DatabaseConnection;
import repositories.H2ProductoRepository;
import services.productos.ConsultarProductoUseCase;
import services.productos.ListarProductosStockBajoUseCase;
import services.productos.ModificarProductoUseCase;
import services.productos.RegistrarProductoUseCase;

import java.util.List;


public class ProductoController {

    // Campos del formulario
    @FXML private TextField txtNombre;
    @FXML private TextField txtMarca;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtPrecioCompra;
    @FXML private TextField txtPrecioVenta;
    @FXML private TextField txtStockActual;
    @FXML private TextField txtStockMinimo;
    @FXML private ComboBox<Integer> cmbCategoria;
    @FXML private CheckBox chkActivo;
    @FXML private Label lblMargen;

    // Tabla de productos
    @FXML private TableView<Producto> tableProductos;
    @FXML private TableColumn<Producto, Integer> colId;
    @FXML private TableColumn<Producto, String> colNombre;
    @FXML private TableColumn<Producto, String> colMarca;
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
    private final ObservableList<Producto> productosObservable;

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
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colMarca.setCellValueFactory(new PropertyValueFactory<>("marca"));
        colPrecioVenta.setCellValueFactory(new PropertyValueFactory<>("precioVenta"));
        colStock.setCellValueFactory(new PropertyValueFactory<>("stockActual"));
        colActivo.setCellValueFactory(new PropertyValueFactory<>("activo"));

        tableProductos.setItems(productosObservable);

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
        // Temporal mientras conectas categorías reales desde BD
        cmbCategoria.setItems(FXCollections.observableArrayList(1, 2, 3));
    }

    private void configurarEventos() {
        txtPrecioCompra.textProperty().addListener((obs, old, nuevo) -> calcularMargen());
        txtPrecioVenta.textProperty().addListener((obs, old, nuevo) -> calcularMargen());

        txtBuscar.textProperty().addListener((obs, old, nuevo) -> buscarProductos());
        chkStockBajo.selectedProperty().addListener((obs, old, nuevo) -> buscarProductos());
    }

    private void calcularMargen() {
        try {
            double compra = Double.parseDouble(txtPrecioCompra.getText().trim());
            double venta = Double.parseDouble(txtPrecioVenta.getText().trim());

            if (compra > 0) {
                double margen = ((venta - compra) / compra) * 100;
                lblMargen.setText(String.format("Margen: %.2f%%", margen));
            } else {
                lblMargen.setText("Margen: -");
            }

        } catch (Exception e) {
            lblMargen.setText("Margen: -");
        }
    }

    @FXML
    public void registrarProducto() {
        try {
            validarCampos();

            Producto producto = registrarProductoUseCase.ejecutar(
                    txtNombre.getText().trim(),
                    txtDescripcion.getText().trim(),
                    txtMarca.getText().trim(),
                    Double.parseDouble(txtPrecioCompra.getText().trim()),
                    Double.parseDouble(txtPrecioVenta.getText().trim()),
                    Integer.parseInt(txtStockActual.getText().trim()),
                    Integer.parseInt(txtStockMinimo.getText().trim()),
                    cmbCategoria.getValue(),
                    chkActivo.isSelected()
            );

            mostrarMensaje("Producto registrado exitosamente: " + producto.getNombre());
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
                    txtMarca.getText().trim(),
                    Double.parseDouble(txtPrecioCompra.getText().trim()),
                    Double.parseDouble(txtPrecioVenta.getText().trim()),
                    Integer.parseInt(txtStockActual.getText().trim()),
                    Integer.parseInt(txtStockMinimo.getText().trim()),
                    cmbCategoria.getValue(),
                    chkActivo.isSelected()
            );

            if (actualizado) {
                mostrarMensaje("Producto actualizado exitosamente");
                limpiarFormulario();
                cargarProductos();
            } else {
                mostrarError("No se pudo actualizar el producto");
            }

        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("Error al modificar producto: " + e.getMessage());
        }
    }

    @FXML
    public void limpiarFormulario() {
        txtNombre.clear();
        txtMarca.clear();
        txtDescripcion.clear();
        txtPrecioCompra.clear();
        txtPrecioVenta.clear();
        txtStockActual.clear();
        txtStockMinimo.clear();
        cmbCategoria.setValue(null);
        chkActivo.setSelected(true);
        lblMargen.setText("Margen: -");

        productoSeleccionado = null;
        tableProductos.getSelectionModel().clearSelection();
    }

    private void cargarDatosFormulario(Producto producto) {
        txtNombre.setText(producto.getNombre());
        txtMarca.setText(producto.getMarca());
        txtDescripcion.setText(producto.getDescripcion());
        txtPrecioCompra.setText(String.valueOf(producto.getPrecioCompra()));
        txtPrecioVenta.setText(String.valueOf(producto.getPrecioVenta()));
        txtStockActual.setText(String.valueOf(producto.getStockActual()));
        txtStockMinimo.setText(String.valueOf(producto.getStockMinimo()));
        cmbCategoria.setValue(producto.getCategoriaId());
        chkActivo.setSelected(producto.isActivo());

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
                productos = consultarProductoUseCase.buscarPorNombre(txtBuscar.getText().trim());
            }

            productosObservable.setAll(productos);

        } catch (Exception e) {
            mostrarError("Error en la búsqueda: " + e.getMessage());
        }
    }

    private void validarCampos() {
        if (txtNombre.getText().isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        if (txtMarca.getText().isBlank()) {
            throw new IllegalArgumentException("La marca es obligatoria");
        }

        if (txtPrecioCompra.getText().isBlank() || txtPrecioVenta.getText().isBlank()) {
            throw new IllegalArgumentException("Los precios son obligatorios");
        }

        if (txtStockActual.getText().isBlank() || txtStockMinimo.getText().isBlank()) {
            throw new IllegalArgumentException("Los campos de stock son obligatorios");
        }

        if (cmbCategoria.getValue() == null) {
            throw new IllegalArgumentException("Debe seleccionar una categoría");
        }

        try {
            double precioCompra = Double.parseDouble(txtPrecioCompra.getText().trim());
            double precioVenta = Double.parseDouble(txtPrecioVenta.getText().trim());
            int stockActual = Integer.parseInt(txtStockActual.getText().trim());
            int stockMinimo = Integer.parseInt(txtStockMinimo.getText().trim());

            if (precioCompra <= 0) {
                throw new IllegalArgumentException("El precio de compra debe ser mayor a cero");
            }

            if (precioVenta <= 0) {
                throw new IllegalArgumentException("El precio de venta debe ser mayor a cero");
            }

            if (stockActual < 0 || stockMinimo < 0) {
                throw new IllegalArgumentException("Los valores de stock no pueden ser negativos");
            }

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Precios y stocks deben ser valores numéricos válidos");
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
