package controllers.cajero;

import controllers.MainApp;
import entities.Usuario;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import services.autenticacion.SesionUsuario;
import services.ventas.ProcesarDevolucionUseCase;
import services.ventas.ProcesarDevolucionUseCase.DetalleVentaRetornable;
import services.ventas.ProcesarDevolucionUseCase.EstadoProductoDevolucion;
import services.ventas.ProcesarDevolucionUseCase.ItemDevolucion;
import services.ventas.ProcesarDevolucionUseCase.MetodoReembolso;
import services.ventas.ProcesarDevolucionUseCase.ResultadoDevolucion;
import services.ventas.ProcesarDevolucionUseCase.SolicitudDevolucion;
import services.ventas.ProcesarFinalizarVentaUseCase;

import java.util.ArrayList;
import java.util.List;

public class DevolucionController {

    @FXML private TextField txtIdVenta;
    @FXML private TextField txtCantidadDevolver;
    @FXML private TextField txtMotivoProducto;
    @FXML private TextField txtMotivoGeneral;

    @FXML private ComboBox<String> cmbEstadoProducto;
    @FXML private ComboBox<String> cmbMetodoReembolso;

    @FXML private TextField txtSupervisorUsuario;
    @FXML private PasswordField txtSupervisorPassword;

    @FXML private Label lblTotalDevolver;
    @FXML private Label lblMensaje;

    @FXML private TableView<DetalleDevolucionFx> tblDetalles;
    @FXML private TableColumn<DetalleDevolucionFx, Number> colCodigo;
    @FXML private TableColumn<DetalleDevolucionFx, String> colProducto;
    @FXML private TableColumn<DetalleDevolucionFx, Number> colComprada;
    @FXML private TableColumn<DetalleDevolucionFx, Number> colDevuelta;
    @FXML private TableColumn<DetalleDevolucionFx, String> colPrecio;
    @FXML private TableColumn<DetalleDevolucionFx, Number> colADevolver;
    @FXML private TableColumn<DetalleDevolucionFx, String> colMotivo;
    @FXML private TableColumn<DetalleDevolucionFx, String> colEstado;

    private final ObservableList<DetalleDevolucionFx> detallesObList = FXCollections.observableArrayList();

    private final ProcesarDevolucionUseCase procesarDevolucionUseCase;
    private final Usuario usuarioActual;

    private int idVentaActual = -1;

    public DevolucionController(ProcesarDevolucionUseCase procesarDevolucionUseCase) {
        this.procesarDevolucionUseCase = procesarDevolucionUseCase;
        this.usuarioActual = SesionUsuario.getUsuarioActual();
    }

    @FXML
    public void initialize() {
        configurarTabla();
        tblDetalles.setItems(detallesObList);

        cmbEstadoProducto.setItems(FXCollections.observableArrayList(
                "CERRADO",
                "ABIERTO",
                "DEFECTUOSO",
                "VENCIDO"
        ));
        cmbMetodoReembolso.setItems(FXCollections.observableArrayList(
                "EFECTIVO",
                "NOTA_CREDITO",
                "CAMBIO_PRODUCTO"
        ));

        lblTotalDevolver.setText("$0.00");

        if (usuarioActual == null) {
            mostrarMensaje("No hay un cajero autenticado. Inicie sesión nuevamente.", true);
        } else {
            mostrarMensaje("Módulo de devoluciones listo.", false);
        }
    }

    @FXML
    public void volverPOS() {
        String nombreEmpleado = usuarioActual != null ? usuarioActual.getEmpleado().getNombre() : "Cajero";
        MainApp.navegarA(
                "/infrastructure/ui/cajero/pos.fxml",
                "MasterMarket - Punto de Venta | " + nombreEmpleado,
                1100, 700
        );
    }

