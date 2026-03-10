package infrastructure.persistence;

import domain.model.Empleado;
import domain.repository.EmpleadoRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Optional;

public class H2EmpleadoRepository implements EmpleadoRepository {

    private final DatabaseConnection dbConnection;

    public H2EmpleadoRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<Empleado> buscarPorUsuarioYPassword(String usuario, String password) {
        String sql = "SELECT * FROM empleados WHERE usuario = ? AND password = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Empleado empleado = new Empleado(
                        rs.getInt("id"),
                        rs.getString("usuario"),
                        rs.getString("password"),
                        rs.getString("rol"),
                        true
                );
                return Optional.of(empleado);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }
}