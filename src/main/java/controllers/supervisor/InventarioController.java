package controllers.supervisor;

import aggregates.Producto;
import controllers.MainApp;
import entities.MovimientoInventario;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import repositories.DatabaseConnection;
import repositories.H2MovimientoInventarioRepository;
import repositories.H2ProductoRepository;
import services.inventario.ControlarInventarioUseCase;

import java.time.format.DateTimeFormatter;
import java.util.List;


public class InventarioController {

    // Campos del formulario
    @FXML private TextField txtCodigo;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtBuscar;
    @FXML private TextField txtOrdenId;
    @FXML private ComboBox<String> cmbTipo;
    @FXML private ComboBox<String> cmbMotivo;
    @FXML private Label lblInfoProducto;
    @FXML private VBox panelOrden;

    // Tabla de movimientos
    @FXML private TableView<MovimientoInventario> tableMovimientos;
    @FXML private TableColumn<MovimientoInventario, String> colFecha;
    @FXML private TableColumn<MovimientoInventario, String> colTipo;
    @FXML private TableColumn<MovimientoInventario, String> colProducto;
    @FXML private TableColumn<MovimientoInventario, Integer> colCantidad;
    @FXML private TableColumn<MovimientoInventario, String> colMotivo;

    private ControlarInventarioUseCase useCase;

    // ID del empleado autenticado (se obtiene de la sesión)
    private int empleadoId = 1;


    @FXML
    public void initialize() {
        // Construir dependencias manualmente (inyección por constructor)
        DatabaseConnection db = new DatabaseConnection();
        useCase = new ControlarInventarioUseCase(
                new H2ProductoRepository(db),
                new H2MovimientoInventarioRepository(db)
        );

        // Cargar opciones del ComboBox de tipo
        cmbTipo.setItems(FXCollections.observableArrayList(
                "ENTRADA", "SALIDA", "AJUSTE"
        ));

        // Configurar columnas de la tabla
        colFecha.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getFecha()
                            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                ));
        colTipo.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTipo().name()
                ));
        colCantidad.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(
                        data.getValue().getCantidad()).asObject()
                );
        colMotivo.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getMotivo()
                ));
        // colProducto muestra el id del producto hasta tener join
        colProducto.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        "ID: " + data.getValue().getProductoId()
                ));

        cargarMovimientos();
    }

    //Busca el producto por código y muestra su info 
    @FXML
    public void buscarProducto() {
        String codigo = txtCodigo.getText().trim();
        if (codigo.isBlank()) {
            mostrarAlerta("Ingrese un código de producto");
            return;
        }
        // Usamos el repositorio directo para mostrar info del producto
        DatabaseConnection db = new DatabaseConnection();
        H2ProductoRepository repo = new H2ProductoRepository(db);
        repo.buscarPorCodigo(codigo).ifPresentOrElse(
                p -> lblInfoProducto.setText(
                        p.getNombre() + " | Stock actual: " + p.getStockActual()),
                () -> lblInfoProducto.setText("Producto no encontrado")
        );
    }

    //Actualiza los motivos disponibles según el tipo seleccionado 
    @FXML
    public void onTipoChanged() {
        String tipo = cmbTipo.getValue();
        if (tipo == null) return;

        if (tipo.equals("ENTRADA")) {
            cmbMotivo.setItems(FXCollections.observableArrayList(
                    "Compra a proveedor", "Devolucion de cliente", "Ajuste por conteo"));
            panelOrden.setVisible(true);
        } else if (tipo.equals("SALIDA")) {
            cmbMotivo.setItems(FXCollections.observableArrayList(
                    "Merma", "Robo", "Vencimiento", "Ajuste por conteo"));
            panelOrden.setVisible(false);
        } else {
            cmbMotivo.setItems(FXCollections.observableArrayList(
                    "Correccion manual", "Conteo fisico"));
            panelOrden.setVisible(false);
        }
    }

    //Registra el movimiento de inventario
    @FXML
    public void registrarMovimiento() {
        try {
            String codigo = txtCodigo.getText().trim();
            String tipo = cmbTipo.getValue();
            String motivo = cmbMotivo.getValue();
            int cantidad = Integer.parseInt(txtCantidad.getText().trim());
            int ordenId = txtOrdenId.getText().isBlank() ? 0
                          : Integer.parseInt(txtOrdenId.getText().trim());

            int nuevoStock;

            if (tipo.equals("ENTRADA")) {
                nuevoStock = useCase.registrarEntrada(codigo, cantidad, motivo,
                                                      empleadoId, ordenId);
            } else if (tipo.equals("SALIDA")) {
                nuevoStock = useCase.registrarSalida(codigo, cantidad, motivo, empleadoId);
            } else {
                nuevoStock = useCase.ajustarStock(codigo, cantidad, motivo, empleadoId);
            }

            mostrarInfo("Movimiento registrado. Stock actual: " + nuevoStock);
            limpiarFormulario();
            cargarMovimientos();

        } catch (NumberFormatException e) {
            mostrarAlerta("La cantidad debe ser un número entero");
        } catch (IllegalArgumentException e) {
            mostrarAlerta(e.getMessage());
        }
    }

    // Muestra productos con stock bajo en una alerta 
    @FXML
    public void verAlertasStock() {
        List<Producto> alertas = useCase.obtenerAlertasStockBajo();
        if (alertas.isEmpty()) {
            mostrarInfo("No hay productos con stock bajo");
            return;
        }
        StringBuilder mensaje = new StringBuilder("Productos con stock bajo:\n\n");
        for (Producto p : alertas) {
            mensaje.append("- ").append(p.getNombre())
                   .append(" | Stock: ").append(p.getStockActual())
                   .append(" | Minimo: ").append(p.getStockMinimo()).append("\n");
        }
        mostrarInfo(mensaje.toString());
    }

    // Limpia todos los campos del formulario
    @FXML
    public void limpiarFormulario() {
        txtCodigo.clear();
        txtCantidad.clear();
        txtOrdenId.clear();
        cmbTipo.setValue(null);
        cmbMotivo.setValue(null);
        lblInfoProducto.setText("");
        panelOrden.setVisible(false);
    }

    //Cierra sesión y vuelve al login 
    @FXML
    public void cerrarSesion() {
        MainApp.navegarA(
                "/infrastructure/ui/autenticacion/login.fxml",
                "MasterMarket - Login",
                420, 550
        );
    }

    // Carga todos los movimientos en la tabla
    private void cargarMovimientos() {
        ObservableList<MovimientoInventario> lista = FXCollections.observableArrayList(
                useCase.consultarMovimientos(0)
        );
        // Si no hay producto seleccionado cargamos todos
        DatabaseConnection db = new DatabaseConnection();
        H2MovimientoInventarioRepository repo = new H2MovimientoInventarioRepository(db);
        tableMovimientos.setItems(
                FXCollections.observableArrayList(repo.listarTodos())
        );
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
        alert.setTitle("Informacion");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
