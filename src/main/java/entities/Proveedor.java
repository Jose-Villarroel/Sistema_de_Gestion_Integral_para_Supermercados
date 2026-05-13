package entities;

import java.time.LocalDate;

public class Proveedor {

    private final int id;
    private final String nombre;
    private final String correo;
    private final String telefono;
    private final String direccion;
    private final LocalDate fechaRegistro;
    private boolean activo;

    public Proveedor(int id, String nombre, String correo, String telefono,
                     String direccion, LocalDate fechaRegistro, boolean activo) {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        if (correo != null && !correo.isBlank() && !correo.contains("@")) {
            throw new IllegalArgumentException("El correo no tiene un formato válido");
        }

        this.id = id;
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.direccion = direccion;
        this.fechaRegistro = fechaRegistro;
        this.activo = activo;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    // Getters
    public int getId() { return id; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }
    public String getTelefono() { return telefono; }
    public String getDireccion() { return direccion; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public boolean isActivo() { return activo; }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}