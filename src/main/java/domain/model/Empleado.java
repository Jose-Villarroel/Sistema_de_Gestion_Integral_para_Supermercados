package domain.model;

import java.time.LocalDate;

public class Empleado {

    private final int id;
    private final String nombre;
    private final String apellido;
    private final String correo;
    private final String telefono;
    private final LocalDate fechaRegistro;
    private boolean activo;


    //constructor se verifica que el nombre no sea null ni

    public Empleado(int id, String nombre, String apellido, String correo, String telefono, LocalDate fechaRegistro, boolean activo) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        this.id = id;
        this.nombre = nombre;
        this.apellido = apellido;
        this.correo = correo;
        this.telefono = telefono;
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
    }

    public void desactivar(){
        this.activo = false;
    }
    //getters y setters

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}