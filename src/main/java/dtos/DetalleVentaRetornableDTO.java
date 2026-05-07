package dtos;

public record DetalleVentaRetornableDTO(
        int idProducto,
        String nombreProducto,
        int comprada,
        int devuelta,
        double precioRealUnitario
) {
}
