package services.proveedores;

import entities.Proveedor;
import repositories.ProveedorRepository;

public class ModificarProveedorUseCase {

    private final ProveedorRepository proveedorRepository;

    public ModificarProveedorUseCase(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public boolean ejecutar(int id, String nombre, String correo,
                            String telefono, String direccion) {

        Proveedor existente = proveedorRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un proveedor con el id: " + id));

        Proveedor modificado = new Proveedor(
                existente.getId(),
                nombre,
                correo,
                telefono,
                direccion,
                existente.getFechaRegistro(),
                existente.isActivo()
        );

        return proveedorRepository.actualizar(modificado);
    }
}