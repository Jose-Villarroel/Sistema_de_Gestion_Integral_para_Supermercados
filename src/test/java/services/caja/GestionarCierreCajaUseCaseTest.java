package services.caja;

import dtos.ResumenCierreCajaDTO;
import entities.CierreCaja;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import repositories.CierreCajaRepository;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2CierreCajaRepository;
import valueobjects.EstadoCierreCaja;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Pruebas de integracion del servicio GestionarCierreCajaUseCase.
 *
 * Cubre el CU-011: Gestionar cierre de caja, incluyendo:
 *   - obtenerResumen: consulta de transacciones del turno.
 *   - registrarCierre: registro del cierre con estados CUADRADO, SOBRANTE y FALTANTE,
 *     validaciones de entrada y verificacion de duplicados.
 *
 */
@DisplayName("Pruebas de integracion - GestionarCierreCajaUseCase (CU-011)")
class GestionarCierreCajaUseCaseTest {

    private static final int EMPLEADO_ID = 1;
    private static final LocalDate FECHA = LocalDate.of(2026, 5, 15);
    private static final String TURNO = "MANANA";

    private GestionarCierreCajaUseCase useCase;
    private DatabaseConnection dbConnection;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Resetear la BD a un estado conocido
        dbConnection = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(dbConnection);
        initializer.init();

        // 2. Instanciar dependencias reales
        CierreCajaRepository cierreCajaRepository = new H2CierreCajaRepository(dbConnection);

