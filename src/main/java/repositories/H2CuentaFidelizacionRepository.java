package repositories;

import entities.CuentaFidelizacion;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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
}