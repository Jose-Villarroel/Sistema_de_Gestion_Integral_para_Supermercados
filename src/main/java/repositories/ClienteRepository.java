package repositories;

import entities.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository {

    Optional<Cliente> buscarPorId(int id);

    Optional<Cliente> buscarPorIdOTarjeta(String identificador);

    List<Cliente> buscarPorNombre(String nombre);

    List<Cliente> listarTodos();

    List<Cliente> listarActivos();

    Cliente guardar(Cliente cliente);

    boolean actualizar(Cliente cliente);

    boolean desactivar(int id);
}
