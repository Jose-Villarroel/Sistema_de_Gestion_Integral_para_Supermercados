package valueobjects;

public enum CategoriaFidelidad {

    BRONCE(0, 999, "Bronce"),
    PLATA(1000, 4999, "Plata"),
    ORO(5000, Integer.MAX_VALUE, "Oro");

    private final int puntosMinimos;
    private final int puntosMaximos;
    private final String descripcion;

    CategoriaFidelidad(int puntosMinimos, int puntosMaximos, String descripcion) {
        this.puntosMinimos = puntosMinimos;
        this.puntosMaximos = puntosMaximos;
        this.descripcion = descripcion;
    }

    /**
     * Calcula la categoría de fidelidad según los puntos acumulados.
     */
    public static CategoriaFidelidad calcularPorPuntos(int puntos) {
        if (puntos < 0) {
            throw new IllegalArgumentException("Los puntos no pueden ser negativos");
        }
        for (CategoriaFidelidad categoria : values()) {
            if (puntos >= categoria.puntosMinimos && puntos <= categoria.puntosMaximos) {
                return categoria;
            }
        }
        return BRONCE;
    }

    public int getPuntosMinimos() {
        return puntosMinimos;
    }

    public int getPuntosMaximos() {
        return puntosMaximos;
    }

    public String getDescripcion() {
        return descripcion;
    }
}