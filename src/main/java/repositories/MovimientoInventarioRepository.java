package repositories;

import java.sql.Connection;
import entities.MovimientoInventario;

import java.sql.SQLException;
import java.util.List;

public interface MovimientoInventarioRepository {

    void guardar(MovimientoInventario movimiento);

    List<MovimientoInventario> listarPorProducto(int productoId);

    List<MovimientoInventario> listarTodos();

    void guardar(Connection conn, MovimientoInventario movimiento) throws SQLException;
}