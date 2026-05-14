package services.ordenes;

import entities.OrdenCompra;
import repositories.OrdenCompraRepository;

public class CancelarOrdenCompraUseCase {

    private final OrdenCompraRepository ordenRepository;

    public CancelarOrdenCompraUseCase(OrdenCompraRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    /**
     * Cancela una orden de compra existente. Una orden ya cancelada no
     * puede volver a cancelarse.
     */
    public boolean ejecutar(int id) {
        OrdenCompra orden = ordenRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe una orden de compra con el id: " + id));

        if (!orden.isActiva()) {
            throw new IllegalStateException(
                    "La orden " + id + " ya está cancelada");
        }

        return ordenRepository.cancelar(id);
    }
}