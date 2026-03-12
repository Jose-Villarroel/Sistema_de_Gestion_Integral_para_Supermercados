package application.empleado;

import domain.model.Empleado;
import domain.repository.EmpleadoRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

public class AdministrarEmpleadoUseCase {

    private final EmpleadoRepository empleadoRepository;

    public AdministrarEmpleadoUseCase(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;
    }

    // ── Listar ──────────────────────────────────────────────
    public List<Empleado> listarTodos() {
        return empleadoRepository.listarTodos();
    }

    // ── Registrar ────────────────────────────────────────────
    public String registrar(String nombre, String usuario, String passwordTemporal,
                            String rol, String correo, String telefono) {
        // Paso 6 — Validar campos obligatorios
        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        if (usuario == null || usuario.isBlank())
            throw new IllegalArgumentException("El usuario no puede estar vacío");
        if (passwordTemporal == null || passwordTemporal.isBlank())
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        if (rol == null || rol.isBlank())
            throw new IllegalArgumentException("El rol no puede estar vacío");

        // Paso 7 — Verificar identificación duplicada
        if (empleadoRepository.existeUsuario(usuario)) {
            throw new IllegalArgumentException(
                    "La identificación ya está registrada en el sistema"
            );
        }

        // Paso 11 — Validar formato de correo
        if (correo != null && !correo.isBlank() && !correo.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            throw new IllegalArgumentException("Correo electrónico inválido");
        }

        // Paso 9 — Generar código único automáticamente
        String codigo = empleadoRepository.generarCodigoUnico();

        // Paso 13 — Encriptar contraseña
        String passwordEncriptado = encriptar(passwordTemporal);

        // Paso 15 — Guardar
        Empleado empleado = new Empleado(
                0, codigo, nombre, usuario, passwordEncriptado,
                rol, correo, telefono, true
        );
        empleadoRepository.guardar(empleado);

        // Paso 18 — Retornar código para mostrar en mensaje
        return codigo;
    }

    // ── Actualizar ───────────────────────────────────────────
    public void actualizar(int id, String nombre, String rol,
                           String correo, String telefono) {
        Empleado existente = empleadoRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        if (nombre == null || nombre.isBlank())
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        if (rol == null || rol.isBlank())
            throw new IllegalArgumentException("El rol no puede estar vacío");

        // Paso 11 — Validar formato de correo
        if (correo != null && !correo.isBlank() && !correo.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            throw new IllegalArgumentException("Correo electrónico inválido");
        }

        Empleado actualizado = new Empleado(
                existente.getId(), existente.getCodigo(), nombre,
                existente.getUsuario(), existente.getPassword(),
                rol, correo, telefono, existente.isActivo()
        );
        empleadoRepository.actualizar(actualizado);
    }

    // ── Desactivar ───────────────────────────────────────────
    public void desactivar(int id) {
        empleadoRepository.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));

        // Excepción — No desactivar el único administrador
        int totalAdmins = empleadoRepository.contarAdministradores();
        Empleado empleado = empleadoRepository.buscarPorId(id).get();

        if (empleado.getRol().equals("ADMIN") && totalAdmins <= 1) {
            throw new RuntimeException(
                    "No se puede desactivar el único administrador del sistema"
            );
        }

        empleadoRepository.desactivar(id);
    }

    // ── Encriptación SHA-256 ─────────────────────────────────
    private String encriptar(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error al encriptar la contraseña");
        }
    }
}