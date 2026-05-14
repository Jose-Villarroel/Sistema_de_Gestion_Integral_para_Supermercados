package repositories;

import services.dashboard.DashboardGerencial;
import services.dashboard.ProductoVendido;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class H2DashboardRepository implements DashboardRepository {

    private static final int LIMITE_TOP_PRODUCTOS = 5;

    private final DatabaseConnection dbConnection;

    public H2DashboardRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public DashboardGerencial consultarDashboard() {
        LocalDate hoy = LocalDate.now();

        try (Connection conn = dbConnection.getConnection()) {

            BigDecimal ventasDelDia = obtenerVentasDelDia(conn, hoy);
            int transacciones = obtenerNumeroTransaccionesDelDia(conn, hoy);
            int stockCritico = obtenerProductosConStockCritico(conn);
            List<ProductoVendido> topProductos = obtenerTopProductosVendidos(conn);

            return new DashboardGerencial(
                    hoy,
                    ventasDelDia,
                    transacciones,
                    stockCritico,
                    topProductos
            );

        } catch (Exception e) {
            throw new RuntimeException("Error al consultar el dashboard gerencial", e);
        }
    }

    private BigDecimal obtenerVentasDelDia(Connection conn, LocalDate hoy) throws Exception {
        String sql = """
            SELECT COALESCE(SUM(total_final), 0) AS total
            FROM Venta
            WHERE estado_venta = TRUE AND fecha_venta = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(hoy));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getBigDecimal("total");
            }
            return BigDecimal.ZERO;
        }
    }

    private int obtenerNumeroTransaccionesDelDia(Connection conn, LocalDate hoy) throws Exception {
        String sql = """
            SELECT COUNT(*) AS num
            FROM Venta
            WHERE estado_venta = TRUE AND fecha_venta = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(hoy));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("num");
            }
            return 0;
        }
    }

    private int obtenerProductosConStockCritico(Connection conn) throws Exception {
        String sql = """
            SELECT COUNT(*) AS num
            FROM Producto
            WHERE estado_activo = TRUE AND stock_actual < stock_minimo
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (rs.next()) {
                return rs.getInt("num");
            }
            return 0;
        }
    }

    private List<ProductoVendido> obtenerTopProductosVendidos(Connection conn) throws Exception {
        List<ProductoVendido> top = new ArrayList<>();
        String sql = """
            SELECT p.id_producto, p.nombre, COALESCE(SUM(dv.cantidad), 0) AS total_vendido
            FROM Producto p
            INNER JOIN Detalle_venta dv ON p.id_producto = dv.id_producto
            INNER JOIN Venta v ON dv.id_venta = v.id_venta
            WHERE v.estado_venta = TRUE
            GROUP BY p.id_producto, p.nombre
            ORDER BY total_vendido DESC
            LIMIT ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, LIMITE_TOP_PRODUCTOS);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                top.add(new ProductoVendido(
                        rs.getInt("id_producto"),
                        rs.getString("nombre"),
                        rs.getInt("total_vendido")
                ));
            }
        }

        return top;
    }
}