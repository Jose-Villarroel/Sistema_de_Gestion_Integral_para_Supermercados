package entities;

import java.time.LocalDate;

public class MovimientoInventario {

    private final int idMovimiento;
    private final int idEmpleado;
    private final int idTipoMovimiento;
    private final int idProducto;
    private final int cantidad;
    private final int stockAnterior;
    private final int stockNuevo;
    private final String motivo;
    private final LocalDate fechaMovimiento;

    public MovimientoInventario(int idMovimiento,
                                int idEmpleado,
                                int idTipoMovimiento,
                                int idProducto,
                                int cantidad,
                                int stockAnterior,
                                int stockNuevo,
                                String motivo,
                                LocalDate fechaMovimiento) {

        if (idEmpleado <= 0) {
            throw new IllegalArgumentException("El id del empleado debe ser válido");
        }

        if (idTipoMovimiento <= 0) {
            throw new IllegalArgumentException("El id del tipo de movimiento debe ser válido");
        }

        if (idProducto <= 0) {
            throw new IllegalArgumentException("El id del producto debe ser válido");
        }

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser un número positivo");
        }

        if (stockAnterior < 0) {
            throw new IllegalArgumentException("El stock anterior no puede ser negativo");
        }

        if (stockNuevo < 0) {
            throw new IllegalArgumentException("El stock nuevo no puede ser negativo");
        }

        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo es obligatorio");
        }

        if (fechaMovimiento == null) {
            throw new IllegalArgumentException("La fecha del movimiento es obligatoria");
        }

        this.idMovimiento = idMovimiento;
        this.idEmpleado = idEmpleado;
        this.idTipoMovimiento = idTipoMovimiento;
        this.idProducto = idProducto;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.motivo = motivo;
        this.fechaMovimiento = fechaMovimiento;
    }

    public int getIdMovimiento() { return idMovimiento; }
    public int getIdEmpleado() { return idEmpleado; }
    public int getIdTipoMovimiento() { return idTipoMovimiento; }
    public int getIdProducto() { return idProducto; }
    public int getCantidad() { return cantidad; }
    public int getStockAnterior() { return stockAnterior; }
    public int getStockNuevo() { return stockNuevo; }
    public String getMotivo() { return motivo; }
    public LocalDate getFechaMovimiento() { return fechaMovimiento; }
}