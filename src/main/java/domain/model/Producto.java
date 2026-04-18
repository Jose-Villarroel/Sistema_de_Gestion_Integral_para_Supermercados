package domain.model;

public class Producto {
    private final int id;
    private final String codigo;
    private String nombre;
    private String descripcion;
    private double precioCompra;
    private double precioVenta;
    private int stockActual;
    private int stockMinimo;
    private int stockMaximo;
    private int categoriaId;
    private int proveedorId;
    private boolean activo;

    // Constructor completo (desde BD)
    public Producto(int id, String codigo, String nombre, String descripcion,
                    double precioCompra, double precioVenta, int stockActual,
                    int stockMinimo, int stockMaximo, int categoriaId,
                    int proveedorId, boolean activo) {
        this.id = id;
        this.codigo = validarCodigo(codigo);
        this.nombre = validarNombre(nombre);
        this.descripcion = descripcion;
        this.precioCompra = validarPrecioCompra(precioCompra);
        this.precioVenta = validarPrecioVenta(precioVenta, precioCompra);
        this.stockActual = stockActual;
        this.stockMinimo = validarStock(stockMinimo);
        this.stockMaximo = validarStockMaximo(stockMaximo, stockMinimo);
        this.categoriaId = categoriaId;
        this.proveedorId = proveedorId;
        this.activo = activo;
    }

    // Constructor para nuevo producto (sin id, sin stock actual)
    public Producto(String codigo, String nombre, String descripcion,
                    double precioCompra, double precioVenta, int stockMinimo,
                    int stockMaximo, int categoriaId, int proveedorId) {
        this(0, codigo, nombre, descripcion, precioCompra, precioVenta,
             0, stockMinimo, stockMaximo, categoriaId, proveedorId, true);
    }

    // Validaciones
    private String validarCodigo(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new IllegalArgumentException("El código del producto no puede estar vacío");
        }
        if (codigo.length() > 20) {
            throw new IllegalArgumentException("El código no puede exceder 20 caracteres");
        }
        return codigo.trim().toUpperCase();
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío");
        }
        if (nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre no puede exceder 100 caracteres");
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
        if (precioVenta <= 0) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor a cero");
        }
        if (precioVenta <= precioCompra) {
            throw new IllegalArgumentException("El precio de venta debe ser mayor al precio de compra");
        }
        return precioVenta;
    }

    private int validarStock(int stock) {
        if (stock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        return stock;
    }

    private int validarStockMaximo(int stockMaximo, int stockMinimo) {
        if (stockMaximo < 0) {
            throw new IllegalArgumentException("El stock máximo no puede ser negativo");
        }
        if (stockMaximo > 0 && stockMaximo < stockMinimo) {
            throw new IllegalArgumentException("El stock máximo no puede ser menor al stock mínimo");
        }
        return stockMaximo;
    }

    // Métodos de negocio
    public double calcularMargenGanancia() {
        return ((precioVenta - precioCompra) / precioCompra) * 100;
    }

    public boolean tieneStockBajo() {
        return stockActual < stockMinimo;
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    public void actualizarDatos(String nombre, String descripcion, double precioCompra,
                                double precioVenta, int stockMinimo, int stockMaximo,
                                int categoriaId, int proveedorId) {
        this.nombre = validarNombre(nombre);
        this.descripcion = descripcion;
        this.precioCompra = validarPrecioCompra(precioCompra);
        this.precioVenta = validarPrecioVenta(precioVenta, precioCompra);
        this.stockMinimo = validarStock(stockMinimo);
        this.stockMaximo = validarStockMaximo(stockMaximo, stockMinimo);
        this.categoriaId = categoriaId;
        this.proveedorId = proveedorId;
    }

    // Getters
    public int getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getDescripcion() { return descripcion; }
    public double getPrecioCompra() { return precioCompra; }
    public double getPrecioVenta() { return precioVenta; }
    public int getStockActual() { return stockActual; }
    public int getStockMinimo() { return stockMinimo; }
    public int getStockMaximo() { return stockMaximo; }
    public int getCategoriaId() { return categoriaId; }
    public int getProveedorId() { return proveedorId; }
    public boolean isActivo() { return activo; }

    //Permite actualizar el stock después de un movimiento de inventario
    public void setStockActual(int stockActual) {
        this.stockActual = validarStock(stockActual);
    }

    @Override
    public String toString() {
        return String.format("Producto[%s - %s | $%.2f | Stock: %d]",
                codigo, nombre, precioVenta, stockActual);
    }
}