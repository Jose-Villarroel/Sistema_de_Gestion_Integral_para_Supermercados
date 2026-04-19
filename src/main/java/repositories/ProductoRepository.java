package repositories;

import aggregates.Producto;
import java.util.List;
import java.util.Optional;

public interface ProductoRepository {

    Producto guardar(Producto producto);

    Optional<Producto> buscarPorId(int id);
    List<Producto> listarTodos();
    List<Producto> listarActivos();
    List<Producto> listarPorCategoria(int categoriaId);
    List<Producto> listarConStockBajo();
    List<Producto> buscarPorNombre(String nombre);

    boolean actualizar(Producto producto);
    boolean desactivar(int id);

    boolean existeNombre(String nombre);
}