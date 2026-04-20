package entities;

import valueobjects.EstadoCierreCaja;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CierreCaja {
    private final int idCierre;
    private final String numeroCierre;
    private final LocalDate fechaCierre;
    private final String turno;
    private final int idEmpleado;
    private final BigDecimal efectivoEsperado;
    private final BigDecimal efectivoContado;
    private final BigDecimal diferencia;
    private final EstadoCierreCaja estadoCierre;
    private final int totalTransacciones;
    private final String observacion;

    public CierreCaja(int idCierre, String numeroCierre, LocalDate fechaCierre, String turno,
                      int idEmpleado, BigDecimal efectivoEsperado, BigDecimal efectivoContado,
                      BigDecimal diferencia, EstadoCierreCaja estadoCierre,
                      int totalTransacciones, String observacion) {
        this.idCierre = idCierre;
        this.numeroCierre = numeroCierre;
        this.fechaCierre = fechaCierre;
        this.turno = turno;
        this.idEmpleado = idEmpleado;
        this.efectivoEsperado = efectivoEsperado;
        this.efectivoContado = efectivoContado;
        this.diferencia = diferencia;
        this.estadoCierre = estadoCierre;
        this.totalTransacciones = totalTransacciones;
        this.observacion = observacion;
    }

    public int getIdCierre() { return idCierre; }
    public String getNumeroCierre() { return numeroCierre; }
    public LocalDate getFechaCierre() { return fechaCierre; }
    public String getTurno() { return turno; }
    public int getIdEmpleado() { return idEmpleado; }
    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public BigDecimal getEfectivoContado() { return efectivoContado; }
    public BigDecimal getDiferencia() { return diferencia; }
    public EstadoCierreCaja getEstadoCierre() { return estadoCierre; }
    public int getTotalTransacciones() { return totalTransacciones; }
    public String getObservacion() { return observacion; }
}