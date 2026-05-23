package controllers;

import controllers.admin.ClienteController;
import controllers.admin.EmpleadoController;
import controllers.admin.ProductoController;
import controllers.autenticacion.LoginController;
import controllers.caja.CierreCajaController;
import controllers.cajero.DevolucionController;
import controllers.cajero.PosController;
import controllers.gerente.DashboardController;
import controllers.supervisor.InventarioController;
import repositories.DatabaseConnection;
import repositories.DetalleVentaRepository;
import repositories.H2CierreCajaRepository;
import repositories.H2DetalleVentaRepository;
import repositories.H2ClienteRepository;
import repositories.H2CuentaFidelizacionRepository;
import repositories.H2EmpleadoRepository;
import repositories.H2MovimientoInventarioRepository;
import repositories.H2PagoVentaRepository;
import repositories.H2ProductoRepository;
import repositories.H2UsuarioRepository;
import repositories.H2VentaRepository;
import repositories.PagoVentaRepository;
import repositories.VentaRepository;
import services.autenticacion.AutenticarEmpleadoUseCase;
import services.caja.GestionarCierreCajaUseCase;
import services.clientes.ConsultarClienteUseCase;
import services.clientes.DesactivarClienteUseCase;
import services.clientes.GestionarPuntosUseCase;
import services.clientes.ModificarClienteUseCase;
import services.clientes.RegistrarClienteUseCase;
import services.empleados.ConsultarEmpleadoUseCase;
import services.empleados.DesactivarEmpleadoUseCase;
import services.empleados.ModificarEmpleadoUseCase;
import services.empleados.RegistrarEmpleadoUseCase;
import services.inventario.ControlarInventarioUseCase;
import services.productos.ConsultarProductoUseCase;
import services.productos.ListarProductosStockBajoUseCase;
import services.productos.ModificarProductoUseCase;
import services.productos.RegistrarProductoUseCase;
import services.ventas.ProcesarDevolucionUseCase;
import services.ventas.ProcesarFinalizarVentaUseCase;
import repositories.H2DevolucionRepository;
import repositories.H2DetalleDevolucionRepository;
import repositories.H2CajaRepository;


public final class AppDependencies {

    private static final AppDependencies INSTANCE = new AppDependencies();

    // ── Infraestructura ────────────────────────────────────────
    private final DatabaseConnection databaseConnection;

    // ── Repositorios ───────────────────────────────────────────
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

    // ── Casos de uso ───────────────────────────────────────────
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

    private final RegistrarProductoUseCase registrarProductoUseCase;
    private final ModificarProductoUseCase modificarProductoUseCase;
    private final ConsultarProductoUseCase consultarProductoUseCase;
    private final ListarProductosStockBajoUseCase listarProductosStockBajoUseCase;

    private final ProcesarFinalizarVentaUseCase procesarFinalizarVentaUseCase;
    private final ProcesarDevolucionUseCase procesarDevolucionUseCase;
    private final GestionarCierreCajaUseCase gestionarCierreCajaUseCase;
    private final ControlarInventarioUseCase controlarInventarioUseCase;

    private AppDependencies() {
        this.databaseConnection = new DatabaseConnection();

        // Repositorios
        this.usuarioRepository              = new H2UsuarioRepository(databaseConnection);
        this.clienteRepository              = new H2ClienteRepository(databaseConnection);
        this.cuentaFidelizacionRepository   = new H2CuentaFidelizacionRepository(databaseConnection);
        this.productoRepository             = new H2ProductoRepository(databaseConnection);
        this.movimientoInventarioRepository = new H2MovimientoInventarioRepository(databaseConnection);
        this.cierreCajaRepository           = new H2CierreCajaRepository(databaseConnection);
        this.ventaRepository                = new H2VentaRepository();
        this.detalleVentaRepository         = new H2DetalleVentaRepository();
        this.pagoVentaRepository            = new H2PagoVentaRepository();
        this.devolucionRepository           = new H2DevolucionRepository();
        this.detalleDevolucionRepository    = new H2DetalleDevolucionRepository();
        this.cajaRepository                 = new H2CajaRepository();
        this.empleadoRepository             = new H2EmpleadoRepository(databaseConnection);

        // Casos de uso — autenticación
        this.autenticarEmpleadoUseCase = new AutenticarEmpleadoUseCase(usuarioRepository);

        // Casos de uso — clientes
        this.registrarClienteUseCase  = new RegistrarClienteUseCase(clienteRepository, cuentaFidelizacionRepository);
        this.consultarClienteUseCase  = new ConsultarClienteUseCase(clienteRepository);
        this.modificarClienteUseCase  = new ModificarClienteUseCase(clienteRepository);
        this.desactivarClienteUseCase = new DesactivarClienteUseCase(clienteRepository);
        this.gestionarPuntosUseCase   = new GestionarPuntosUseCase(cuentaFidelizacionRepository);

        // Casos de uso — empleados
        this.registrarEmpleadoUseCase  = new RegistrarEmpleadoUseCase(empleadoRepository);
        this.consultarEmpleadoUseCase  = new ConsultarEmpleadoUseCase(empleadoRepository);
        this.modificarEmpleadoUseCase  = new ModificarEmpleadoUseCase(empleadoRepository);
        this.desactivarEmpleadoUseCase = new DesactivarEmpleadoUseCase(empleadoRepository);

        // Casos de uso — productos
        this.registrarProductoUseCase        = new RegistrarProductoUseCase(productoRepository);
        this.modificarProductoUseCase        = new ModificarProductoUseCase(productoRepository);
        this.consultarProductoUseCase        = new ConsultarProductoUseCase(productoRepository);
        this.listarProductosStockBajoUseCase = new ListarProductosStockBajoUseCase(productoRepository);

        // Casos de uso — ventas, devoluciones, caja, inventario
        this.procesarFinalizarVentaUseCase = new ProcesarFinalizarVentaUseCase(
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
        this.procesarDevolucionUseCase = new ProcesarDevolucionUseCase(
                databaseConnection,
                devolucionRepository,
                detalleDevolucionRepository,
                productoRepository,
                movimientoInventarioRepository,
                cajaRepository,
                usuarioRepository,
                cuentaFidelizacionRepository
        );
        this.gestionarCierreCajaUseCase = new GestionarCierreCajaUseCase(cierreCajaRepository);
        this.controlarInventarioUseCase = new ControlarInventarioUseCase(productoRepository, movimientoInventarioRepository);
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

        try {
            return controllerType.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "No se pudo crear el controlador: " + controllerType.getName(), e);
        }
    }
}