package services.ordenes;

import entities.OrdenCompra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2OrdenCompraRepository;
import repositories.OrdenCompraRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio ConsultarOrdenCompraUseCase (CU-009).
 *
 * Datos iniciales (cargados por DatabaseInitializer):
 *   - Orden 1: Proveedor 1 (Diana), 20 Arroz, $60.000, activa
 *   - Orden 2: Proveedor 2 (Alpina), 10 Leche, $25.000, activa
 *   - Orden 3: Proveedor 3 (Coca Cola FEMSA), 10 Gaseosa, $40.000, cancelada
 *
 * Total: 3 órdenes; Activas: 2; Canceladas: 1
 */
@DisplayName("Pruebas del servicio ConsultarOrdenCompraUseCase")
class ConsultarOrdenCompraUseCaseTest {

    private ConsultarOrdenCompraUseCase useCase;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        OrdenCompraRepository ordenRepository = new H2OrdenCompraRepository(conn);
        useCase = new ConsultarOrdenCompraUseCase(ordenRepository);
    }

    // ==================== porId ====================

    /*
     * CP-004: Verifica que al buscar por id existente se recupere la orden
     * con todos sus detalles cargados.
     */
    @Test
    @DisplayName("CP-004: Buscar orden por id existente debe retornarla con detalles")
    void porId_conIdExistente_debeRetornarOrden() {
        Optional<OrdenCompra> resultado = useCase.porId(1);

        assertTrue(resultado.isPresent(), "La orden 1 debe existir");
        assertEquals(1, resultado.get().getIdProveedor());
        assertFalse(resultado.get().getDetalles().isEmpty(),
                "La orden debe tener al menos un detalle cargado");
    }

    /*
     * CP-005: Verifica que al buscar un id inexistente se retorne
     * Optional.empty sin lanzar excepción.
     */
    @Test
    @DisplayName("CP-005: Buscar orden por id inexistente debe retornar Optional vacío")
    void porId_conIdInexistente_debeRetornarOptionalVacio() {
        Optional<OrdenCompra> resultado = useCase.porId(99999);

        assertTrue(resultado.isEmpty());
    }

    // ==================== listarTodas ====================

    /*
     * CP-006: Verifica que listarTodas retorna todas las órdenes (activas
     * y canceladas). La BD tiene 3 órdenes precargadas.
     */
    @Test
    @DisplayName("CP-006: Listar todas las órdenes debe retornar las 3 cargadas")
    void listarTodas_debeRetornarTodas() {
        List<OrdenCompra> resultado = useCase.listarTodas();

        assertEquals(3, resultado.size(),
                "Deben retornarse las 3 órdenes (2 activas + 1 cancelada)");
    }

    // ==================== listarActivas ====================

    /*
     * CP-007: Verifica el filtro de listarActivas. De las 3 órdenes en BD,
     * solo 2 están activas. Esta prueba garantiza que el filtro
     * WHERE estado = TRUE funciona correctamente.
     */
    @Test
    @DisplayName("CP-007: Listar órdenes activas debe excluir las canceladas")
    void listarActivas_debeRetornarSoloActivas() {
        List<OrdenCompra> resultado = useCase.listarActivas();

        assertEquals(2, resultado.size(),
                "Deben retornarse solo las 2 órdenes activas");
        resultado.forEach(o ->
                assertTrue(o.isActiva(),
                        "Todas las órdenes retornadas deben estar activas"));
    }

    // ==================== listarPorProveedor ====================

    /*
     * CP-008: Verifica el filtro por proveedor. El proveedor 1 (Diana)
     * tiene una orden asociada en la BD inicial.
     */
    @Test
    @DisplayName("CP-008: Listar órdenes por proveedor debe retornar las suyas")
    void listarPorProveedor_conProveedorConOrdenes_debeRetornarLista() {
        List<OrdenCompra> resultado = useCase.listarPorProveedor(1);

        assertFalse(resultado.isEmpty(),
                "El proveedor 1 debe tener al menos una orden");
        resultado.forEach(o ->
                assertEquals(1, o.getIdProveedor(),
                        "Todas las órdenes deben pertenecer al proveedor 1"));
    }

    /*
     * CP-009: Verifica la validación de proveedor inválido. Un id <= 0
     * debe lanzar IllegalArgumentException antes de consultar la BD.
     */
    @Test
    @DisplayName("CP-009: Listar órdenes con proveedor inválido debe lanzar excepción")
    void listarPorProveedor_conProveedorInvalido_debeLanzarExcepcion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.listarPorProveedor(0)
        );
    }
}