package repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:h2:./supermercado_db;AUTO_SERVER=TRUE;AUTO_RECONNECT=TRUE";
    private static final String USER = "sa";
    private static final String PASS = "";

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}