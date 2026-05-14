package services.ordenes;

import entities.OrdenCompra;
import repositories.OrdenCompraRepository;

import java.util.List;
import java.util.Optional;

public class ConsultarOrdenCompraUseCase {

    private final OrdenCompraRepository ordenRepository;

    public ConsultarOrdenCompraUseCase(OrdenCompraRepository ordenRepository) {
        this.ordenRepository = ordenRepository;
    }

    public Optional<OrdenCompra> porId(int id) {
        return ordenRepository.buscarPorId(id);
    }

    public List<OrdenCompra> listarTodas() {
        return ordenRepository.listarTodas();
    }

    public List<OrdenCompra> listarActivas() {
        return ordenRepository.listarActivas();
    }

    public List<OrdenCompra> listarPorProveedor(int idProveedor) {
        if (idProveedor <= 0) {
            throw new IllegalArgumentException("El proveedor debe ser válido");
        }
        return ordenRepository.listarPorProveedor(idProveedor);
    }
}