package services.empleados;

import entities.Empleado;
import repositories.EmpleadoRepository;

import java.time.LocalDate;

public class RegistrarEmpleadoUseCase {

    private final EmpleadoRepository empleadoRepository;

    public RegistrarEmpleadoUseCase(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    /**
     * Registra un empleado nuevo en el sistema.
     */
    public Empleado ejecutar(String nombre, String apellido, String correo, String telefono) {

        Empleado nuevo = new Empleado(
                0,
                nombre,
                apellido,
                correo,
                telefono,
                LocalDate.now(),
                true
        );

        return empleadoRepository.guardar(nuevo);
    }
}