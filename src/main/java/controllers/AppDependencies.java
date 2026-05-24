package controllers;

import controllers.admin.*;
import controllers.autenticacion.LoginController;
import controllers.caja.CierreCajaController;
import controllers.cajero.DevolucionController;
import controllers.cajero.PosController;
import controllers.gerente.ReporteVentasController;
import controllers.supervisor.InventarioController;
import repositories.*;
import services.autenticacion.AutenticarEmpleadoUseCase;
import services.caja.GestionarCierreCajaUseCase;
import services.clientes.*;
import services.empleados.*;
import services.inventario.ControlarInventarioUseCase;
import services.ordenes.*;
import services.productos.*;
import services.proveedores.*;
import services.reportes.GenerarReporteVentasUseCase;
import services.ventas.*;

public final class AppDependencies {

    private static final AppDependencies INSTANCE = new AppDependencies();

    private final DatabaseConnection databaseConnection;

    private final H2UsuarioRepository usuarioRepository;
    private final H2ClienteRepository clienteRepository;
    private final H2CuentaFidelizacionRepository cuentaFidelizacionRepository;
    private final H2ProductoRepository productoRepository;
    private final H2MovimientoInventarioRepository movimientoInventarioRepository;
    private final H2CierreCajaRepository cierreCajaRepository;
    private final VentaRepository ventaRepository;
    private final DetalleVentaRepository detalleVentaRepository;
    private final PagoVentaRepository pagoVentaRepository;
    private final H2DevolucionRepository devolucionRepository;
    private final H2DetalleDevolucionRepository detalleDevolucionRepository;
    private final H2CajaRepository cajaRepository;
    private final H2EmpleadoRepository empleadoRepository;
    private final H2ProveedorRepository proveedorRepository;
    private final H2OrdenCompraRepository ordenCompraRepository;
    private final ReporteVentasRepository reporteVentasRepository;

    private final AutenticarEmpleadoUseCase autenticarEmpleadoUseCase;

    private final RegistrarClienteUseCase registrarClienteUseCase;
    private final ConsultarClienteUseCase consultarClienteUseCase;
    private final ModificarClienteUseCase modificarClienteUseCase;
    private final DesactivarClienteUseCase desactivarClienteUseCase;
    private final GestionarPuntosUseCase gestionarPuntosUseCase;

    private final RegistrarEmpleadoUseCase registrarEmpleadoUseCase;
    private final ConsultarEmpleadoUseCase consultarEmpleadoUseCase;
    private final ModificarEmpleadoUseCase modificarEmpleadoUseCase;
    private final DesactivarEmpleadoUseCase desactivarEmpleadoUseCase;

    private final RegistrarProveedorUseCase registrarProveedorUseCase;
    private final ConsultarProveedorUseCase consultarProveedorUseCase;
    private final ModificarProveedorUseCase modificarProveedorUseCase;
    private final DesactivarProveedorUseCase desactivarProveedorUseCase;

    private final CrearOrdenCompraUseCase crearOrdenCompraUseCase;
    private final ConsultarOrdenCompraUseCase consultarOrdenCompraUseCase;
    private final CancelarOrdenCompraUseCase cancelarOrdenCompraUseCase;

    private final RegistrarProductoUseCase registrarProductoUseCase;
    private final ModificarProductoUseCase modificarProductoUseCase;
    private final ConsultarProductoUseCase consultarProductoUseCase;
    private final ListarProductosStockBajoUseCase listarProductosStockBajoUseCase;

    private final ProcesarFinalizarVentaUseCase procesarFinalizarVentaUseCase;
    private final ProcesarDevolucionUseCase procesarDevolucionUseCase;
    private final GestionarCierreCajaUseCase gestionarCierreCajaUseCase;
    private final ControlarInventarioUseCase controlarInventarioUseCase;

    private final GenerarReporteVentasUseCase generarReporteVentasUseCase;

