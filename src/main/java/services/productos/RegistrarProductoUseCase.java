package services.productos;

import aggregates.Producto;
import repositories.ProductoRepository;

import java.time.LocalDate;

public class RegistrarProductoUseCase {

    private final ProductoRepository productoRepository;

    public RegistrarProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Producto ejecutar(String nombre, String descripcion, String marca,
                             double precioCompra, double precioVenta,
                             int stockActual, int stockMinimo,
                             int categoriaId, boolean estadoActivo) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }

        if (productoRepository.existeNombre(nombre)) {
            throw new IllegalArgumentException("Ya existe un producto con el nombre: " + nombre);
        }

        Producto producto = new Producto(
                0,
                categoriaId,
                nombre,
                descripcion,
                marca,
                precioCompra,
                precioVenta,
                stockActual,
                stockMinimo,
                estadoActivo,
                LocalDate.now()
        );

        return productoRepository.guardar(producto);
    }
}