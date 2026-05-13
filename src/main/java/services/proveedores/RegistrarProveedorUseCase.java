package services.proveedores;

import entities.Proveedor;
import repositories.ProveedorRepository;

import java.time.LocalDate;

public class RegistrarProveedorUseCase {

    private final ProveedorRepository proveedorRepository;

    public RegistrarProveedorUseCase(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    /**
     * Registra un proveedor nuevo en el sistema.
     */
    public Proveedor ejecutar(String nombre, String correo, String telefono, String direccion) {

        Proveedor nuevo = new Proveedor(
                0,
                nombre,
                correo,
                telefono,
                direccion,
                LocalDate.now(),
                true
        );

        return proveedorRepository.guardar(nuevo);
    }
}