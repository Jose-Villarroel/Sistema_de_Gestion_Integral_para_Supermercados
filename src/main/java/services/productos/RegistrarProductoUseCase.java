package services.productos;

import entities.Producto;
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

        if (productoRepository.existeNombre(producto.getNombre())) {
            throw new IllegalArgumentException("Ya existe un producto con el nombre: " + producto.getNombre());
        }

        return productoRepository.guardar(producto);
    }
}