        // 3. Instanciar el UseCase a probar
        useCase = new GestionarCierreCajaUseCase(cierreCajaRepository);
    }

    /*
     * Inserta una Venta + Caja vinculadas para que obtenerResumenTurno
     * encuentre datos. Retorna el efectivo esperado generado.
     */
    private BigDecimal insertarVentaConCaja(LocalDate fecha, String turno,
                                            BigDecimal montoFinalCaja) throws Exception {
        try (Connection conn = dbConnection.getConnection()) {
            // Insertar Venta
            int idVenta;
            String sqlVenta = """
                    INSERT INTO Venta
                    (id_empleado, fecha_venta, turno, metodo_pago, subtotal,
                     descuento_total, impuesto_total, total_final, estado_venta)
                    VALUES (?, ?, ?, 'EFECTIVO', ?, 0, 0, ?, TRUE)
                    """;
            try (PreparedStatement ps = conn.prepareStatement(
                    sqlVenta, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, EMPLEADO_ID);
                ps.setDate(2, Date.valueOf(fecha));
                ps.setString(3, turno);
                ps.setBigDecimal(4, montoFinalCaja);
                ps.setBigDecimal(5, montoFinalCaja);
                ps.executeUpdate();
                var rs = ps.getGeneratedKeys();
                rs.next();
                idVenta = rs.getInt(1);
            }

            // Insertar Caja vinculada
            String sqlCaja = """
                    INSERT INTO Caja
                    (id_empleado, id_venta, fecha_apertura, fecha_cierre,
                     monto_inicial, monto_final, estado)
                    VALUES (?, ?, ?, ?, 0, ?, TRUE)
                    """;
            try (PreparedStatement ps = conn.prepareStatement(sqlCaja)) {
                ps.setInt(1, EMPLEADO_ID);
                ps.setInt(2, idVenta);
                ps.setDate(3, Date.valueOf(fecha));
                ps.setDate(4, Date.valueOf(fecha));
                ps.setBigDecimal(5, montoFinalCaja);
                ps.executeUpdate();
            }
        }
        return montoFinalCaja;
    }

    // ===========================================================
    // obtenerResumen
    // ===========================================================

    /*
     * CP-001: obtenerResumen sin ventas del turno retorna 0 transacciones
     * y efectivo esperado 0.
     */
    @Test
    @DisplayName("CP-001: obtenerResumen sin ventas retorna ceros")
    void obtenerResumen_sinVentas_retornaCeros() throws Exception {
        ResumenCierreCajaDTO resumen = useCase.obtenerResumen(FECHA, TURNO);

        assertNotNull(resumen, "El resumen no debe ser nulo");
        assertEquals(0, resumen.getTotalTransacciones(),
                "Sin ventas, las transacciones del turno deben ser 0");
        assertEquals(0, resumen.getEfectivoEsperado().compareTo(BigDecimal.ZERO),
                "Sin ventas, el efectivo esperado debe ser 0");
    }

    /*
     * CP-002: obtenerResumen con una venta del turno retorna el monto
     * de la caja vinculada y el conteo de transacciones.
     */
    @Test
    @DisplayName("CP-002: obtenerResumen con una venta retorna su monto")
    void obtenerResumen_conUnaVenta_retornaMontoCorrecto() throws Exception {
        insertarVentaConCaja(FECHA, TURNO, new BigDecimal("100.00"));

        ResumenCierreCajaDTO resumen = useCase.obtenerResumen(FECHA, TURNO);

        assertEquals(1, resumen.getTotalTransacciones(),
                "Debe contar la venta insertada");
        assertEquals(0, resumen.getEfectivoEsperado().compareTo(new BigDecimal("100.00")),
                "El efectivo esperado debe coincidir con el monto_final de la Caja");
    }

    // ===========================================================
    // registrarCierre - validaciones de entrada
    // ===========================================================

    /*
     * CP-003: registrarCierre con efectivoContado null lanza excepcion.
     */
    @Test
    @DisplayName("CP-003: registrarCierre con efectivo null lanza IllegalArgumentException")
    void registrarCierre_efectivoNull_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.registrarCierre(FECHA, TURNO, EMPLEADO_ID, null, "Sin observaciones")
        );
        assertEquals("El efectivo contado es obligatorio.", ex.getMessage());
    }

    /*
     * CP-004: registrarCierre con efectivoContado negativo lanza excepcion.
     */
    @Test
    @DisplayName("CP-004: registrarCierre con efectivo negativo lanza IllegalArgumentException")
    void registrarCierre_efectivoNegativo_lanzaExcepcion() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> useCase.registrarCierre(
                        FECHA, TURNO, EMPLEADO_ID, new BigDecimal("-10.00"), "Sin obs")
        );
        assertEquals("El efectivo contado no puede ser negativo.", ex.getMessage());
    }

    /*
     * CP-005: Intento de registrar dos cierres para la misma fecha y turno.
     * El segundo debe lanzar IllegalStateException.
     */
    @Test
    @DisplayName("CP-005: registrarCierre duplicado lanza IllegalStateException")
    void registrarCierre_duplicado_lanzaExcepcion() throws Exception {
        // Primer cierre exitoso (sin ventas, todo cero, queda CUADRADO)
        useCase.registrarCierre(FECHA, TURNO, EMPLEADO_ID, BigDecimal.ZERO, "Primer cierre");

        // Segundo intento debe fallar
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> useCase.registrarCierre(FECHA, TURNO, EMPLEADO_ID, BigDecimal.ZERO, "Duplicado")
        );
        assertEquals("Ya existe un cierre para la fecha y turno indicados.", ex.getMessage());
    }

    // ===========================================================
    // registrarCierre - estados del cierre (CUADRADO, SOBRANTE, FALTANTE)
    // ===========================================================

    /*
     * CP-006: Cierre CUADRADO cuando efectivo contado = efectivo esperado.
     * Sin ventas el esperado es 0, contado=0 da diferencia=0 -> CUADRADO.
     */
    @Test
    @DisplayName("CP-006: registrarCierre cuadrado (contado = esperado)")
    void registrarCierre_cuadrado_estadoCUADRADO() throws Exception {
        CierreCaja cierre = useCase.registrarCierre(
                FECHA, TURNO, EMPLEADO_ID, BigDecimal.ZERO, "Cierre cuadrado");

        assertNotNull(cierre, "El cierre no debe ser nulo");
        assertEquals(EstadoCierreCaja.CUADRADO, cierre.getEstadoCierre(),
                "Con contado igual al esperado, el estado debe ser CUADRADO");
        assertEquals(0, cierre.getDiferencia().compareTo(BigDecimal.ZERO),
                "La diferencia debe ser 0");
        assertEquals(EMPLEADO_ID, cierre.getIdEmpleado());
        assertEquals(FECHA, cierre.getFechaCierre());
        assertEquals(TURNO, cierre.getTurno());
        assertEquals("Cierre cuadrado", cierre.getObservacion());
        assertNotNull(cierre.getNumeroCierre(), "Debe generarse un numero de cierre");
        assertTrue(cierre.getNumeroCierre().startsWith("CC-"),
                "El numero de cierre debe seguir el formato CC-XXXXXXXX");
    }

    /*
     * CP-007: Cierre SOBRANTE cuando efectivo contado > efectivo esperado.
     * Sin ventas el esperado es 0, contado=50 da diferencia=+50 -> SOBRANTE.
     */
    @Test
    @DisplayName("CP-007: registrarCierre sobrante (contado > esperado)")
    void registrarCierre_sobrante_estadoSOBRANTE() throws Exception {
        CierreCaja cierre = useCase.registrarCierre(
                FECHA, TURNO, EMPLEADO_ID, new BigDecimal("50.00"), "Sobra dinero");

        assertEquals(EstadoCierreCaja.SOBRANTE, cierre.getEstadoCierre(),
                "Con contado mayor al esperado, el estado debe ser SOBRANTE");
        assertEquals(0, cierre.getDiferencia().compareTo(new BigDecimal("50.00")),
                "La diferencia debe ser +50.00");
    }

    /*
     * CP-008: Cierre FALTANTE cuando efectivo contado < efectivo esperado.
     * Insertamos una venta con monto 100 -> esperado=100. Contado=60 da
     * diferencia=-40 -> FALTANTE.
     */
    @Test
    @DisplayName("CP-008: registrarCierre faltante (contado < esperado)")
    void registrarCierre_faltante_estadoFALTANTE() throws Exception {
        insertarVentaConCaja(FECHA, TURNO, new BigDecimal("100.00"));

        CierreCaja cierre = useCase.registrarCierre(
                FECHA, TURNO, EMPLEADO_ID, new BigDecimal("60.00"), "Falta dinero");

        assertEquals(EstadoCierreCaja.FALTANTE, cierre.getEstadoCierre(),
                "Con contado menor al esperado, el estado debe ser FALTANTE");
        assertEquals(0, cierre.getDiferencia().compareTo(new BigDecimal("-40.00")),
                "La diferencia debe ser -40.00");
        assertEquals(0, cierre.getEfectivoEsperado().compareTo(new BigDecimal("100.00")),
                "El efectivo esperado debe ser el de la venta insertada");
        assertEquals(1, cierre.getTotalTransacciones(),
                "Debe registrar 1 transaccion (la venta insertada)");
    }
}