package services.proveedores;

import entities.Proveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ProveedorRepository;
import repositories.ProveedorRepository;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio RegistrarProveedorUseCase (CU-008).
 */
@DisplayName("Pruebas del servicio RegistrarProveedorUseCase")
class RegistrarProveedorUseCaseTest {

    private RegistrarProveedorUseCase useCase;
    private ProveedorRepository proveedorRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        proveedorRepository = new H2ProveedorRepository(conn);
        useCase = new RegistrarProveedorUseCase(proveedorRepository);
    }

    /*
     * CP-001: Verifica el flujo principal del CU-008. Cuando se reciben
     * datos válidos, el servicio crea un nuevo proveedor, le asigna id,
     * lo marca como activo y lo persiste en la base de datos.
     */
    @Test
    @DisplayName("CP-001: Registrar proveedor con datos válidos debe persistirlo")
    void registrarProveedor_conDatosValidos_debePersistirEnBD() {
        Proveedor resultado = useCase.ejecutar(
                "Nuevo Proveedor SA",
                "contacto@nuevo.com",
                "6019999999",
                "Calle Falsa 123"
        );

        assertNotNull(resultado, "El proveedor registrado no debe ser null");
        assertTrue(resultado.getId() > 0, "El proveedor debe tener un id asignado");
        assertEquals("Nuevo Proveedor SA", resultado.getNombre());
        assertTrue(resultado.isActivo(), "El proveedor debe quedar activo por defecto");

        assertTrue(proveedorRepository.buscarPorId(resultado.getId()).isPresent(),
                "El proveedor debe quedar persistido en la BD");
    }
}