package services.dashboard;

/**
 * DTO que representa un producto en el ranking de más vendidos del dashboard.
 */
public record ProductoVendido(
        int idProducto,
        String nombre,
        int cantidadVendida
) {
}