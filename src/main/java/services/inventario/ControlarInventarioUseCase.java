package services.inventario;

import entities.Producto;
import entities.MovimientoInventario;
import repositories.MovimientoInventarioRepository;
import repositories.ProductoRepository;

import java.time.LocalDate;
import java.util.List;

public class ControlarInventarioUseCase {

    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoRepository;

    public ControlarInventarioUseCase(ProductoRepository productoRepository,
                                      MovimientoInventarioRepository movimientoRepository) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
    }

    public int registrarEntrada(int productoId, int cantidad,
                                String motivo, int empleadoId, int tipoMovimientoId) {

        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado. Debe registrarlo primero en el catálogo"));

        int stockAnterior = producto.getStockActual();
        int stockNuevo = stockAnterior + cantidad;

        // Validar e instanciar movimiento de inventario primero (falla temprano si hay error)
        MovimientoInventario movimiento = new MovimientoInventario(
                0,
                empleadoId,
                tipoMovimientoId,
                producto.getId(),
                cantidad,
                stockAnterior,
                stockNuevo,
                motivo,
                LocalDate.now()
        );

        actualizarStock(producto, stockNuevo);
        movimientoRepository.guardar(movimiento);

        return stockNuevo;
    }

    public int registrarSalida(int productoId, int cantidad,
                               String motivo, int empleadoId, int tipoMovimientoId) {

        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado. Debe registrarlo primero en el catálogo"));

        int stockAnterior = producto.getStockActual();

        if (cantidad > stockAnterior) {
            throw new IllegalArgumentException(
                    "Stock insuficiente. Stock actual: " + stockAnterior + " unidades");
        }

        int stockNuevo = stockAnterior - cantidad;

        // Validar e instanciar movimiento primero
        MovimientoInventario movimiento = new MovimientoInventario(
                0,
                empleadoId,
                tipoMovimientoId,
                producto.getId(),
                cantidad,
                stockAnterior,
                stockNuevo,
                motivo,
                LocalDate.now()
        );

        actualizarStock(producto, stockNuevo);
        movimientoRepository.guardar(movimiento);

        return stockNuevo;
    }

    public int ajustarStock(int productoId, int nuevoStock,
                            String motivo, int empleadoId, int tipoMovimientoId) {

        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado. Debe registrarlo primero en el catálogo"));

        int stockAnterior = producto.getStockActual();
        int diferencia = Math.abs(nuevoStock - stockAnterior);

        if (diferencia == 0) {
            throw new IllegalArgumentException("El stock ingresado es igual al stock actual");
        }

        int stockNuevo = nuevoStock;

        // Validar e instanciar movimiento primero
        MovimientoInventario movimiento = new MovimientoInventario(
                0,
                empleadoId,
                tipoMovimientoId,
                producto.getId(),
                diferencia,
                stockAnterior,
                stockNuevo,
                motivo,
                LocalDate.now()
        );

        actualizarStock(producto, stockNuevo);
        movimientoRepository.guardar(movimiento);

        return stockNuevo;
    }

    public List<Producto> obtenerAlertasStockBajo() {
        return productoRepository.listarConStockBajo();
    }

    public List<MovimientoInventario> consultarMovimientos(int productoId) {
        return movimientoRepository.listarPorProducto(productoId);
    }

    private void actualizarStock(Producto producto, int nuevoStock) {
        producto.setStockActual(nuevoStock);
        productoRepository.actualizar(producto);
    }
}