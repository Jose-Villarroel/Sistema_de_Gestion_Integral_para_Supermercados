package controllers.gerente;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.embed.swing.SwingFXUtils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import services.reportes.GenerarReporteVentasUseCase;
import services.reportes.ReporteVentas;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
    private ReporteVentas ultimoReporte;

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
            this.ultimoReporte = reporte;

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
        if (ultimoReporte == null) {
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

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                float lineHeight = 18;

                cs.beginText();
                cs.setFont(fontBold, 20);
                cs.newLineAtOffset(margin, y);
                cs.showText("MasterMarket - Reporte de Ventas");
                cs.endText();
                y -= lineHeight * 1.5f;

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Generado el: " + LocalDate.now());
                cs.endText();
                y -= lineHeight;

                cs.beginText();
                cs.setFont(fontRegular, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Periodo: " + ultimoReporte.fechaDesde() + " al " + ultimoReporte.fechaHasta());
                cs.endText();
                y -= lineHeight * 1.5f;

                cs.moveTo(margin, y);
                cs.lineTo(page.getMediaBox().getWidth() - margin, y);
                cs.stroke();
                y -= lineHeight;

                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Resumen del periodo");
                cs.endText();
                y -= lineHeight * 1.2f;

                String[][] datos = {
                        {"Total vendido:", formatearMoneda(ultimoReporte.totalVendido())},
                        {"Transacciones:", String.valueOf(ultimoReporte.numeroTransacciones())},
                        {"Ticket promedio:", formatearMoneda(ultimoReporte.ticketPromedio())},
                        {"Total descuentos:", formatearMoneda(ultimoReporte.totalDescuentos())},
                        {"Total impuestos:", formatearMoneda(ultimoReporte.totalImpuestos())}
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

                cs.moveTo(margin, y);
                cs.lineTo(page.getMediaBox().getWidth() - margin, y);
                cs.stroke();
                y -= lineHeight;

                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Graficas del reporte");
                cs.endText();
                y -= lineHeight * 1.2f;

                PDImageXObject imgTendencia = crearImagenTendenciaPDF(doc);
                PDImageXObject imgMetodos = crearImagenMetodosPagoPDF(doc);

                float chartWidth = 230;
                float chartHeight = 170;

                cs.drawImage(imgTendencia, margin, y - chartHeight, chartWidth, chartHeight);
                cs.drawImage(imgMetodos, margin + chartWidth + 30, y - chartHeight, chartWidth, chartHeight);

                y -= chartHeight + lineHeight;

                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Ventas por metodo de pago");
                cs.endText();
                y -= lineHeight * 1.2f;

                for (Map.Entry<String, BigDecimal> entry : ultimoReporte.ventasPorMetodoPago().entrySet()) {
                    if (y < margin + 40) break;

                    cs.beginText();
                    cs.setFont(fontRegular, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(entry.getKey() + " -> " + formatearMoneda(entry.getValue()));
                    cs.endText();
                    y -= lineHeight;
                }

                y -= lineHeight * 0.5f;

                cs.beginText();
                cs.setFont(fontBold, 13);
                cs.newLineAtOffset(margin, y);
                cs.showText("Tendencia de ventas por dia");
                cs.endText();
                y -= lineHeight * 1.2f;

                for (Map.Entry<LocalDate, BigDecimal> entry : ultimoReporte.ventasPorDia().entrySet()) {
                    if (y < margin + 40) break;

                    cs.beginText();
                    cs.setFont(fontRegular, 11);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(entry.getKey() + " -> " + formatearMoneda(entry.getValue()));
                    cs.endText();
                    y -= lineHeight;
                }

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

        ultimoReporte = null;

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

                String etiqueta = metodo + " - " + formatearMoneda(valor);

                chartMetodosPago.getData().add(
                        new PieChart.Data(etiqueta, valor.doubleValue())
                );
            });
        }

        txtDetalleMetodosPago.setText(sb.toString());
    }

    private void mostrarTendenciaVentas(ReporteVentas reporte) {
        chartTendenciaVentas.getData().clear();
        chartTendenciaVentas.setLegendVisible(false);
        chartTendenciaVentas.setCreateSymbols(true);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Ventas por día");

        if (reporte.ventasPorDia() == null || reporte.ventasPorDia().isEmpty()) {
            return;
        }

        reporte.ventasPorDia().forEach((fecha, total) -> {
            serie.getData().add(
                    new XYChart.Data<>(
                            fecha.getDayOfMonth() + "/" + fecha.getMonthValue(),
                            total.doubleValue()
                    )
            );
        });

        chartTendenciaVentas.getData().add(serie);
    }

    private PDImageXObject crearImagenTendenciaPDF(PDDocument doc) throws Exception {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Fecha");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Valor ($)");

        LineChart<String, Number> chartCopia = new LineChart<>(xAxis, yAxis);
        chartCopia.setTitle("Tendencia de ventas");
        chartCopia.setAnimated(false);
        chartCopia.setCreateSymbols(true);
        chartCopia.setLegendVisible(false);
        chartCopia.setPrefSize(650, 420);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Ventas");

        if (ultimoReporte != null && ultimoReporte.ventasPorDia() != null) {
            ultimoReporte.ventasPorDia().forEach((fecha, total) -> {
                serie.getData().add(
                        new XYChart.Data<>(
                                fecha.getDayOfMonth() + "/" + fecha.getMonthValue(),
                                total.doubleValue()
                        )
                );
            });
        }

        chartCopia.getData().add(serie);

        StackPane root = new StackPane(chartCopia);
        root.setPrefSize(650, 420);

        new Scene(root);

        root.applyCss();
        root.layout();

        WritableImage image = root.snapshot(new SnapshotParameters(), null);
        return convertirImagen(doc, image, "tendencia.png");
    }

    private PDImageXObject crearImagenMetodosPagoPDF(PDDocument doc) throws Exception {
        PieChart pieCopia = new PieChart();
        pieCopia.setTitle("Ventas por metodo de pago");
        pieCopia.setLabelsVisible(true);
        pieCopia.setLegendVisible(true);
        pieCopia.setPrefSize(650, 420);

        if (ultimoReporte != null && ultimoReporte.ventasPorMetodoPago() != null) {
            ultimoReporte.ventasPorMetodoPago().forEach((metodo, valor) -> {
                String etiqueta = metodo + " - " + formatearMoneda(valor);
                pieCopia.getData().add(
                        new PieChart.Data(etiqueta, valor.doubleValue())
                );
            });
        }

        StackPane root = new StackPane(pieCopia);
        root.setPrefSize(650, 420);

        new Scene(root);

        root.applyCss();
        root.layout();

        WritableImage image = root.snapshot(new SnapshotParameters(), null);
        return convertirImagen(doc, image, "metodos_pago.png");
    }

    private PDImageXObject convertirImagen(PDDocument doc, WritableImage image, String nombre) throws Exception {
        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "png", baos);

        return PDImageXObject.createFromByteArray(doc, baos.toByteArray(), nombre);
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