package services.clientes;

import entities.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repositories.ClienteRepository;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ClienteRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de integracion del servicio ConsultarClienteUseCase")
class ConsultarClienteUseCaseTest {

    private ConsultarClienteUseCase consultarClienteUseCase;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Resetear la base de datos a un estado conocido
        DatabaseConnection databaseConnection = new DatabaseConnection();
        DatabaseInitializer databaseInitializer = new DatabaseInitializer(databaseConnection);
        databaseInitializer.init();

        // 2. Instanciar dependencias reales
        ClienteRepository clienteRepository = new H2ClienteRepository(databaseConnection);

        // 3. Instanciar el UseCase a probar
        consultarClienteUseCase = new ConsultarClienteUseCase(clienteRepository);
    }

    /**
     * CP-001: Consulta de cliente existente por id.
     * Cubre el flujo normal del CU-006: Administrar clientes.
     */
    @Test
    @DisplayName("CP-001: Debe retornar un cliente existente por id")
    void porId_clienteExistente_retornaCliente() {

        Optional<Cliente> resultado = consultarClienteUseCase.porId(1);

        // Verificar
        assertTrue(resultado.isPresent());
        assertEquals(1, resultado.get().getId());
        assertEquals("Mariana", resultado.get().getNombre());
        assertEquals("Perez", resultado.get().getApellido());
    }

    /**
     * CP-002: Consulta de cliente inexistente por id.
     * Cubre el flujo alterno del CU-006: Administrar clientes.
     */
    @Test
    @DisplayName("CP-002: Debe retornar Optional vacío si el cliente no existe")
    void porId_clienteInexistente_retornaOptionalVacio() {
        // Actuar
        Optional<Cliente> resultado = consultarClienteUseCase.porId(999);

        // Verificar
        assertTrue(resultado.isEmpty());
    }

    /**
     * CP-003: Consulta de clientes por nombre válido.
     * Cubre el flujo normal del CU-006: Administrar clientes.
     */
    @Test
    @DisplayName("CP-003: Debe retornar clientes cuando el nombre es válido")
    void porNombre_nombreValido_retornaClientes() {
        // Actuar
        List<Cliente> resultado = consultarClienteUseCase.porNombre("Juan");

        // Verificar
        assertFalse(resultado.isEmpty());
        assertTrue(
                resultado.stream().anyMatch(cliente ->
                        cliente.getNombre().equalsIgnoreCase("Juan")
                )
        );
    }

    /**
     * CP-004: Consulta por nombre nulo.
     * Cubre la validación de excepción del CU-006: Administrar clientes.
     */
    @Test
    @DisplayName("CP-004: Debe lanzar excepción cuando el nombre es null")
    void porNombre_nombreNull_lanzaExcepcion() {
        // Actuar y verificar
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> consultarClienteUseCase.porNombre(null)
        );

        assertEquals(
                "El nombre de búsqueda no puede estar vacío",
                exception.getMessage()
        );
    }

    /**
     * CP-005: Consulta por nombre vacío.
     * Cubre la validación de excepción del CU-006: Administrar clientes.
     */
    @Test
    @DisplayName("CP-005: Debe lanzar excepción cuando el nombre está vacío")
    void porNombre_nombreVacio_lanzaExcepcion() {
        // Actuar y verificar
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> consultarClienteUseCase.porNombre("")
        );

        assertEquals(
                "El nombre de búsqueda no puede estar vacío",
                exception.getMessage()
        );
    }

    /**
     * CP-006: Consulta por nombre con solo espacios.
     * Cubre la validación isBlank del CU-006: Administrar clientes.
     */
    @Test
    @DisplayName("CP-006: Debe lanzar excepción cuando el nombre solo tiene espacios")
    void porNombre_nombreConEspacios_lanzaExcepcion() {
        // Actuar y verificar
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> consultarClienteUseCase.porNombre("   ")
        );

        assertEquals(
                "El nombre de búsqueda no puede estar vacío",
                exception.getMessage()
        );
    }

    /**
     * CP-007: Listado completo de clientes.
     * Cubre el flujo normal del CU-006: Administrar clientes.
     */
    @Test
    @DisplayName("CP-007: Debe listar todos los clientes registrados")
    void listarTodos_retornaTodosLosClientes() {
        // Actuar
        List<Cliente> resultado = consultarClienteUseCase.listarTodos();

        // Verificar
        int cantidadClientesEsperada = 10;

        assertNotNull(resultado);
        assertEquals(cantidadClientesEsperada, resultado.size());
    }

    /**
     * CP-008: Listado de clientes activos.
     * Cubre el flujo normal del CU-006: Administrar clientes.
     */
    @Test
    @DisplayName("CP-008: Debe listar solo los clientes activos")
    void listarActivos_retornaSoloClientesActivos() {
        // Actuar
        List<Cliente> resultado = consultarClienteUseCase.listarActivos();

        // Verificar
        assertNotNull(resultado);
        assertFalse(resultado.isEmpty());
        assertTrue(resultado.stream().allMatch(Cliente::isActivo));

        int cantidadClientesActivosEsperada = 10;
        assertEquals(cantidadClientesActivosEsperada, resultado.size());
    }
}