package repositories;

import aggregates.Producto;

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
            INSERT INTO productos (codigo, nombre, descripcion, precio_compra, 
                                   precio_venta, stock_actual, stock_minimo, 
                                   stock_maximo, categoria_id, proveedor_id, activo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, producto.getCodigo());
            stmt.setString(2, producto.getNombre());
            stmt.setString(3, producto.getDescripcion());
            stmt.setDouble(4, producto.getPrecioCompra());
            stmt.setDouble(5, producto.getPrecioVenta());
            stmt.setInt(6, producto.getStockActual());
            stmt.setInt(7, producto.getStockMinimo());
            stmt.setInt(8, producto.getStockMaximo());
            stmt.setInt(9, producto.getCategoriaId());
            stmt.setInt(10, producto.getProveedorId());
            stmt.setBoolean(11, producto.isActivo());

            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                return new Producto(
                    id,
                    producto.getCodigo(),
                    producto.getNombre(),
                    producto.getDescripcion(),
                    producto.getPrecioCompra(),
                    producto.getPrecioVenta(),
                    producto.getStockActual(),
                    producto.getStockMinimo(),
                    producto.getStockMaximo(),
                    producto.getCategoriaId(),
                    producto.getProveedorId(),
                    producto.isActivo()
                );
            }

            throw new RuntimeException("Error al obtener el ID del producto guardado");

        } catch (SQLException e) {
            throw new RuntimeException("Error al guardar el producto: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Producto> buscarPorId(int id) {
        String sql = "SELECT * FROM productos WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearProducto(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto por ID: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    @Override
    public Optional<Producto> buscarPorCodigo(String codigo) {
        String sql = "SELECT * FROM productos WHERE codigo = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo.toUpperCase());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapearProducto(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar producto por código: " + e.getMessage(), e);
        }

        return Optional.empty();
    }

    @Override
    public List<Producto> listarTodos() {
        String sql = "SELECT * FROM productos ORDER BY nombre";
        return ejecutarConsultaLista(sql);
    }

    @Override
    public List<Producto> listarActivos() {
        String sql = "SELECT * FROM productos WHERE activo = TRUE ORDER BY nombre";
        return ejecutarConsultaLista(sql);
    }

    @Override
    public List<Producto> listarPorCategoria(int categoriaId) {
        String sql = "SELECT * FROM productos WHERE categoria_id = ? AND activo = TRUE ORDER BY nombre";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, categoriaId);
            ResultSet rs = stmt.executeQuery();
            
            return mapearListaProductos(rs);

        } catch (SQLException e) {
            throw new RuntimeException("Error al listar productos por categoría: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Producto> listarConStockBajo() {
        String sql = """
            SELECT * FROM productos 
            WHERE stock_actual < stock_minimo AND activo = TRUE 
            ORDER BY (stock_minimo - stock_actual) DESC
        """;
        return ejecutarConsultaLista(sql);
    }

    @Override
    public List<Producto> buscarPorNombre(String nombre) {
        String sql = "SELECT * FROM productos WHERE LOWER(nombre) LIKE ? AND activo = TRUE ORDER BY nombre";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, "%" + nombre.toLowerCase() + "%");
            ResultSet rs = stmt.executeQuery();
            
            return mapearListaProductos(rs);

        } catch (SQLException e) {
            throw new RuntimeException("Error al buscar productos por nombre: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean actualizar(Producto producto) {
        String sql = """
            UPDATE productos 
            SET nombre = ?, descripcion = ?, precio_compra = ?, precio_venta = ?,
                stock_minimo = ?, stock_maximo = ?, categoria_id = ?, proveedor_id = ?
            WHERE id = ?
        """;

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, producto.getNombre());
            stmt.setString(2, producto.getDescripcion());
            stmt.setDouble(3, producto.getPrecioCompra());
            stmt.setDouble(4, producto.getPrecioVenta());
            stmt.setInt(5, producto.getStockMinimo());
            stmt.setInt(6, producto.getStockMaximo());
            stmt.setInt(7, producto.getCategoriaId());
            stmt.setInt(8, producto.getProveedorId());
            stmt.setInt(9, producto.getId());

            int filasAfectadas = stmt.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al actualizar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean desactivar(int id) {
        String sql = "UPDATE productos SET activo = FALSE WHERE id = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            int filasAfectadas = stmt.executeUpdate();
            return filasAfectadas > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Error al desactivar producto: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existeCodigo(String codigo) {
        String sql = "SELECT COUNT(*) FROM productos WHERE codigo = ?";

        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, codigo.toUpperCase());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error al verificar existencia de código: " + e.getMessage(), e);
        }

        return false;
    }

    // Métodos auxiliares privados
    private List<Producto> ejecutarConsultaLista(String sql) {
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            ResultSet rs = stmt.executeQuery();
            return mapearListaProductos(rs);

        } catch (SQLException e) {
            throw new RuntimeException("Error al ejecutar consulta: " + e.getMessage(), e);
        }
    }

    private List<Producto> mapearListaProductos(ResultSet rs) throws SQLException {
        List<Producto> productos = new ArrayList<>();
        while (rs.next()) {
            productos.add(mapearProducto(rs));
        }
        return productos;
    }

    private Producto mapearProducto(ResultSet rs) throws SQLException {
        return new Producto(
            rs.getInt("id"),
            rs.getString("codigo"),
            rs.getString("nombre"),
            rs.getString("descripcion"),
            rs.getDouble("precio_compra"),
            rs.getDouble("precio_venta"),
            rs.getInt("stock_actual"),
            rs.getInt("stock_minimo"),
            rs.getInt("stock_maximo"),
            rs.getInt("categoria_id"),
            rs.getInt("proveedor_id"),
            rs.getBoolean("activo")
        );
    }
}
