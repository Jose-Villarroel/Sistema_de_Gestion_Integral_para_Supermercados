package services.productos;

import entities.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2ProductoRepository;
import repositories.ProductoRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio ModificarProductoUseCase (CU-003).
 *
 * El servicio modifica los datos de un producto ya existente. Las pruebas
 * verifican tanto el flujo principal (modificar correctamente) como el
 * flujo de excepción (producto inexistente).
 */
@DisplayName("Pruebas del servicio ModificarProductoUseCase")
class ModificarProductoUseCaseTest {

    private ModificarProductoUseCase useCase;
    private ProductoRepository productoRepository;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseConnection conn = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(conn);
        initializer.init();

        productoRepository = new H2ProductoRepository(conn);
        useCase = new ModificarProductoUseCase(productoRepository);
    }

    /*
     * CP-013: Verifica el flujo principal del UseCase. Modifica el producto
     * con id=1 (Arroz, precarga del script) cambiando varios atributos, y
     * confirma que los cambios se persistieron consultando la BD nuevamente.
     */
    @Test
    @DisplayName("CP-013: Modificar producto existente debe persistir los cambios")
    void modificarProducto_conIdExistente_debePersistirCambios() {
        boolean resultado = useCase.ejecutar(
                1,
                "Arroz Premium",
                "Arroz blanco premium 1kg",
                "Diana Gold",
                3500.0,
                5000.0,
                25,
                10,
                1,
                true
        );

        assertTrue(resultado, "El método debe retornar true al actualizar correctamente");

        Optional<Producto> actualizado = productoRepository.buscarPorId(1);
        assertTrue(actualizado.isPresent(), "El producto debe seguir existiendo");
        assertEquals("Arroz Premium", actualizado.get().getNombre(),
                "El nombre debe haberse actualizado en la BD");
        assertEquals(25, actualizado.get().getStockActual(),
                "El stock debe haberse actualizado");
    }

    /*
     * CP-014: Verifica el flujo de excepción. Cuando el id no existe en la
     * BD, el UseCase debe lanzar IllegalArgumentException y no debe modificar
     * ningún registro existente.
     */
    @Test
    @DisplayName("CP-014: Modificar producto inexistente debe lanzar excepción")
    void modificarProducto_conIdInexistente_debeLanzarExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.ejecutar(
                        99999,
                        "No importa",
                        "No importa",
                        "No importa",
                        2000.0,
                        3000.0,
                        5,
                        1,
                        1,
                        true
                )
        );

        assertTrue(ex.getMessage().contains("99999"),
                "El mensaje debe incluir el id no encontrado");
    }
}