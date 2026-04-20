package repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:h2:./supermercado_db;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASS = "";

    public Connection getConnection() throws SQLException {
        System.out.println("Conectando a H2...");
        return DriverManager.getConnection(URL, USER, PASS);
    }
}