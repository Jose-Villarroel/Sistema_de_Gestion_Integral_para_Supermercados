package entities;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Aggregate root del módulo de órdenes de compra.
 * Contiene la información cabecera de la orden y sus detalles (líneas).
 * 
 * Estado:
 *   true  = orden activa (creada o pendiente de recibir)
 *   false = orden cancelada
 */
public class OrdenCompra {

    private final int id;
    private final int idProveedor;
    private final int idEmpleado;
    private final LocalDate fechaCreacion;
    private final LocalDate fechaEntrega;
    private final BigDecimal total;
    private boolean activa;
    private final List<DetalleOrdenCompra> detalles;

    public OrdenCompra(int id, int idProveedor, int idEmpleado,
                       LocalDate fechaCreacion, LocalDate fechaEntrega,
                       boolean activa, List<DetalleOrdenCompra> detalles) {

        if (idProveedor <= 0) {
            throw new IllegalArgumentException("El proveedor es obligatorio");
        }
        if (idEmpleado <= 0) {
            throw new IllegalArgumentException("El empleado es obligatorio");
        }
        if (fechaCreacion == null) {
            throw new IllegalArgumentException("La fecha de creación es obligatoria");
        }
        if (detalles == null || detalles.isEmpty()) {
            throw new IllegalArgumentException("La orden debe tener al menos un producto");
        }

        this.id = id;
        this.idProveedor = idProveedor;
        this.idEmpleado = idEmpleado;
        this.fechaCreacion = fechaCreacion;
        this.fechaEntrega = fechaEntrega;
        this.activa = activa;
        this.detalles = new ArrayList<>(detalles);
        this.total = calcularTotal();
    }

    private BigDecimal calcularTotal() {
        return detalles.stream()
                .map(DetalleOrdenCompra::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Cancela la orden. Una orden cancelada no puede recibir mercancía.
     */
    public void cancelar() {
        if (!activa) {
            throw new IllegalStateException("La orden ya está cancelada");
        }
        this.activa = false;
    }

    // Getters
    public int getId() { return id; }
    public int getIdProveedor() { return idProveedor; }
    public int getIdEmpleado() { return idEmpleado; }
    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public LocalDate getFechaEntrega() { return fechaEntrega; }
    public BigDecimal getTotal() { return total; }
    public boolean isActiva() { return activa; }
    public List<DetalleOrdenCompra> getDetalles() {
        return Collections.unmodifiableList(detalles);
    }
}