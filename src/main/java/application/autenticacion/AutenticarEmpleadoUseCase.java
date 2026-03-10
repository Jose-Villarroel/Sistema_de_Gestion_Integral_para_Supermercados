package application.autenticacion;

import domain.model.Empleado;
import domain.repository.EmpleadoRepository;
import java.util.Optional;

public class AutenticarEmpleadoUseCase {

    private final EmpleadoRepository empleadoRepository;

    public AutenticarEmpleadoUseCase(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    public Empleado ejecutar(String usuario, String password) {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario no puede estar vacío");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        return empleadoRepository
                .buscarPorUsuarioYPassword(usuario, password)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));
    }
}