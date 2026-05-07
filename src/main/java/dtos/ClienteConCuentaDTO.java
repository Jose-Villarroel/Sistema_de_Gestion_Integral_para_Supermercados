package dtos;

import entities.Cliente;

public record ClienteConCuentaDTO(Cliente cliente, Integer cuentaId, Integer numeroTarjeta, Integer puntosActuales) {
}
