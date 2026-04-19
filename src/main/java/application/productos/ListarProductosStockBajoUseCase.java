package application.productos;

import domain.model.Producto;
import domain.repository.ProductoRepository;
import java.util.List;

public class ListarProductosStockBajoUseCase {
    private final ProductoRepository productoRepository;

    public ListarProductosStockBajoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }
    //se añadio un mensaje si no hay productos con bajo stock
    public List<Producto> ejecutar() {
        List<Producto> productos = productoRepository.listarConStockBajo();
        if (productos.isEmpty()) {
            throw new IllegalArgumentException("No hay productos con bajo stock");
        }
        return productos;
    }
}