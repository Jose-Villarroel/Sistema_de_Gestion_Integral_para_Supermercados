package services.empleados;

import entities.Empleado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.EmpleadoRepository;
import repositories.H2EmpleadoRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio ModificarEmpleadoUseCase (CU-002).
 */
@DisplayName("Pruebas del servicio ModificarEmpleadoUseCase")
class ModificarEmpleadoUseCaseTest {

    private ModificarEmpleadoUseCase useCase;
    private EmpleadoRepository empleadoRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        empleadoRepository = new H2EmpleadoRepository(conn);
        useCase = new ModificarEmpleadoUseCase(empleadoRepository);
    }

    /*
     * CP-008: Verifica el flujo principal. Modifica el empleado 1 (Andres)
     * y confirma que los cambios se persistieron consultando la BD.
     */
    @Test
    @DisplayName("CP-008: Modificar empleado existente debe persistir los cambios")
    void modificarEmpleado_conIdExistente_debePersistirCambios() {
        boolean resultado = useCase.ejecutar(
                1,
                "AndresActualizado",
                "GonzalesNuevo",
                "andres.nuevo@mail.com",
                "3001234567"
        );

        assertTrue(resultado, "El método debe retornar true al actualizar");

        Optional<Empleado> actualizado = empleadoRepository.buscarPorId(1);
        assertTrue(actualizado.isPresent());
        assertEquals("AndresActualizado", actualizado.get().getNombre(),
                "El nombre debe haberse actualizado en la BD");
        assertEquals("andres.nuevo@mail.com", actualizado.get().getCorreo(),
                "El correo debe haberse actualizado");
    }

    /*
     * CP-009: Verifica el flujo de excepción. Cuando el id no existe en la
     * BD, debe lanzar IllegalArgumentException con un mensaje que incluya
     * el id buscado.
     */
    @Test
    @DisplayName("CP-009: Modificar empleado inexistente debe lanzar excepción")
    void modificarEmpleado_conIdInexistente_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(
                        99999,
                        "No importa",
                        "No importa",
                        "noimporta@mail.com",
                        "3000000000"
                )
        );

        assertTrue(ex.getMessage().contains("99999"),
                "El mensaje debe incluir el id no encontrado");
    }
}