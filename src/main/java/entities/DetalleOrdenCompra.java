package entities;

import java.math.BigDecimal;

/**
 * Entity que representa una línea de una orden de compra.
 * Pertenece al aggregate root OrdenCompra.
 */
public class DetalleOrdenCompra {

    private final int id;
    private final int idProducto;
    private final int cantidad;
    private final BigDecimal costoUnitario;
    private final BigDecimal subtotal;

    public DetalleOrdenCompra(int id, int idProducto, int cantidad,
                              BigDecimal costoUnitario) {

        if (idProducto <= 0) {
            throw new IllegalArgumentException("El producto es obligatorio");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        if (costoUnitario == null || costoUnitario.signum() <= 0) {
            throw new IllegalArgumentException("El costo unitario debe ser positivo");
        }

        this.id = id;
        this.idProducto = idProducto;
        this.cantidad = cantidad;
        this.costoUnitario = costoUnitario;
        this.subtotal = costoUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    // Getters
    public int getId() { return id; }
    public int getIdProducto() { return idProducto; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getCostoUnitario() { return costoUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }
}