package repositories;

import java.sql.Connection;
import entities.Producto;

import java.sql.SQLException;
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

    int obtenerStockActual(Connection conn, int productoId) throws SQLException;

    void aumentarStock(Connection conn, int productoId, int cantidad) throws SQLException;
}