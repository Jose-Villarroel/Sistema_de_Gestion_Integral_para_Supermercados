package application.autenticacion;

import domain.model.Empleado;
import domain.repository.EmpleadoRepository;

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

        //Busca el empleado por usuario en la base de datos
        //Si no lo encuentra, lanza el error "Credenciales incorrectas"
        Empleado empleado = empleadoRepository
                .buscarPorUsuario(usuario)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!empleado.isActivo()) {
            throw new RuntimeException("Cuenta desactivada. Contacte al administrador");
        }

        if (!empleado.tieneCredenciales(usuario, password)) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        return empleado;
    }
}
