package entities;

import java.time.LocalDateTime;

public class MovimientoInventario {

    public enum TipoMovimiento {
        ENTRADA, SALIDA, AJUSTE, CONTEO_FISICO
    }

    private final int id;
    private final LocalDateTime fecha;
    private final TipoMovimiento tipo;
    private final int cantidad;
    private final String motivo;
    private final int productoId;
    private final int empleadoId;
    private final int ordenId;

    public MovimientoInventario(int id, LocalDateTime fecha, TipoMovimiento tipo,
                                int cantidad, String motivo,
                                int productoId, int empleadoId, int ordenId) {
        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad debe ser un número positivo");
        if (motivo == null || motivo.isBlank())
            throw new IllegalArgumentException("Debe seleccionar un motivo para el movimiento");

        this.id = id;
        this.fecha = fecha;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.productoId = productoId;
        this.empleadoId = empleadoId;
        this.ordenId = ordenId;
    }

    public int getId() { return id; }
    public LocalDateTime getFecha() { return fecha; }
    public TipoMovimiento getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public String getMotivo() { return motivo; }
    public int getProductoId() { return productoId; }
    public int getEmpleadoId() { return empleadoId; }
    public int getOrdenId() { return ordenId; }
}
