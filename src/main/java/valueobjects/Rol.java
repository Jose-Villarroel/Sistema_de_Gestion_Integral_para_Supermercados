package valueobjects;


public enum Rol {   //Se usa enum porque le estamos diciendo que un rol puede ser una de estas 4 opciones
    ADMINISTRADOR,
    CAJERO,
    SUPERVISOR_INVENTARIO,
    GERENTE;
    
    // Este metodo convierte el texto por ejemplo "CAJERO" a un valor del enum
    public static Rol fromString(String valor) { 
        return switch (valor.toUpperCase()) {
            case "ADMIN", "ADMINISTRADOR" -> ADMINISTRADOR;
            case "CAJERO" -> CAJERO;
            case "SUPERVISOR", "SUPERVISOR_INVENTARIO" -> SUPERVISOR_INVENTARIO;
            case "GERENTE" -> GERENTE;
            //Si no coincide el texto que viene de la base de datos, el sistema lanza un error explicando cuál fue el valor inválido
            default -> throw new IllegalArgumentException("Rol no válido: " + valor); 
        };
    }
}
