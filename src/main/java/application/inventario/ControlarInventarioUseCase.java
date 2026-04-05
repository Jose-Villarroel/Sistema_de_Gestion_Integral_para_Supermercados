package application.inventario;

import domain.model.MovimientoInventario;
import domain.model.MovimientoInventario.TipoMovimiento;
import domain.model.Producto;
import domain.repository.MovimientoInventarioRepository;
import domain.repository.ProductoRepository;

import java.time.LocalDateTime;
import java.util.List;


public class ControlarInventarioUseCase {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    // Inyección de dependencias, ninguna clase crea sus propias dependencias
    public ControlarInventarioUseCase(ProductoRepository productoRepository,
                                      MovimientoInventarioRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    /**
     * Registra una entrada de inventario
     * Suma la cantidad al stock actual del producto.
     */
    public int registrarEntrada(String codigoProducto, int cantidad,
                                String motivo, int empleadoId, int ordenId) {
        //Buscar el producto en el catálogo
        Producto producto = productoRepository.buscarPorCodigo(codigoProducto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado. Debe registrarlo primero en el catálogo"));

        //Validar que la cantidad sea positiva
        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad debe ser un número positivo");

        //Calcular nuevo stock
        int nuevoStock = producto.getStockActual() + cantidad;
        actualizarStock(producto, nuevoStock);

        //Registrar movimiento en el historial
        guardarMovimiento(TipoMovimiento.ENTRADA, cantidad, motivo,
                          producto.getId(), empleadoId, ordenId);

        return nuevoStock;
    }

    /**
     * Registra una salida de inventario.
     * Resta la cantidad al stock actual del producto.
     */
    public int registrarSalida(String codigoProducto, int cantidad,
                               String motivo, int empleadoId) {
        Producto producto = productoRepository.buscarPorCodigo(codigoProducto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado. Debe registrarlo primero en el catálogo"));

        if (cantidad <= 0)
            throw new IllegalArgumentException("La cantidad debe ser un número positivo");

        //No permitir stock negativo
        if (cantidad > producto.getStockActual())
            throw new IllegalArgumentException(
                    "Stock insuficiente. Stock actual: " + producto.getStockActual() + " unidades");

        int nuevoStock = producto.getStockActual() - cantidad;
        actualizarStock(producto, nuevoStock);

        guardarMovimiento(TipoMovimiento.SALIDA, cantidad, motivo,
                          producto.getId(), empleadoId, 0);

        return nuevoStock;
    }

    /**
     * Ajusta el stock a un valor exacto.
     * Calcula la diferencia y la registra como movimiento.
     */
    public int ajustarStock(String codigoProducto, int nuevoStock,
                            String motivo, int empleadoId) {
        Producto producto = productoRepository.buscarPorCodigo(codigoProducto)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado. Debe registrarlo primero en el catálogo"));

        if (nuevoStock < 0)
            throw new IllegalArgumentException("El stock no puede ser negativo");

        // La cantidad del movimiento es la diferencia absoluta
        int diferencia = Math.abs(nuevoStock - producto.getStockActual());

        // Si no hay diferencia no hay nada que ajustar
        if (diferencia == 0)
            throw new IllegalArgumentException("El stock ingresado es igual al stock actual");

        TipoMovimiento tipo;
        if (nuevoStock > producto.getStockActual()) {
            tipo = TipoMovimiento.ENTRADA;
        } else {
            tipo = TipoMovimiento.AJUSTE;
        }

        actualizarStock(producto, nuevoStock);
        guardarMovimiento(tipo, diferencia, motivo, producto.getId(), empleadoId, 0);

        return nuevoStock;
    }

    /**
     * Retorna todos los productos con stock por debajo del mínimo.
     * Usado para generar alertas de reposición.
     */
    public List<Producto> obtenerAlertasStockBajo() {
        return productoRepository.listarConStockBajo();
    }

    /**
     * Retorna el historial completo de movimientos de un producto.
     */
    public List<MovimientoInventario> consultarMovimientos(int productoId) {
        return movimientoRepository.listarPorProducto(productoId);
    }

    //Métodos privados de apoyo 

    // Actualiza el stock del producto en la base de datos
    private void actualizarStock(Producto producto, int nuevoStock) {
        producto.actualizarDatos(
                producto.getNombre(), producto.getDescripcion(),
                producto.getPrecioCompra(), producto.getPrecioVenta(),
                producto.getStockMinimo(), producto.getStockMaximo(),
                producto.getCategoriaId(), producto.getProveedorId()
        );
        // Actualizamos directamente el stock via SQL en el repositorio
        productoRepository.actualizar(producto);
    }

    // Crea y persiste el registro del movimiento
    private void guardarMovimiento(TipoMovimiento tipo, int cantidad, String motivo,
                                   int productoId, int empleadoId, int ordenId) {
        MovimientoInventario movimiento = new MovimientoInventario(
                0, LocalDateTime.now(), tipo, cantidad,
                motivo, productoId, empleadoId, ordenId
        );
        movimientoRepository.guardar(movimiento);
    }
}