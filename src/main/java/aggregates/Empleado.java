package aggregates;

import valueobjects.Contrasena;
import valueobjects.Rol;

public class Empleado {
    private final int id;
    private final String codigo;
    private final String nombre;
    private final String usuario;
    private final Contrasena contrasena;
    private final Rol rol;
    private final String correo;
    private final String telefono;
    private boolean activo;

    public Empleado(int id, String codigo, String nombre, String usuario,
                    String contrasena, String rol, String correo,
                    String telefono, boolean activo) {
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalArgumentException("El usuario no puede estar vacío");
        }
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.usuario = usuario;
        this.contrasena = new Contrasena(contrasena);
        this.rol = Rol.fromString(rol);
        this.correo = correo;
        this.telefono = telefono;
        this.activo = activo;
    }

    public boolean tieneCredenciales(String usuario, String password) {
        return this.usuario.equals(usuario) &&
               this.contrasena.equals(new Contrasena(password));
    }

    public void desactivar() {
        this.activo = false;
    }

    public int getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getUsuario() { return usuario; }
    public Rol getRol() { return rol; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public boolean isActivo() { return activo; }
    public String getContrasena() { return contrasena.getValor(); }
}
