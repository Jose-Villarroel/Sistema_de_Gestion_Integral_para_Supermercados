package repositories;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Inicializador de la base de datos para pruebas y arranque del sistema.
 * 
 * Lee el script SQL de inicialización desde los recursos del proyecto
 * y lo ejecuta sobre la conexión H2 proporcionada. El script borra todas
 * las tablas existentes, las recrea y carga datos de prueba conocidos.
 * 
 * Uso típico en pruebas de integración:
 * <pre>
 *     DatabaseConnection conn = new DatabaseConnection();
 *     DatabaseInitializer initializer = new DatabaseInitializer(conn);
 *     initializer.init();
 * </pre>
 */
public class DatabaseInitializer {

    private static final String SCRIPT_PATH = "/db/BD-fundamentos.sql";

    private final DatabaseConnection dbConnection;

    public DatabaseInitializer(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    /**
     * Reinicializa la base de datos al estado limpio definido en el script SQL.
     * Borra todas las tablas, las recrea y carga datos de prueba.
     * 
     * @throws SQLException si ocurre un error al ejecutar el SQL
     * @throws IOException  si no se puede leer el script
     */
    public void init() throws SQLException, IOException {
        String sql = leerScript();
        Connection connection = dbConnection.getConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String leerScript() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(SCRIPT_PATH)) {
            if (input == null) {
                throw new IOException("No se encontró el script SQL en: " + SCRIPT_PATH);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}