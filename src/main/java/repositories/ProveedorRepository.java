package repositories;

import entities.Proveedor;

import java.util.List;
import java.util.Optional;

public interface ProveedorRepository {

    Optional<Proveedor> buscarPorId(int id);

    List<Proveedor> buscarPorNombre(String nombre);

    List<Proveedor> listarTodos();

    List<Proveedor> listarActivos();

    Proveedor guardar(Proveedor proveedor);

    boolean actualizar(Proveedor proveedor);

    boolean desactivar(int id);
}