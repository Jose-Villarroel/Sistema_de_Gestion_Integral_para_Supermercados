package services.inventario;

import aggregates.Producto;
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

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser un número positivo");
        }

        int stockAnterior = producto.getStockActual();
        int stockNuevo = stockAnterior + cantidad;

        actualizarStock(producto, stockNuevo);

        guardarMovimiento(
                producto.getId(),
                empleadoId,
                tipoMovimientoId,
                cantidad,
                stockAnterior,
                stockNuevo,
                motivo
        );

        return stockNuevo;
    }

    public int registrarSalida(int productoId, int cantidad,
                               String motivo, int empleadoId, int tipoMovimientoId) {

        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado. Debe registrarlo primero en el catálogo"));

        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser un número positivo");
        }

        int stockAnterior = producto.getStockActual();

        if (cantidad > stockAnterior) {
            throw new IllegalArgumentException(
                    "Stock insuficiente. Stock actual: " + stockAnterior + " unidades");
        }

        int stockNuevo = stockAnterior - cantidad;

        actualizarStock(producto, stockNuevo);

        guardarMovimiento(
                producto.getId(),
                empleadoId,
                tipoMovimientoId,
                cantidad,
                stockAnterior,
                stockNuevo,
                motivo
        );

        return stockNuevo;
    }

    public int ajustarStock(int productoId, int nuevoStock,
                            String motivo, int empleadoId, int tipoMovimientoId) {

        Producto producto = productoRepository.buscarPorId(productoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Producto no encontrado. Debe registrarlo primero en el catálogo"));

        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }

        int stockAnterior = producto.getStockActual();
        int diferencia = Math.abs(nuevoStock - stockAnterior);

        if (diferencia == 0) {
            throw new IllegalArgumentException("El stock ingresado es igual al stock actual");
        }

        actualizarStock(producto, nuevoStock);

        guardarMovimiento(
                producto.getId(),
                empleadoId,
                tipoMovimientoId,
                diferencia,
                stockAnterior,
                nuevoStock,
                motivo
        );

        return nuevoStock;
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

    private void guardarMovimiento(int productoId,
                                   int empleadoId,
                                   int tipoMovimientoId,
                                   int cantidad,
                                   int stockAnterior,
                                   int stockNuevo,
                                   String motivo) {

        MovimientoInventario movimiento = new MovimientoInventario(
                0,
                empleadoId,
                tipoMovimientoId,
                productoId,
                cantidad,
                stockAnterior,
                stockNuevo,
                motivo,
                LocalDate.now()
        );

        movimientoRepository.guardar(movimiento);
    }
}