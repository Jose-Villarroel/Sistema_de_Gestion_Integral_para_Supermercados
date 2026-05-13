package services.empleados;

import entities.Empleado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.EmpleadoRepository;
import repositories.H2EmpleadoRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio ConsultarEmpleadoUseCase (CU-002).
 *
 * Datos iniciales relevantes (cargados por DatabaseInitializer):
 *   - Empleado 1: Andres Gonzales (activo)
 *   - Empleado 2: Laura Gomez (activo)
 *   - Empleado 3: Carlos Ruiz (activo)
 */
@DisplayName("Pruebas del servicio ConsultarEmpleadoUseCase")
class ConsultarEmpleadoUseCaseTest {

    private ConsultarEmpleadoUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        EmpleadoRepository empleadoRepository = new H2EmpleadoRepository(conn);
        useCase = new ConsultarEmpleadoUseCase(empleadoRepository);
    }

    // ==================== porId ====================

    /*
     * CP-002: Verifica el flujo principal. El empleado 1 (Andres) está
     * precargado en la BD por el script de inicialización.
     */
    @Test
    @DisplayName("CP-002: Buscar empleado por id existente debe retornarlo")
    void porId_conIdExistente_debeRetornarEmpleado() {
        Optional<Empleado> resultado = useCase.porId(1);

        assertTrue(resultado.isPresent(), "El empleado debe existir");
        assertEquals("Andres", resultado.get().getNombre());
    }

    /*
     * CP-003: Verifica que cuando el id no existe se retorna Optional.empty
     * sin lanzar excepción. El UseCase delega esta decisión al consumidor.
     */
    @Test
    @DisplayName("CP-003: Buscar empleado por id inexistente debe retornar Optional vacío")
    void porId_conIdInexistente_debeRetornarOptionalVacio() {
        Optional<Empleado> resultado = useCase.porId(99999);

        assertTrue(resultado.isEmpty(),
                "Un id inexistente debe retornar Optional.empty");
    }

    // ==================== porNombre ====================

    /*
     * CP-004: Verifica el flujo principal de búsqueda por nombre. La BD
     * tiene "Andres Gonzales", debe encontrarse al buscar "andres".
     */
    @Test
    @DisplayName("CP-004: Buscar empleado por nombre existente debe retornar resultados")
    void porNombre_conNombreExistente_debeRetornarResultados() {
        List<Empleado> resultado = useCase.porNombre("andres");

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty(),
                "Debe encontrar al menos un empleado con el nombre 'andres'");
    }

    /*
     * CP-005: Verifica la validación de nombre vacío. Debe lanzar
     * IllegalArgumentException sin tocar la base de datos.
     */
    @Test
    @DisplayName("CP-005: Buscar empleado por nombre vacío debe lanzar excepción")
    void porNombre_conNombreVacio_debeLanzarExcepcion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.porNombre("")
        );
    }

    // ==================== listarTodos ====================

    /*
     * CP-006: Verifica que listarTodos retorna todos los empleados cargados.
     * El script BD-fundamentos.sql carga 3 empleados.
     */
    @Test
    @DisplayName("CP-006: Listar todos los empleados debe retornar los 3 cargados")
    void listarTodos_debeRetornarTodos() {
        List<Empleado> resultado = useCase.listarTodos();

        assertNotNull(resultado);
        assertEquals(3, resultado.size(),
                "Deben retornarse los 3 empleados cargados en BD");
    }

    // ==================== listarActivos ====================

    /*
     * CP-007: Verifica que listarActivos retorna únicamente los empleados
     * activos. Los 3 empleados iniciales están activos en la BD.
     */
    @Test
    @DisplayName("CP-007: Listar empleados activos debe retornar solo los activos")
    void listarActivos_debeRetornarSoloActivos() {
        List<Empleado> resultado = useCase.listarActivos();

        assertNotNull(resultado);
        assertEquals(3, resultado.size(),
                "Los 3 empleados cargados están activos");
        resultado.forEach(e ->
                assertTrue(e.isActivo(),
                        "Todos los empleados retornados deben estar activos"));
    }
}