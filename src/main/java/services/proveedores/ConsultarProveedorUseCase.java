package services.proveedores;

import entities.Proveedor;
import repositories.ProveedorRepository;

import java.util.List;
import java.util.Optional;

public class ConsultarProveedorUseCase {

    private final ProveedorRepository proveedorRepository;

    public ConsultarProveedorUseCase(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    public Optional<Proveedor> porId(int id) {
        return proveedorRepository.buscarPorId(id);
    }

    public List<Proveedor> porNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de búsqueda no puede estar vacío");
        }
        return proveedorRepository.buscarPorNombre(nombre);
    }

    public List<Proveedor> listarTodos() {
        return proveedorRepository.listarTodos();
    }

    public List<Proveedor> listarActivos() {
        return proveedorRepository.listarActivos();
    }
}