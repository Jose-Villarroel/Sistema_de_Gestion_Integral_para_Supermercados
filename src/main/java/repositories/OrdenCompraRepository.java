package repositories;

import entities.OrdenCompra;

import java.util.List;
import java.util.Optional;

public interface OrdenCompraRepository {

    Optional<OrdenCompra> buscarPorId(int id);

    List<OrdenCompra> listarTodas();

    List<OrdenCompra> listarActivas();

    List<OrdenCompra> listarPorProveedor(int idProveedor);

    OrdenCompra guardar(OrdenCompra orden);

    boolean cancelar(int id);
}