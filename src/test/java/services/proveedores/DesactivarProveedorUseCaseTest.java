package services.proveedores;

import entities.Proveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ProveedorRepository;
import repositories.ProveedorRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio DesactivarProveedorUseCase (CU-008).
 */
@DisplayName("Pruebas del servicio DesactivarProveedorUseCase")
class DesactivarProveedorUseCaseTest {

    private DesactivarProveedorUseCase useCase;
    private ProveedorRepository proveedorRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        proveedorRepository = new H2ProveedorRepository(conn);
        useCase = new DesactivarProveedorUseCase(proveedorRepository);
    }

    /*
     * CP-010: Verifica el flujo principal. El proveedor 1 (Distribuidora
     * Diana SA) está activo en la BD inicial; tras desactivarlo debe quedar
     * con estado_activo = false en la BD.
     */
    @Test
    @DisplayName("CP-010: Desactivar proveedor existente debe marcarlo como inactivo")
    void desactivarProveedor_conIdExistente_debePersistirCambio() {
        boolean resultado = useCase.ejecutar(1);

        assertTrue(resultado, "El método debe retornar true al desactivar");

        Optional<Proveedor> proveedor = proveedorRepository.buscarPorId(1);
        assertTrue(proveedor.isPresent(), "El proveedor debe seguir existiendo");
        assertFalse(proveedor.get().isActivo(),
                "El proveedor debe quedar marcado como inactivo");
    }

    /*
     * CP-011: Verifica el flujo de excepción. Cuando el id no existe debe
     * lanzar IllegalArgumentException sin afectar la BD.
     */
    @Test
    @DisplayName("CP-011: Desactivar proveedor inexistente debe lanzar excepción")
    void desactivarProveedor_conIdInexistente_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(99999)
        );

        assertTrue(ex.getMessage().contains("99999"),
                "El mensaje debe incluir el id no encontrado");
    }
}