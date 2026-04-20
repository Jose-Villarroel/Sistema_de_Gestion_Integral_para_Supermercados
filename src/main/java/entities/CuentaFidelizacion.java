package entities;

import valueobjects.CategoriaFidelidad;
import java.time.LocalDate;

public class CuentaFidelizacion {

    private final int id;
    private final int idCliente;
    private final int numeroTarjeta;
    private int puntosActuales;
    private final LocalDate fechaCreacion;
    private boolean activa;

    public CuentaFidelizacion(int id, int idCliente, int numeroTarjeta,
                              int puntosActuales, LocalDate fechaCreacion, boolean activa) {

        if (puntosActuales < 0) {
            throw new IllegalArgumentException("Los puntos no pueden ser negativos");
        }
        if (idCliente <= 0) {
            throw new IllegalArgumentException("La cuenta debe pertenecer a un cliente válido");
        }

        this.id = id;
        this.idCliente = idCliente;
        this.numeroTarjeta = numeroTarjeta;
        this.puntosActuales = puntosActuales;
        this.fechaCreacion = fechaCreacion;
        this.activa = activa;
    }

    /**
     * Acumula puntos a la cuenta.
     */
    public void acumularPuntos(int puntos) {
        if (puntos <= 0) {
            throw new IllegalArgumentException("Los puntos a acumular deben ser positivos");
        }
        this.puntosActuales += puntos;
    }

    /**
     * Canjea puntos de la cuenta.
     */
    public void canjearPuntos(int puntos) {
        if (puntos <= 0) {
            throw new IllegalArgumentException("Los puntos a canjear deben ser positivos");
        }
        if (puntos > puntosActuales) {
            throw new IllegalArgumentException("Puntos insuficientes. Disponibles: " + puntosActuales);
        }
        this.puntosActuales -= puntos;
    }

    /**
     * Devuelve la categoría de fidelidad según los puntos acumulados.
     */
    public CategoriaFidelidad getCategoria() {
        return CategoriaFidelidad.calcularPorPuntos(puntosActuales);
    }

    public void desactivar() {
        this.activa = false;
    }

    // Getters
    public int getId() { return id; }
    public int getIdCliente() { return idCliente; }
    public int getNumeroTarjeta() { return numeroTarjeta; }
    public int getPuntosActuales() { return puntosActuales; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public boolean isActiva() { return activa; }
}