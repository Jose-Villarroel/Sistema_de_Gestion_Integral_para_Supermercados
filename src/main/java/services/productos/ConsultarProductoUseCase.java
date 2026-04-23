package services.productos;

import entities.Producto;
import repositories.ProductoRepository;

import java.util.List;

// Se realizaron cambios para que quede alineado a la BD.
// Se quitó buscarPorCodigo porque codigo no existe en la tabla Producto.

public class ConsultarProductoUseCase {

    private final ProductoRepository productoRepository;

    public ConsultarProductoUseCase(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public Producto buscarPorId(int id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El id del producto debe ser mayor que cero");
        }

        return productoRepository.buscarPorId(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Producto no encontrado con ID: " + id));
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
        if (categoriaId <= 0) {
            throw new IllegalArgumentException("La categoría debe ser válida");
        }

        return productoRepository.listarPorCategoria(categoriaId);
    }
}