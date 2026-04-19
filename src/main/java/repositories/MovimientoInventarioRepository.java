package repositories;

import entities.MovimientoInventario;
import java.util.List;

public interface MovimientoInventarioRepository {

    // Registra un nuevo movimiento en la base de datos
    void guardar(MovimientoInventario movimiento);

    // Retorna todos los movimientos de un producto específico
    List<MovimientoInventario> listarPorProducto(int productoId);

    // Retorna todos los movimientos registrados en el sistema
    List<MovimientoInventario> listarTodos();
}
