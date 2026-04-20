package services.autenticacion;

import entities.Usuario;

public final class SesionUsuario {
    private static Usuario usuarioActual;

    private SesionUsuario() {
    }

    public static void iniciar(Usuario usuario) {
        usuarioActual = usuario;
    }

    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    public static void cerrar() {
        usuarioActual = null;
    }
}
