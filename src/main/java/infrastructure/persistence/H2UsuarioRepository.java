package infrastructure.persistence;

import domain.model.Empleado;
import domain.model.Rol;
import domain.model.Usuario;
import domain.repository.UsuarioRepository;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2UsuarioRepository implements UsuarioRepository {

    private final DatabaseConnection dbConnection;

    public H2UsuarioRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        String sql = """
            SELECT u.id_usuario, u.username, u.password_hash, u.intentos_fallidos,
                   u.bloqueado_hasta, u.ultimo_acceso, u.estado_usuario,
                   r.id_rol, r.nombre_rol, r.descripcion AS descripcion_rol,
                   e.id_empleado, e.nombre, e.apellido, e.correo, e.telefono,
                   e.estado_activo, e.fecha_registro
            FROM Usuario u
            INNER JOIN Rol r ON u.id_rol = r.id_rol
            INNER JOIN Empleado e ON u.id_empleado = e.id_empleado
            WHERE u.username = ?
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearUsuario(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar usuario por username", e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Usuario> buscarPorId(int id) {
        String sql = """
            SELECT u.id_usuario, u.username, u.password_hash, u.intentos_fallidos,
                   u.bloqueado_hasta, u.ultimo_acceso, u.estado_usuario,
                   r.id_rol, r.nombre_rol, r.descripcion AS descripcion_rol,
                   e.id_empleado, e.nombre, e.apellido, e.correo, e.telefono,
                   e.estado_activo, e.fecha_registro
            FROM Usuario u
            INNER JOIN Rol r ON u.id_rol = r.id_rol
            INNER JOIN Empleado e ON u.id_empleado = e.id_empleado
            WHERE u.id_usuario = ?
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearUsuario(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar usuario por id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();

        String sql = """
            SELECT u.id_usuario, u.username, u.password_hash, u.intentos_fallidos,
                   u.bloqueado_hasta, u.ultimo_acceso, u.estado_usuario,
                   r.id_rol, r.nombre_rol, r.descripcion AS descripcion_rol,
                   e.id_empleado, e.nombre, e.apellido, e.correo, e.telefono,
                   e.estado_activo, e.fecha_registro
            FROM Usuario u
            INNER JOIN Rol r ON u.id_rol = r.id_rol
            INNER JOIN Empleado e ON u.id_empleado = e.id_empleado
            ORDER BY u.username
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar usuarios", e);
        }

        return usuarios;
    }

    @Override
    public Usuario guardar(Usuario usuario) {
        String sql = """
            INSERT INTO Usuario
            (id_rol, id_empleado, username, password_hash, intentos_fallidos,
             bloqueado_hasta, ultimo_acceso, estado_usuario)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, usuario.getRol().getIdRol());
            stmt.setInt(2, usuario.getEmpleado().getId());
            stmt.setString(3, usuario.getUsername());
            stmt.setString(4, usuario.getPasswordHash());
            stmt.setInt(5, usuario.getIntentosFallidos());

            if (usuario.getBloqueadoHasta() != null) {
                stmt.setTime(6, Time.valueOf(usuario.getBloqueadoHasta()));
            } else {
                stmt.setNull(6, java.sql.Types.TIME);
            }

            if (usuario.getUltimoAcceso() != null) {
                stmt.setDate(7, Date.valueOf(usuario.getUltimoAcceso()));
            } else {
                stmt.setNull(7, java.sql.Types.DATE);
            }

            stmt.setBoolean(8, usuario.isEstadoUsuario());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return new Usuario(
                        rs.getInt(1),
                        usuario.getUsername(),
                        usuario.getPasswordHash(),
                        usuario.getRol(),
                        usuario.getEmpleado(),
                        usuario.getIntentosFallidos(),
                        usuario.getBloqueadoHasta(),
                        usuario.getUltimoAcceso(),
                        usuario.isEstadoUsuario()
                );
            }

            throw new RuntimeException("No se pudo obtener el id del usuario guardado");

        } catch (Exception e) {
            throw new RuntimeException("Error al guardar usuario", e);
        }
    }

    @Override
    public boolean actualizar(Usuario usuario) {
        String sql = """
            UPDATE Usuario
            SET id_rol = ?, id_empleado = ?, username = ?, password_hash = ?,
                intentos_fallidos = ?, bloqueado_hasta = ?, ultimo_acceso = ?, estado_usuario = ?
            WHERE id_usuario = ?
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, usuario.getRol().getIdRol());
            stmt.setInt(2, usuario.getEmpleado().getId());
            stmt.setString(3, usuario.getUsername());
            stmt.setString(4, usuario.getPasswordHash());
            stmt.setInt(5, usuario.getIntentosFallidos());

            if (usuario.getBloqueadoHasta() != null) {
                stmt.setTime(6, Time.valueOf(usuario.getBloqueadoHasta()));
            } else {
                stmt.setNull(6, java.sql.Types.TIME);
            }

            if (usuario.getUltimoAcceso() != null) {
                stmt.setDate(7, Date.valueOf(usuario.getUltimoAcceso()));
            } else {
                stmt.setNull(7, java.sql.Types.DATE);
            }

            stmt.setBoolean(8, usuario.isEstadoUsuario());
            stmt.setInt(9, usuario.getId());

            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar usuario", e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE Usuario SET estado_usuario = FALSE WHERE id_usuario = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al desactivar usuario", e);
        }
    }

    @Override
    public boolean existeUsername(String username) {
        String sql = "SELECT COUNT(*) FROM Usuario WHERE username = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al verificar username", e);
        }

        return false;
    }

    private Usuario mapearUsuario(ResultSet rs) throws Exception {
        Empleado empleado = new Empleado(
                rs.getInt("id_empleado"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("correo"),
                rs.getString("telefono"),
                rs.getDate("fecha_registro").toLocalDate(),
                rs.getBoolean("estado_activo")
        );

        Rol rol = new Rol(
                rs.getInt("id_rol"),
                rs.getString("nombre_rol"),
                rs.getString("descripcion_rol")
        );

        return new Usuario(
                rs.getInt("id_usuario"),
                rs.getString("username"),
                rs.getString("password_hash"),
                rol,
                empleado,
                rs.getInt("intentos_fallidos"),
                rs.getTime("bloqueado_hasta") != null
                        ? rs.getTime("bloqueado_hasta").toLocalTime()
                        : null,
                rs.getDate("ultimo_acceso") != null
                        ? rs.getDate("ultimo_acceso").toLocalDate()
                        : null,
                rs.getBoolean("estado_usuario")
        );
    }
}