package services.proveedores;

import repositories.ProveedorRepository;

public class DesactivarProveedorUseCase {

    private final ProveedorRepository proveedorRepository;

    public DesactivarProveedorUseCase(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public boolean ejecutar(int id) {
        if (proveedorRepository.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe un proveedor con el id: " + id);
        }
        return proveedorRepository.desactivar(id);
    }
}