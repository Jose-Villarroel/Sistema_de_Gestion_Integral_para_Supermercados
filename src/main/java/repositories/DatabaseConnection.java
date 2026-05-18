package repositories;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:h2:file:./supermercado_db;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASS = "";
    private static Connection connection;

    /**
     * Retorna una conexion compartida envuelta en un proxy que ignora
     * las llamadas a close(). De esta forma, los try-with-resources de
     * los repositorios no cierran la conexion subyacente y los cambios
     * persisten entre operaciones consecutivas (necesario para FKs).
     *
     * La conexion real solo se cierra al terminar el proceso de la JVM.
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("Conectando a H2...");
            Connection realConnection = DriverManager.getConnection(URL, USER, PASS);
            connection = wrapNoClose(realConnection);
        }
        return connection;
    }

    /**
     * Envuelve una Connection en un proxy dinamico que intercepta close()
     * y lo convierte en un no-op. El resto de metodos se delegan al
     * objeto real.
     */
    private static Connection wrapNoClose(Connection real) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    if ("isClosed".equals(method.getName())) {
                        return real.isClosed();
                    }
                    return method.invoke(real, args);
                }
        );
    }
}