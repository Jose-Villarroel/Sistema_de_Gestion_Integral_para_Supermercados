package dtos;

import java.math.BigDecimal;

public class ResumenCierreCajaDTO {
    private final BigDecimal efectivoEsperado;
    private final int totalTransacciones;

    public ResumenCierreCajaDTO(BigDecimal efectivoEsperado, int totalTransacciones) {
        this.efectivoEsperado = efectivoEsperado;
        this.totalTransacciones = totalTransacciones;
    }

    public BigDecimal getEfectivoEsperado() {
        return efectivoEsperado;
    }

    public int getTotalTransacciones() {
        return totalTransacciones;
    }
}