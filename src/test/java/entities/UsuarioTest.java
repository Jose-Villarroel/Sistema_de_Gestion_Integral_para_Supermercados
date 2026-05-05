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

    @Test
    @DisplayName("CP-001: Contraseña correcta debe coincidir")
    void passwordCorrecta_debeRetornarTrue() {
        Usuario usuario = new Usuario(1, "admin", "admin", rol, empleado, true);

        assertTrue(usuario.passwordCoincide("admin"),
                "La contraseña 'admin' debería coincidir con su propio hash");
    }

    @Test
    @DisplayName("CP-002: Contraseña incorrecta no debe coincidir")
    void passwordIncorrecta_debeRetornarFalse() {
        Usuario usuario = new Usuario(1, "admin", "admin", rol, empleado, true);

        assertFalse(usuario.passwordCoincide("wrongpassword"),
                "Una contraseña incorrecta no debería coincidir");
    }

    @Test
    @DisplayName("CP-003: Contraseña vacía no debe coincidir con contraseña real")
    void passwordVacia_noDebeCoincidirConPasswordReal() {
        Usuario usuario = new Usuario(1, "admin", "admin", rol, empleado, true);

        assertFalse(usuario.passwordCoincide(""),
                "Una contraseña vacía no debería coincidir con 'admin'");
    }

    // Pruebas de estaBloqueado()

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