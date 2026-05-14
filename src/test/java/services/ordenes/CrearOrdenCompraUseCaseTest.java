package services.ordenes;

import entities.DetalleOrdenCompra;
import entities.OrdenCompra;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2OrdenCompraRepository;
import repositories.OrdenCompraRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio CrearOrdenCompraUseCase (CU-009).
 *
 * Valida la creación de órdenes de compra con sus respectivos detalles,
 * verificando que tanto la cabecera como las líneas se persistan
 * correctamente dentro de una transacción SQL.
 */
@DisplayName("Pruebas del servicio CrearOrdenCompraUseCase")
class CrearOrdenCompraUseCaseTest {

    private CrearOrdenCompraUseCase useCase;
    private OrdenCompraRepository ordenRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        ordenRepository = new H2OrdenCompraRepository(conn);
        useCase = new CrearOrdenCompraUseCase(ordenRepository);
    }

    /*
     * CP-001: Verifica el flujo principal. Crea una orden con dos líneas,
     * confirma que se persiste con id asignado, que el total se calcula
     * correctamente y que ambos detalles quedan guardados.
     */
    @Test
    @DisplayName("CP-001: Crear orden con datos válidos debe persistir cabecera y detalles")
    void crearOrden_conDatosValidos_debePersistirTodo() {
        DetalleOrdenCompra linea1 = new DetalleOrdenCompra(
                0, 1, 10, new BigDecimal("3000.00"));
        DetalleOrdenCompra linea2 = new DetalleOrdenCompra(
                0, 3, 5, new BigDecimal("2500.00"));

        OrdenCompra resultado = useCase.ejecutar(
                1,
                1,
                LocalDate.now().plusDays(7),
                List.of(linea1, linea2)
        );

        assertNotNull(resultado);
        assertTrue(resultado.getId() > 0, "La orden debe tener un id asignado");
        assertEquals(new BigDecimal("42500.00"), resultado.getTotal(),
                "El total debe ser la suma de los subtotales (10*3000 + 5*2500)");
        assertTrue(resultado.isActiva(), "La orden debe nacer activa");

        OrdenCompra recargada = ordenRepository.buscarPorId(resultado.getId()).orElseThrow();
        assertEquals(2, recargada.getDetalles().size(),
                "La orden debe tener 2 detalles persistidos en BD");
    }

    /*
     * CP-002: Verifica la validación del aggregate OrdenCompra. Una orden
     * sin detalles debe lanzar IllegalArgumentException sin persistir nada.
     */
    @Test
    @DisplayName("CP-002: Crear orden sin detalles debe lanzar excepción")
    void crearOrden_sinDetalles_debeLanzarExcepcion() {
        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(1, 1, LocalDate.now().plusDays(7), List.of())
        );
    }

    /*
     * CP-003: Verifica la validación del aggregate. Un proveedor inválido
     * (id <= 0) debe lanzar IllegalArgumentException.
     */
    @Test
    @DisplayName("CP-003: Crear orden sin proveedor válido debe lanzar excepción")
    void crearOrden_sinProveedorValido_debeLanzarExcepcion() {
        DetalleOrdenCompra linea = new DetalleOrdenCompra(
                0, 1, 5, new BigDecimal("3000.00"));

        assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(0, 1, LocalDate.now().plusDays(7), List.of(linea))
        );
    }
}