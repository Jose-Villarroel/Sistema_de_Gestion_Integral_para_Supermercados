package domain.repository;

import domain.model.Empleado;
import java.util.Optional;

public interface EmpleadoRepository {
    Optional<Empleado> buscarPorUsuarioYPassword(String usuario, String password);
}