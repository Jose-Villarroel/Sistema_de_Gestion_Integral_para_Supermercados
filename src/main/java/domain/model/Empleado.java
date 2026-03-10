package domain.model;

public class Empleado {
    private int id;
    private String usuario;
    private String password;
    private String rol;
    private boolean activo;

    public Empleado(int id, String usuario, String password, String rol, boolean activo) {
        this.id = id;
        this.usuario = usuario;
        this.password = password;
        this.rol = rol;
        this.activo = activo;
    }

    public int getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getPassword() { return password; }
    public String getRol() { return rol; }
    public boolean isActivo() { return activo; }
}