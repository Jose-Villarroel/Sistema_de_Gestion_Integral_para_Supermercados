package services.empleados;

import entities.Empleado;
import repositories.EmpleadoRepository;

public class ModificarEmpleadoUseCase {

    private final EmpleadoRepository empleadoRepository;

    public ModificarEmpleadoUseCase(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    public boolean ejecutar(int id, String nombre, String apellido,
                            String correo, String telefono) {

        Empleado existente = empleadoRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe un empleado con el id: " + id));

        Empleado modificado = new Empleado(
                existente.getId(),
                nombre,
                apellido,
                correo,
                telefono,
                existente.getFechaRegistro(),
                existente.isActivo()
        );

        return empleadoRepository.actualizar(modificado);
    }
}