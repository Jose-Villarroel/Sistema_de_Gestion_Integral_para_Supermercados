package domain.model;

public class Empleado {
    private int id;
    private String codigo;
    private String nombre;
    private String usuario;
    private String password;
    private String rol;
    private String correo;
    private String telefono;
    private boolean activo;

    public Empleado(int id, String codigo, String nombre, String usuario,
                    String password, String rol, String correo,
                    String telefono, boolean activo) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.usuario = usuario;
        this.password = password;
        this.rol = rol;
        this.correo = correo;
        this.telefono = telefono;
        this.activo = activo;
    }

    public int getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getUsuario() { return usuario; }
    public String getPassword() { return password; }
    public String getRol() { return rol; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public boolean isActivo() { return activo; }
}