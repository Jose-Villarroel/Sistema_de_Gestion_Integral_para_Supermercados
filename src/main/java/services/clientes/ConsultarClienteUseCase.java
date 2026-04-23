package services.clientes;

import entities.Cliente;
import repositories.ClienteRepository;

import java.util.List;
import java.util.Optional;

public class ConsultarClienteUseCase {

    private final ClienteRepository clienteRepository;

    public ConsultarClienteUseCase(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Optional<Cliente> porId(int id) {
        return clienteRepository.buscarPorId(id);
    }

    public List<Cliente> porNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de búsqueda no puede estar vacío");
        }
        return clienteRepository.buscarPorNombre(nombre);
    }

    public List<Cliente> listarTodos() {
        return clienteRepository.listarTodos();
    }

    public List<Cliente> listarActivos() {
        return clienteRepository.listarActivos();
    }
}