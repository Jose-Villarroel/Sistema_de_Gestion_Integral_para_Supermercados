package services.productos;

import aggregates.Producto;
import repositories.ProductoRepository;
import java.util.List;

public class ConsultarProductoUseCase {
    private final ProductoRepository productoRepository;

    public ConsultarProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Producto buscarPorId(int id) {
        return productoRepository.buscarPorId(id)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + id));
    }

    public Producto buscarPorCodigo(String codigo) {
        return productoRepository.buscarPorCodigo(codigo)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con código: " + codigo));
    }

    public List<Producto> listarTodos() {
        return productoRepository.listarTodos();
    }

    public List<Producto> listarActivos() {
        return productoRepository.listarActivos();
    }

    public List<Producto> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de búsqueda no puede estar vacío");
        }
        return productoRepository.buscarPorNombre(nombre);
    }

    public List<Producto> listarPorCategoria(int categoriaId) {
        return productoRepository.listarPorCategoria(categoriaId);
    }
}
