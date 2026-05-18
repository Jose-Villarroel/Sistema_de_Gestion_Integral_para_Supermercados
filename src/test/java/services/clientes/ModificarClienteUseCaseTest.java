package services.clientes;

import entities.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repositories.ClienteRepository;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ClienteRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Pruebas de integracion del servicio ModificarClienteUseCase.
 *
 * Cubre el CU-006: Administrar clientes (flujo de modificacion).
 * Cada prueba reinicia la BD a un estado conocido mediante DatabaseInitializer
 * 
 */
@DisplayName("Pruebas de integracion - ModificarClienteUseCase (CU-006)")
class ModificarClienteUseCaseTest {

    private ModificarClienteUseCase modificarClienteUseCase;
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
        modificarClienteUseCase = new ModificarClienteUseCase(clienteRepository);
    }

    /*
     * CP-001: Modificacion exitosa de un cliente existente.
     * Cubre el flujo principal del CU-006: el repositorio retorna el cliente,
     * se construye uno nuevo con los datos modificados y se persiste.
     */
    @Test
    @DisplayName("CP-001: Modificar cliente existente debe actualizar sus datos y retornar true")
    void clienteExistente_debeModificarYRetornarTrue() {
        int idCliente = 1;
        String nuevoNombre = "Mariana Actualizada";
        String nuevoApellido = "Perez Actualizada";
        String nuevoCorreo = "mariana.nueva@correo.com";
        String nuevoTelefono = "3001234567";
        String nuevaDireccion = "Calle 100 # 20-30";

        boolean resultado = modificarClienteUseCase.ejecutar(
                idCliente, nuevoNombre, nuevoApellido, nuevoCorreo,
                nuevoTelefono, nuevaDireccion);

        assertTrue(resultado, "El metodo debe retornar true al actualizar exitosamente");

        Optional<Cliente> actualizado = clienteRepository.buscarPorId(idCliente);
        assertTrue(actualizado.isPresent(), "El cliente debe seguir existiendo");
        assertEquals(nuevoNombre, actualizado.get().getNombre(),
                "El nombre debe quedar actualizado en BD");
        assertEquals(nuevoApellido, actualizado.get().getApellido(),
                "El apellido debe quedar actualizado en BD");
        assertEquals(nuevoCorreo, actualizado.get().getCorreo(),
                "El correo debe quedar actualizado en BD");
        assertEquals(nuevoTelefono, actualizado.get().getTelefono(),
                "El telefono debe quedar actualizado en BD");
        assertEquals(nuevaDireccion, actualizado.get().getDireccion(),
                "La direccion debe quedar actualizada en BD");
    }

    /*
     * CP-002: Intento de modificar un cliente que no existe.
     * Cubre la rama orElseThrow del CU-006.
     */
    @Test
    @DisplayName("CP-002: Modificar cliente inexistente debe lanzar IllegalArgumentException")
    void clienteInexistente_debeLanzarExcepcion() {
        int idInexistente = 9999;

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> modificarClienteUseCase.ejecutar(
                        idInexistente, "Nombre", "Apellido", "correo@test.com",
                        "3001112233", "Calle 1")
        );

        assertEquals("No existe un cliente con el id: " + idInexistente,
                ex.getMessage(),
                "El mensaje debe identificar el id del cliente buscado");
    }

    /*
     * CP-003: Modificar con nombre vacio debe lanzar excepcion de la entidad.
     * Cubre indirectamente la validacion del constructor de Cliente.
     */
    @Test
    @DisplayName("CP-003: Modificar con nombre vacio debe lanzar excepcion de la entidad")
    void nombreVacio_debeLanzarExcepcion() {
        int idCliente = 1;

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> modificarClienteUseCase.ejecutar(
                        idCliente, "", "Apellido", "correo@test.com",
                        "3001112233", "Calle 1")
        );

        assertEquals("El nombre es obligatorio", ex.getMessage(),
                "Debe propagarse la validacion de la entidad Cliente");
    }

    /*
     * CP-004: Modificar con apellido vacio debe lanzar excepcion de la entidad.
     */
    @Test
    @DisplayName("CP-004: Modificar con apellido vacio debe lanzar excepcion de la entidad")
    void apellidoVacio_debeLanzarExcepcion() {
        int idCliente = 1;

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> modificarClienteUseCase.ejecutar(
                        idCliente, "Nombre", "", "correo@test.com",
                        "3001112233", "Calle 1")
        );

        assertEquals("El apellido es obligatorio", ex.getMessage(),
                "Debe propagarse la validacion de la entidad Cliente");
    }

    /*
     * CP-005: Modificar con correo en formato invalido debe lanzar excepcion.
     */
    @Test
    @DisplayName("CP-005: Modificar con correo invalido debe lanzar excepcion")
    void correoInvalido_debeLanzarExcepcion() {
        int idCliente = 1;

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> modificarClienteUseCase.ejecutar(
                        idCliente, "Nombre", "Apellido", "correo-sin-arroba",
                        "3001112233", "Calle 1")
        );

        assertEquals("El correo no tiene un formato válido", ex.getMessage(),
                "Debe propagarse la validacion del formato de correo");
    }
}