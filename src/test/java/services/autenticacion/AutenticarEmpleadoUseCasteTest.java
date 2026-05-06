package services.autenticacion;

import entities.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2UsuarioRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio AutenticarEmpleadoUseCase.
 * 
 * Cubre el CU-001: Gestionar autenticación del sistema.
 * Cada prueba reinicia la BD a un estado conocido mediante DatabaseInitializer
 * y usa dependencias reales (H2UsuarioRepository + conexión H2 embebida).
 * 
 * Cobertura: 100% de las ramas del método ejecutar().
 */
@DisplayName("Pruebas de integración - AutenticarEmpleadoUseCase (CU-001)")
class AutenticarEmpleadoUseCaseTest {

    private AutenticarEmpleadoUseCase useCase;
    private DatabaseConnection dbConnection;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Resetear la BD a un estado conocido
        dbConnection = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(dbConnection);
        initializer.init();

        // 2. Instanciar dependencias reales
        H2UsuarioRepository usuarioRepository = new H2UsuarioRepository(dbConnection);

        // 3. Instanciar el UseCase a probar
        useCase = new AutenticarEmpleadoUseCase(usuarioRepository);
    }

    /*
     * CP-001: Verifica que el sistema lanza excepción cuando el username
     * es nulo. Cubre la primera validación de entrada del CU-001.
     */
    @Test
    @DisplayName("CP-001: Username nulo debe lanzar IllegalArgumentException")
    void usernameNulo_debeLanzarExcepcion() {
        // Actuar y Verificar
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(null, "admin")
        );
        assertEquals("El usuario es obligatorio", ex.getMessage());
    }

    /*
     * CP-002: Verifica que el sistema lanza excepción cuando el username
     * está vacío. Cubre la validación de campos vacíos del paso 4 del CU-001.
     */
    @Test
    @DisplayName("CP-002: Username vacío debe lanzar IllegalArgumentException")
    void usernameVacio_debeLanzarExcepcion() {
        // Actuar y Verificar
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar("   ", "admin")
        );
        assertEquals("El usuario es obligatorio", ex.getMessage());
    }

    /*
     * CP-003: Verifica que el sistema lanza excepción cuando la contraseña
     * es nula. Cubre la segunda validación de entrada del CU-001.
     */
    @Test
    @DisplayName("CP-003: Password nulo debe lanzar IllegalArgumentException")
    void passwordNulo_debeLanzarExcepcion() {
        // Actuar y Verificar
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar("admin", null)
        );
        assertEquals("La contraseña es obligatoria", ex.getMessage());
    }

    /*
     * CP-004: Verifica que el sistema lanza excepción cuando la contraseña
     * está vacía. Cubre la validación de campos vacíos del paso 4 del CU-001.
     */
    @Test
    @DisplayName("CP-004: Password vacío debe lanzar IllegalArgumentException")
    void passwordVacio_debeLanzarExcepcion() {
        // Actuar y Verificar
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar("admin", "")
        );
        assertEquals("La contraseña es obligatoria", ex.getMessage());
    }

    /*
     * CP-005: Verifica que el sistema lanza excepción cuando la contraseña
     * tiene menos de 4 caracteres. Cubre la validación de longitud mínima.
     */
    @Test
    @DisplayName("CP-005: Password menor a 4 caracteres debe lanzar IllegalArgumentException")
    void passwordMenorA4Caracteres_debeLanzarExcepcion() {
        // Actuar y Verificar
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar("admin", "abc")
        );
        assertEquals("La contraseña debe tener mínimo 4 caracteres", ex.getMessage());
    }

    /*
     * CP-006: Verifica que el sistema lanza excepción cuando el usuario
     * no existe en la BD. Cubre la excepción del paso 6 del CU-001:
     * "Si las credenciales son incorrectas, el sistema muestra
     * Usuario o contraseña incorrectos".
     */
    @Test
    @DisplayName("CP-006: Usuario inexistente debe lanzar RuntimeException")
    void usuarioInexistente_debeLanzarExcepcion() {
        // Actuar y Verificar
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> useCase.ejecutar("usuarioQueNoExiste", "admin1234")
        );
        assertEquals("Credenciales incorrectas", ex.getMessage());
    }

    /*
     * CP-007: Verifica que el sistema lanza excepción cuando el usuario
     * existe pero está desactivado. Cubre la excepción del paso 8 del CU-001:
     * "Si la cuenta está inactiva, el sistema muestra Cuenta desactivada".
     * Se desactiva el usuario 'cajero' directamente en la BD antes de la prueba.
     */
    @Test
    @DisplayName("CP-007: Usuario desactivado debe lanzar RuntimeException")
    void usuarioDesactivado_debeLanzarExcepcion() throws Exception {
        // Preparar: desactivar el usuario 'cajero' en la BD
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Usuario SET estado_usuario = FALSE WHERE username = 'cajero'")) {
            stmt.executeUpdate();
        }

        // Actuar y Verificar
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> useCase.ejecutar("cajero", "1234")
        );
        assertEquals("Usuario desactivado", ex.getMessage());
    }

    /*
     * CP-008: Verifica que el sistema lanza excepción cuando el usuario
     * está bloqueado temporalmente. Cubre la excepción del CU-001:
     * "Si se superan 3 intentos fallidos, el sistema bloquea la cuenta
     * por 15 minutos".
     * Se bloquea el usuario 'inventario' directamente en la BD antes de la prueba.
     */
    @Test
    @DisplayName("CP-008: Usuario bloqueado debe lanzar RuntimeException")
    void usuarioBloqueado_debeLanzarExcepcion() throws Exception {
        // Preparar: bloquear el usuario 'inventario' por 15 minutos
        LocalTime bloqueoFuturo = LocalTime.now().plusMinutes(15);
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE Usuario SET bloqueado_hasta = ? WHERE username = 'inventario'")) {
            stmt.setTime(1, java.sql.Time.valueOf(bloqueoFuturo));
            stmt.executeUpdate();
        }

        // Actuar y Verificar
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> useCase.ejecutar("inventario", "1234")
        );
        assertEquals("Usuario bloqueado temporalmente", ex.getMessage());
    }

    /*
     * CP-009: Verifica que el sistema lanza excepción cuando la contraseña
     * ingresada no coincide con el hash almacenado. Cubre la excepción del
     * paso 6 del CU-001 para el caso de contraseña incorrecta.
     */
    @Test
    @DisplayName("CP-009: Contraseña incorrecta debe lanzar RuntimeException")
    void passwordIncorrecta_debeLanzarExcepcion() {
        // Actuar y Verificar
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> useCase.ejecutar("admin", "passwordMalEscrita")
        );
        assertEquals("Credenciales incorrectas", ex.getMessage());
    }

    /*
     * CP-010: Verifica el flujo exitoso de autenticación. El usuario 'admin'
     * con contraseña 'admin' debe autenticarse correctamente y retornar
     * el objeto Usuario con sus datos completos.
     * Cubre el flujo principal del CU-001 pasos 1-10.
     */
    @Test
    @DisplayName("CP-010: Credenciales correctas deben retornar el Usuario autenticado")
    void credencialesCorrectas_debeRetornarUsuario() {
        // Actuar
        Usuario resultado = useCase.ejecutar("admin", "admin");

        // Verificar
        assertNotNull(resultado, "El usuario retornado no debe ser nulo");
        assertEquals("admin", resultado.getUsername(),
                "El username debe coincidir con el ingresado");
        assertTrue(resultado.isEstadoUsuario(),
                "El usuario retornado debe estar activo");
        assertNotNull(resultado.getRol(),
                "El usuario debe tener un rol asignado");
        assertEquals("ADMINISTRADOR", resultado.getRol().getNombreRol(),
                "El rol del usuario admin debe ser ADMINISTRADOR");
        assertNotNull(resultado.getEmpleado(),
                "El usuario debe tener un empleado asociado");
    }
}