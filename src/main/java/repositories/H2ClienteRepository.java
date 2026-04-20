package repositories;

import aggregates.Cliente;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2ClienteRepository implements ClienteRepository {

    private final DatabaseConnection dbConnection;

    public H2ClienteRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<Cliente> buscarPorId(int id) {
        String sql = "SELECT * FROM Cliente WHERE id_cliente = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearCliente(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar cliente por id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Cliente> buscarPorNombre(String nombre) {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM Cliente WHERE LOWER(nombre) LIKE ? OR LOWER(apellido) LIKE ? ORDER BY nombre, apellido";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String parametro = "%" + nombre.toLowerCase() + "%";
            stmt.setString(1, parametro);
            stmt.setString(2, parametro);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar cliente por nombre", e);
        }

        return clientes;
    }

    @Override
    public List<Cliente> listarTodos() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM Cliente ORDER BY nombre, apellido";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar clientes", e);
        }

        return clientes;
    }

    @Override
    public List<Cliente> listarActivos() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT * FROM Cliente WHERE estado_activo = TRUE ORDER BY nombre, apellido";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar clientes activos", e);
        }

        return clientes;
    }

    @Override
    public Cliente guardar(Cliente cliente) {
        String sql = """
            INSERT INTO Cliente
            (nombre, apellido, correo, telefono, direccion, estado_activo, fecha_registro)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getApellido());
            stmt.setString(3, cliente.getCorreo());
            stmt.setString(4, cliente.getTelefono());
            stmt.setString(5, cliente.getDireccion());
            stmt.setBoolean(6, cliente.isActivo());
            stmt.setDate(7, Date.valueOf(cliente.getFechaRegistro()));

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return new Cliente(
                        rs.getInt(1),
                        cliente.getNombre(),
                        cliente.getApellido(),
                        cliente.getCorreo(),
                        cliente.getTelefono(),
                        cliente.getDireccion(),
                        cliente.getFechaRegistro(),
                        cliente.isActivo()
                );
            }

            throw new RuntimeException("No se pudo obtener el id del cliente guardado");

        } catch (Exception e) {
            throw new RuntimeException("Error al guardar cliente", e);
        }
    }

    @Override
    public boolean actualizar(Cliente cliente) {
        String sql = """
            UPDATE Cliente
            SET nombre = ?, apellido = ?, correo = ?, telefono = ?, direccion = ?, estado_activo = ?
            WHERE id_cliente = ?
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getApellido());
            stmt.setString(3, cliente.getCorreo());
            stmt.setString(4, cliente.getTelefono());
            stmt.setString(5, cliente.getDireccion());
            stmt.setBoolean(6, cliente.isActivo());
            stmt.setInt(7, cliente.getId());

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar cliente", e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE Cliente SET estado_activo = FALSE WHERE id_cliente = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al desactivar cliente", e);
        }
    }

    private Cliente mapearCliente(ResultSet rs) throws Exception {
        return new Cliente(
                rs.getInt("id_cliente"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("correo"),
                rs.getString("telefono"),
                rs.getString("direccion"),
                rs.getDate("fecha_registro").toLocalDate(),
                rs.getBoolean("estado_activo")
        );
    }
}