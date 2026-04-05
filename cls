[33mcommit baa4432bb2f738b7dd0e2652d8b1713671f47ac6[m
Author: Jose-Villarroel <josealejandrovm05@gmail.com>
Date:   Thu Mar 12 11:53:40 2026 -0500

    feat: implementar CU002 administrar empleados
    
    - Registrar empleado con codigo autogenerado
    - Encriptar contraseña con SHA-256
    - Validar usuario duplicado y formato de correo
    - Actualizar datos de empleado
    - Desactivar con proteccion al unico administrador
    - Busqueda por nombre, codigo o rol
    - Navegacion desde panel de administrador

[1mdiff --git a/src/main/java/application/empleado/AdministrarEmpleadoUseCase.java b/src/main/java/application/empleado/AdministrarEmpleadoUseCase.java[m
[1mnew file mode 100644[m
[1mindex 0000000..0c8bc65[m
[1m--- /dev/null[m
[1m+++ b/src/main/java/application/empleado/AdministrarEmpleadoUseCase.java[m
[36m@@ -0,0 +1,119 @@[m
[32m+[m[32mpackage application.empleado;[m
[32m+[m
[32m+[m[32mimport domain.model.Empleado;[m
[32m+[m[32mimport domain.repository.EmpleadoRepository;[m
[32m+[m
[32m+[m[32mimport java.nio.charset.StandardCharsets;[m
[32m+[m[32mimport java.security.MessageDigest;[m
[32m+[m[32mimport java.util.List;[m
[32m+[m
[32m+[m[32mpublic class AdministrarEmpleadoUseCase {[m
[32m+[m
[32m+[m[32m    private final EmpleadoRepository empleadoRepository;[m
[32m+[m
[32m+[m[32m    public AdministrarEmpleadoUseCase(EmpleadoRepository empleadoRepository) {[m
[32m+[m[32m        this.empleadoRepository = empleadoRepository;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    // ── Listar ──────────────────────────────────────────────[m
[32m+[m[32m    public List<Empleado> listarTodos() {[m
[32m+[m[32m        return empleadoRepository.listarTodos();[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    // ── Registrar ────────────────────────────────────────────[m
[32m+[m[32m    public String registrar(String nombre, String usuario, String passwordTemporal,[m
[32m+[m[32m                            String rol, String correo, String telefono) {[m
[32m+[m[32m        // Paso 6 — Validar campos obligatorios[m
[32m+[m[32m        if (nombre == null || nombre.isBlank())[m
[32m+[m[32m            throw new IllegalArgumentException("El nombre no puede estar vacío");[m
[32m+[m[32m        if (usuario == null || usuario.isBlank())[m
[32m+[m[32m            throw new IllegalArgumentException("El usuario no puede estar vacío");[m
[32m+[m[32m        if (passwordTemporal == null || passwordTemporal.isBlank())[m
[32m+[m[32m            throw new IllegalArgumentException("La contraseña no puede estar vacía");[m
[32m+[m[32m        if (rol == null || rol.isBlank())[m
[32m+[m[32m            throw new IllegalArgumentException("El rol no puede estar vacío");[m
[32m+[m
[32m+[m[32m        // Paso 7 — Verificar identificación duplicada[m
[32m+[m[32m        if (empleadoRepository.existeUsuario(usuario)) {[m
[32m+[m[32m            throw new IllegalArgumentException([m
[32m+[m[32m                    "La identificación ya está registrada en el sistema"[m
[32m+[m[32m            );[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        // Paso 11 — Validar formato de correo[m
[32m+[m[32m        if (correo != null && !correo.isBlank() && !correo.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {[m
[32m+[m[32m            throw new IllegalArgumentException("Correo electrónico inválido");[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        // Paso 9 — Generar código único automáticamente[m
[32m+[m[32m        String codigo = empleadoRepository.generarCodigoUnico();[m
[32m+[m
[32m+[m[32m        // Paso 13 — Encriptar contraseña[m
[32m+[m[32m        String passwordEncriptado = encriptar(passwordTemporal);[m
[32m+[m
[32m+[m[32m        // Paso 15 — Guardar[m
[32m+[m[32m        Empleado empleado = new Empleado([m
[32m+[m[32m                0, codigo, nombre, usuario, passwordEncriptado,[m
[32m+[m[32m                rol, correo, telefono, true[m
[32m+[m[32m        );[m
[32m+[m[32m        empleadoRepository.guardar(empleado);[m
[32m+[m
[32m+[m[32m        // Paso 18 — Retornar código para mostrar en mensaje[m
[32m+[m[32m        return codigo;[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    // ── Actualizar ───────────────────────────────────────────[m
[32m+[m[32m    public void actualizar(int id, String nombre, String rol,[m
[32m+[m[32m                           String correo, String telefono) {[m
[32m+[m[32m        Empleado existente = empleadoRepository.buscarPorId(id)[m
[32m+[m[32m                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));[m
[32m+[m
[32m+[m[32m        if (nombre == null || nombre.isBlank())[m
[32m+[m[32m            throw new IllegalArgumentException("El nombre no puede estar vacío");[m
[32m+[m[32m        if (rol == null || rol.isBlank())[m
[32m+[m[32m            throw new IllegalArgumentException("El rol no puede estar vacío");[m
[32m+[m
[32m+[m[32m        // Paso 11 — Validar formato de correo[m
[32m+[m[32m        if (correo != null && !correo.isBlank() && !correo.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {[m
[32m+[m[32m            throw new IllegalArgumentException("Correo electrónico inválido");[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        Empleado actualizado = new Empleado([m
[32m+[m[32m                existente.getId(), existente.getCodigo(), nombre,[m
[32m+[m[32m                existente.getUsuario(), existente.getPassword(),[m
[32m+[m[32m                rol, correo, telefono, existente.isActivo()[m
[32m+[m[32m        );[m
[32m+[m[32m        empleadoRepository.actualizar(actualizado);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    // ── Desactivar ───────────────────────────────────────────[m
[32m+[m[32m    public void desactivar(int id) {[m
[32m+[m[32m        empleadoRepository.buscarPorId(id)[m
[32m+[m[32m                .orElseThrow(() -> new RuntimeException("Empleado no encontrado"));[m
[32m+[m
[32m+[m[32m        // Excepción — No desactivar el único administrador[m
[32m+[m[32m        int totalAdmins = empleadoRepository.contarAdministradores();[m
[32m+[m[32m        Empleado empleado = empleadoRepository.buscarPorId(id).get();[m
[32m+[m
[32m+[m[32m        if (empleado.getRol().equals("ADMIN") && totalAdmins <= 1) {[m
[32m+[m[32m            throw new RuntimeException([m
[32m+[m[32m                    "No se puede desactivar el único administrador del sistema"[m
[32m+[m[32m            );[m
[32m+[m[32m        }[m
[32m+[m
[32m+[m[32m        empleadoRepository.desactivar(id);[m
[32m+[m[32m    }[m
[32m+[m
[32m+[m[32m    // ── Encriptación SHA-256 ─────────────────────────────────[m
[32m+[m[32m    private String encriptar(String password) {[m
[32m+[m[32m        try {[m
[32m+[m[32m            MessageDigest digest = MessageDigest.getInstance("SHA-256");[m
[32m+[m[32m            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));[m
[32m+[m[32m            StringBuilder hex = new StringBuilder();[m
[32m+[m[32m            for (byte b : hash) hex.append(String.format("%02x", b));[m
[32m+[m[32m            return hex.toString();[m
[32m+[m[32m        } catch (Exception e) {[m
[32m+[m[32m            throw new RuntimeException("Error al encriptar la contraseña");[m
[32m+[m[32m        }[m
[32m+[m[32m    }[m
[32m+[m[32m}[m
\ No newline at end of file[m
