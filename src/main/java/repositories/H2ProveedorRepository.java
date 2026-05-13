package repositories;

import entities.Proveedor;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2ProveedorRepository implements ProveedorRepository {

    private final DatabaseConnection dbConnection;

    public H2ProveedorRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<Proveedor> buscarPorId(int id) {
        String sql = "SELECT * FROM Proveedor WHERE id_proveedor = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearProveedor(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar proveedor por id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Proveedor> buscarPorNombre(String nombre) {
        List<Proveedor> proveedores = new ArrayList<>();
        String sql = "SELECT * FROM Proveedor WHERE LOWER(nombre) LIKE ? ORDER BY nombre";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre.toLowerCase() + "%");

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                proveedores.add(mapearProveedor(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar proveedor por nombre", e);
        }

        return proveedores;
    }

    @Override
    public List<Proveedor> listarTodos() {
        List<Proveedor> proveedores = new ArrayList<>();
        String sql = "SELECT * FROM Proveedor ORDER BY nombre";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                proveedores.add(mapearProveedor(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar proveedores", e);
        }

        return proveedores;
    }

    @Override
    public List<Proveedor> listarActivos() {
        List<Proveedor> proveedores = new ArrayList<>();
        String sql = "SELECT * FROM Proveedor WHERE estado_activo = TRUE ORDER BY nombre";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                proveedores.add(mapearProveedor(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar proveedores activos", e);
        }

        return proveedores;
    }

    @Override
    public Proveedor guardar(Proveedor proveedor) {
        String sql = """
            INSERT INTO Proveedor
            (nombre, correo, telefono, direccion, estado_activo, fecha_registro)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, proveedor.getNombre());
            stmt.setString(2, proveedor.getCorreo());
            stmt.setString(3, proveedor.getTelefono());
            stmt.setString(4, proveedor.getDireccion());
            stmt.setBoolean(5, proveedor.isActivo());
            stmt.setDate(6, Date.valueOf(proveedor.getFechaRegistro()));

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return new Proveedor(
                        rs.getInt(1),
                        proveedor.getNombre(),
                        proveedor.getCorreo(),
                        proveedor.getTelefono(),
                        proveedor.getDireccion(),
                        proveedor.getFechaRegistro(),
                        proveedor.isActivo()
                );
            }

            throw new RuntimeException("No se pudo obtener el id del proveedor guardado");

        } catch (Exception e) {
            throw new RuntimeException("Error al guardar proveedor", e);
        }
    }

    @Override
    public boolean actualizar(Proveedor proveedor) {
        String sql = """
            UPDATE Proveedor
            SET nombre = ?, correo = ?, telefono = ?, direccion = ?, estado_activo = ?
            WHERE id_proveedor = ?
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, proveedor.getNombre());
            stmt.setString(2, proveedor.getCorreo());
            stmt.setString(3, proveedor.getTelefono());
            stmt.setString(4, proveedor.getDireccion());
            stmt.setBoolean(5, proveedor.isActivo());
            stmt.setInt(6, proveedor.getId());

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar proveedor", e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE Proveedor SET estado_activo = FALSE WHERE id_proveedor = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al desactivar proveedor", e);
        }
    }

    private Proveedor mapearProveedor(ResultSet rs) throws Exception {
        return new Proveedor(
                rs.getInt("id_proveedor"),
                rs.getString("nombre"),
                rs.getString("correo"),
                rs.getString("telefono"),
                rs.getString("direccion"),
                rs.getDate("fecha_registro").toLocalDate(),
                rs.getBoolean("estado_activo")
        );
    }
}