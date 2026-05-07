package dtos;

import java.time.LocalDate;

public record VentaInfoDTO(int idVenta, int idEmpleado, LocalDate fechaVenta, String turno) {
}
