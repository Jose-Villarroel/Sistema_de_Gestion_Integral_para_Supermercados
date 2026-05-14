package services.ordenes;

import entities.OrdenCompra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2OrdenCompraRepository;
import repositories.OrdenCompraRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio CancelarOrdenCompraUseCase (CU-009).
 *
 * Datos iniciales relevantes:
 *   - Orden 1 y 2: activas
 *   - Orden 3: ya cancelada
 */
@DisplayName("Pruebas del servicio CancelarOrdenCompraUseCase")
class CancelarOrdenCompraUseCaseTest {

    private CancelarOrdenCompraUseCase useCase;
    private OrdenCompraRepository ordenRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        ordenRepository = new H2OrdenCompraRepository(conn);
        useCase = new CancelarOrdenCompraUseCase(ordenRepository);
    }

    /*
     * CP-010: Verifica el flujo principal. La orden 1 está activa en la BD
     * inicial; tras cancelarla debe quedar con estado = false en la BD.
     */
    @Test
    @DisplayName("CP-010: Cancelar orden activa debe marcarla como inactiva en BD")
    void cancelarOrden_conOrdenActiva_debePersistirCambio() {
        boolean resultado = useCase.ejecutar(1);

        assertTrue(resultado, "El método debe retornar true al cancelar");

        Optional<OrdenCompra> orden = ordenRepository.buscarPorId(1);
        assertTrue(orden.isPresent(), "La orden debe seguir existiendo");
        assertFalse(orden.get().isActiva(),
                "La orden debe quedar marcada como cancelada");
    }

    /*
     * CP-011: Verifica el flujo de excepción cuando la orden no existe.
     * Debe lanzar IllegalArgumentException con un mensaje que incluya el id.
     */
    @Test
    @DisplayName("CP-011: Cancelar orden inexistente debe lanzar excepción")
    void cancelarOrden_conIdInexistente_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(99999)
        );

        assertTrue(ex.getMessage().contains("99999"),
                "El mensaje debe incluir el id no encontrado");
    }

    /*
     * CP-012: Verifica que no se puede cancelar una orden que ya está
     * cancelada. La orden 3 está cancelada en la BD inicial; intentar
     * cancelarla de nuevo debe lanzar IllegalStateException.
     */
    @Test
    @DisplayName("CP-012: Cancelar orden ya cancelada debe lanzar excepción")
    void cancelarOrden_yaCancelada_debeLanzarExcepcion() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> useCase.ejecutar(3)
        );

        assertTrue(ex.getMessage().contains("ya está cancelada"),
                "El mensaje debe indicar que ya está cancelada");
    }
}