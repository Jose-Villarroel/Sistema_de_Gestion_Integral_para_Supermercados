package services.caja;

import dtos.ResumenCierreCajaDTO;
import entities.CierreCaja;
import repositories.CierreCajaRepository;
import valueobjects.EstadoCierreCaja;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

public class GestionarCierreCajaUseCase {

    private final CierreCajaRepository cierreCajaRepository;

    public GestionarCierreCajaUseCase(CierreCajaRepository cierreCajaRepository) {
        this.cierreCajaRepository = cierreCajaRepository;
    }

    public ResumenCierreCajaDTO obtenerResumen(LocalDate fecha, String turno) throws SQLException {
        return cierreCajaRepository.obtenerResumenTurno(fecha, turno);
    }

    public CierreCaja registrarCierre(LocalDate fecha, String turno, int idEmpleado,
                                      BigDecimal efectivoContado, String observacion) throws SQLException {

        if (efectivoContado == null) {
            throw new IllegalArgumentException("El efectivo contado es obligatorio.");
        }

        if (efectivoContado.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El efectivo contado no puede ser negativo.");
        }

        if (cierreCajaRepository.existeCierre(fecha, turno)) {
            throw new IllegalStateException("Ya existe un cierre para la fecha y turno indicados.");
        }

        ResumenCierreCajaDTO resumen = cierreCajaRepository.obtenerResumenTurno(fecha, turno);

        BigDecimal esperado = resumen.getEfectivoEsperado().setScale(2, RoundingMode.HALF_UP);
        BigDecimal contado = efectivoContado.setScale(2, RoundingMode.HALF_UP);
        BigDecimal diferencia = contado.subtract(esperado).setScale(2, RoundingMode.HALF_UP);

        EstadoCierreCaja estado;
        int comparacion = diferencia.compareTo(BigDecimal.ZERO);

        if (comparacion == 0) {
            estado = EstadoCierreCaja.CUADRADO;
        } else if (comparacion > 0) {
            estado = EstadoCierreCaja.SOBRANTE;
        } else {
            estado = EstadoCierreCaja.FALTANTE;
        }

        String numeroCierre = "CC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        CierreCaja cierreCaja = new CierreCaja(
                0,
                numeroCierre,
                fecha,
                turno,
                idEmpleado,
                esperado,
                contado,
                diferencia,
                estado,
                resumen.getTotalTransacciones(),
                observacion
        );

        cierreCajaRepository.guardar(cierreCaja);
        return cierreCaja;
    }
}