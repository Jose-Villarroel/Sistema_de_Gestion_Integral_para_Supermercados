package services.ventas;

import dtos.DetalleVentaRetornableDTO;
import dtos.PuntosVentaDTO;
import dtos.SupervisorAutorizacionDTO;
import dtos.VentaInfoDTO;
import repositories.CierreCajaRepository;
import repositories.DatabaseConnection;
import repositories.ProductoRepository;
import repositories.UsuarioRepository;
import repositories.VentaRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class ProcesarDevolucionUseCase {

    private static final int DIAS_MAXIMOS_DEVOLUCION = 30;

    private final DatabaseConnection databaseConnection;
    private final VentaRepository ventaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CierreCajaRepository cierreCajaRepository;
    private final ProductoRepository productoRepository;

    public ProcesarDevolucionUseCase(DatabaseConnection databaseConnection,
                                     VentaRepository ventaRepository,
                                     UsuarioRepository usuarioRepository,
                                     CierreCajaRepository cierreCajaRepository,
                                     ProductoRepository productoRepository) {
        this.databaseConnection = databaseConnection;
        this.ventaRepository = ventaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cierreCajaRepository = cierreCajaRepository;
        this.productoRepository = productoRepository;
    }

    public List<DetalleVentaRetornable> obtenerDetallesVenta(int idVenta) {
        return ventaRepository.obtenerDetallesRetornables(idVenta).stream()
                .map(this::mapearDetalleRetornable)
                .toList();
    }

    public ResultadoDevolucion procesarDevolucion(SolicitudDevolucion solicitud) {
        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un producto para devolver.");
        }

        try (Connection conn = databaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                VentaInfo venta = ventaRepository.buscarVentaActivaPorId(solicitud.idVenta())
                        .map(this::mapearVentaInfo)
                        .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado o invalido."));

                validarPeriodoDevolucion(
                        venta.fechaVenta(),
                        solicitud.supervisorUsuario(),
                        solicitud.supervisorPassword(),
                        conn
                );

                double totalDevuelto = 0;
                List<DetalleVentaRetornable> disponibles = obtenerDetallesVenta(solicitud.idVenta());

                boolean requiereSupervisor = false;

                for (ItemDevolucion item : solicitud.items()) {
                    if (item.cantidad() <= 0) {
                        continue;
                    }

                    DetalleVentaRetornable detalle = disponibles.stream()
                            .filter(d -> d.idProducto() == item.idProducto())
                            .findFirst()
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "El producto " + item.idProducto() + " no pertenece a la compra original."
                            ));

                    if (item.cantidad() > (detalle.comprada() - detalle.devuelta())) {
                        throw new IllegalArgumentException("Cantidad a devolver excede cantidad comprada.");
                    }

                    if (item.estadoProducto() == EstadoProductoDevolucion.ABIERTO) {
                        requiereSupervisor = true;
                    }

                    totalDevuelto += item.cantidad() * detalle.precioRealUnitario();
                }

                if (requiereSupervisor) {
                    validarSupervisor(conn, solicitud.supervisorUsuario(), solicitud.supervisorPassword());
                }

                if (totalDevuelto <= 0) {
                    throw new IllegalArgumentException("El total a devolver debe ser mayor a cero.");
                }

                if (solicitud.metodoReembolso() == MetodoReembolso.EFECTIVO) {
                    validarEfectivoDisponible(conn, venta.idEmpleado(), venta.turno(), totalDevuelto);
                }

                String numeroDevolucion = "DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

                int idDevolucion = guardarDevolucion(conn, solicitud, totalDevuelto, numeroDevolucion);
                guardarDetalles(conn, idDevolucion, solicitud, disponibles);
                actualizarStockYMovimiento(conn, solicitud);
                registrarImpactoCaja(conn, solicitud.idEmpleado(), solicitud.idVenta(), solicitud.metodoReembolso(), totalDevuelto);
                descontarPuntosSiAplica(conn, solicitud.idVenta());
                generarComprobante(numeroDevolucion, solicitud.idVenta(), totalDevuelto, solicitud.metodoReembolso());

                conn.commit();
                return new ResultadoDevolucion(numeroDevolucion, totalDevuelto);

            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error procesando la devolucion: " + e.getMessage(), e);
        }
    }

    private void validarPeriodoDevolucion(
            LocalDate fechaVenta,
            String supervisorUsuario,
            String supervisorPassword,
            Connection conn
    ) throws SQLException {
        long dias = ChronoUnit.DAYS.between(fechaVenta, LocalDate.now());

        if (dias > DIAS_MAXIMOS_DEVOLUCION) {
            validarSupervisor(conn, supervisorUsuario, supervisorPassword);
        }
    }

    private void validarSupervisor(Connection conn, String username, String password) throws SQLException {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Se requiere autorizacion del supervisor.");
        }

        SupervisorAutorizacionDTO supervisor = usuarioRepository.buscarSupervisorActivoPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Supervisor no encontrado."));

        if (!"SUPERVISOR_INVENTARIO".equalsIgnoreCase(supervisor.nombreRol())) {
            throw new IllegalArgumentException("El usuario ingresado no tiene rol de supervisor.");
        }

        if (!supervisor.passwordHash().equals(String.valueOf(password.hashCode()))) {
            throw new IllegalArgumentException("Credenciales de supervisor invalidas.");
        }
    }

    private void validarEfectivoDisponible(Connection conn, int idEmpleado, String turno, double montoDevuelto) throws SQLException {
        double disponible = cierreCajaRepository.obtenerEfectivoDisponible(idEmpleado, turno, LocalDate.now());
        if (disponible < montoDevuelto) {
            throw new IllegalArgumentException("Efectivo insuficiente en caja. Ofrezca otro metodo de reembolso.");
        }
    }

    private int guardarDevolucion(
            Connection conn,
            SolicitudDevolucion solicitud,
            double totalDevuelto,
            String numeroDevolucion
    ) throws SQLException {
        String sql = """
            INSERT INTO Devoluciones
            (id_empleado, id_venta, fecha_devolucion, motivo, total_devuelto, estado, metodo_reembolso, numero_devolucion)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, solicitud.idEmpleado());
            stmt.setInt(2, solicitud.idVenta());
            stmt.setDate(3, Date.valueOf(LocalDate.now()));
            stmt.setString(4, solicitud.motivoGeneral());
            stmt.setDouble(5, totalDevuelto);
            stmt.setBoolean(6, true);
            stmt.setString(7, solicitud.metodoReembolso().name());
            stmt.setString(8, numeroDevolucion);
            stmt.executeUpdate();

            var rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }

            throw new SQLException("No se pudo registrar la devolucion principal.");
        }
    }

    private void guardarDetalles(
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
                if (item.cantidad() <= 0) {
                    continue;
                }

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

    private void actualizarStockYMovimiento(Connection conn, SolicitudDevolucion solicitud) throws SQLException {
        String sqlStock = "UPDATE Producto SET stock_actual = stock_actual + ? WHERE id_producto = ?";
        String sqlMovimiento = """
            INSERT INTO Movimiento_inventario
            (id_empleado, id_tipo_movimiento, id_producto, cantidad, stock_anterior, stock_nuevo, motivo, fecha_movimiento)
            VALUES (?, 1, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmtStock = conn.prepareStatement(sqlStock);
             PreparedStatement stmtMov = conn.prepareStatement(sqlMovimiento)) {

            for (ItemDevolucion item : solicitud.items()) {
                if (item.cantidad() <= 0) {
                    continue;
                }

                if (item.estadoProducto() == EstadoProductoDevolucion.DEFECTUOSO
                        || item.estadoProducto() == EstadoProductoDevolucion.VENCIDO) {
                    continue;
                }

                int stockAnterior = productoRepository.obtenerStockActual(item.idProducto());

                stmtStock.setInt(1, item.cantidad());
                stmtStock.setInt(2, item.idProducto());
                stmtStock.addBatch();

                stmtMov.setInt(1, solicitud.idEmpleado());
                stmtMov.setInt(2, item.idProducto());
                stmtMov.setInt(3, item.cantidad());
                stmtMov.setInt(4, stockAnterior);
                stmtMov.setInt(5, stockAnterior + item.cantidad());
                stmtMov.setString(6, "Devolucion de venta " + solicitud.idVenta());
                stmtMov.setDate(7, Date.valueOf(LocalDate.now()));
                stmtMov.addBatch();
            }

            stmtStock.executeBatch();
            stmtMov.executeBatch();
        }
    }

    private void registrarImpactoCaja(
            Connection conn,
            int empleadoId,
            int ventaId,
            MetodoReembolso metodo,
            double montoDevuelto
    ) throws SQLException {
        if (metodo != MetodoReembolso.EFECTIVO) {
            return;
        }

        String sql = """
            INSERT INTO Caja
            (id_empleado, id_venta, fecha_apertura, fecha_cierre, monto_inicial, monto_final, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, empleadoId);
            stmt.setInt(2, ventaId);
            stmt.setDate(3, Date.valueOf(LocalDate.now()));
            stmt.setDate(4, Date.valueOf(LocalDate.now()));
            stmt.setDouble(5, 0);
            stmt.setDouble(6, -montoDevuelto);
            stmt.setBoolean(7, true);
            stmt.executeUpdate();
        }
    }

    private void descontarPuntosSiAplica(Connection conn, int idVenta) throws SQLException {
        String sqlActualizar = """
            UPDATE Cuenta_fidelizacion
            SET puntos_actuales = CASE
                WHEN puntos_actuales >= ? THEN puntos_actuales - ?
                ELSE 0
            END
            WHERE id_fidelizacion = ?
        """;

        for (PuntosVentaDTO puntosVenta : ventaRepository.obtenerPuntosOtorgadosPorVenta(idVenta)) {
            try (PreparedStatement stmtActualizar = conn.prepareStatement(sqlActualizar)) {
                stmtActualizar.setInt(1, puntosVenta.puntos());
                stmtActualizar.setInt(2, puntosVenta.puntos());
                stmtActualizar.setInt(3, puntosVenta.idCuentaFidelizacion());
                stmtActualizar.executeUpdate();
            }
        }
    }

    private void generarComprobante(
            String numeroDevolucion,
            int idVenta,
            double totalDevuelto,
            MetodoReembolso metodo
    ) throws IOException {
        Path directorio = Path.of("target", "devoluciones");
        Files.createDirectories(directorio);

        String contenido = """
                MasterMarket
                Comprobante de devolucion
                Numero: %s
                Venta original: %d
                Fecha: %s
                Metodo: %s
                Total devuelto: %s
                """.formatted(
                numeroDevolucion,
                idVenta,
                LocalDate.now(),
                metodo.name(),
                ProcesarFinalizarVentaUseCase.formatearMoneda(totalDevuelto)
        );

        Files.writeString(directorio.resolve("devolucion-" + numeroDevolucion + ".txt"), contenido);
    }

    public record DetalleVentaRetornable(
            int idProducto,
            String nombreProducto,
            int comprada,
            int devuelta,
            double precioRealUnitario
    ) {
        public int cantidadDisponible() {
            return comprada - devuelta;
        }
    }

    public record SolicitudDevolucion(
            int idVenta,
            int idEmpleado,
            String motivoGeneral,
            MetodoReembolso metodoReembolso,
            String supervisorUsuario,
            String supervisorPassword,
            List<ItemDevolucion> items
    ) {}

    public record ItemDevolucion(
            int idProducto,
            int cantidad,
            String motivo,
            EstadoProductoDevolucion estadoProducto
    ) {}

    public record ResultadoDevolucion(String numeroDevolucion, double totalDevuelto) {}

    public enum MetodoReembolso {
        EFECTIVO,
        NOTA_CREDITO,
        CAMBIO_PRODUCTO
    }

    public enum EstadoProductoDevolucion {
        CERRADO,
        ABIERTO,
        DEFECTUOSO,
        VENCIDO
    }

    private record VentaInfo(int idVenta, int idEmpleado, LocalDate fechaVenta, String turno) {}

    private DetalleVentaRetornable mapearDetalleRetornable(DetalleVentaRetornableDTO dto) {
        return new DetalleVentaRetornable(
                dto.idProducto(),
                dto.nombreProducto(),
                dto.comprada(),
                dto.devuelta(),
                dto.precioRealUnitario()
        );
    }

    private VentaInfo mapearVentaInfo(VentaInfoDTO dto) {
        return new VentaInfo(dto.idVenta(), dto.idEmpleado(), dto.fechaVenta(), dto.turno());
    }
}
