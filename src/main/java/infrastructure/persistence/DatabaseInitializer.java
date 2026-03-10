package infrastructure.persistence;

import java.sql.Connection;
import java.sql.Statement;

public class DatabaseInitializer {

    public static void init() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            // Tabla empleados
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS empleados (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    usuario VARCHAR(50) UNIQUE,
                    password VARCHAR(100),
                    rol VARCHAR(20)
                );
            """);

            // Tabla productos
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS productos (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nombre VARCHAR(100),
                    precio DOUBLE,
                    stock INT
                );
            """);

            // Usuario admin por defecto
            stmt.execute("""
                INSERT INTO empleados (usuario, password, rol)
                SELECT 'admin', 'admin', 'ADMIN'
                WHERE NOT EXISTS (
                    SELECT 1 FROM empleados WHERE usuario = 'admin'
                );
            """);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