    @FXML
    public void buscarVenta() {
        try {
            int idVenta = Integer.parseInt(txtIdVenta.getText().trim());
            List<DetalleVentaRetornable> detalles = procesarDevolucionUseCase.obtenerDetallesVenta(idVenta);

            if (detalles.isEmpty()) {
                mostrarMensaje("No se encontró la venta o no tiene detalles.", true);
                return;
            }

            idVentaActual = idVenta;
            detallesObList.clear();

            for (DetalleVentaRetornable d : detalles) {
                detallesObList.add(new DetalleDevolucionFx(
                        d.idProducto(),
                        d.nombreProducto(),
                        d.comprada(),
                        d.devuelta(),
                        d.precioRealUnitario()
                ));
            }

            calcularTotal();
            mostrarMensaje("Venta cargada exitosamente.", false);

        } catch (NumberFormatException e) {
            mostrarMensaje("El ID de la venta debe ser numérico.", true);
        } catch (Exception e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    @FXML
    public void asignarCantidadDevolver() {
        DetalleDevolucionFx seleccionado = tblDetalles.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            mostrarMensaje("Seleccione un producto de la tabla.", true);
            return;
        }

        try {
            int cantidad = Integer.parseInt(txtCantidadDevolver.getText().trim());
            if (cantidad < 0) {
                mostrarMensaje("La cantidad no puede ser negativa.", true);
                return;
            }

            int disponibleParaDevolver = seleccionado.getComprada() - seleccionado.getDevuelta();
            if (cantidad > disponibleParaDevolver) {
                mostrarMensaje("La cantidad a devolver no puede superar la cantidad disponible (" + disponibleParaDevolver + ").", true);
                return;
            }

            String estado = cmbEstadoProducto.getValue();
            if (estado == null || estado.isBlank()) {
                mostrarMensaje("Debe seleccionar el estado del producto.", true);
                return;
            }

            seleccionado.setADevolver(cantidad);
            seleccionado.setMotivo(txtMotivoProducto.getText().trim());
            seleccionado.setEstadoProducto(estado);

            tblDetalles.refresh();
            calcularTotal();

            txtCantidadDevolver.clear();
            txtMotivoProducto.clear();
            cmbEstadoProducto.getSelectionModel().clearSelection();

            mostrarMensaje("Producto preparado para devolución.", false);

        } catch (NumberFormatException e) {
            mostrarMensaje("La cantidad debe ser numérica.", true);
        }
    }

    @FXML
    public void procesarDevolucion() {
        if (idVentaActual == -1) {
            mostrarMensaje("Primero busque una venta.", true);
            return;
        }

        if (cmbMetodoReembolso.getValue() == null) {
            mostrarMensaje("Debe seleccionar un método de reembolso.", true);
            return;
        }

        List<ItemDevolucion> itemsParaDevolver = new ArrayList<>();
        for (DetalleDevolucionFx d : detallesObList) {
            if (d.getADevolver() > 0) {
                itemsParaDevolver.add(new ItemDevolucion(
                        d.getIdProducto(),
                        d.getADevolver(),
                        d.getMotivo(),
                        EstadoProductoDevolucion.valueOf(d.getEstadoProducto())
                ));
            }
        }

        if (itemsParaDevolver.isEmpty()) {
            mostrarMensaje("No ha asignado ninguna cantidad a devolver.", true);
            return;
        }

        String motivoGeneral = txtMotivoGeneral.getText().trim();
        if (motivoGeneral.isEmpty()) {
            mostrarMensaje("Debe ingresar un motivo general para la devolución.", true);
            return;
        }

        try {
            SolicitudDevolucion solicitud = new SolicitudDevolucion(
                    idVentaActual,
                    usuarioActual.getEmpleado().getId(),
                    motivoGeneral,
                    MetodoReembolso.valueOf(cmbMetodoReembolso.getValue()),
                    txtSupervisorUsuario.getText().trim(),
                    txtSupervisorPassword.getText(),
                    itemsParaDevolver
            );

            ResultadoDevolucion resultado = procesarDevolucionUseCase.procesarDevolucion(solicitud);

            mostrarAlerta(
                    "Devolución Procesada",
                    "Devolución procesada.\n" +
                            "Reembolso: " + ProcesarFinalizarVentaUseCase.formatearMoneda(resultado.totalDevuelto()) +
                            "\nNúmero: " + resultado.numeroDevolucion()
            );

            limpiarFormulario();

        } catch (Exception e) {
            mostrarMensaje(e.getMessage(), true);
        }
    }

    private void calcularTotal() {
        double total = 0;
        for (DetalleDevolucionFx d : detallesObList) {
            total += d.getADevolver() * d.getPrecioUnitario();
        }
        lblTotalDevolver.setText(ProcesarFinalizarVentaUseCase.formatearMoneda(total));
    }

    private void limpiarFormulario() {
        idVentaActual = -1;
        detallesObList.clear();
        txtIdVenta.clear();
        txtCantidadDevolver.clear();
        txtMotivoProducto.clear();
        txtMotivoGeneral.clear();
        txtSupervisorUsuario.clear();
        txtSupervisorPassword.clear();
        cmbMetodoReembolso.getSelectionModel().clearSelection();
        cmbEstadoProducto.getSelectionModel().clearSelection();
        lblTotalDevolver.setText("$0.00");
        mostrarMensaje("Listo para una nueva devolución.", false);
    }

    private void configurarTabla() {
        tblDetalles.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        colCodigo.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getIdProducto()));
        colProducto.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombre()));
        colComprada.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getComprada()));
        colDevuelta.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getDevuelta()));
        colPrecio.setCellValueFactory(data -> new SimpleStringProperty(
                ProcesarFinalizarVentaUseCase.formatearMoneda(data.getValue().getPrecioUnitario())
        ));
        colADevolver.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getADevolver()));
        colMotivo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getMotivo()));
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEstadoProducto()));
    }

    private void mostrarMensaje(String mensaje, boolean error) {
        lblMensaje.setText(mensaje);
        lblMensaje.setStyle(error
                ? "-fx-text-fill: #c0392b; -fx-font-weight: bold;"
                : "-fx-text-fill: #1e8449; -fx-font-weight: bold;");
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, mensaje, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle(titulo);
        alert.showAndWait();
    }

    public static class DetalleDevolucionFx {
        private final int idProducto;
        private final String nombre;
        private final int comprada;
        private final int devuelta;
        private final double precioUnitario;
        private int aDevolver;
        private String motivo;
        private String estadoProducto;

        public DetalleDevolucionFx(int idProducto, String nombre, int comprada, int devuelta, double precioUnitario) {
            this.idProducto = idProducto;
            this.nombre = nombre;
            this.comprada = comprada;
            this.devuelta = devuelta;
            this.precioUnitario = precioUnitario;
            this.aDevolver = 0;
            this.motivo = "";
            this.estadoProducto = "";
        }

        public int getIdProducto() { return idProducto; }
        public String getNombre() { return nombre; }
        public int getComprada() { return comprada; }
        public int getDevuelta() { return devuelta; }
        public double getPrecioUnitario() { return precioUnitario; }
        public int getADevolver() { return aDevolver; }
        public void setADevolver(int aDevolver) { this.aDevolver = aDevolver; }
        public String getMotivo() { return motivo; }
        public void setMotivo(String motivo) { this.motivo = motivo == null ? "" : motivo; }
        public String getEstadoProducto() { return estadoProducto; }
        public void setEstadoProducto(String estadoProducto) { this.estadoProducto = estadoProducto == null ? "" : estadoProducto; }
    }
}