package repositories;

import aggregates.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository {

    Optional<Cliente> buscarPorId(int id);

    List<Cliente> buscarPorNombre(String nombre);

    List<Cliente> listarTodos();

    List<Cliente> listarActivos();

    Cliente guardar(Cliente cliente);

    boolean actualizar(Cliente cliente);

    boolean desactivar(int id);
}