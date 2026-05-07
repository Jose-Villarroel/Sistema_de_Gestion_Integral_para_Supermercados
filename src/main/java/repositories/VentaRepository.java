package repositories;

import dtos.DetalleVentaRetornableDTO;
import dtos.PuntosVentaDTO;
import dtos.VentaInfoDTO;

import java.util.List;
import java.util.Optional;

public interface VentaRepository {

    Optional<VentaInfoDTO> buscarVentaActivaPorId(int idVenta);

    List<DetalleVentaRetornableDTO> obtenerDetallesRetornables(int idVenta);

    List<PuntosVentaDTO> obtenerPuntosOtorgadosPorVenta(int idVenta);
}
