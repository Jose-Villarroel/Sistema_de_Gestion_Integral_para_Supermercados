package valueobjects;

//se cambio enum para que quede alineado con el diagrama entidad relacion

public class Rol {
    private final int idRol;
    private final String nombreRol;
    private final String descripcion;

    //constructor
    public Rol(int idRol, String nombreRol, String descripcion) {
        if(nombreRol == null || nombreRol.isBlank()) {
            throw new IllegalArgumentException("El nombre del rol es obligatorio.");
        }
        this.idRol = idRol;
        this.nombreRol = nombreRol;
        this.descripcion = descripcion;
    }
    //getters
    public int getIdRol() {
        return idRol;
    }

    public String getNombreRol() {
        return nombreRol;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
