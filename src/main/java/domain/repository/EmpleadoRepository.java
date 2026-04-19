package domain.repository;

import domain.model.Empleado;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository {
    Optional<Empleado> buscarPorId(int id);
    List<Empleado> listarTodos();
    Empleado guardar(Empleado empleado);
    boolean actualizar(Empleado empleado);
    boolean desactivar(int id);
}