package application.autenticacion;

import domain.model.Empleado;
import domain.repository.EmpleadoRepository;

import java.util.HashMap;
import java.util.Map;

public class AutenticarEmpleadoUseCase {

    private static final int MAX_INTENTOS = 3;
    private static final long TIEMPO_BLOQUEO_MS = 15 * 60 * 1000; // 15 minutos

    private final EmpleadoRepository empleadoRepository;
    private final Map<String, Integer> intentosFallidos = new HashMap<>();
    private final Map<String, Long> tiempoBloqueo = new HashMap<>();

    public AutenticarEmpleadoUseCase(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    public Empleado ejecutar(String usuario, String password) {
        // Validar campos vacíos
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario no puede estar vacío");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }

        // Verificar si la cuenta está bloqueada
        if (estaBloqueado(usuario)) {
            long restante = (tiempoBloqueo.get(usuario) + TIEMPO_BLOQUEO_MS
                    - System.currentTimeMillis()) / 1000 / 60;
            throw new RuntimeException(
                    "Cuenta bloqueada por " + restante + " minuto(s) por exceso de intentos fallidos"
            );
        }

        // Buscar en la base de datos
        Empleado empleado = empleadoRepository
                .buscarPorUsuarioYPassword(usuario, password)
                .orElse(null);

        // Credenciales incorrectas
        if (empleado == null) {
            registrarIntentoFallido(usuario);
            int intentos = intentosFallidos.getOrDefault(usuario, 0);
            int restantes = MAX_INTENTOS - intentos;
            if (restantes > 0) {
                throw new RuntimeException(
                        "Credenciales incorrectas. Intentos restantes: " + restantes
                );
            } else {
                throw new RuntimeException(
                        "Cuenta bloqueada por 15 minutos por exceso de intentos fallidos"
                );
            }
        }

        // Verificar que la cuenta esté activa
        if (!empleado.isActivo()) {
            throw new RuntimeException("La cuenta está desactivada. Contacte al administrador");
        }

        // Login exitoso — limpiar intentos fallidos
        intentosFallidos.remove(usuario);
        tiempoBloqueo.remove(usuario);

        return empleado;
    }

    private boolean estaBloqueado(String usuario) {
        if (!tiempoBloqueo.containsKey(usuario)) return false;
        long tiempoDesdeBloqueo = System.currentTimeMillis() - tiempoBloqueo.get(usuario);
        if (tiempoDesdeBloqueo >= TIEMPO_BLOQUEO_MS) {
            // El bloqueo ya expiró
            intentosFallidos.remove(usuario);
            tiempoBloqueo.remove(usuario);
            return false;
        }
        return true;
    }

    private void registrarIntentoFallido(String usuario) {
        int intentos = intentosFallidos.getOrDefault(usuario, 0) + 1;
        intentosFallidos.put(usuario, intentos);
        if (intentos >= MAX_INTENTOS) {
            tiempoBloqueo.put(usuario, System.currentTimeMillis());
        }
    }
}
