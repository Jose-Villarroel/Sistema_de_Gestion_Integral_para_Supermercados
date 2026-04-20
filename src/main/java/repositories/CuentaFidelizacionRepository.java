package repositories;

import entities.CuentaFidelizacion;

import java.util.List;
import java.util.Optional;

public interface CuentaFidelizacionRepository {

    Optional<CuentaFidelizacion> buscarPorId(int id);

    Optional<CuentaFidelizacion> buscarPorCliente(int idCliente);

    List<CuentaFidelizacion> listarTodas();

    CuentaFidelizacion guardar(CuentaFidelizacion cuenta);

    boolean actualizarPuntos(int idCuenta, int nuevosPuntos);

    boolean desactivar(int id);
}