package controllers.gerente;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import javafx.stage.FileChooser;
import java.io.File;

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
        String totalVendido   = lblTotalVendido.getText();
        String transacciones  = lblTransacciones.getText();
        String ticketPromedio = lblTicketPromedio.getText();
        String descuentos     = lblDescuentos.getText();
        String impuestos      = lblImpuestos.getText();
        String detallePagos   = txtDetalleMetodosPago.getText();

        if (totalVendido.equals("0.00") && transacciones.equals("0")) {
            mostrarError("Primero genere un reporte antes de exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar reporte PDF");
        fileChooser.setInitialFileName("Reporte_Ventas_MasterMarket.pdf");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf")
        );

        File archivo = fileChooser.showSaveDialog(lblTotalVendido.getScene().getWindow());
        if (archivo == null) return;

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float margin     = 50;
                float y          = page.getMediaBox().getHeight() - margin;
                float lineHeight = 18;

                // Título
                cs.beginText();
                cs.setFont(fontBold, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("MasterMarket - Reporte de Ventas");
                cs.endText();
                y -= lineHeight * 1.5f;

                // Fecha de generación
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Generado el: " + LocalDate.now());
                cs.endText();
                y -= lineHeight;

                // Rango de fechas
                String desde = dpFechaDesde.getValue() != null ? dpFechaDesde.getValue().toString() : "-";
                String hasta  = dpFechaHasta.getValue()  != null ? dpFechaHasta.getValue().toString()  : "-";
                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Periodo: " + desde + " al " + hasta);
                cs.endText();
                y -= lineHeight * 1.5f;

                // Línea separadora
                cs.moveTo(margin, y);
                cs.lineTo(page.getMediaBox().getWidth() - margin, y);
                cs.stroke();
                y -= lineHeight;

                // Título sección
                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Resumen del periodo");
                cs.endText();
                y -= lineHeight * 1.2f;

                // Indicadores
                String[][] datos = {
                    {"Total vendido:",    totalVendido},
                    {"Transacciones:",    transacciones},
                    {"Ticket promedio:",  ticketPromedio},
                    {"Total descuentos:", descuentos},
                    {"Total impuestos:",  impuestos},
                };

                for (String[] fila : datos) {
                    cs.beginText();
                    cs.setFont(fontBold, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(fila[0]);
                    cs.endText();

                    cs.beginText();
                    cs.setFont(fontRegular, 11);
                    cs.newLineAtOffset(margin + 160, y);
                    cs.showText(fila[1]);
                    cs.endText();

                    y -= lineHeight;
                }

                y -= lineHeight * 0.5f;

                // Línea separadora
                cs.moveTo(margin, y);
                cs.lineTo(page.getMediaBox().getWidth() - margin, y);
                cs.stroke();
                y -= lineHeight;

                // Detalle métodos de pago
                if (detallePagos != null && !detallePagos.isBlank()) {
                    cs.beginText();
                    cs.setFont(fontBold, 13);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Ventas por metodo de pago");
                    cs.endText();
                    y -= lineHeight * 1.2f;

                    for (String linea : detallePagos.split("\n")) {
                        if (y < margin + 20) break;
                        cs.beginText();
                        cs.setFont(fontRegular, 11);
                        cs.newLineAtOffset(margin, y);
                        cs.showText(linea);
                        cs.endText();
                        y -= lineHeight;
                    }
                }

                // Pie de página
                cs.beginText();
                cs.setFont(fontRegular, 9);
                cs.newLineAtOffset(margin, margin);
                cs.showText("MasterMarket - Sistema de Gestion Integral para Supermercados");
                cs.endText();
            }

            doc.save(archivo);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("PDF exportado");
            alert.setHeaderText(null);
            alert.setContentText("Reporte exportado correctamente en:\n" + archivo.getAbsolutePath());
            alert.showAndWait();

            lblMensaje.setText("PDF exportado correctamente.");
            lblMensaje.getStyleClass().removeAll("message-error");
            lblMensaje.getStyleClass().add("message-ok");

        } catch (Exception e) {
            mostrarError("Error al exportar PDF: " + e.getMessage());
        }
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
        serie.getData().add(new XYChart.Data<>("Medio",  total.multiply(new BigDecimal("0.70"))));
        serie.getData().add(new XYChart.Data<>("Final",  total));

        chartTendenciaVentas.getData().add(serie);
    }

    private String formatearMoneda(BigDecimal valor) {
        if (valor == null) return "$0.00";
        return "$" + String.format("%,.2f", valor);
    }

    private void mostrarError(String mensaje) {
        lblMensaje.setText(mensaje);
        lblMensaje.getStyleClass().removeAll("message-ok");
        lblMensaje.getStyleClass().add("message-error");
    }
}