package application.productos;

import domain.model.Producto;
import domain.repository.ProductoRepository;
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