package repositories;

import dtos.ResumenCierreCajaDTO;
import entities.CierreCaja;

import java.sql.SQLException;
import java.time.LocalDate;

public interface CierreCajaRepository {

    boolean existeCierre(LocalDate fecha, String turno) throws SQLException;

    ResumenCierreCajaDTO obtenerResumenTurno(LocalDate fecha, String turno) throws SQLException;

    void guardar(CierreCaja cierreCaja) throws SQLException;
}