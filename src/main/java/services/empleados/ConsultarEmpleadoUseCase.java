package services.empleados;

import entities.Empleado;
import repositories.EmpleadoRepository;

import java.util.List;
import java.util.Optional;

public class ConsultarEmpleadoUseCase {

    private final EmpleadoRepository empleadoRepository;

    public ConsultarEmpleadoUseCase(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    public Optional<Empleado> porId(int id) {
        return empleadoRepository.buscarPorId(id);
    }

    public List<Empleado> porNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre de búsqueda no puede estar vacío");
        }
        return empleadoRepository.buscarPorNombre(nombre);
    }

    public List<Empleado> listarTodos() {
        return empleadoRepository.listarTodos();
    }

    public List<Empleado> listarActivos() {
        return empleadoRepository.listarActivos();
    }
}