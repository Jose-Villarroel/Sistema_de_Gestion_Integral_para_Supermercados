package repositories;

import dtos.SupervisorAutorizacionDTO;
import entities.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> buscarPorUsername(String username);

    Optional<SupervisorAutorizacionDTO> buscarSupervisorActivoPorUsername(String username);

    Optional<Usuario> buscarPorId(int id);

    List<Usuario> listarTodos();

    Usuario guardar(Usuario usuario);

    boolean actualizar(Usuario usuario);

    boolean desactivar(int id);

    boolean existeUsername(String username);
}
