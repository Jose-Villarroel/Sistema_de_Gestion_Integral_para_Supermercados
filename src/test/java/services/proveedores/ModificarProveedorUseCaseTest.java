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
 * Pruebas de integración del servicio ModificarProveedorUseCase (CU-008).
 */
@DisplayName("Pruebas del servicio ModificarProveedorUseCase")
class ModificarProveedorUseCaseTest {

    private ModificarProveedorUseCase useCase;
    private ProveedorRepository proveedorRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        proveedorRepository = new H2ProveedorRepository(conn);
        useCase = new ModificarProveedorUseCase(proveedorRepository);
    }

    /*
     * CP-008: Verifica el flujo principal. Modifica el proveedor 1
     * (Distribuidora Diana SA) y confirma que los cambios se persistieron
     * consultando la BD.
     */
    @Test
    @DisplayName("CP-008: Modificar proveedor existente debe persistir los cambios")
    void modificarProveedor_conIdExistente_debePersistirCambios() {
        boolean resultado = useCase.ejecutar(
                1,
                "Distribuidora Diana Premium",
                "premium@diana.com",
                "6010000000",
                "Nueva Sede Calle 80"
        );

        assertTrue(resultado, "El método debe retornar true al actualizar");

        Optional<Proveedor> actualizado = proveedorRepository.buscarPorId(1);
        assertTrue(actualizado.isPresent());
        assertEquals("Distribuidora Diana Premium", actualizado.get().getNombre(),
                "El nombre debe haberse actualizado en la BD");
        assertEquals("premium@diana.com", actualizado.get().getCorreo(),
                "El correo debe haberse actualizado");
    }

    /*
     * CP-009: Verifica el flujo de excepción. Cuando el id no existe en la
     * BD, debe lanzar IllegalArgumentException con un mensaje que incluya
     * el id buscado.
     */
    @Test
    @DisplayName("CP-009: Modificar proveedor inexistente debe lanzar excepción")
    void modificarProveedor_conIdInexistente_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(
                        99999,
                        "No importa",
                        "noimporta@mail.com",
                        "6000000000",
                        "Calle X"
                )
        );

        assertTrue(ex.getMessage().contains("99999"),
                "El mensaje debe incluir el id no encontrado");
    }
}