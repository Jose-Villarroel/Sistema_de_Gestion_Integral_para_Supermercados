package services.clientes;

import aggregates.Cliente;
import repositories.ClienteRepository;

public class ModificarClienteUseCase {

    private final ClienteRepository clienteRepository;

    public ModificarClienteUseCase(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public boolean ejecutar(int id, String nombre, String apellido, String correo,
                            String telefono, String direccion) {

        Cliente existente = clienteRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un cliente con el id: " + id));

        Cliente modificado = new Cliente(
                existente.getId(),
                nombre,
                apellido,
                correo,
                telefono,
                direccion,
                existente.getFechaRegistro(),
                existente.isActivo()
        );

        return clienteRepository.actualizar(modificado);
    }
}