package repositories;

import entities.Producto;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class H2ProductoRepository implements ProductoRepository {

    private final DatabaseConnection dbConnection;

    public H2ProductoRepository(DatabaseConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    @Override
    public Producto guardar(Producto producto) {
        String sql = """
            INSERT INTO Producto
            (id_categoria, nombre, descripcion, marca,
             precio_compra, precio_venta, stock_actual,
             stock_minimo, estado_activo, fecha_registro)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, producto.getCategoriaId());
            stmt.setString(2, producto.getNombre());
            stmt.setString(3, producto.getDescripcion());
            stmt.setString(4, producto.getMarca());
            stmt.setDouble(5, producto.getPrecioCompra());
            stmt.setDouble(6, producto.getPrecioVenta());
            stmt.setInt(7, producto.getStockActual());
            stmt.setInt(8, producto.getStockMinimo());
            stmt.setBoolean(9, producto.isActivo());
            stmt.setDate(10, Date.valueOf(producto.getFechaRegistro()));

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return new Producto(
                        rs.getInt(1),
                        producto.getCategoriaId(),
                        producto.getNombre(),
                        producto.getDescripcion(),
                        producto.getMarca(),
                        producto.getPrecioCompra(),
                        producto.getPrecioVenta(),
                        producto.getStockActual(),
                        producto.getStockMinimo(),
                        producto.isActivo(),
                        producto.getFechaRegistro()
                );
            }

            throw new RuntimeException("No se pudo obtener el id del producto guardado");

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar producto", e);
        }
    }

    @Override
    public Optional<Producto> buscarPorId(int id) {
        String sql = "SELECT * FROM Producto WHERE id_producto = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapear(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto por id", e);
        }

        return Optional.empty();
    }

    @Override
    public List<Producto> listarTodos() {
        return ejecutarLista("SELECT * FROM Producto ORDER BY nombre");
    }

    @Override
    public List<Producto> listarActivos() {
        return ejecutarLista("SELECT * FROM Producto WHERE estado_activo = TRUE ORDER BY nombre");
    }

    @Override
    public List<Producto> listarPorCategoria(int categoriaId) {
        String sql = "SELECT * FROM Producto WHERE id_categoria = ? AND estado_activo = TRUE ORDER BY nombre";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoriaId);
            ResultSet rs = stmt.executeQuery();
            return mapearLista(rs);

        } catch (SQLException e) {
            throw new RuntimeException("Error al listar productos por categoría", e);
        }
    }

    @Override
    public List<Producto> listarConStockBajo() {
        return ejecutarLista("""
            SELECT * FROM Producto
            WHERE stock_actual < stock_minimo
            AND estado_activo = TRUE
            ORDER BY nombre
        """);
    }

    @Override
    public List<Producto> buscarPorNombre(String nombre) {
        String sql = "SELECT * FROM Producto WHERE LOWER(nombre) LIKE ? ORDER BY nombre";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre.toLowerCase() + "%");
            ResultSet rs = stmt.executeQuery();
            return mapearLista(rs);

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto por nombre", e);
        }
    }

    @Override
    public boolean actualizar(Producto producto) {
        String sql = """
            UPDATE Producto
            SET id_categoria = ?, nombre = ?, descripcion = ?, marca = ?,
                precio_compra = ?, precio_venta = ?, stock_actual = ?,
                stock_minimo = ?, estado_activo = ?
            WHERE id_producto = ?
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, producto.getCategoriaId());
            stmt.setString(2, producto.getNombre());
            stmt.setString(3, producto.getDescripcion());
            stmt.setString(4, producto.getMarca());
            stmt.setDouble(5, producto.getPrecioCompra());
            stmt.setDouble(6, producto.getPrecioVenta());
            stmt.setInt(7, producto.getStockActual());
            stmt.setInt(8, producto.getStockMinimo());
            stmt.setBoolean(9, producto.isActivo());
            stmt.setInt(10, producto.getId());

            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar producto", e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE Producto SET estado_activo = FALSE WHERE id_producto = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al desactivar producto", e);
        }
    }

    @Override
    public boolean existeNombre(String nombre) {
        String sql = "SELECT COUNT(*) FROM Producto WHERE LOWER(nombre) = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombre.toLowerCase().trim());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

            return false;

        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar si existe el nombre del producto", e);
        }
    }

    private List<Producto> ejecutarLista(String sql) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            return mapearLista(rs);

        } catch (SQLException e) {
            throw new RuntimeException("Error al ejecutar consulta de productos", e);
        }
    }

    private List<Producto> mapearLista(ResultSet rs) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(mapear(rs));
        }
        return lista;
    }

    private Producto mapear(ResultSet rs) throws SQLException {
        return new Producto(
                rs.getInt("id_producto"),
                rs.getInt("id_categoria"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getString("marca"),
                rs.getDouble("precio_compra"),
                rs.getDouble("precio_venta"),
                rs.getInt("stock_actual"),
                rs.getInt("stock_minimo"),
                rs.getBoolean("estado_activo"),
                rs.getDate("fecha_registro").toLocalDate()
        );
    }
}
