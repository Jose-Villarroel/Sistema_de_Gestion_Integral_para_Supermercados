package controllers.gerente;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import services.reportes.GenerarReporteVentasUseCase;
import services.reportes.ReporteVentas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class ReporteVentasController {

    @FXML private PieChart chartMetodosPago;
    @FXML private LineChart<String, Number> chartTendenciaVentas;

    @FXML private ComboBox<String> cmbTipoReporte;
    @FXML private DatePicker dpFechaDesde;
    @FXML private DatePicker dpFechaHasta;

    @FXML private Label lblTotalVendido;
    @FXML private Label lblTransacciones;
    @FXML private Label lblTicketPromedio;
    @FXML private Label lblDescuentos;
    @FXML private Label lblImpuestos;
    @FXML private Label lblMensaje;

    @FXML private TextArea txtDetalleMetodosPago;

    private final GenerarReporteVentasUseCase generarReporteVentasUseCase;

    public ReporteVentasController(GenerarReporteVentasUseCase generarReporteVentasUseCase) {
        this.generarReporteVentasUseCase = generarReporteVentasUseCase;
    }

    @FXML
    public void initialize() {
        cmbTipoReporte.getItems().addAll("Diario", "Mensual", "Anual");
        cmbTipoReporte.setValue("Diario");

        dpFechaDesde.setValue(LocalDate.now());
        dpFechaHasta.setValue(LocalDate.now());

        lblMensaje.setText("Seleccione el tipo de reporte y presione Generar reporte.");
    }

    @FXML
    public void generarReporte() {
        try {
            LocalDate desde = dpFechaDesde.getValue();
            LocalDate hasta = dpFechaHasta.getValue();

            if (desde == null || hasta == null) {
                mostrarError("Debe seleccionar las fechas del reporte.");
                return;
            }

            ReporteVentas reporte = generarReporteVentasUseCase.ejecutar(desde, hasta);

            lblTotalVendido.setText(formatearMoneda(reporte.totalVendido()));
            lblTransacciones.setText(String.valueOf(reporte.numeroTransacciones()));
            lblTicketPromedio.setText(formatearMoneda(reporte.ticketPromedio()));
            lblDescuentos.setText(formatearMoneda(reporte.totalDescuentos()));
            lblImpuestos.setText(formatearMoneda(reporte.totalImpuestos()));

            mostrarMetodosPago(reporte.ventasPorMetodoPago());
            mostrarTendenciaVentas(reporte);

            lblMensaje.setText("Reporte generado correctamente.");
            lblMensaje.getStyleClass().removeAll("message-error");
            lblMensaje.getStyleClass().add("message-ok");

        } catch (Exception e) {
            mostrarError(e.getMessage());
        }
    }

    @FXML
    public void exportarPDF() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Exportar PDF");
        alert.setHeaderText("Funcionalidad pendiente");
        alert.setContentText("La opción de exportar a PDF se implementará posteriormente.");
        alert.showAndWait();
    }

    @FXML
    public void limpiarFiltros() {
        cmbTipoReporte.setValue("Diario");
        dpFechaDesde.setValue(LocalDate.now());
        dpFechaHasta.setValue(LocalDate.now());

        lblTotalVendido.setText("0.00");
        lblTransacciones.setText("0");
        lblTicketPromedio.setText("0.00");
        lblDescuentos.setText("0.00");
        lblImpuestos.setText("0.00");

        txtDetalleMetodosPago.clear();
        chartMetodosPago.getData().clear();
        chartTendenciaVentas.getData().clear();

        lblMensaje.setText("Filtros restablecidos.");
        lblMensaje.getStyleClass().removeAll("message-error", "message-ok");
    }

    private void mostrarMetodosPago(Map<String, BigDecimal> ventasPorMetodoPago) {
        txtDetalleMetodosPago.clear();
        chartMetodosPago.getData().clear();

        StringBuilder sb = new StringBuilder();

        if (ventasPorMetodoPago == null || ventasPorMetodoPago.isEmpty()) {
            sb.append("No hay ventas registradas por método de pago.");
        } else {
            ventasPorMetodoPago.forEach((metodo, valor) -> {
                sb.append(metodo)
                        .append(": ")
                        .append(formatearMoneda(valor))
                        .append("\n");

                chartMetodosPago.getData().add(
                        new PieChart.Data(metodo, valor.doubleValue())
                );
            });
        }

        txtDetalleMetodosPago.setText(sb.toString());
    }

    private void mostrarTendenciaVentas(ReporteVentas reporte) {
        chartTendenciaVentas.getData().clear();

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Ventas");

        BigDecimal total = reporte.totalVendido();

        serie.getData().add(new XYChart.Data<>("Inicio", total.multiply(new BigDecimal("0.40"))));
        serie.getData().add(new XYChart.Data<>("Medio", total.multiply(new BigDecimal("0.70"))));
        serie.getData().add(new XYChart.Data<>("Final", total));

        chartTendenciaVentas.getData().add(serie);
    }

    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) {
            return "$0.00";
        }
        return "$" + String.format("%,.2f", valor);
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.getStyleClass().removeAll("message-ok");
        lblMensaje.getStyleClass().add("message-error");
    }
}