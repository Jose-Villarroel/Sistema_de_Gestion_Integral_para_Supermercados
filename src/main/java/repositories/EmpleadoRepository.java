package repositories;

import entities.Empleado;

import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository {

    Optional<Empleado> buscarPorId(int id);

    List<Empleado> buscarPorNombre(String nombre);

    List<Empleado> listarTodos();

    List<Empleado> listarActivos();

    Empleado guardar(Empleado empleado);

    boolean actualizar(Empleado empleado);

    boolean desactivar(int id);
}