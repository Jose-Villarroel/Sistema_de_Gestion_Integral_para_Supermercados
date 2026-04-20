package controllers.cajero;

import aggregates.Producto;
import controllers.MainApp;
import entities.Usuario;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import repositories.DatabaseConnection;
import repositories.H2ProductoRepository;
import services.autenticacion.SesionUsuario;
import services.ventas.ProcesarFinalizarVentaUseCase;
import services.ventas.ProcesarFinalizarVentaUseCase.ClienteConCuenta;
import services.ventas.ProcesarFinalizarVentaUseCase.DescuentoManual;
import services.ventas.ProcesarFinalizarVentaUseCase.ItemVenta;
import services.ventas.ProcesarFinalizarVentaUseCase.MetodoPago;
import services.ventas.ProcesarFinalizarVentaUseCase.ResultadoVenta;
import services.ventas.ProcesarFinalizarVentaUseCase.ResumenVenta;
import services.ventas.ProcesarFinalizarVentaUseCase.SolicitudVenta;
import services.ventas.ProcesarFinalizarVentaUseCase.TipoDescuento;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PosController {

    @FXML private Label lblCajero;
    @FXML private Label lblCliente;
    @FXML private Label lblResumenPago;
    @FXML private Label lblSubtotal;
    @FXML private Label lblDescuentoAuto;
    @FXML private Label lblDescuentoManual;
    @FXML private Label lblImpuestos;
    @FXML private Label lblTotal;
    @FXML private Label lblCambio;
    @FXML private Label lblMensaje;

    @FXML private TextField txtCodigoProducto;
    @FXML private TextField txtCantidad;
    @FXML private TextField txtCodigoCliente;
    @FXML private TextField txtDescuentoManual;
    @FXML private TextField txtMontoRecibido;
    @FXML private TextField txtReferenciaPago;
    @FXML private TextField txtCorreoTicket;
    @FXML private TextField txtDatosFacturacion;

    @FXML private ComboBox<String> cmbTipoDescuento;
    @FXML private ComboBox<String> cmbMetodoPago;

    @FXML private CheckBox chkFacturaElectronica;
    @FXML private CheckBox chkImprimir;
    @FXML private CheckBox chkEnviarCorreo;

    @FXML private TableView<LineaVentaFx> tblVenta;
    @FXML private TableColumn<LineaVentaFx, Number> colCodigo;
    @FXML private TableColumn<LineaVentaFx, String> colProducto;
    @FXML private TableColumn<LineaVentaFx, Number> colCantidad;
    @FXML private TableColumn<LineaVentaFx, String> colPrecio;
    @FXML private TableColumn<LineaVentaFx, String> colSubtotal;
    @FXML private TableColumn<LineaVentaFx, Number> colStock;

    private final ObservableList<LineaVentaFx> lineas = FXCollections.observableArrayList();
    private final ProcesarFinalizarVentaUseCase procesarVentaUseCase;
    private final Usuario usuarioActual;

    private ClienteConCuenta clienteSeleccionado;
    private ResumenVenta resumenActual;

    public PosController() {
        DatabaseConnection db = new DatabaseConnection();
        this.procesarVentaUseCase = new ProcesarFinalizarVentaUseCase(db, new H2ProductoRepository(db));
        this.usuarioActual = SesionUsuario.getUsuarioActual();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarCombos();
        aplicarEstilosLegibles();
        tblVenta.setItems(lineas);
        chkImprimir.setSelected(true);

        if (usuarioActual == null) {
            lblCajero.setText("Sin sesion");
            mostrarMensaje("No hay un cajero autenticado. Inicie sesion nuevamente.", true);
        } else {
            lblCajero.setText(usuarioActual.getEmpleado().getNombre() + " " + usuarioActual.getEmpleado().getApellido());
            mostrarMensaje("POS listo. Escanee usando el id_producto.", false);
        }

        lblCliente.setText("Venta sin fidelizacion");
        lblResumenPago.setText("Seleccione 'Complete Sale' para validar el cobro.");
        actualizarResumen();
        actualizarSeccionPago();
    }

    @FXML
    public void agregarProducto() {
        try {
            String codigo = txtCodigoProducto.getText();
            Producto producto = procesarVentaUseCase.buscarProductoPorId(codigo);

            if (producto.getStockActual() <= 0) {
                throw new IllegalArgumentException("Product out of stock. Stock: 0 units");
            }

            LineaVentaFx linea = buscarLinea(producto.getId());
            if (linea == null) {
                lineas.add(new LineaVentaFx(producto.getId(), producto.getNombre(), 1,
                        producto.getPrecioVenta(), producto.getStockActual()));
            } else {
                int nuevaCantidad = linea.getCantidad() + 1;
                validarStock(linea.getStockDisponible(), nuevaCantidad);
                linea.setCantidad(nuevaCantidad);
                tblVenta.refresh();
            }

            txtCodigoProducto.clear();
            actualizarResumen();
            mostrarMensaje(producto.getNombre() + " agregado a la venta", false);
        } catch (Exception e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    @FXML
    public void actualizarCantidad() {
        LineaVentaFx seleccionada = tblVenta.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Seleccione un producto de la lista", true);
            return;
        }

        try {
            int nuevaCantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (nuevaCantidad <= 0) {
                eliminarProducto();
                return;
            }
            validarStock(seleccionada.getStockDisponible(), nuevaCantidad);
            seleccionada.setCantidad(nuevaCantidad);
            tblVenta.refresh();
            actualizarResumen();
            mostrarMensaje("Cantidad actualizada", false);
        } catch (NumberFormatException e) {
            mostrarMensaje("La cantidad debe ser numerica", true);
        } catch (Exception e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    @FXML
    public void eliminarProducto() {
        LineaVentaFx seleccionada = tblVenta.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            mostrarMensaje("Seleccione un producto para eliminar", true);
            return;
        }
        lineas.remove(seleccionada);
        actualizarResumen();
        mostrarMensaje("Producto eliminado de la venta", false);
    }

    @FXML
    public void buscarCliente() {
        String codigo = txtCodigoCliente.getText();
        if (codigo == null || codigo.isBlank()) {
            clienteSeleccionado = null;
            lblCliente.setText("Venta sin fidelizacion");
            actualizarResumen();
            mostrarMensaje("La venta continua sin cliente asociado", false);
            return;
        }

        Optional<ClienteConCuenta> cliente = procesarVentaUseCase.buscarClientePorCodigo(codigo);
        if (cliente.isEmpty()) {
            clienteSeleccionado = null;
            lblCliente.setText("Venta sin fidelizacion");
            actualizarResumen();
            mostrarMensaje("Customer not found", true);
            return;
        }

        clienteSeleccionado = cliente.get();
        String tarjeta = clienteSeleccionado.numeroTarjeta() == null
                ? "sin tarjeta activa"
                : "tarjeta " + clienteSeleccionado.numeroTarjeta() + " | puntos "
                + clienteSeleccionado.puntosActuales();
        lblCliente.setText(clienteSeleccionado.cliente().getNombreCompleto() + " | " + tarjeta);
        actualizarResumen();
        mostrarMensaje("Cliente encontrado. Descuentos y puntos listos para aplicar.", false);
    }

    @FXML
    public void aplicarDescuentoManual() {
        try {
            actualizarResumen();
            mostrarMensaje("Descuento manual recalculado", false);
        } catch (Exception e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    @FXML
    public void prepararFinalizacion() {
        try {
            validarVentaNoVacia();
            actualizarResumen();
            lblResumenPago.setText("Total a pagar: " + ProcesarFinalizarVentaUseCase.formatearMoneda(resumenActual.total()));
            mostrarMensaje("Resumen listo. Seleccione metodo de pago y confirme.", false);
        } catch (Exception e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    @FXML
    public void confirmarVenta() {
        try {
            validarVentaNoVacia();
            SolicitudVenta solicitud = new SolicitudVenta(
                    construirItemsVenta(),
                    clienteSeleccionado,
                    usuarioActual.getEmpleado(),
                    obtenerDescuentoManual(),
                    obtenerMetodoPago(),
                    obtenerMontoRecibido(),
                    txtReferenciaPago.getText(),
                    chkFacturaElectronica.isSelected(),
                    txtDatosFacturacion.getText(),
                    chkEnviarCorreo.isSelected() && !chkImprimir.isSelected(),
                    txtCorreoTicket.getText()
            );

            ResultadoVenta resultado = procesarVentaUseCase.procesarVenta(solicitud);
            mostrarAlertaConfirmacion(resultado.mensaje());
            limpiarVenta();
            mostrarMensaje(resultado.mensaje(), false);
        } catch (Exception e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    @FXML
    public void actualizarSeccionPago() {
        String metodo = cmbMetodoPago.getValue();
        boolean requiereMonto = "EFECTIVO".equals(metodo) || "MIXTO".equals(metodo);
        boolean requiereReferencia = "TARJETA".equals(metodo) || "TRANSFERENCIA".equals(metodo) || "MIXTO".equals(metodo);

        txtMontoRecibido.setDisable(!requiereMonto);
        txtReferenciaPago.setDisable(!requiereReferencia);
        txtCorreoTicket.setDisable(!(chkEnviarCorreo.isSelected() && !chkImprimir.isSelected()));
        txtDatosFacturacion.setDisable(!chkFacturaElectronica.isSelected());
    }

    @FXML
    public void cerrarSesion() {
        SesionUsuario.cerrar();
        MainApp.navegarA(
                "/infrastructure/ui/autenticacion/login.fxml",
                "MasterMarket - Login",
                420, 550
        );
    }

    private void configurarTabla() {
        tblVenta.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        colCodigo.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getProductoId()));
        colProducto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colCantidad.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCantidad()));
        colPrecio.setCellValueFactory(data -> new SimpleStringProperty(
                ProcesarFinalizarVentaUseCase.formatearMoneda(data.getValue().getPrecioUnitario())));
        colSubtotal.setCellValueFactory(data -> new SimpleStringProperty(
                ProcesarFinalizarVentaUseCase.formatearMoneda(data.getValue().getSubtotal())));
        colStock.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getStockDisponible()));
    }

    private void configurarCombos() {
        cmbTipoDescuento.setItems(FXCollections.observableArrayList("NINGUNO", "PORCENTAJE", "VALOR_FIJO"));
        cmbTipoDescuento.setValue("NINGUNO");
        cmbMetodoPago.setItems(FXCollections.observableArrayList("EFECTIVO", "TARJETA", "TRANSFERENCIA", "MIXTO"));
    }

    private void aplicarEstilosLegibles() {
        String campoStyle = """
                -fx-text-fill: #1f2933;
                -fx-prompt-text-fill: #7f8c8d;
                -fx-control-inner-background: white;
                -fx-background-color: white;
                """;

        for (TextField campo : List.of(
                txtCodigoProducto,
                txtCantidad,
                txtCodigoCliente,
                txtDescuentoManual,
                txtMontoRecibido,
                txtReferenciaPago,
                txtCorreoTicket,
                txtDatosFacturacion
        )) {
            campo.setStyle(campoStyle);
        }

        String comboStyle = "-fx-text-fill: #1f2933; -fx-background-color: white;";
        cmbTipoDescuento.setStyle(comboStyle);
        cmbMetodoPago.setStyle(comboStyle);
        configurarTextoCombo(cmbTipoDescuento);
        configurarTextoCombo(cmbMetodoPago);

        chkFacturaElectronica.setStyle("-fx-text-fill: #1f2933;");
        chkImprimir.setStyle("-fx-text-fill: #1f2933;");
        chkEnviarCorreo.setStyle("-fx-text-fill: #1f2933;");

        lblSubtotal.setStyle("-fx-text-fill: #1f2933;");
        lblDescuentoAuto.setStyle("-fx-text-fill: #1f2933;");
        lblDescuentoManual.setStyle("-fx-text-fill: #1f2933;");
        lblImpuestos.setStyle("-fx-text-fill: #1f2933;");
        lblCambio.setStyle("-fx-text-fill: #1f2933;");
    }

    private void configurarTextoCombo(ComboBox<String> comboBox) {
        comboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #1f2933;");
            }
        });
        comboBox.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item);
                setStyle("-fx-text-fill: #1f2933;");
            }
        });
    }

    private void actualizarResumen() {
        resumenActual = procesarVentaUseCase.calcularResumen(construirItemsVenta(), clienteSeleccionado, obtenerDescuentoManual());
        lblSubtotal.setText(ProcesarFinalizarVentaUseCase.formatearMoneda(resumenActual.subtotal()));
        lblDescuentoAuto.setText(ProcesarFinalizarVentaUseCase.formatearMoneda(
                resumenActual.descuentoPromocion() + resumenActual.descuentoFidelidad()));
        lblDescuentoManual.setText(ProcesarFinalizarVentaUseCase.formatearMoneda(resumenActual.descuentoManual()));
        lblImpuestos.setText(ProcesarFinalizarVentaUseCase.formatearMoneda(resumenActual.impuestos()));
        lblTotal.setText(ProcesarFinalizarVentaUseCase.formatearMoneda(resumenActual.total()));
        lblCambio.setText(ProcesarFinalizarVentaUseCase.formatearMoneda(calcularCambioVisual()));
    }

    private double calcularCambioVisual() {
        try {
            return obtenerMetodoPago() == MetodoPago.EFECTIVO && obtenerMontoRecibido() >= resumenActual.total()
                    ? obtenerMontoRecibido() - resumenActual.total()
                    : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<ItemVenta> construirItemsVenta() {
        List<ItemVenta> items = new ArrayList<>();
        for (LineaVentaFx linea : lineas) {
            items.add(new ItemVenta(linea.getProductoId(), linea.getNombre(),
                    linea.getCantidad(), linea.getPrecioUnitario(), linea.getStockDisponible()));
        }
        return items;
    }

    private DescuentoManual obtenerDescuentoManual() {
        String texto = txtDescuentoManual.getText() == null ? "" : txtDescuentoManual.getText().trim();
        if (texto.isBlank()) {
            return DescuentoManual.ninguno();
        }

        try {
            double valor = Double.parseDouble(texto);
            return valor <= 0
                    ? DescuentoManual.ninguno()
                    : new DescuentoManual(TipoDescuento.valueOf(cmbTipoDescuento.getValue()), valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El descuento manual debe ser numerico");
        }
    }

    private MetodoPago obtenerMetodoPago() {
        String metodo = cmbMetodoPago.getValue();
        if (metodo == null || metodo.isBlank()) {
            throw new IllegalArgumentException("You must select a payment method");
        }
        return MetodoPago.valueOf(metodo);
    }

    private double obtenerMontoRecibido() {
        String texto = txtMontoRecibido.getText() == null ? "" : txtMontoRecibido.getText().trim();
        if (texto.isBlank()) {
            return 0;
        }
        try {
            return Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El monto recibido debe ser numerico");
        }
    }

    private void validarVentaNoVacia() {
        if (usuarioActual == null) {
            throw new IllegalArgumentException("No hay un cajero autenticado. Inicie sesion nuevamente.");
        }
        if (lineas.isEmpty()) {
            throw new IllegalArgumentException("You must add at least one product to the sale");
        }
    }

    private void validarStock(int stockDisponible, int cantidadSolicitada) {
        if (cantidadSolicitada > stockDisponible) {
            throw new IllegalArgumentException("Insufficient stock. Available: " + stockDisponible + " units");
        }
    }

    private LineaVentaFx buscarLinea(int productoId) {
        for (LineaVentaFx linea : lineas) {
            if (linea.getProductoId() == productoId) {
                return linea;
            }
        }
        return null;
    }

    private void limpiarVenta() {
        lineas.clear();
        clienteSeleccionado = null;
        txtCodigoProducto.clear();
        txtCantidad.clear();
        txtCodigoCliente.clear();
        txtDescuentoManual.clear();
        txtMontoRecibido.clear();
        txtReferenciaPago.clear();
        txtCorreoTicket.clear();
        txtDatosFacturacion.clear();
        cmbMetodoPago.getSelectionModel().clearSelection();
        cmbTipoDescuento.setValue("NINGUNO");
        chkFacturaElectronica.setSelected(false);
        chkImprimir.setSelected(true);
        chkEnviarCorreo.setSelected(false);
        lblCliente.setText("Venta sin fidelizacion");
        lblResumenPago.setText("POS listo para una nueva venta.");
        actualizarSeccionPago();
        actualizarResumen();
    }

    private void mostrarAlertaConfirmacion(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alert.setHeaderText("Venta registrada");
        alert.setTitle("MasterMarket POS");
        alert.showAndWait();
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error
                ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;"
                : "-fx-text-fill: #1e8449; -fx-font-weight: bold;");
    }

    public static class LineaVentaFx {
        private final int productoId;
        private final String nombre;
        private int cantidad;
        private final double precioUnitario;
        private final int stockDisponible;

        public LineaVentaFx(int productoId, String nombre, int cantidad,
                            double precioUnitario, int stockDisponible) {
            this.productoId = productoId;
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.precioUnitario = precioUnitario;
            this.stockDisponible = stockDisponible;
        }

        public int getProductoId() { return productoId; }
        public String getNombre() { return nombre; }
        public int getCantidad() { return cantidad; }
        public double getPrecioUnitario() { return precioUnitario; }
        public int getStockDisponible() { return stockDisponible; }
        public double getSubtotal() { return cantidad * precioUnitario; }
        public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    }
}
