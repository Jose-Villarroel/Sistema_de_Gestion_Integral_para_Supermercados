package services.productos;

import aggregates.Producto;
import repositories.ProductoRepository;

public class ModificarProductoUseCase {
    private final ProductoRepository productoRepository;

    public ModificarProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public boolean ejecutar(int id, String nombre, String descripcion,
                           double precioCompra, double precioVenta,
                           int stockMinimo, int stockMaximo,
                           int categoriaId, int proveedorId) {

        // Buscar el producto existente
        Producto producto = productoRepository.buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));

        // Actualizar datos (las validaciones están en el método)
        producto.actualizarDatos(nombre, descripcion, precioCompra, precioVenta,
                                stockMinimo, stockMaximo, categoriaId, proveedorId);

        // Guardar cambios
        return productoRepository.actualizar(producto);
    }
}
