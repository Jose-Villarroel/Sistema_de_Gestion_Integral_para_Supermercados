package infrastructure.persistence;

import domain.model.Empleado;
import domain.repository.EmpleadoRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2EmpleadoRepository implements EmpleadoRepository {

    private final DatabaseConnection dbConnection;

    public H2EmpleadoRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<Empleado> buscarPorId(int id) {
        String sql = "SELECT * FROM Empleado WHERE id_empleado = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearEmpleado(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar empleado por id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Empleado> listarTodos() {
        List<Empleado> empleados = new ArrayList<>();
        String sql = "SELECT * FROM Empleado ORDER BY nombre, apellido";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                empleados.add(mapearEmpleado(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar empleados", e);
        }

        return empleados;
    }

    @Override
    public Empleado guardar(Empleado empleado) {
        String sql = """
            INSERT INTO Empleado
            (nombre, apellido, correo, telefono, estado_activo, fecha_registro)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, empleado.getNombre());
            stmt.setString(2, empleado.getApellido());
            stmt.setString(3, empleado.getCorreo());
            stmt.setString(4, empleado.getTelefono());
            stmt.setBoolean(5, empleado.isActivo());
            stmt.setDate(6, Date.valueOf(empleado.getFechaRegistro()));

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return new Empleado(
                        rs.getInt(1),
                        empleado.getNombre(),
                        empleado.getApellido(),
                        empleado.getCorreo(),
                        empleado.getTelefono(),
                        empleado.getFechaRegistro(),
                        empleado.isActivo()
                );
            }

            throw new RuntimeException("No se pudo obtener el id del empleado guardado");

        } catch (Exception e) {
            throw new RuntimeException("Error al guardar empleado", e);
        }
    }

    @Override
    public boolean actualizar(Empleado empleado) {
        String sql = """
            UPDATE Empleado
            SET nombre = ?, apellido = ?, correo = ?, telefono = ?, estado_activo = ?, fecha_registro = ?
            WHERE id_empleado = ?
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, empleado.getNombre());
            stmt.setString(2, empleado.getApellido());
            stmt.setString(3, empleado.getCorreo());
            stmt.setString(4, empleado.getTelefono());
            stmt.setBoolean(5, empleado.isActivo());
            stmt.setDate(6, Date.valueOf(empleado.getFechaRegistro()));
            stmt.setInt(7, empleado.getId());

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar empleado", e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE Empleado SET estado_activo = FALSE WHERE id_empleado = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al desactivar empleado", e);
        }
    }

    private Empleado mapearEmpleado(ResultSet rs) throws Exception {
        return new Empleado(
                rs.getInt("id_empleado"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("correo"),
                rs.getString("telefono"),
                rs.getDate("fecha_registro").toLocalDate(),
                rs.getBoolean("estado_activo")
        );
    }
}