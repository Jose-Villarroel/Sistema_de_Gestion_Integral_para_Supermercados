package repositories;

import entities.CuentaFidelizacion;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2CuentaFidelizacionRepository implements CuentaFidelizacionRepository {

    private final DatabaseConnection dbConnection;

    public H2CuentaFidelizacionRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<CuentaFidelizacion> buscarPorId(int id) {
        String sql = "SELECT * FROM Cuenta_fidelizacion WHERE id_fidelizacion = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearCuenta(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar cuenta de fidelización por id", e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<CuentaFidelizacion> buscarPorCliente(int idCliente) {
        String sql = "SELECT * FROM Cuenta_fidelizacion WHERE id_cliente = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearCuenta(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar cuenta por cliente", e);
        }

        return Optional.empty();
    }

    @Override
    public List<CuentaFidelizacion> listarTodas() {
        List<CuentaFidelizacion> cuentas = new ArrayList<>();
        String sql = "SELECT * FROM Cuenta_fidelizacion ORDER BY id_fidelizacion";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                cuentas.add(mapearCuenta(rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar cuentas de fidelización", e);
        }

        return cuentas;
    }

    @Override
    public CuentaFidelizacion guardar(CuentaFidelizacion cuenta) {
        String sql = """
            INSERT INTO Cuenta_fidelizacion
            (id_cliente, numero_tarjeta, puntos_actuales, fecha_creacion, estado)
            VALUES (?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, cuenta.getIdCliente());
            stmt.setInt(2, cuenta.getNumeroTarjeta());
            stmt.setInt(3, cuenta.getPuntosActuales());
            stmt.setDate(4, Date.valueOf(cuenta.getFechaCreacion()));
            stmt.setBoolean(5, cuenta.isActiva());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return new CuentaFidelizacion(
                        rs.getInt(1),
                        cuenta.getIdCliente(),
                        cuenta.getNumeroTarjeta(),
                        cuenta.getPuntosActuales(),
                        cuenta.getFechaCreacion(),
                        cuenta.isActiva()
                );
            }

            throw new RuntimeException("No se pudo obtener el id de la cuenta guardada");

        } catch (Exception e) {
            throw new RuntimeException("Error al guardar cuenta de fidelización", e);
        }
    }

    @Override
    public boolean actualizarPuntos(int idCuenta, int nuevosPuntos) {
        String sql = "UPDATE Cuenta_fidelizacion SET puntos_actuales = ? WHERE id_fidelizacion = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, nuevosPuntos);
            stmt.setInt(2, idCuenta);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar puntos", e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE Cuenta_fidelizacion SET estado = FALSE WHERE id_fidelizacion = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al desactivar cuenta", e);
        }
    }

    private CuentaFidelizacion mapearCuenta(ResultSet rs) throws Exception {
        return new CuentaFidelizacion(
                rs.getInt("id_fidelizacion"),
                rs.getInt("id_cliente"),
                rs.getInt("numero_tarjeta"),
                rs.getInt("puntos_actuales"),
                rs.getDate("fecha_creacion").toLocalDate(),
                rs.getBoolean("estado")
        );
    }

    @Override
    public void acreditarPuntos(Connection conn, int idCuenta, int idVenta, int puntos) throws SQLException {
        String sqlCuenta = """
            UPDATE Cuenta_fidelizacion
            SET puntos_actuales = puntos_actuales + ?
            WHERE id_fidelizacion = ?
        """;
        String sqlMovimiento = """
            INSERT INTO Movimiento_puntos
            (id_tipo_movimiento_puntos, id_venta, puntos, fecha_movimiento)
            VALUES (?, ?, ?, ?)
        """;
        String sqlRelacion = """
            INSERT INTO CuentaXMovimiento
            (id_movimiento, id_cuenta_fidelizacion)
            VALUES (?, ?)
        """;

        try (PreparedStatement stmtCuenta = conn.prepareStatement(sqlCuenta);
             PreparedStatement stmtMovimiento = conn.prepareStatement(sqlMovimiento, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtRelacion = conn.prepareStatement(sqlRelacion)) {

            stmtCuenta.setInt(1, puntos);
            stmtCuenta.setInt(2, idCuenta);
            stmtCuenta.executeUpdate();

            stmtMovimiento.setInt(1, 1);
            stmtMovimiento.setInt(2, idVenta);
            stmtMovimiento.setInt(3, puntos);
            stmtMovimiento.setDate(4, Date.valueOf(java.time.LocalDate.now()));
            stmtMovimiento.executeUpdate();

            ResultSet rs = stmtMovimiento.getGeneratedKeys();
            if (rs.next()) {
                stmtRelacion.setInt(1, rs.getInt(1));
                stmtRelacion.setInt(2, idCuenta);
                stmtRelacion.executeUpdate();
            }
        }
    }

    @Override
    public void descontarPuntosSiAplica(Connection conn, int idVenta) throws SQLException {
        String sqlBuscar = """
            SELECT cxm.id_cuenta_fidelizacion, mp.puntos
            FROM Movimiento_puntos mp
            JOIN CuentaXMovimiento cxm ON cxm.id_movimiento = mp.id_movimiento
            WHERE mp.id_venta = ?
              AND mp.id_tipo_movimiento_puntos = 1
        """;

        String sqlActualizar = """
            UPDATE Cuenta_fidelizacion
            SET puntos_actuales = CASE
                WHEN puntos_actuales >= ? THEN puntos_actuales - ?
                ELSE 0
            END
            WHERE id_fidelizacion = ?
        """;

        try (PreparedStatement stmtBuscar = conn.prepareStatement(sqlBuscar)) {
            stmtBuscar.setInt(1, idVenta);

            ResultSet rs = stmtBuscar.executeQuery();

            while (rs.next()) {
                int idCuenta = rs.getInt("id_cuenta_fidelizacion");
                int puntos = rs.getInt("puntos");

                try (PreparedStatement stmtActualizar = conn.prepareStatement(sqlActualizar)) {
                    stmtActualizar.setInt(1, puntos);
                    stmtActualizar.setInt(2, puntos);
                    stmtActualizar.setInt(3, idCuenta);

                    stmtActualizar.executeUpdate();
                }
            }
        }
    }
}
