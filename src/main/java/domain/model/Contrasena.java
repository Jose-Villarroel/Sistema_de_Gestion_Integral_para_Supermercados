package domain.model;

public class Contrasena {
    private final String valor;

    public Contrasena(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        if (valor.length() < 3) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 3 caracteres");
        }
        this.valor = valor;
    }

    public String getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; //Pregunta si son el mismo objeto en memoria
        if (!(o instanceof Contrasena)) return false; // Si "o" no es una Contrasena, retorna false. Esto evita comparar una contraseña con un objeto de otro tipo
        Contrasena that = (Contrasena) o; //Casteamos a Contrasena porque anteriormente se manejaba como un Object
        return valor.equals(that.valor);
    }
    //Si 2 objetos son iguales, deben tener el mismo hashcode
    @Override
    public int hashCode() {
        return valor.hashCode();
    }
}