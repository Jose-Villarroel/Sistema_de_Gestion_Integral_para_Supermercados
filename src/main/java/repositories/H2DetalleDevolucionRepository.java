package repositories;

import services.ventas.ProcesarDevolucionUseCase.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public class H2DetalleDevolucionRepository implements DetalleDevolucionRepository {

    @Override
    public void guardarDetalles(
            Connection conn,
            int idDevolucion,
            SolicitudDevolucion solicitud,
            List<DetalleVentaRetornable> disponibles
    ) throws SQLException {

        String sql = """
            INSERT INTO Detalle_devolucion
            (id_devoluciones, id_producto, cantidad, subtotal_devuelto, motivo, estado_producto, reintegra_inventario)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ItemDevolucion item : solicitud.items()) {
                if (item.cantidad() <= 0) continue;

                DetalleVentaRetornable detalle = disponibles.stream()
                        .filter(d -> d.idProducto() == item.idProducto())
                        .findFirst()
                        .orElseThrow();

                boolean reintegra = item.estadoProducto() != EstadoProductoDevolucion.DEFECTUOSO
                        && item.estadoProducto() != EstadoProductoDevolucion.VENCIDO;

                stmt.setInt(1, idDevolucion);
                stmt.setInt(2, item.idProducto());
                stmt.setInt(3, item.cantidad());
                stmt.setDouble(4, item.cantidad() * detalle.precioRealUnitario());
                stmt.setString(5, item.motivo());
                stmt.setString(6, item.estadoProducto().name());
                stmt.setBoolean(7, reintegra);

                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }
}