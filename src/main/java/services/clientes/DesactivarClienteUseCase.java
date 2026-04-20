package services.clientes;

import repositories.ClienteRepository;

public class DesactivarClienteUseCase {

    private final ClienteRepository clienteRepository;

    public DesactivarClienteUseCase(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public boolean ejecutar(int id) {
        if (clienteRepository.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe un cliente con el id: " + id);
        }
        return clienteRepository.desactivar(id);
    }
}