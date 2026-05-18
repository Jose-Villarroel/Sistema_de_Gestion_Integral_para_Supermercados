package services.autenticacion;

import entities.Usuario;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Pruebas unitarias de la clase SesionUsuario.
 */
@DisplayName("Pruebas unitarias - SesionUsuario")
class SesionUsuarioTest {

    /*
     * Crea un Usuario de prueba minimo (sin Rol ni Empleado reales)
     * usando el constructor publico de la entidad.
     * Como SesionUsuario solo almacena la referencia, no se necesitan
     * dependencias reales de Rol/Empleado.
     */
    private Usuario crearUsuario(int id, String username) {
        return new Usuario(id, username, "1234", null, null, true);
    }

    /*
     * Garantiza que cada prueba arranque sin estado residual
     * de pruebas anteriores (la clase mantiene estado estatico).
     */
    @BeforeEach
    void limpiarSesionAntes() {
        SesionUsuario.cerrar();
    }

    /*
     * Garantiza que el estado no se filtre a otras pruebas
     * que se ejecuten despues en la suite.
     */
    @AfterEach
    void limpiarSesionDespues() {
        SesionUsuario.cerrar();
    }

    /*
     * CP-001: Cuando no se ha iniciado sesion, getUsuarioActual()
     * debe retornar null. Cubre el estado inicial de la clase.
     */
    @Test
    @DisplayName("CP-001: Sin sesion iniciada, getUsuarioActual debe retornar null")
    void sinSesionIniciada_getUsuarioActualDebeRetornarNull() {
        assertNull(SesionUsuario.getUsuarioActual(),
                "Al no haber sesion iniciada el usuario actual debe ser null");
    }

    /*
     * CP-002: Al iniciar sesion con un Usuario, getUsuarioActual()
     * debe retornar exactamente ese mismo objeto.
     */
    @Test
    @DisplayName("CP-002: iniciar() debe guardar el usuario y getUsuarioActual debe retornarlo")
    void iniciar_debeGuardarUsuarioYDebeSerRetornadoPorGetUsuarioActual() {
        Usuario usuario = crearUsuario(1, "admin");

        SesionUsuario.iniciar(usuario);

        Usuario actual = SesionUsuario.getUsuarioActual();
        assertNotNull(actual, "El usuario actual no debe ser nulo despues de iniciar()");
        assertSame(usuario, actual,
                "getUsuarioActual debe retornar exactamente el objeto pasado a iniciar()");
        assertEquals("admin", actual.getUsername(),
                "El username del usuario en sesion debe coincidir con el original");
    }

    /*
     * CP-003: Al iniciar sesion con un nuevo Usuario despues de haber iniciado
     * con otro, el usuario actual debe reemplazarse por el nuevo.
     * Cubre la rama de sobrescritura de la variable estatica.
     */
    @Test
    @DisplayName("CP-003: iniciar() dos veces debe reemplazar el usuario actual")
    void iniciarDosVeces_debeReemplazarUsuarioActual() {
        Usuario primero = crearUsuario(1, "admin");
        Usuario segundo = crearUsuario(2, "cajero");

        SesionUsuario.iniciar(primero);
        SesionUsuario.iniciar(segundo);

        Usuario actual = SesionUsuario.getUsuarioActual();
        assertSame(segundo, actual,
                "Tras iniciar dos veces, el usuario actual debe ser el segundo");
        assertEquals("cajero", actual.getUsername(),
                "El username del usuario actual debe ser el del segundo iniciar()");
    }

    /*
     * CP-004: Al pasar null a iniciar(), el usuario actual debe quedar en null.
     * El metodo no valida, asi que es un escenario aceptado.
     */
    @Test
    @DisplayName("CP-004: iniciar(null) debe dejar el usuario actual en null")
    void iniciarConNull_debeDejarUsuarioActualEnNull() {
        SesionUsuario.iniciar(null);

        assertNull(SesionUsuario.getUsuarioActual(),
                "Pasar null a iniciar() debe dejar el usuario actual en null");
    }

    /*
     * CP-005: Despues de iniciar sesion, cerrar() debe poner
     * el usuario actual en null.
     */
    @Test
    @DisplayName("CP-005: cerrar() debe poner el usuario actual en null")
    void cerrar_debeLimpiarUsuarioActual() {
        Usuario usuario = crearUsuario(1, "admin");
        SesionUsuario.iniciar(usuario);
        assertNotNull(SesionUsuario.getUsuarioActual(),
                "Precondicion: debe haber un usuario en sesion");

        SesionUsuario.cerrar();

        assertNull(SesionUsuario.getUsuarioActual(),
                "Tras cerrar(), el usuario actual debe ser null");
    }

    /*
     * CP-006: Llamar cerrar() cuando no hay sesion iniciada
     * no debe lanzar excepcion y debe dejar el usuario actual en null.
     */
    @Test
    @DisplayName("CP-006: cerrar() sin sesion iniciada no debe lanzar excepcion")
    void cerrarSinSesion_noDebeLanzarExcepcion() {
        assertDoesNotThrow(() -> SesionUsuario.cerrar(),
                "cerrar() no debe lanzar excepcion aunque no haya sesion iniciada");
        assertNull(SesionUsuario.getUsuarioActual(),
                "El usuario actual debe seguir siendo null");
    }
}