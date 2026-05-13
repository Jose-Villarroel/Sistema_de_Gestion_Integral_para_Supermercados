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
 * Pruebas de integración del servicio DesactivarEmpleadoUseCase (CU-002).
 */
@DisplayName("Pruebas del servicio DesactivarEmpleadoUseCase")
class DesactivarEmpleadoUseCaseTest {

    private DesactivarEmpleadoUseCase useCase;
    private EmpleadoRepository empleadoRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        empleadoRepository = new H2EmpleadoRepository(conn);
        useCase = new DesactivarEmpleadoUseCase(empleadoRepository);
    }

    /*
     * CP-010: Verifica el flujo principal. El empleado 1 (Andres) está
     * activo en la BD inicial; tras desactivarlo debe quedar con
     * estado_activo = false en la BD.
     */
    @Test
    @DisplayName("CP-010: Desactivar empleado existente debe marcarlo como inactivo")
    void desactivarEmpleado_conIdExistente_debePersistirCambio() {
        boolean resultado = useCase.ejecutar(1);

        assertTrue(resultado, "El método debe retornar true al desactivar");

        Optional<Empleado> empleado = empleadoRepository.buscarPorId(1);
        assertTrue(empleado.isPresent(), "El empleado debe seguir existiendo");
        assertFalse(empleado.get().isActivo(),
                "El empleado debe quedar marcado como inactivo");
    }

    /*
     * CP-011: Verifica el flujo de excepción. Cuando el id no existe debe
     * lanzar IllegalArgumentException sin afectar la BD.
     */
    @Test
    @DisplayName("CP-011: Desactivar empleado inexistente debe lanzar excepción")
    void desactivarEmpleado_conIdInexistente_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(99999)
        );

        assertTrue(ex.getMessage().contains("99999"),
                "El mensaje debe incluir el id no encontrado");
    }
}