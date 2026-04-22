package repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String URL = "jdbc:h2:file:./supermercado_db;AUTO_SERVER=TRUE";
    private static final String USER = "sa";
    private static final String PASS = "";

    private static Connection connection;

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("Conectando a H2...");
            connection = DriverManager.getConnection(URL, USER, PASS);
            asegurarEsquemaCierreCaja(connection);
        }
        return connection;
    }

    private void asegurarEsquemaCierreCaja(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE Venta ADD COLUMN IF NOT EXISTS turno VARCHAR(20)");
            stmt.execute("ALTER TABLE Venta ADD COLUMN IF NOT EXISTS metodo_pago VARCHAR(30)");
            stmt.execute("""
                    CREATE TABLE IF NOT EXISTS CIERRE_CAJA (
                        id_cierre INT PRIMARY KEY AUTO_INCREMENT,
                        numero_cierre VARCHAR(50) UNIQUE,
                        fecha_cierre DATE,
                        turno VARCHAR(20),
                        id_empleado INT,
                        efectivo_esperado DECIMAL(10,2),
                        efectivo_contado DECIMAL(10,2),
                        diferencia DECIMAL(10,2),
                        estado_cierre VARCHAR(30),
                        total_transacciones INT,
                        observacion VARCHAR(500),
                        FOREIGN KEY (id_empleado) REFERENCES Empleado(id_empleado),
                        UNIQUE (fecha_cierre, turno)
                    )
                    """);
        }
    }
}
