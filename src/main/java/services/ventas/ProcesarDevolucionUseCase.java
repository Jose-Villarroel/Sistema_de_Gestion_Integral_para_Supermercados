package services.ventas;

import entities.MovimientoInventario;
import repositories.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

public class ProcesarDevolucionUseCase {

    private static final int DIAS_MAXIMOS_DEVOLUCION = 30;

    private final DatabaseConnection databaseConnection;
    private final DevolucionRepository devolucionRepository;
    private final DetalleDevolucionRepository detalleDevolucionRepository;
    private final ProductoRepository productoRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final CajaRepository cajaRepository;
    private final UsuarioRepository usuarioRepository;
    private final CuentaFidelizacionRepository cuentaFidelizacionRepository;

    public ProcesarDevolucionUseCase(
            DatabaseConnection databaseConnection,
            DevolucionRepository devolucionRepository,
            DetalleDevolucionRepository detalleDevolucionRepository,
            ProductoRepository productoRepository,
            MovimientoInventarioRepository movimientoInventarioRepository,
            CajaRepository cajaRepository,
            UsuarioRepository usuarioRepository,
            CuentaFidelizacionRepository cuentaFidelizacionRepository
    ) {
        this.databaseConnection = databaseConnection;
        this.devolucionRepository = devolucionRepository;
        this.detalleDevolucionRepository = detalleDevolucionRepository;
        this.productoRepository = productoRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.cajaRepository = cajaRepository;
        this.usuarioRepository = usuarioRepository;
        this.cuentaFidelizacionRepository = cuentaFidelizacionRepository;
    }

    public List<DetalleVentaRetornable> obtenerDetallesVenta(int idVenta) {
        try (Connection conn = databaseConnection.getConnection()) {
            return devolucionRepository.obtenerDetallesVenta(conn, idVenta);
        } catch (SQLException e) {
            throw new RuntimeException("Error al obtener detalles de la venta", e);
        }
    }

    public ResultadoDevolucion procesarDevolucion(SolicitudDevolucion solicitud) {
        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un producto para devolver.");
        }

        try (Connection conn = databaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                VentaInfo venta = devolucionRepository.obtenerVenta(conn, solicitud.idVenta());

                if (venta == null) {
                    throw new IllegalArgumentException("Ticket no encontrado o inválido.");
                }

                validarPeriodoDevolucion(
                        conn,
                        venta.fechaVenta(),
                        solicitud.supervisorUsuario(),
                        solicitud.supervisorPassword()
                );

                List<DetalleVentaRetornable> disponibles =
                        devolucionRepository.obtenerDetallesVenta(conn, solicitud.idVenta());

                double totalDevuelto = calcularTotalYValidarItems(solicitud, disponibles);

                boolean requiereSupervisor = requiereSupervisor(solicitud);

                if (requiereSupervisor) {
                    usuarioRepository.validarSupervisor(
                            conn,
                            solicitud.supervisorUsuario(),
                            solicitud.supervisorPassword()
                    );
                }

                if (totalDevuelto <= 0) {
                    throw new IllegalArgumentException("El total a devolver debe ser mayor a cero.");
                }

                if (solicitud.metodoReembolso() == MetodoReembolso.EFECTIVO) {
                    cajaRepository.validarEfectivoDisponible(
                            conn,
                            venta.idEmpleado(),
                            venta.turno(),
                            totalDevuelto
                    );
                }

                String numeroDevolucion = generarNumeroDevolucion();

                int idDevolucion = devolucionRepository.guardarDevolucion(
                        conn,
                        solicitud,
                        totalDevuelto,
                        numeroDevolucion
                );

                detalleDevolucionRepository.guardarDetalles(
                        conn,
                        idDevolucion,
                        solicitud,
                        disponibles
                );

                actualizarStockYMovimiento(conn, solicitud);

                cajaRepository.registrarImpactoCaja(
                        conn,
                        solicitud.idEmpleado(),
                        solicitud.idVenta(),
                        solicitud.metodoReembolso(),
                        totalDevuelto
                );

                cuentaFidelizacionRepository.descontarPuntosSiAplica(
                        conn,
                        solicitud.idVenta()
                );

                generarComprobante(
                        numeroDevolucion,
                        solicitud.idVenta(),
                        totalDevuelto,
                        solicitud.metodoReembolso()
                );

                conn.commit();

                return new ResultadoDevolucion(numeroDevolucion, totalDevuelto);

            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error procesando la devolución: " + e.getMessage(), e);
        }
    }

    private void validarPeriodoDevolucion(
            Connection conn,
            LocalDate fechaVenta,
            String supervisorUsuario,
            String supervisorPassword
    ) throws SQLException {
        long dias = ChronoUnit.DAYS.between(fechaVenta, LocalDate.now());

        if (dias > DIAS_MAXIMOS_DEVOLUCION) {
            usuarioRepository.validarSupervisor(conn, supervisorUsuario, supervisorPassword);
        }
    }

    private double calcularTotalYValidarItems(
            SolicitudDevolucion solicitud,
            List<DetalleVentaRetornable> disponibles
    ) {
        double totalDevuelto = 0;

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

            if (item.cantidad() > detalle.cantidadDisponible()) {
                throw new IllegalArgumentException("Cantidad a devolver excede cantidad comprada.");
            }

            totalDevuelto += item.cantidad() * detalle.precioRealUnitario();
        }

        return totalDevuelto;
    }

    private boolean requiereSupervisor(SolicitudDevolucion solicitud) {
        return solicitud.items().stream()
                .anyMatch(item -> item.estadoProducto() == EstadoProductoDevolucion.ABIERTO);
    }

    private void actualizarStockYMovimiento(
            Connection conn,
            SolicitudDevolucion solicitud
    ) throws SQLException {

        for (ItemDevolucion item : solicitud.items()) {
            if (item.cantidad() <= 0) {
                continue;
            }

            if (item.estadoProducto() == EstadoProductoDevolucion.DEFECTUOSO
                    || item.estadoProducto() == EstadoProductoDevolucion.VENCIDO) {
                continue;
            }

            int stockAnterior = productoRepository.obtenerStockActual(conn, item.idProducto());

            productoRepository.aumentarStock(conn, item.idProducto(), item.cantidad());

            MovimientoInventario movimiento = new MovimientoInventario(
                    0,
                    solicitud.idEmpleado(),
                    1,
                    item.idProducto(),
                    item.cantidad(),
                    stockAnterior,
                    stockAnterior + item.cantidad(),
                    "Devolución de venta " + solicitud.idVenta(),
                    LocalDate.now()
            );

            movimientoInventarioRepository.guardar(conn, movimiento);
        }
    }

    private String generarNumeroDevolucion() {
        return "DEV-" + UUID.randomUUID()
                .toString()
                .substring(0, 8)
                .toUpperCase();
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
                Comprobante de devolución
                Número: %s
                Venta original: %d
                Fecha: %s
                Método: %s
                Total devuelto: %s
                """.formatted(
                numeroDevolucion,
                idVenta,
                LocalDate.now(),
                metodo.name(),
                ProcesarFinalizarVentaUseCase.formatearMoneda(totalDevuelto)
        );

        Files.writeString(
                directorio.resolve("devolucion-" + numeroDevolucion + ".txt"),
                contenido
        );
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

    public record ResultadoDevolucion(
            String numeroDevolucion,
            double totalDevuelto
    ) {}

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

    public record VentaInfo(
            int idVenta,
            int idEmpleado,
            LocalDate fechaVenta,
            String turno
    ) {}
}