    private AppDependencies() {
        databaseConnection = new DatabaseConnection();

        usuarioRepository = new H2UsuarioRepository(databaseConnection);
        clienteRepository = new H2ClienteRepository(databaseConnection);
        cuentaFidelizacionRepository = new H2CuentaFidelizacionRepository(databaseConnection);
        productoRepository = new H2ProductoRepository(databaseConnection);
        movimientoInventarioRepository = new H2MovimientoInventarioRepository(databaseConnection);
        cierreCajaRepository = new H2CierreCajaRepository(databaseConnection);
        ventaRepository = new H2VentaRepository();
        detalleVentaRepository = new H2DetalleVentaRepository();
        pagoVentaRepository = new H2PagoVentaRepository();
        devolucionRepository = new H2DevolucionRepository();
        detalleDevolucionRepository = new H2DetalleDevolucionRepository();
        cajaRepository = new H2CajaRepository();
        empleadoRepository = new H2EmpleadoRepository(databaseConnection);
        proveedorRepository = new H2ProveedorRepository(databaseConnection);
        ordenCompraRepository = new H2OrdenCompraRepository(databaseConnection);
        reporteVentasRepository = new H2ReporteVentasRepository(databaseConnection);

        autenticarEmpleadoUseCase = new AutenticarEmpleadoUseCase(usuarioRepository);

        registrarClienteUseCase = new RegistrarClienteUseCase(clienteRepository, cuentaFidelizacionRepository);
        consultarClienteUseCase = new ConsultarClienteUseCase(clienteRepository);
        modificarClienteUseCase = new ModificarClienteUseCase(clienteRepository);
        desactivarClienteUseCase = new DesactivarClienteUseCase(clienteRepository);
        gestionarPuntosUseCase = new GestionarPuntosUseCase(cuentaFidelizacionRepository);

        registrarEmpleadoUseCase = new RegistrarEmpleadoUseCase(empleadoRepository);
        consultarEmpleadoUseCase = new ConsultarEmpleadoUseCase(empleadoRepository);
        modificarEmpleadoUseCase = new ModificarEmpleadoUseCase(empleadoRepository);
        desactivarEmpleadoUseCase = new DesactivarEmpleadoUseCase(empleadoRepository);

        registrarProveedorUseCase = new RegistrarProveedorUseCase(proveedorRepository);
        consultarProveedorUseCase = new ConsultarProveedorUseCase(proveedorRepository);
        modificarProveedorUseCase = new ModificarProveedorUseCase(proveedorRepository);
        desactivarProveedorUseCase = new DesactivarProveedorUseCase(proveedorRepository);

        crearOrdenCompraUseCase = new CrearOrdenCompraUseCase(ordenCompraRepository);
        consultarOrdenCompraUseCase = new ConsultarOrdenCompraUseCase(ordenCompraRepository);
        cancelarOrdenCompraUseCase = new CancelarOrdenCompraUseCase(ordenCompraRepository);

        registrarProductoUseCase = new RegistrarProductoUseCase(productoRepository);
        modificarProductoUseCase = new ModificarProductoUseCase(productoRepository);
        consultarProductoUseCase = new ConsultarProductoUseCase(productoRepository);
        listarProductosStockBajoUseCase = new ListarProductosStockBajoUseCase(productoRepository);

        procesarFinalizarVentaUseCase = new ProcesarFinalizarVentaUseCase(
                databaseConnection,
                clienteRepository,
                cuentaFidelizacionRepository,
                productoRepository,
                ventaRepository,
                detalleVentaRepository,
                pagoVentaRepository,
                movimientoInventarioRepository,
                cajaRepository
        );

        procesarDevolucionUseCase = new ProcesarDevolucionUseCase(
                databaseConnection,
                devolucionRepository,
                detalleDevolucionRepository,
                productoRepository,
                movimientoInventarioRepository,
                cajaRepository,
                usuarioRepository,
                cuentaFidelizacionRepository
        );

        gestionarCierreCajaUseCase = new GestionarCierreCajaUseCase(cierreCajaRepository);

        controlarInventarioUseCase = new ControlarInventarioUseCase(
                productoRepository,
                movimientoInventarioRepository
        );

        generarReporteVentasUseCase = new GenerarReporteVentasUseCase(reporteVentasRepository);
    }

    public static AppDependencies getInstance() {
        return INSTANCE;
    }

    public Object createController(Class<?> controllerType) {

        if (controllerType == LoginController.class) {
            return new LoginController(autenticarEmpleadoUseCase);
        }

        if (controllerType == EmpleadoController.class) {
            return new EmpleadoController(
                    registrarEmpleadoUseCase,
                    consultarEmpleadoUseCase,
                    modificarEmpleadoUseCase,
                    desactivarEmpleadoUseCase
            );
        }

        if (controllerType == ProveedorController.class) {
            return new ProveedorController(
                    registrarProveedorUseCase,
                    consultarProveedorUseCase,
                    modificarProveedorUseCase,
                    desactivarProveedorUseCase
            );
        }

        if (controllerType == OrdenCompraController.class) {
            return new OrdenCompraController(
                    crearOrdenCompraUseCase,
                    consultarOrdenCompraUseCase,
                    cancelarOrdenCompraUseCase,
                    consultarProveedorUseCase,
                    consultarProductoUseCase
            );
        }

        if (controllerType == ClienteController.class) {
            return new ClienteController(
                    cuentaFidelizacionRepository,
                    registrarClienteUseCase,
                    consultarClienteUseCase,
                    modificarClienteUseCase,
                    desactivarClienteUseCase,
                    gestionarPuntosUseCase
            );
        }

        if (controllerType == ProductoController.class) {
            return new ProductoController(
                    registrarProductoUseCase,
                    modificarProductoUseCase,
                    consultarProductoUseCase,
                    listarProductosStockBajoUseCase
            );
        }

        if (controllerType == PosController.class) {
            return new PosController(procesarFinalizarVentaUseCase);
        }

        if (controllerType == DevolucionController.class) {
            return new DevolucionController(procesarDevolucionUseCase);
        }

        if (controllerType == CierreCajaController.class) {
            return new CierreCajaController(gestionarCierreCajaUseCase);
        }

        if (controllerType == InventarioController.class) {
            return new InventarioController(
                    controlarInventarioUseCase,
                    productoRepository,
                    movimientoInventarioRepository
            );
        }

        if (controllerType == ReporteVentasController.class) {
            return new ReporteVentasController(generarReporteVentasUseCase);
        }

        try {
            return controllerType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo crear el controlador: " + controllerType.getName(), e
            );
        }
    }
}