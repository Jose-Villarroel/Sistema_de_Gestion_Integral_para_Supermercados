package services.empleados;

import repositories.EmpleadoRepository;

public class DesactivarEmpleadoUseCase {

    private final EmpleadoRepository empleadoRepository;

    public DesactivarEmpleadoUseCase(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    public boolean ejecutar(int id) {
        if (empleadoRepository.buscarPorId(id).isEmpty()) {
            throw new IllegalArgumentException("No existe un empleado con el id: " + id);
        }
        return empleadoRepository.desactivar(id);
    }
}