package services.clientes;

import entities.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repositories.ClienteRepository;
import repositories.CuentaFidelizacionRepository;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ClienteRepository;
import repositories.H2CuentaFidelizacionRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Pruebas de integracion del servicio RegistrarClienteUseCase.
 *
 * Cubre el CU-006: Administrar clientes (flujo de registro)
 * y la creacion automatica de la cuenta de fidelizacion asociada.
 * Cada prueba reinicia la BD a un estado conocido mediante DatabaseInitializer
 * y usa dependencias reales (H2ClienteRepository, H2CuentaFidelizacionRepository
 * y conexion H2 embebida).
 */
@DisplayName("Pruebas de integracion - RegistrarClienteUseCase (CU-006)")
class RegistrarClienteUseCaseTest {

    private RegistrarClienteUseCase registrarClienteUseCase;
    private ClienteRepository clienteRepository;
    private CuentaFidelizacionRepository cuentaRepository;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Resetear la BD a un estado conocido
        DatabaseConnection databaseConnection = new DatabaseConnection();
        DatabaseInitializer databaseInitializer = new DatabaseInitializer(databaseConnection);
        databaseInitializer.init();

        // 2. Instanciar dependencias reales
        clienteRepository = new H2ClienteRepository(databaseConnection);
        cuentaRepository = new H2CuentaFidelizacionRepository(databaseConnection);

        // 3. Instanciar el UseCase a probar
        registrarClienteUseCase = new RegistrarClienteUseCase(clienteRepository, cuentaRepository);
    }

    /*
     * CP-001: Registro exitoso de un cliente nuevo.
     * Cubre el flujo principal del CU-006: se persiste el cliente,
     * se genera la cuenta de fidelizacion con 0 puntos y se retorna
     * el cliente guardado con su id auto-generado.
     */
    @Test
    @DisplayName("CP-001: Registrar cliente nuevo debe persistirlo y retornarlo con id")
    void clienteNuevo_debeRegistrarseYRetornarseConId() {
        String nombre = "Laura";
        String apellido = "Gomez";
        String correo = "laura.gomez@correo.com";
        String telefono = "3001234567";
        String direccion = "Carrera 15 # 30-40";

        Cliente resultado = registrarClienteUseCase.ejecutar(
                nombre, apellido, correo, telefono, direccion);

        // Verificar el objeto retornado
        assertNotNull(resultado, "El cliente retornado no debe ser nulo");
        assertTrue(resultado.getId() > 0,
                "El cliente debe tener un id auto-generado por la BD");
        assertEquals(nombre, resultado.getNombre());
        assertEquals(apellido, resultado.getApellido());
        assertEquals(correo, resultado.getCorreo());
        assertEquals(telefono, resultado.getTelefono());
        assertEquals(direccion, resultado.getDireccion());
        assertTrue(resultado.isActivo(),
                "El cliente recien creado debe quedar activo");
        assertNotNull(resultado.getFechaRegistro(),
                "El cliente debe tener fecha de registro asignada");

        // Verificar que efectivamente quedo persistido en BD
        Optional<Cliente> enBD = clienteRepository.buscarPorId(resultado.getId());
        assertTrue(enBD.isPresent(),
                "El cliente debe poder consultarse en BD despues de registrarse");
        assertEquals(nombre, enBD.get().getNombre());
    }

    /*
     * CP-002: Al registrar un cliente, debe crearse automaticamente
     * su cuenta de fidelizacion con 0 puntos iniciales.
     */
    @Test
    @DisplayName("CP-002: Al registrar cliente debe crearse su cuenta de fidelizacion con 0 puntos")
    void clienteNuevo_debeCrearCuentaFidelizacionAsociada() {
        Cliente resultado = registrarClienteUseCase.ejecutar(
                "Pedro", "Ramirez", "pedro@correo.com",
                "3009998877", "Calle 50 # 10-20");

        // Verificar que existe una cuenta de fidelizacion vinculada al cliente
        assertTrue(cuentaRepository.buscarPorCliente(resultado.getId()).isPresent(),
                "Debe crearse automaticamente la cuenta de fidelizacion del cliente");
        assertEquals(0,
                cuentaRepository.buscarPorCliente(resultado.getId()).get().getPuntosActuales(),
                "La cuenta de fidelizacion debe iniciar con 0 puntos");
        assertTrue(cuentaRepository.buscarPorCliente(resultado.getId()).get().isActiva(),
                "La cuenta de fidelizacion debe quedar activa");
    }

    /*
     * CP-003: La cuenta de fidelizacion debe tener un numero de tarjeta
     * de 8 digitos generado aleatoriamente (entre 10.000.000 y 99.999.999).
     */
    @Test
    @DisplayName("CP-003: La cuenta de fidelizacion debe tener un numero de tarjeta de 8 digitos")
    void cuentaFidelizacion_debeTenerNumeroTarjetaDeOchoDigitos() {
        Cliente resultado = registrarClienteUseCase.ejecutar(
                "Carla", "Diaz", "carla@correo.com",
                "3007776655", "Av. 68 # 1-2");

        int numeroTarjeta = cuentaRepository.buscarPorCliente(resultado.getId())
                .get()
                .getNumeroTarjeta();

        assertTrue(numeroTarjeta >= 10_000_000 && numeroTarjeta < 100_000_000,
                "El numero de tarjeta debe estar entre 10.000.000 y 99.999.999 (8 digitos)");
    }

    /*
     * CP-004: Intento de registrar cliente con nombre vacio.
     * Cubre la validacion del constructor de Cliente.
     */
    @Test
    @DisplayName("CP-004: Registrar con nombre vacio debe lanzar excepcion de la entidad")
    void nombreVacio_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> registrarClienteUseCase.ejecutar(
                        "", "Apellido", "correo@test.com", "3001112233", "Calle 1")
        );

        assertEquals("El nombre es obligatorio", ex.getMessage());
    }

    /*
     * CP-005: Intento de registrar cliente con apellido nulo.
     * Cubre la validacion del constructor de Cliente.
     */
    @Test
    @DisplayName("CP-005: Registrar con apellido nulo debe lanzar excepcion de la entidad")
    void apellidoNulo_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> registrarClienteUseCase.ejecutar(
                        "Nombre", null, "correo@test.com", "3001112233", "Calle 1")
        );

        assertEquals("El apellido es obligatorio", ex.getMessage());
    }

    /*
     * CP-006: Intento de registrar cliente con correo en formato invalido.
     * Cubre la validacion del formato de correo en la entidad.
     */
    @Test
    @DisplayName("CP-006: Registrar con correo invalido debe lanzar excepcion")
    void correoInvalido_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> registrarClienteUseCase.ejecutar(
                        "Nombre", "Apellido", "correo-sin-arroba",
                        "3001112233", "Calle 1")
        );

        assertEquals("El correo no tiene un formato válido", ex.getMessage());
    }
}