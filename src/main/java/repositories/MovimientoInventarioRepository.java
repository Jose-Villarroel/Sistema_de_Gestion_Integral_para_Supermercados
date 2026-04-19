package repositories;

import entities.MovimientoInventario;
import java.util.List;

public interface MovimientoInventarioRepository {

    void guardar(MovimientoInventario movimiento);

    List<MovimientoInventario> listarPorProducto(int productoId);

    List<MovimientoInventario> listarTodos();
}