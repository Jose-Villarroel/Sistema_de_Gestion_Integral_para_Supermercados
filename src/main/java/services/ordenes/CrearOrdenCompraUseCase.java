package services.ordenes;

import entities.DetalleOrdenCompra;
import entities.OrdenCompra;
import repositories.OrdenCompraRepository;

import java.time.LocalDate;
import java.util.List;

public class CrearOrdenCompraUseCase {

    private final OrdenCompraRepository ordenRepository;

    public CrearOrdenCompraUseCase(OrdenCompraRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    /**
     * Crea una nueva orden de compra dirigida a un proveedor.
     * 
     * @param idProveedor   proveedor que surtirá la orden
     * @param idEmpleado    empleado que crea la orden
     * @param fechaEntrega  fecha estimada de entrega (puede ser null)
     * @param detalles      líneas de la orden (mínimo una)
     * @return la orden creada con su id asignado
     */
    public OrdenCompra ejecutar(int idProveedor, int idEmpleado,
                                LocalDate fechaEntrega,
                                List<DetalleOrdenCompra> detalles) {

        OrdenCompra orden = new OrdenCompra(
                0,
                idProveedor,
                idEmpleado,
                LocalDate.now(),
                fechaEntrega,
                true,
                detalles
        );

        return ordenRepository.guardar(orden);
    }
}