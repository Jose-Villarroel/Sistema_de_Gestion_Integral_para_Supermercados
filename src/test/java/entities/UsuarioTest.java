package entities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de la entidad Usuario")
class UsuarioTest {

    private Rol rol;
    private Empleado empleado;

    @BeforeEach
    void setUp() {
        rol = new Rol(1, "ADMINISTRADOR", "Control total");
        empleado = new Empleado(
                1, "Andres", "Gonzalez",
                "andres@mail.com", "3001111111",
                LocalDate.now(), true
        );
    }

    // Pruebas de passwordCoincide()
    // Verifica que el sistema valide correctamente
    // las contraseñas ingresadas por el usuario


    /*
     * CP-001: Verifica que cuando el usuario ingresa la contraseña correcta,
     * el metodo passwordCoincide() retorna true. Este es el flujo principal
     * del CU-001 (Gestionar autenticación del sistema).
     */
    @Test
    @DisplayName("CP-001: Contraseña correcta debe coincidir")
    void passwordCorrecta_debeRetornarTrue() {
        Usuario usuario = new Usuario(1, "admin", "admin", rol, empleado, true);

        assertTrue(usuario.passwordCoincide("admin"),
                "La contraseña 'admin' debería coincidir con su propio hash");
    }

    /*
     * CP-002: Verifica que cuando el usuario ingresa una contraseña incorrecta,
     * el metodo passwordCoincide() retorna false. Cubre la excepcion del paso 6
     * del CU-001: "Si las credenciales son incorrectas, el sistema muestra
     * Usuario o contraseña incorrectos".
     */
    @Test
    @DisplayName("CP-002: Contraseña incorrecta no debe coincidir")
    void passwordIncorrecta_debeRetornarFalse() {
        Usuario usuario = new Usuario(1, "admin", "admin", rol, empleado, true);

        assertFalse(usuario.passwordCoincide("wrongpassword"),
                "Una contraseña incorrecta no debería coincidir");
    }

    /*
     * CP-003: Verifica que una contraseña vacia no coincida con una contraseña
     * real registrada. Garantiza que el sistema no permita acceso con campos
     * vacíos, complementando la validación del paso 4 del CU-001.
     */
    @Test
    @DisplayName("CP-003: Contraseña vacía no debe coincidir con contraseña real")
    void passwordVacia_noDebeCoincidirConPasswordReal() {
        Usuario usuario = new Usuario(1, "admin", "admin", rol, empleado, true);

        assertFalse(usuario.passwordCoincide(""),
                "Una contraseña vacía no debería coincidir con 'admin'");
    }

    // Pruebas de estaBloqueado()
    // Verifica el comportamiento del bloqueo de cuenta
    // tras superar los intentos fallidos permitidos.

    /*
     * CP-004: Verifica que un usuario recien creado sin historial de bloqueo
     * no este bloqueado. Este es el estado normal de cualquier usuario activo
     * en el sistema.
     */
    @Test
    @DisplayName("CP-004: Usuario sin bloqueo no debe estar bloqueado")
    void usuarioSinBloqueo_noDebeEstarBloqueado() {
        Usuario usuario = new Usuario(
                1, "admin", "admin123hash",
                rol, empleado,
                0, null, LocalDate.now(), true
        );

        assertFalse(usuario.estaBloqueado(),
                "Un usuario sin bloqueadoHasta no debe estar bloqueado");
    }

    /*
     * CP-005: Verifica que un usuario con bloqueo activo
     * sea reconocido como bloqueado. Cubre la excepción del CU-001:
     * "Si se superan 3 intentos fallidos, el sistema bloquea la cuenta por
     * 15 minutos".
     */
    @Test
    @DisplayName("CP-005: Usuario con bloqueo futuro debe estar bloqueado")
    void usuarioConBloqueoFuturo_debeEstarBloqueado() {
        LocalTime bloqueoFuturo = LocalTime.now().plusMinutes(15);
        Usuario usuario = new Usuario(
                1, "admin", "admin123hash",
                rol, empleado,
                3, bloqueoFuturo, LocalDate.now(), true
        );

        assertTrue(usuario.estaBloqueado(),
                "Un usuario con bloqueo en el futuro debe estar bloqueado");
    }

    /*
     * CP-006: Verifica que un usuario cuyo tiempo de bloqueo ya expiro
     * no sea reconocido como bloqueado. Garantiza que el desbloqueo automatico
     * funcione correctamente una vez transcurridos los 15 minutos de penalizacion.
     */
    @Test
    @DisplayName("CP-006: Usuario con bloqueo pasado no debe estar bloqueado")
    void usuarioConBloqueoPasado_noDebeEstarBloqueado() {
        LocalTime bloqueoPasado = LocalTime.now().minusMinutes(1);
        Usuario usuario = new Usuario(
                1, "admin", "admin123hash",
                rol, empleado,
                3, bloqueoPasado, LocalDate.now(), true
        );

        assertFalse(usuario.estaBloqueado(),
                "Un usuario con bloqueo en el pasado no debe estar bloqueado");
    }

    // Pruebas de incrementarIntentosFallidos()
    // Verifica el manejo del contador de intentos
    // fallidos de autenticación.

    /*
     * CP-007: Verifica que el contador de intentos fallidos se incremente
     * correctamente cada vez que el usuario falla al iniciar sesion. Este
     * contador es clave para activar el bloqueo automático de la cuenta
     * tras 3 intentos fallidos.
     */
    @Test
    @DisplayName("CP-007: Incrementar intentos fallidos debe aumentar el contador")
    void incrementarIntentosFallidos_debeAumentarContador() {
        Usuario usuario = new Usuario(
                1, "admin", "admin123hash",
                rol, empleado,
                0, null, LocalDate.now(), true
        );

        usuario.incrementarIntentosFallidos();
        usuario.incrementarIntentosFallidos();

        assertEquals(2, usuario.getIntentosFallidos(),
                "Después de 2 incrementos, los intentos fallidos deben ser 2");
    }

    /*
     * CP-008: Verifica que el contador de intentos fallidos se reinicie a 0
     * cuando el usuario inicia sesion correctamente. Esto garantiza que un
     * usuario que falla varias veces pero luego acierta no quede con intentos
     * acumulados que puedan bloquearlo en el futuro.
     */
    @Test
    @DisplayName("CP-008: Reiniciar intentos fallidos debe dejar el contador en 0")
    void reiniciarIntentosFallidos_debeDejarContadorEnCero() {
        Usuario usuario = new Usuario(
                1, "admin", "admin123hash",
                rol, empleado,
                3, null, LocalDate.now(), true
        );

        usuario.reiniciarIntentosFallidos();

        assertEquals(0, usuario.getIntentosFallidos(),
                "Después de reiniciar, los intentos fallidos deben ser 0");
    }
}