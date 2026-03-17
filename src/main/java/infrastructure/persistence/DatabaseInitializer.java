package infrastructure.persistence;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    private final DatabaseConnection dbConnection;

    public DatabaseInitializer(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public void init() {
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ejecutarScript(conn, "/db/schema.sql");
            ejecutarScript(conn, "/db/data.sql");

        } catch (Exception e) {
            System.err.println("Error al inicializar la base de datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void ejecutarScript(Connection conn, String rutaScript) throws Exception {
        InputStream is = DatabaseInitializer.class.getResourceAsStream(rutaScript);

        if (is == null) {
            throw new RuntimeException("No se encontró el script: " + rutaScript);
        }

        String sql = new String(is.readAllBytes());

        for (String sentencia : sql.split(";")) {
            if (!sentencia.trim().isEmpty()) {
                conn.createStatement().execute(sentencia.trim());
            }
        }
    }
}
