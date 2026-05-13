package services.proveedores;

import entities.Proveedor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ProveedorRepository;
import repositories.ProveedorRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio ConsultarProveedorUseCase (CU-008).
 *
 * Datos iniciales (cargados por DatabaseInitializer):
 *   - 4 proveedores activos: Distribuidora Diana, Alpina, Coca Cola FEMSA, Cristal Aguas
 *   - 1 proveedor inactivo: "Proveedor Inactivo SA"
 *   - Total: 5 proveedores; Activos: 4
 */
@DisplayName("Pruebas del servicio ConsultarProveedorUseCase")
class ConsultarProveedorUseCaseTest {

    private ConsultarProveedorUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        ProveedorRepository proveedorRepository = new H2ProveedorRepository(conn);
        useCase = new ConsultarProveedorUseCase(proveedorRepository);
    }

    // ==================== porId ====================

    /*
     * CP-002: Verifica el flujo principal. El proveedor 1 (Distribuidora
     * Diana SA) está precargado en la BD por el script de inicialización.
     */
    @Test
    @DisplayName("CP-002: Buscar proveedor por id existente debe retornarlo")
    void porId_conIdExistente_debeRetornarProveedor() {
        Optional<Proveedor> resultado = useCase.porId(1);

        assertTrue(resultado.isPresent(), "El proveedor debe existir");
        assertEquals("Distribuidora Diana SA", resultado.get().getNombre());
    }

    /*
     * CP-003: Verifica que cuando el id no existe se retorna Optional.empty
     * sin lanzar excepción.
     */
    @Test
    @DisplayName("CP-003: Buscar proveedor por id inexistente debe retornar Optional vacío")
    void porId_conIdInexistente_debeRetornarOptionalVacio() {
        Optional<Proveedor> resultado = useCase.porId(99999);

        assertTrue(resultado.isEmpty(),
                "Un id inexistente debe retornar Optional.empty");
    }

    // ==================== porNombre ====================

    /*
     * CP-004: Verifica el flujo principal de búsqueda por nombre. La BD
     * tiene "Distribuidora Diana SA", debe encontrarse al buscar "diana".
     */
    @Test
    @DisplayName("CP-004: Buscar proveedor por nombre existente debe retornar resultados")
    void porNombre_conNombreExistente_debeRetornarResultados() {
        List<Proveedor> resultado = useCase.porNombre("diana");

        assertNotNull(resultado);
        assertFalse(resultado.isEmpty(),
                "Debe encontrar al menos un proveedor que contenga 'diana'");
    }

    /*
     * CP-005: Verifica la validación de nombre vacío. Debe lanzar
     * IllegalArgumentException sin tocar la base de datos.
     */
    @Test
    @DisplayName("CP-005: Buscar proveedor por nombre vacío debe lanzar excepción")
    void porNombre_conNombreVacio_debeLanzarExcepcion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.porNombre("")
        );
    }

    // ==================== listarTodos ====================

    /*
     * CP-006: Verifica que listarTodos retorna TODOS los proveedores,
     * activos e inactivos. El script carga 5 proveedores en total.
     */
    @Test
    @DisplayName("CP-006: Listar todos los proveedores debe retornar los 5 cargados")
    void listarTodos_debeRetornarTodos() {
        List<Proveedor> resultado = useCase.listarTodos();

        assertNotNull(resultado);
        assertEquals(5, resultado.size(),
                "Deben retornarse los 5 proveedores cargados (4 activos + 1 inactivo)");
    }

    // ==================== listarActivos ====================

    /*
     * CP-007: Verifica el filtro de listarActivos. La BD tiene 5 proveedores
     * en total pero solo 4 están activos. Esta prueba garantiza que el
     * filtro WHERE estado_activo = TRUE funciona correctamente.
     */
    @Test
    @DisplayName("CP-007: Listar proveedores activos debe excluir los inactivos")
    void listarActivos_debeRetornarSoloActivos() {
        List<Proveedor> resultado = useCase.listarActivos();

        assertNotNull(resultado);
        assertEquals(4, resultado.size(),
                "Deben retornarse solo los 4 proveedores activos (no el inactivo)");
        resultado.forEach(p ->
                assertTrue(p.isActivo(),
                        "Todos los proveedores retornados deben estar activos"));
    }
}