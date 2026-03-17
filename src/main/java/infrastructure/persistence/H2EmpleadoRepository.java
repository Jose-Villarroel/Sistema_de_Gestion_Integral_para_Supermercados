package infrastructure.persistence;

import domain.model.Empleado;
import domain.repository.EmpleadoRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2EmpleadoRepository implements EmpleadoRepository {

    private final DatabaseConnection dbConnection;

    public H2EmpleadoRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<Empleado> buscarPorUsuario(String usuario) {
        String sql = "SELECT * FROM empleados WHERE usuario = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearEmpleado(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Empleado> buscarPorId(int id) {
        String sql = "SELECT * FROM empleados WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearEmpleado(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Empleado> listarTodos() {
        List<Empleado> empleados = new ArrayList<>();
        String sql = "SELECT * FROM empleados";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                empleados.add(mapearEmpleado(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return empleados;
    }

    @Override
    public void guardar(Empleado empleado) {
        String sql = "INSERT INTO empleados (usuario, password, rol, activo) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, empleado.getUsuario());
            stmt.setString(2, empleado.getContrasena());
            stmt.setString(3, empleado.getRol().name());
            stmt.setBoolean(4, empleado.isActivo());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void actualizar(Empleado empleado) {
        String sql = "UPDATE empleados SET usuario=?, password=?, rol=?, activo=? WHERE id=?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, empleado.getUsuario());
            stmt.setString(2, empleado.getContrasena());
            stmt.setString(3, empleado.getRol().name());
            stmt.setBoolean(4, empleado.isActivo());
            stmt.setInt(5, empleado.getId());
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void desactivar(int id) {
        String sql = "UPDATE empleados SET activo = false WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Empleado mapearEmpleado(ResultSet rs) throws SQLException {
        return new Empleado(
                rs.getInt("id"),
                rs.getString("codigo"),
                rs.getString("nombre"),
                rs.getString("usuario"),
                rs.getString("password"),
                rs.getString("rol"),
                rs.getString("correo"),
                rs.getString("telefono"),
                rs.getBoolean("activo")
        );
    }
}
