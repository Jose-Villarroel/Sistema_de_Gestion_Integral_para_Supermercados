package repositories;

import entities.Usuario;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> buscarPorUsername(String username);

    Optional<Usuario> buscarPorId(int id);

    List<Usuario> listarTodos();

    Usuario guardar(Usuario usuario);

    boolean actualizar(Usuario usuario);

    boolean desactivar(int id);

    boolean existeUsername(String username);

    void validarSupervisor(
            Connection conn,
            String username,
            String password
    ) throws SQLException;
}