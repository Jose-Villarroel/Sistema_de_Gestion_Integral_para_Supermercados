package domain.repository;

import domain.model.Empleado;
import java.util.List;
import java.util.Optional;

public interface EmpleadoRepository {
    // CU-001
    Optional<Empleado> buscarPorUsuarioYPassword(String usuario, String password);

    // CU-002
    List<Empleado> listarTodos();
    Optional<Empleado> buscarPorId(int id);
    void guardar(Empleado empleado);
    void actualizar(Empleado empleado);
    void desactivar(int id);
    boolean existeUsuario(String usuario);
    int contarAdministradores();
    String generarCodigoUnico();
}