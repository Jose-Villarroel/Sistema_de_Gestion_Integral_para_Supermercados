package services.autenticacion;

import entities.Usuario;
import repositories.UsuarioRepository;

public class AutenticarEmpleadoUseCase {

    private final UsuarioRepository usuarioRepository;

    public AutenticarEmpleadoUseCase(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario ejecutar(String username, String password) {

        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        if (password.length() < 4) {
            throw new IllegalArgumentException("La contraseña debe tener mínimo 4 caracteres");
        }

        Usuario usuario = usuarioRepository
                .buscarPorUsername(username)
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas"));

        if (!usuario.isEstadoUsuario()) {
            throw new RuntimeException("Usuario desactivado");
        }

        if (usuario.estaBloqueado()) {
            throw new RuntimeException("Usuario bloqueado temporalmente");
        }
        System.out.println("Usuario encontrado: " + usuario.getUsername());
        System.out.println("Hash guardado en BD: " + usuario.getPasswordHash());
        System.out.println("Hash ingresado: " + String.valueOf(password.hashCode()));
        System.out.println("Estado usuario: " + usuario.isEstadoUsuario());
        if (!usuario.passwordCoincide(password)) {
            throw new RuntimeException("Credenciales incorrectas");
        }

        return usuario;
    }
}