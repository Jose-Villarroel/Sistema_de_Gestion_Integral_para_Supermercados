package services.clientes;

import entities.CuentaFidelizacion;
import repositories.CuentaFidelizacionRepository;
import valueobjects.CategoriaFidelidad;

public class GestionarPuntosUseCase {

    // Regla de negocio: 1 punto por cada $1.000 pesos de compra
    private static final double PESOS_POR_PUNTO = 1000.0;

    private final CuentaFidelizacionRepository cuentaRepository;

    public GestionarPuntosUseCase(CuentaFidelizacionRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    /**
     * Acumula puntos en la cuenta del cliente según el monto de la compra.
     * Lo llamará el CU-005 (Procesar venta) al finalizar una venta.
     */
    public int acumularPorCompra(int idCliente, double montoCompra) {
        if (montoCompra <= 0) {
            throw new IllegalArgumentException("El monto de compra debe ser positivo");
        }

        CuentaFidelizacion cuenta = cuentaRepository.buscarPorCliente(idCliente)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El cliente no tiene cuenta de fidelización: " + idCliente));

        int puntosGanados = (int) (montoCompra / PESOS_POR_PUNTO);
        if (puntosGanados <= 0) {
            return 0;
        }

        cuenta.acumularPuntos(puntosGanados);
        cuentaRepository.actualizarPuntos(cuenta.getId(), cuenta.getPuntosActuales());

        return puntosGanados;
    }

    /**
     * Canjea puntos de la cuenta del cliente.
     */
    public void canjearPuntos(int idCliente, int puntosACanjear) {
        CuentaFidelizacion cuenta = cuentaRepository.buscarPorCliente(idCliente)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El cliente no tiene cuenta de fidelización: " + idCliente));

        cuenta.canjearPuntos(puntosACanjear);
        cuentaRepository.actualizarPuntos(cuenta.getId(), cuenta.getPuntosActuales());
    }

    /**
     * Consulta los puntos actuales del cliente.
     */
    public int consultarPuntos(int idCliente) {
        CuentaFidelizacion cuenta = cuentaRepository.buscarPorCliente(idCliente)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El cliente no tiene cuenta de fidelización: " + idCliente));
        return cuenta.getPuntosActuales();
    }

    /**
     * Consulta la categoría de fidelidad del cliente según sus puntos acumulados.
     */
    public CategoriaFidelidad consultarCategoria(int idCliente) {
        CuentaFidelizacion cuenta = cuentaRepository.buscarPorCliente(idCliente)
                .orElseThrow(() -> new IllegalArgumentException(
                        "El cliente no tiene cuenta de fidelización: " + idCliente));
        return cuenta.getCategoria();
    }
}