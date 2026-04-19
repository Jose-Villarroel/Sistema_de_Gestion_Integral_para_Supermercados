package aggregates;

import java.time.LocalDate;

public class Producto {

    private final int id;
    private int categoriaId;
    private String nombre;
    private String descripcion;
    private String marca;
    private double precioCompra;
    private double precioVenta;
    private int stockActual;
    private int stockMinimo;
    private boolean activo;
    private LocalDate fechaRegistro;

    // Constructor completo (desde BD)
    public Producto(int id, int categoriaId, String nombre, String descripcion,
                    String marca, double precioCompra, double precioVenta,
                    int stockActual, int stockMinimo,
                    boolean activo, LocalDate fechaRegistro) {

        this.id = id;
        this.categoriaId = categoriaId;
        this.nombre = validarNombre(nombre);
        this.descripcion = descripcion;
        this.marca = marca;
        this.precioCompra = validarPrecioCompra(precioCompra);
        this.precioVenta = validarPrecioVenta(precioVenta, precioCompra);
        this.stockActual = validarStock(stockActual);
        this.stockMinimo = validarStock(stockMinimo);
        this.activo = activo;
        this.fechaRegistro = fechaRegistro;
    }

    // Constructor para nuevo producto
    public Producto(int categoriaId, String nombre, String descripcion,
                    String marca, double precioCompra, double precioVenta,
                    int stockActual, int stockMinimo, boolean activo, LocalDate fechaRegistro) {

        this(0, categoriaId, nombre, descripcion, marca, precioCompra,
                precioVenta, stockActual, stockMinimo, activo, fechaRegistro);
    }

    // Validaciones
    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío");
        }
        return nombre.trim();
    }

    private double validarPrecioCompra(double precio) {
        if (precio <= 0) {
            throw new IllegalArgumentException("El precio de compra debe ser mayor a cero");
        }
        return precio;
    }

    private double validarPrecioVenta(double precioVenta, double precioCompra) {
        if (precioVenta <= precioCompra) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor al de compra");
        }
        return precioVenta;
    }

    private int validarStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        return stock;
    }

    // Métodos de negocio
    public boolean tieneStockBajo() {
        return stockActual < stockMinimo;
    }

    public void actualizarDatos(String nombre, String descripcion, String marca,
                                double precioCompra, double precioVenta,
                                int stockMinimo, int categoriaId, boolean activo) {

        this.nombre = validarNombre(nombre);
        this.descripcion = descripcion;
        this.marca = marca;
        this.precioCompra = validarPrecioCompra(precioCompra);
        this.precioVenta = validarPrecioVenta(precioVenta, precioCompra);
        this.stockMinimo = validarStock(stockMinimo);
        this.categoriaId = categoriaId;
        this.activo = activo;
    }

    public void setStockActual(int stockActual) {
        this.stockActual = validarStock(stockActual);
    }

    public int getId() { return id; }
    public int getCategoriaId() { return categoriaId; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public String getMarca() { return marca; }
    public double getPrecioCompra() { return precioCompra; }
    public double getPrecioVenta() { return precioVenta; }
    public int getStockActual() { return stockActual; }
    public int getStockMinimo() { return stockMinimo; }
    public boolean isActivo() { return activo; }
    public LocalDate getFechaRegistro() { return fechaRegistro; }

    @Override
    public String toString() {
        return String.format("Producto[%d - %s | $%.2f | Stock: %d]",
                id, nombre, precioVenta, stockActual);
    }
}