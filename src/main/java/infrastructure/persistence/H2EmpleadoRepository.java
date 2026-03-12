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
    public Optional<Empleado> buscarPorUsuarioYPassword(String usuario, String password) {
        String sql = "SELECT * FROM empleados WHERE usuario = ? AND password = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Empleado> listarTodos() {
        List<Empleado> lista = new ArrayList<>();
        String sql = "SELECT * FROM empleados ORDER BY nombre";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public Optional<Empleado> buscarPorId(int id) {
        String sql = "SELECT * FROM empleados WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(mapear(rs));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void guardar(Empleado e) {
        String sql = """
            INSERT INTO empleados
                (codigo, nombre, usuario, password, rol, correo, telefono, activo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.getCodigo());
            stmt.setString(2, e.getNombre());
            stmt.setString(3, e.getUsuario());
            stmt.setString(4, e.getPassword());
            stmt.setString(5, e.getRol());
            stmt.setString(6, e.getCorreo());
            stmt.setString(7, e.getTelefono());
            stmt.setBoolean(8, e.isActivo());
            stmt.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Error al guardar los datos, intente nuevamente");
        }
    }

    @Override
    public void actualizar(Empleado e) {
        String sql = "UPDATE empleados SET nombre=?, rol=?, correo=?, telefono=? WHERE id=?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, e.getNombre());
            stmt.setString(2, e.getRol());
            stmt.setString(3, e.getCorreo());
            stmt.setString(4, e.getTelefono());
            stmt.setInt(5, e.getId());
            stmt.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException("Error al guardar los datos, intente nuevamente");
        }
    }

    @Override
    public void desactivar(int id) {
        String sql = "UPDATE empleados SET activo = FALSE WHERE id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar los datos, intente nuevamente");
        }
    }

    @Override
    public boolean existeUsuario(String usuario) {
        String sql = "SELECT COUNT(*) FROM empleados WHERE usuario = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, usuario);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int contarAdministradores() {
        String sql = "SELECT COUNT(*) FROM empleados WHERE rol = 'ADMIN' AND activo = TRUE";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public String generarCodigoUnico() {
        String sql = "SELECT COUNT(*) FROM empleados";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                int total = rs.getInt(1) + 1;
                return String.format("EMP%03d", total);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "EMP001";
    }

    private Empleado mapear(ResultSet rs) throws SQLException {
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