package services.clientes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repositories.ClienteRepository;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ClienteRepository;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Pruebas de integracion del servicio DesactivarClienteUseCase.
 *
 * Cubre el CU-006: Administrar clientes (flujo de desactivacion).
 * Cada prueba reinicia la BD a un estado conocido mediante DatabaseInitializer
 * y usa dependencias reales (H2ClienteRepository + conexion H2 embebida).
 *
 */
@DisplayName("Pruebas de integracion - DesactivarClienteUseCase (CU-006)")
class DesactivarClienteUseCaseTest {

    private DesactivarClienteUseCase desactivarClienteUseCase;
    private ClienteRepository clienteRepository;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Resetear la BD a un estado conocido
        DatabaseConnection databaseConnection = new DatabaseConnection();
        DatabaseInitializer databaseInitializer = new DatabaseInitializer(databaseConnection);
        databaseInitializer.init();

        // 2. Instanciar dependencias reales
        clienteRepository = new H2ClienteRepository(databaseConnection);

        // 3. Instanciar el UseCase a probar
        desactivarClienteUseCase = new DesactivarClienteUseCase(clienteRepository);
    }

    /*
     * CP-001: Desactivacion exitosa de un cliente existente.
     * Cubre el flujo normal del CU-006: el repositorio retorna el cliente,
     * pasa la validacion y se ejecuta desactivar().
     */
    @Test
    @DisplayName("CP-001: Desactivar cliente existente debe retornar true")
    void clienteExistente_debeDesactivarYRetornarTrue() {
        int idClienteExistente = 1;

        boolean resultado = desactivarClienteUseCase.ejecutar(idClienteExistente);

        assertTrue(resultado, "El metodo debe retornar true al desactivar exitosamente");
        assertTrue(clienteRepository.buscarPorId(idClienteExistente).isPresent(),
                "El cliente debe seguir existiendo en BD (solo cambia su estado)");
        assertFalse(clienteRepository.buscarPorId(idClienteExistente).get().isActivo(),
                "El cliente debe quedar marcado como inactivo en BD");
    }

    /*
     * CP-002: Intento de desactivar un cliente que no existe.
     * Cubre la rama de validacion del CU-006: lanza IllegalArgumentException
     * cuando el id no corresponde a ningun cliente en BD.
     */
    @Test
    @DisplayName("CP-002: Desactivar cliente inexistente debe lanzar IllegalArgumentException")
    void clienteInexistente_debeLanzarExcepcion() {
        int idClienteInexistente = 9999;

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> desactivarClienteUseCase.ejecutar(idClienteInexistente)
        );

        assertEquals("No existe un cliente con el id: " + idClienteInexistente,
                ex.getMessage(),
                "El mensaje de error debe identificar el id del cliente buscado");
    }
}