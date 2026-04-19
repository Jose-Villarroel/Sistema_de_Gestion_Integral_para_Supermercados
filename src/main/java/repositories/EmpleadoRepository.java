package repositories;

import aggregates.Empleado;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository {
    Optional<Empleado> buscarPorUsuario(String usuario);
    Optional<Empleado> buscarPorId(int id);
    List<Empleado> listarTodos();
    void guardar(Empleado empleado);
    void actualizar(Empleado empleado);
    void desactivar(int id);
}
