package services.ventas;

import entities.Cliente;
import entities.CuentaFidelizacion;
import entities.Empleado;
import entities.MovimientoInventario;
import entities.Producto;
import repositories.CajaRepository;
import repositories.ClienteRepository;
import repositories.DatabaseConnection;
import repositories.CuentaFidelizacionRepository;
import repositories.DetalleVentaRepository;
import repositories.MovimientoInventarioRepository;
import repositories.PagoVentaRepository;
import repositories.ProductoRepository;
import repositories.VentaRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class ProcesarFinalizarVentaUseCase {
    private static final double IVA = 0.19;
    private static final Locale LOCALE_CO = Locale.of("es", "CO");

    private final DatabaseConnection databaseConnection;
    private final ClienteRepository clienteRepository;
    private final CuentaFidelizacionRepository cuentaFidelizacionRepository;
    private final ProductoRepository productoRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final CajaRepository cajaRepository;

    public ProcesarFinalizarVentaUseCase(DatabaseConnection databaseConnection,
                                         ClienteRepository clienteRepository,
                                         CuentaFidelizacionRepository cuentaFidelizacionRepository,
                                         ProductoRepository productoRepository,
                                         VentaRepository ventaRepository,
                                         DetalleVentaRepository detalleVentaRepository,
                                         PagoVentaRepository pagoVentaRepository,
                                         MovimientoInventarioRepository movimientoInventarioRepository,
                                         CajaRepository cajaRepository) {
        this.databaseConnection = databaseConnection;
        this.clienteRepository = clienteRepository;
        this.cuentaFidelizacionRepository = cuentaFidelizacionRepository;
        this.productoRepository = productoRepository;
        this.ventaRepository = ventaRepository;
        this.detalleVentaRepository = detalleVentaRepository;
        this.pagoVentaRepository = pagoVentaRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.cajaRepository = cajaRepository;
    }

    public Producto buscarProductoPorId(String codigoEscaneado) {
        int idProducto = parsearEntero(codigoEscaneado, "Product not found. Code: " + codigoEscaneado);
        return productoRepository.buscarPorId(idProducto)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new IllegalArgumentException("Product not found. Code: " + codigoEscaneado));
    }

    public Optional<ClienteConCuenta> buscarClientePorCodigo(String codigoCliente) {
        if (codigoCliente == null || codigoCliente.isBlank()) {
            return Optional.empty();
        }

        parsearEntero(codigoCliente, "Customer not found");
        Cliente cliente = clienteRepository
                .buscarPorIdOTarjeta(codigoCliente)
                .orElse(null);

        if (cliente == null) {
            return Optional.empty();
        }

        CuentaFidelizacion cuenta = cuentaFidelizacionRepository
                .buscarPorCliente(cliente.getId())
                .filter(CuentaFidelizacion::isActiva)
                .orElse(null);

        return Optional.of(new ClienteConCuenta(
                cliente,
                cuenta != null ? cuenta.getId() : null,
                cuenta != null ? cuenta.getNumeroTarjeta() : null,
                cuenta != null ? cuenta.getPuntosActuales() : null
        ));
    }

    public ResumenVenta calcularResumen(List<ItemVenta> items,
                                        ClienteConCuenta cliente,
                                        DescuentoManual descuentoManual) {
        if (items == null || items.isEmpty()) {
            return new ResumenVenta(0, 0, 0, 0, 0, 0, 0, 0);
        }

        double subtotal = items.stream().mapToDouble(ItemVenta::subtotal).sum();
        double descuentoPromocion = items.stream()
                .filter(item -> item.cantidad() >= 3)
                .mapToDouble(item -> item.subtotal() * 0.05)
                .sum();
        double descuentoFidelidad = cliente != null && cliente.cuentaId() != null && subtotal >= 30000
                ? subtotal * 0.03
                : 0;
        double descuentoAutomatico = descuentoPromocion + descuentoFidelidad;
        double descuentoManualValor = descuentoManual.calcular(subtotal - descuentoAutomatico);
        double baseGravable = Math.max(subtotal - descuentoAutomatico - descuentoManualValor, 0);
        double impuestos = baseGravable * IVA;
        double total = baseGravable + impuestos;
        int puntosGanados = cliente == null || cliente.cuentaId() == null ? 0 : (int) Math.floor(total / 1000);

        return new ResumenVenta(
                redondear(subtotal),
                redondear(descuentoPromocion),
                redondear(descuentoFidelidad),
                redondear(descuentoManualValor),
                redondear(descuentoAutomatico + descuentoManualValor),
                redondear(impuestos),
                redondear(total),
                puntosGanados
        );
    }

    public ResultadoVenta procesarVenta(SolicitudVenta solicitud) {
        if (solicitud.items() == null || solicitud.items().isEmpty()) {
            throw new IllegalArgumentException("You must add at least one product to the sale");
        }
        if (solicitud.empleado() == null) {
            throw new IllegalArgumentException("No hay un cajero autenticado. Inicie sesion nuevamente.");
        }
        validarTurno(solicitud.turno());

        ResumenVenta resumen = calcularResumen(solicitud.items(), solicitud.cliente(), solicitud.descuentoManual());
        PagoProcesado pago = procesarPago(solicitud.metodoPago(), resumen.total(), solicitud.montoRecibido(),
                solicitud.referenciaPago());

        if (solicitud.facturaElectronica() &&
                (solicitud.datosFacturacion() == null || solicitud.datosFacturacion().isBlank())) {
            throw new IllegalArgumentException("Debe ingresar los datos fiscales para la factura electronica");
        }
        if (solicitud.enviarPorCorreo() &&
                (solicitud.correoTicket() == null || solicitud.correoTicket().isBlank())) {
            throw new IllegalArgumentException("Debe ingresar un correo para enviar el ticket");
        }

        String numeroTicket = generarTicket();

        try (Connection conn = databaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try {
                int ventaId = guardarVenta(conn, solicitud, resumen);
                guardarDetalles(conn, ventaId, solicitud.items());
                guardarPago(conn, ventaId, solicitud.metodoPago(), resumen.total(), pago);
                actualizarInventario(conn, solicitud.items(), solicitud.empleado().getId(), ventaId);
                actualizarCaja(conn, solicitud.empleado().getId(), ventaId, solicitud.metodoPago(),
                        calcularIncrementoCaja(solicitud.metodoPago(), resumen.total(), pago));
                acreditarPuntos(conn, solicitud.cliente(), ventaId, resumen.puntosGanados());
                conn.commit();

                try {
                    generarTicketArchivo(numeroTicket, ventaId, solicitud, resumen, pago);
                } catch (IOException e) {
                    return new ResultadoVenta(numeroTicket, pago.cambio(),
                            "Sale recorded but printing error. Number: " + numeroTicket,
                            resumen.total(), pago.autorizacion());
                }

                return new ResultadoVenta(numeroTicket, pago.cambio(),
                        "Sale completed. Change: " + formatearMoneda(pago.cambio()) + ". Ticket: " + numeroTicket,
                        resumen.total(), pago.autorizacion());
            } catch (IllegalArgumentException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new RuntimeException("Transaction processing error", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Transaction processing error", e);
        }
    }

    private int guardarVenta(Connection conn, SolicitudVenta solicitud, ResumenVenta resumen) throws SQLException {
        return ventaRepository.guardar(
                conn,
                solicitud.empleado().getId(),
                LocalDate.now(),
                solicitud.turno(),
                solicitud.metodoPago().name(),
                resumen.subtotal(),
                resumen.descuentoTotal(),
                resumen.impuestos(),
                resumen.total()
        );
    }

    private void guardarDetalles(Connection conn, int ventaId, List<ItemVenta> items) throws SQLException {
        List<DetalleVentaRepository.DetalleVentaItem> detalles = items.stream()
                .map(item -> new DetalleVentaRepository.DetalleVentaItem(
                        item.productoId(),
                        item.cantidad(),
                        item.precioUnitario(),
                        item.subtotal()
                ))
                .toList();

        detalleVentaRepository.guardarDetalles(conn, ventaId, detalles);
    }

    private void guardarPago(Connection conn, int ventaId, MetodoPago metodoPago, double total,
                             PagoProcesado pago) throws SQLException {
        if (metodoPago == MetodoPago.MIXTO) {
            pagoVentaRepository.guardarPago(conn, ventaId, 1, redondear(pago.montoEfectivo()), LocalDate.now());
            pagoVentaRepository.guardarPago(
                    conn,
                    ventaId,
                    2,
                    redondear(total - pago.montoEfectivo()),
                    LocalDate.now()
            );
            return;
        }

        pagoVentaRepository.guardarPago(
                conn,
                ventaId,
                obtenerTipoPagoId(metodoPago),
                redondear(total),
                LocalDate.now()
        );
    }

    private void actualizarInventario(Connection conn, List<ItemVenta> items, int empleadoId, int ventaId)
            throws SQLException {
        for (ItemVenta item : items) {
            int stockActual = productoRepository.obtenerStockActual(conn, item.productoId());
            if (stockActual < item.cantidad()) {
                throw new IllegalArgumentException("Insufficient stock. Available: " + stockActual + " units");
            }

            if (!productoRepository.descontarStock(conn, item.productoId(), item.cantidad())) {
                throw new IllegalArgumentException("Insufficient stock. Available: "
                        + productoRepository.obtenerStockActual(conn, item.productoId()) + " units");
            }

            MovimientoInventario movimiento = new MovimientoInventario(
                    0,
                    empleadoId,
                    2,
                    item.productoId(),
                    item.cantidad(),
                    stockActual,
                    stockActual - item.cantidad(),
                    "Venta " + ventaId,
                    LocalDate.now()
            );
            movimientoInventarioRepository.guardar(conn, movimiento);
        }
    }

    private void actualizarCaja(Connection conn, int empleadoId, int ventaId, MetodoPago metodoPago, double montoEfectivo)
            throws SQLException {
        if (metodoPago != MetodoPago.EFECTIVO && metodoPago != MetodoPago.MIXTO) {
            return;
        }

        cajaRepository.registrarIngresoVenta(conn, empleadoId, ventaId, montoEfectivo);
    }

    private void acreditarPuntos(Connection conn, ClienteConCuenta cliente, int ventaId, int puntos) throws SQLException {
        if (cliente == null || cliente.cuentaId() == null || puntos <= 0) {
            return;
        }

        cuentaFidelizacionRepository.acreditarPuntos(conn, cliente.cuentaId(), ventaId, puntos);
    }

    private PagoProcesado procesarPago(MetodoPago metodoPago, double total, double montoRecibido, String referenciaPago) {
        if (metodoPago == null) {
            throw new IllegalArgumentException("You must select a payment method");
        }

        return switch (metodoPago) {
            case EFECTIVO -> procesarPagoEfectivo(total, montoRecibido);
            case TARJETA -> procesarPagoTarjeta(referenciaPago, "AUTH");
            case TRANSFERENCIA -> new PagoProcesado(0, 0, limpiarReferencia(referenciaPago, "TRF"));
            case MIXTO -> procesarPagoMixto(total, montoRecibido, referenciaPago);
        };
    }

    private PagoProcesado procesarPagoEfectivo(double total, double montoRecibido) {
        if (montoRecibido < total) {
            throw new IllegalArgumentException("Insufficient amount. Missing: " + formatearMoneda(total - montoRecibido));
        }
        return new PagoProcesado(redondear(montoRecibido - total), montoRecibido, null);
    }

    private PagoProcesado procesarPagoTarjeta(String referenciaPago, String prefijo) {
        String referencia = referenciaPago == null ? "" : referenciaPago.trim().toUpperCase();
        if ("DECLINE".equals(referencia)) {
            throw new IllegalArgumentException("Payment rejected");
        }
        if ("ERROR".equals(referencia)) {
            throw new IllegalArgumentException("Error processing card payment. Try another method or check the connection");
        }
        return new PagoProcesado(0, 0, limpiarReferencia(referenciaPago, prefijo));
    }

    private PagoProcesado procesarPagoMixto(double total, double montoRecibido, String referenciaPago) {
        if (montoRecibido <= 0 || montoRecibido >= total) {
            throw new IllegalArgumentException("Para pago mixto el efectivo debe ser mayor a cero y menor al total");
        }
        PagoProcesado tarjeta = procesarPagoTarjeta(referenciaPago, "MIX");
        return new PagoProcesado(tarjeta.cambio(), montoRecibido, tarjeta.autorizacion());
    }

    private String limpiarReferencia(String referenciaPago, String prefijo) {
        if (referenciaPago != null && !referenciaPago.isBlank()) {
            return referenciaPago.trim().toUpperCase();
        }
        return prefijo + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private int obtenerTipoPagoId(MetodoPago metodoPago) {
        return switch (metodoPago) {
            case EFECTIVO -> 1;
            case TARJETA -> 2;
            case TRANSFERENCIA -> 3;
            case MIXTO -> 4;
        };
    }

    private double calcularIncrementoCaja(MetodoPago metodoPago, double total, PagoProcesado pago) {
        return switch (metodoPago) {
            case EFECTIVO -> total;
            case MIXTO -> pago.montoEfectivo();
            case TARJETA, TRANSFERENCIA -> 0;
        };
    }

    private void generarTicketArchivo(String numeroTicket, int ventaId, SolicitudVenta solicitud,
                                      ResumenVenta resumen, PagoProcesado pago) throws IOException {
        Path directorio = Path.of("target", "tickets");
        Files.createDirectories(directorio);

        StringBuilder contenido = new StringBuilder();
        contenido.append("MasterMarket").append(System.lineSeparator());
        contenido.append("Ticket: ").append(numeroTicket).append(System.lineSeparator());
        contenido.append("Venta BD: ").append(ventaId).append(System.lineSeparator());
        contenido.append("Cajero: ").append(solicitud.empleado().getNombre()).append(" ")
                .append(solicitud.empleado().getApellido()).append(System.lineSeparator());
        contenido.append("Turno: ").append(solicitud.turno()).append(System.lineSeparator());
        if (solicitud.cliente() != null) {
            contenido.append("Cliente: ").append(solicitud.cliente().cliente().getNombreCompleto()).append(System.lineSeparator());
        }
        contenido.append(System.lineSeparator());

        for (ItemVenta item : solicitud.items()) {
            contenido.append("#").append(item.productoId())
                    .append(" ").append(item.nombreProducto())
                    .append(" x").append(item.cantidad())
                    .append(" -> ").append(formatearMoneda(item.subtotal()))
                    .append(System.lineSeparator());
        }

        contenido.append(System.lineSeparator());
        contenido.append("Subtotal: ").append(formatearMoneda(resumen.subtotal())).append(System.lineSeparator());
        contenido.append("Descuentos: ").append(formatearMoneda(resumen.descuentoTotal())).append(System.lineSeparator());
        contenido.append("Impuestos: ").append(formatearMoneda(resumen.impuestos())).append(System.lineSeparator());
        contenido.append("Total: ").append(formatearMoneda(resumen.total())).append(System.lineSeparator());
        contenido.append("Pago: ").append(solicitud.metodoPago().name()).append(System.lineSeparator());
        if (pago.autorizacion() != null) {
            contenido.append("Autorizacion: ").append(pago.autorizacion()).append(System.lineSeparator());
        }
        if (solicitud.enviarPorCorreo()) {
            contenido.append("Enviar por correo a: ").append(solicitud.correoTicket()).append(System.lineSeparator());
        }
        if (solicitud.facturaElectronica()) {
            contenido.append("Factura electronica: ").append(solicitud.datosFacturacion()).append(System.lineSeparator());
        }

        Files.writeString(directorio.resolve("ticket-" + numeroTicket + ".txt"), contenido.toString());
    }

    private int parsearEntero(String valor, String mensajeError) {
        try {
            return Integer.parseInt(valor == null ? "" : valor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(mensajeError);
        }
    }

    private String generarTicket() {
        return "TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private void validarTurno(String turno) {
        if (turno == null || turno.isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar el turno de la venta");
        }
        try {
            Turno.valueOf(turno);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Turno no valido: " + turno);
        }
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    public static String formatearMoneda(double valor) {
        return NumberFormat.getCurrencyInstance(LOCALE_CO).format(valor);
    }

    public enum MetodoPago {
        EFECTIVO,
        TARJETA,
        TRANSFERENCIA,
        MIXTO
    }

    public enum TipoDescuento {
        NINGUNO,
        PORCENTAJE,
        VALOR_FIJO
    }

    public enum Turno {
        MANANA,
        TARDE,
        NOCHE
    }

    public record ClienteConCuenta(Cliente cliente, Integer cuentaId, Integer numeroTarjeta, Integer puntosActuales) {
    }

    public record ItemVenta(int productoId, String nombreProducto, int cantidad,
                            double precioUnitario, int stockDisponible) {
        public double subtotal() {
            return redondear(cantidad * precioUnitario);
        }
    }

    public record DescuentoManual(TipoDescuento tipo, double valor) {
        public static DescuentoManual ninguno() {
            return new DescuentoManual(TipoDescuento.NINGUNO, 0);
        }

        public double calcular(double base) {
            if (tipo == null || tipo == TipoDescuento.NINGUNO || valor <= 0) {
                return 0;
            }
            return switch (tipo) {
                case PORCENTAJE -> redondear(Math.min(base * (valor / 100), base));
                case VALOR_FIJO -> redondear(Math.min(valor, base));
                case NINGUNO -> 0;
            };
        }
    }

    public record ResumenVenta(double subtotal, double descuentoPromocion, double descuentoFidelidad,
                               double descuentoManual, double descuentoTotal, double impuestos,
                               double total, int puntosGanados) {
    }

    public record SolicitudVenta(List<ItemVenta> items, ClienteConCuenta cliente, Empleado empleado,
                                 String turno, DescuentoManual descuentoManual, MetodoPago metodoPago,
                                 double montoRecibido, String referenciaPago, boolean facturaElectronica,
                                 String datosFacturacion, boolean enviarPorCorreo, String correoTicket) {
    }

    public record ResultadoVenta(String numeroTicket, double cambio, String mensaje,
                                 double total, String autorizacionPago) {
    }

    private record PagoProcesado(double cambio, double montoEfectivo, String autorizacion) {
    }
}
