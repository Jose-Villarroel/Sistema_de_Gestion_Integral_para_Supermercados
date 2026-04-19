package entities;

import aggregates.Empleado;
import valueobjects.Rol;

import java.time.LocalDate;
import java.time.LocalTime;

public class Usuario {
    private final int id;
    private final String username;
    private final String passwordHash;
    private final Rol rol;
    private final Empleado empleado;

    private int intentosFallidos;
    private LocalTime bloqueadoHasta;
    private LocalDate ultimoAcceso;
    private boolean estadoUsuario;

    // Constructor para CREAR usuario
    public Usuario(int id, String username, String passwordPlano,
                   Rol rol, Empleado empleado, boolean estadoUsuario) {

        this.id = id;
        this.username = username;
        this.passwordHash = generarHash(passwordPlano);
        this.rol = rol;
        this.empleado = empleado;
        this.estadoUsuario = estadoUsuario;
    }

    // Constructor para LEER usuario de BD
    public Usuario(int id, String username, String passwordHash,
                   Rol rol, Empleado empleado,
                   int intentosFallidos, LocalTime bloqueadoHasta,
                   LocalDate ultimoAcceso, boolean estadoUsuario) {

        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.empleado = empleado;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
        this.ultimoAcceso = ultimoAcceso;
        this.estadoUsuario = estadoUsuario;
    }

    private String generarHash(String password) {
        return String.valueOf(password.hashCode());
    }

    public boolean passwordCoincide(String passwordPlano) {
        return this.passwordHash.equals(generarHash(passwordPlano));
    }

    public boolean estaBloqueado() {
        if (bloqueadoHasta == null) return false;
        return LocalTime.now().isBefore(bloqueadoHasta);
    }

    public void incrementarIntentosFallidos() {
        this.intentosFallidos++;
    }

    public void reiniciarIntentosFallidos() {
        this.intentosFallidos = 0;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public Empleado getEmpleado() {
        return empleado;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public void setIntentosFallidos(int intentosFallidos) {
        this.intentosFallidos = intentosFallidos;
    }

    public LocalTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public LocalDate getUltimoAcceso() {
        return ultimoAcceso;
    }

    public void setUltimoAcceso(LocalDate ultimoAcceso) {
        this.ultimoAcceso = ultimoAcceso;
    }

    public boolean isEstadoUsuario() {
        return estadoUsuario;
    }

    public void setEstadoUsuario(boolean estadoUsuario) {
        this.estadoUsuario = estadoUsuario;
    }
}