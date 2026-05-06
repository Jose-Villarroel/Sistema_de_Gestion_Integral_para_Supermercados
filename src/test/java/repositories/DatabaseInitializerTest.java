package repositories;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de validación de la infraestructura de pruebas.
 * 
 * Estas pruebas no cubren un UseCase, sino que verifican que el
 * DatabaseInitializer funciona correctamente y deja la BD en el
 * estado esperado para que las demás pruebas de integración puedan
 * partir de un estado conocido.
 */
@DisplayName("Pruebas de validación de DatabaseInitializer")
class DatabaseInitializerTest {

    private DatabaseConnection dbConnection;
    private DatabaseInitializer initializer;

    @BeforeEach
    void setUp() throws Exception {
        dbConnection = new DatabaseConnection();
        initializer = new DatabaseInitializer(dbConnection);
        initializer.init();
    }

    /*
     * Verifica que después de inicializar la BD, existan exactamente
     * los 14 productos definidos en el script de datos de prueba.
     */
    @Test
    @DisplayName("La BD inicializada debe contener 14 productos de prueba")
    void initBD_debeCargar14Productos() throws Exception {
        int cantidad = contarFilas("Producto");
        assertEquals(14, cantidad,
                "Después de inicializar debe haber 14 productos en la BD");
    }

    /*
     * Verifica que la inicialización es idempotente: ejecutarla dos
     * veces seguidas debe dejar la BD en el mismo estado, sin duplicar
     * datos ni lanzar errores.
     */
    @Test
    @DisplayName("Reinicializar la BD debe dejar el mismo estado")
    void reinicializarBD_debeMantenerCantidadDeProductos() throws Exception {
        initializer.init();

        int cantidad = contarFilas("Producto");
        assertEquals(14, cantidad,
                "Después de reinicializar debe seguir habiendo 14 productos");
    }

    /*
     * Verifica que el script SQL incluye los datos esenciales para que
     * las pruebas de los demás módulos puedan funcionar: empleados,
     * clientes, roles y categorías.
     */
    @Test
    @DisplayName("La BD inicializada debe contener los datos base esperados")
    void initBD_debeContenerDatosBase() throws Exception {
        assertEquals(4, contarFilas("Rol"), "Debe haber 4 roles");
        assertEquals(3, contarFilas("Empleado"), "Debe haber 3 empleados");
        assertEquals(10, contarFilas("Cliente"), "Debe haber 10 clientes");
        assertEquals(3, contarFilas("Categoria"), "Debe haber 3 categorías");
        assertEquals(3, contarFilas("Usuario"), "Debe haber 3 usuarios");
    }

    private int contarFilas(String tabla) throws Exception {
        Connection conn = dbConnection.getConnection();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }
}