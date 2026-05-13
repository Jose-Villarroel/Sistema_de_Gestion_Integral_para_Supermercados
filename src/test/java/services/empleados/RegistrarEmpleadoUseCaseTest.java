package services.empleados;

import entities.Empleado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.EmpleadoRepository;
import repositories.H2EmpleadoRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio RegistrarEmpleadoUseCase (CU-002).
 */
@DisplayName("Pruebas del servicio RegistrarEmpleadoUseCase")
class RegistrarEmpleadoUseCaseTest {

    private RegistrarEmpleadoUseCase useCase;
    private EmpleadoRepository empleadoRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        empleadoRepository = new H2EmpleadoRepository(conn);
        useCase = new RegistrarEmpleadoUseCase(empleadoRepository);
    }

    /*
     * CP-001: Verifica el flujo principal del CU-002. Cuando se reciben datos
     * válidos, el servicio crea un nuevo empleado, le asigna id, lo marca
     * como activo y lo persiste en la base de datos.
     */
    @Test
    @DisplayName("CP-001: Registrar empleado con datos válidos debe persistirlo")
    void registrarEmpleado_conDatosValidos_debePersistirEnBD() {
        Empleado resultado = useCase.ejecutar(
                "Diego",
                "Mendoza",
                "diego@mail.com",
                "3009999999"
        );

        assertNotNull(resultado, "El empleado registrado no debe ser null");
        assertTrue(resultado.getId() > 0, "El empleado debe tener un id asignado");
        assertEquals("Diego", resultado.getNombre());
        assertEquals("Mendoza", resultado.getApellido());
        assertTrue(resultado.isActivo(), "El empleado debe quedar activo por defecto");

        assertTrue(empleadoRepository.buscarPorId(resultado.getId()).isPresent(),
                "El empleado debe quedar persistido en la BD");
    }
}