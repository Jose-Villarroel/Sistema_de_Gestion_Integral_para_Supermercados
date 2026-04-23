package services.productos;

import entities.Producto;
import repositories.ProductoRepository;
import java.util.List;

public class ListarProductosStockBajoUseCase {

    private final ProductoRepository productoRepository;

    public ListarProductosStockBajoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> ejecutar() {
        return productoRepository.listarConStockBajo();
    }
}