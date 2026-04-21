package controllers.caja;

import entities.Usuario;
import dtos.ResumenCierreCajaDTO;
import entities.CierreCaja;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import repositories.CierreCajaRepository;
import repositories.DatabaseConnection;
import repositories.H2CierreCajaRepository;
import services.autenticacion.SesionUsuario;
import services.caja.GestionarCierreCajaUseCase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

public class CierreCajaController {

    @FXML private DatePicker dateFecha;
    @FXML private ComboBox<String> cbTurno;
    @FXML private Label lblEsperado;
    @FXML private Label lblTransacciones;
    @FXML private TextField txtContado;
    @FXML private TextArea txtObservacion;
    @FXML private Label lblDiferencia;
    @FXML private Label lblEstado;

    private final GestionarCierreCajaUseCase useCase;

    private BigDecimal efectivoEsperadoActual = BigDecimal.ZERO;

    public CierreCajaController() {
        CierreCajaRepository repo = new H2CierreCajaRepository(new DatabaseConnection());
        this.useCase = new GestionarCierreCajaUseCase(repo);
    }

    @FXML
    public void initialize() {
        cbTurno.getItems().addAll("MANANA", "TARDE", "NOCHE");
        dateFecha.setValue(LocalDate.now());
        lblEstado.getStyleClass().add("status-pending");
    }

    @FXML
    public void cargarResumen() {
        try {
            LocalDate fecha = dateFecha.getValue();
            String turno = cbTurno.getValue();

            if (fecha == null || turno == null) {
                mostrarAlerta("Debe seleccionar fecha y turno.");
                return;
            }

            ResumenCierreCajaDTO resumen = useCase.obtenerResumen(fecha, turno);
            efectivoEsperadoActual = resumen.getEfectivoEsperado();

            lblEsperado.setText("$ " + efectivoEsperadoActual.setScale(2, RoundingMode.HALF_UP));
            lblTransacciones.setText(String.valueOf(resumen.getTotalTransacciones()));

            calcularDiferencia();
        } catch (Exception e) {
            mostrarAlerta("Error al cargar resumen: " + e.getMessage());
        }
    }

    @FXML
    public void calcularDiferencia() {
        try {
            BigDecimal contado = obtenerContado();
            BigDecimal diferencia = contado.subtract(efectivoEsperadoActual).setScale(2, RoundingMode.HALF_UP);

            lblDiferencia.setText(diferencia.toPlainString());

            lblEstado.getStyleClass().removeAll("status-pending", "status-ok", "status-warning", "status-error");

            int cmp = diferencia.compareTo(BigDecimal.ZERO);
            if (cmp == 0) {
                lblEstado.setText("CUADRADO");
                lblEstado.getStyleClass().add("status-ok");
            } else if (cmp > 0) {
                lblEstado.setText("SOBRANTE");
                lblEstado.getStyleClass().add("status-warning");
            } else {
                lblEstado.setText("FALTANTE");
                lblEstado.getStyleClass().add("status-error");
            }
        } catch (Exception e) {
            lblDiferencia.setText("0.00");
            lblEstado.setText("PENDIENTE");
            lblEstado.getStyleClass().removeAll("status-ok", "status-warning", "status-error");
            if (!lblEstado.getStyleClass().contains("status-pending")) {
                lblEstado.getStyleClass().add("status-pending");
            }
        }
    }
    @FXML
    public void confirmarCierre() {
        try {
            LocalDate fecha = dateFecha.getValue();
            String turno = cbTurno.getValue();
            BigDecimal contado = obtenerContado();
            String observacion = txtObservacion.getText();

            if (fecha == null || turno == null) {
                mostrarAlerta("Debe seleccionar fecha y turno.");
                return;
            }

            Usuario usuarioActual = SesionUsuario.getUsuarioActual();

            if (usuarioActual == null) {
                mostrarAlerta("No hay una sesión activa.");
                return;
            }

            int idEmpleado = usuarioActual.getEmpleado().getId();

            CierreCaja cierre = useCase.registrarCierre(fecha, turno, idEmpleado, contado, observacion);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Cierre registrado");
            alert.setHeaderText("Cierre exitoso");
            alert.setContentText(
                    "Número: " + cierre.getNumeroCierre() +
                            "\nEsperado: " + cierre.getEfectivoEsperado() +
                            "\nContado: " + cierre.getEfectivoContado() +
                            "\nDiferencia: " + cierre.getDiferencia() +
                            "\nEstado: " + cierre.getEstadoCierre()
            );
            alert.showAndWait();

        } catch (Exception e) {
            mostrarAlerta("No se pudo registrar el cierre: " + e.getMessage());
        }
    }

    private BigDecimal obtenerContado() {
        String texto = txtContado.getText();

        if (texto == null || texto.isBlank()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(texto.trim());
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Atención");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
