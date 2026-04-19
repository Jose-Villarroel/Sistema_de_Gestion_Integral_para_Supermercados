package services.productos;

import aggregates.Producto;
import repositories.ProductoRepository;

//se realizaron cambios para que producto quedara con los datos que estan en la bd

public class ModificarProductoUseCase {

    private final ProductoRepository productoRepository;

    public ModificarProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public boolean ejecutar(int id, String nombre, String descripcion,
                            String marca, double precioCompra, double precioVenta,
                            int stockMinimo, int categoriaId, boolean estadoActivo) {

        Producto producto = productoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado con ID: " + id));

        producto.actualizarDatos(nombre, descripcion, marca, precioCompra, precioVenta, stockMinimo, categoriaId, estadoActivo);

        return productoRepository.actualizar(producto);
    }
}
