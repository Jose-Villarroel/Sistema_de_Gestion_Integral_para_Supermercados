package repositories;

import entities.DetalleOrdenCompra;
import entities.OrdenCompra;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2OrdenCompraRepository implements OrdenCompraRepository {

    private final DatabaseConnection dbConnection;

    public H2OrdenCompraRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Optional<OrdenCompra> buscarPorId(int id) {
        String sql = "SELECT * FROM Orden_compra WHERE id_orden_compra = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearOrden(conn, rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al buscar orden de compra por id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<OrdenCompra> listarTodas() {
        List<OrdenCompra> ordenes = new ArrayList<>();
        String sql = "SELECT * FROM Orden_compra ORDER BY fecha_creacion DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ordenes.add(mapearOrden(conn, rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar órdenes de compra", e);
        }

        return ordenes;
    }

    @Override
    public List<OrdenCompra> listarActivas() {
        List<OrdenCompra> ordenes = new ArrayList<>();
        String sql = "SELECT * FROM Orden_compra WHERE estado = TRUE ORDER BY fecha_creacion DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ordenes.add(mapearOrden(conn, rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar órdenes activas", e);
        }

        return ordenes;
    }

    @Override
    public List<OrdenCompra> listarPorProveedor(int idProveedor) {
        List<OrdenCompra> ordenes = new ArrayList<>();
        String sql = "SELECT * FROM Orden_compra WHERE id_proveedor = ? ORDER BY fecha_creacion DESC";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idProveedor);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ordenes.add(mapearOrden(conn, rs));
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al listar órdenes por proveedor", e);
        }

        return ordenes;
    }

    @Override
    public OrdenCompra guardar(OrdenCompra orden) {
        String sqlOrden = """
            INSERT INTO Orden_compra
            (id_proveedor, id_empleado, fecha_creacion, fecha_entrega, total, estado)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        String sqlDetalle = """
            INSERT INTO Detalle_orden_compra
            (id_orden_compra, id_producto, cantidad, costo_unitario, subtotal)
            VALUES (?, ?, ?, ?, ?)
        """;

        Connection conn = null;
        try {
            conn = dbConnection.getConnection();
            conn.setAutoCommit(false);

            // 1. Insertar la cabecera de la orden
            int idGenerado;
            try (PreparedStatement stmt = conn.prepareStatement(sqlOrden, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, orden.getIdProveedor());
                stmt.setInt(2, orden.getIdEmpleado());
                stmt.setDate(3, Date.valueOf(orden.getFechaCreacion()));
                stmt.setDate(4, orden.getFechaEntrega() != null ? Date.valueOf(orden.getFechaEntrega()) : null);
                stmt.setBigDecimal(5, orden.getTotal());
                stmt.setBoolean(6, orden.isActiva());

                stmt.executeUpdate();

                ResultSet rs = stmt.getGeneratedKeys();
                if (!rs.next()) {
                    throw new RuntimeException("No se pudo obtener el id de la orden");
                }
                idGenerado = rs.getInt(1);
            }

            // 2. Insertar los detalles
            try (PreparedStatement stmt = conn.prepareStatement(sqlDetalle)) {
                for (DetalleOrdenCompra detalle : orden.getDetalles()) {
                    stmt.setInt(1, idGenerado);
                    stmt.setInt(2, detalle.getIdProducto());
                    stmt.setInt(3, detalle.getCantidad());
                    stmt.setBigDecimal(4, detalle.getCostoUnitario());
                    stmt.setBigDecimal(5, detalle.getSubtotal());
                    stmt.addBatch();
                }
                stmt.executeBatch();
            }

            conn.commit();

            // 3. Reconstruir la orden con el id real
            return new OrdenCompra(
                    idGenerado,
                    orden.getIdProveedor(),
                    orden.getIdEmpleado(),
                    orden.getFechaCreacion(),
                    orden.getFechaEntrega(),
                    orden.isActiva(),
                    orden.getDetalles()
            );

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (Exception rollbackEx) {
                    // ignorar error de rollback
                }
            }
            throw new RuntimeException("Error al guardar orden de compra", e);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                } catch (Exception ex) {
                    // ignorar
                }
            }
        }
    }

    @Override
    public boolean cancelar(int id) {
        String sql = "UPDATE Orden_compra SET estado = FALSE WHERE id_orden_compra = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            throw new RuntimeException("Error al cancelar orden de compra", e);
        }
    }

    // ==================== Helpers de mapeo ====================

    private OrdenCompra mapearOrden(Connection conn, ResultSet rs) throws Exception {
        int idOrden = rs.getInt("id_orden_compra");
        List<DetalleOrdenCompra> detalles = cargarDetalles(conn, idOrden);

        return new OrdenCompra(
                idOrden,
                rs.getInt("id_proveedor"),
                rs.getInt("id_empleado"),
                rs.getDate("fecha_creacion").toLocalDate(),
                rs.getDate("fecha_entrega") != null ? rs.getDate("fecha_entrega").toLocalDate() : null,
                rs.getBoolean("estado"),
                detalles
        );
    }

    private List<DetalleOrdenCompra> cargarDetalles(Connection conn, int idOrden) throws Exception {
        List<DetalleOrdenCompra> detalles = new ArrayList<>();
        String sql = "SELECT * FROM Detalle_orden_compra WHERE id_orden_compra = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idOrden);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                detalles.add(new DetalleOrdenCompra(
                        rs.getInt("id_detalle_orden"),
                        rs.getInt("id_producto"),
                        rs.getInt("cantidad"),
                        rs.getBigDecimal("costo_unitario")
                ));
            }
        }

        return detalles;
    }
}