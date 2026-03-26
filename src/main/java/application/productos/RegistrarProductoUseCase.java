package application.productos;

import domain.model.Producto;
import domain.repository.ProductoRepository;

public class RegistrarProductoUseCase {
    private final ProductoRepository productoRepository;

    public RegistrarProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Producto ejecutar(String codigo, String nombre, String descripcion,
                             double precioCompra, double precioVenta,
                             int stockMinimo, int stockMaximo,
                             int categoriaId, int proveedorId) {

        // Validar que el código no exista
        if (productoRepository.existeCodigo(codigo)) {
            throw new IllegalArgumentException("Ya existe un producto con el código: " + codigo);
        }

        // Crear el producto (las validaciones están en el constructor)
        Producto producto = new Producto(
            codigo, nombre, descripcion, precioCompra, precioVenta,
            stockMinimo, stockMaximo, categoriaId, proveedorId
        );

        // Guardar en BD
        return productoRepository.guardar(producto);
    }
}