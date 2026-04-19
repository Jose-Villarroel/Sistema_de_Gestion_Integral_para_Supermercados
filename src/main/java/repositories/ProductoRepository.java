package repositories;

import aggregates.Producto;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository {
    // Crear
    Producto guardar(Producto producto);
    
    // Leer
    Optional<Producto> buscarPorId(int id);
    Optional<Producto> buscarPorCodigo(String codigo);
    List<Producto> listarTodos();
    List<Producto> listarActivos();
    List<Producto> listarPorCategoria(int categoriaId);
    List<Producto> listarConStockBajo();
    List<Producto> buscarPorNombre(String nombre);
    
    // Actualizar
    boolean actualizar(Producto producto);
    
    // Eliminar (lógico)
    boolean desactivar(int id);
    
    // Verificar existencia
    boolean existeCodigo(String codigo);
}
