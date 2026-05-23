package services.clientes;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import repositories.CuentaFidelizacionRepository;
import repositories.DatabaseConnection;
import repositories.DatabaseInitializer;
import repositories.H2CuentaFidelizacionRepository;
import valueobjects.CategoriaFidelidad;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración del servicio GestionarPuntosUseCase (CU-007).
 *
 * Cubre las cuatro operaciones del programa de fidelización:
 * - acumularPorCompra: 1 punto por cada $1.000 de compra
 * - canjearPuntos: validación de saldo y resta
 * - consultarPuntos: consulta de saldo actual
 * - consultarCategoria: BRONCE (0-999), PLATA (1000-4999), ORO (5000+)
 *
 * Datos iniciales relevantes (cargados por DatabaseInitializer):
 * - Cliente 1 (Mariana): 120 puntos → BRONCE
 * - Cliente 3 (Camila): 0 puntos → BRONCE
 * - Cliente 5 (Valentina): 150 puntos → BRONCE
 */
@DisplayName("Pruebas del servicio GestionarPuntosUseCase")
class GestionarPuntosUseCaseTest {

        private GestionarPuntosUseCase useCase;
        private CuentaFidelizacionRepository cuentaRepository;

        @BeforeEach
        void setUp() throws Exception {
                DatabaseConnection conn = new DatabaseConnection();
                DatabaseInitializer initializer = new DatabaseInitializer(conn);
                initializer.init();

                cuentaRepository = new H2CuentaFidelizacionRepository(conn);
                useCase = new GestionarPuntosUseCase(cuentaRepository);
        }

        // ==================== acumularPorCompra ====================

        /*
         * CP-017: Verifica la primera validación del UseCase. Un monto de compra
         * cero o negativo no es válido y debe lanzar IllegalArgumentException
         * antes de tocar la base de datos.
         */
        @Test
        @DisplayName("CP-017: Acumular con monto cero debe lanzar excepción")
        void acumularPorCompra_conMontoCero_debeLanzarExcepcion() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> useCase.acumularPorCompra(1, 0));
                assertTrue(ex.getMessage().contains("positivo"),
                                "El mensaje debe explicar que el monto debe ser positivo");
        }

        /*
         * CP-018: Verifica el comportamiento cuando el cliente no tiene cuenta
         * de fidelización registrada. Usamos un id de cliente alto (99999) que
         * no existe en la BD. Debe lanzar IllegalArgumentException.
         */
        @Test
        @DisplayName("CP-018: Acumular para cliente sin cuenta debe lanzar excepción")
        void acumularPorCompra_clienteSinCuenta_debeLanzarExcepcion() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> useCase.acumularPorCompra(99999, 5000.0));
                assertTrue(ex.getMessage().contains("fidelizaci"),
                                "El mensaje debe mencionar la falta de cuenta de fidelización");
        }

        /*
         * CP-019: Verifica la rama del cálculo donde puntosGanados resulta cero.
         * La regla de negocio es 1 punto por cada $1.000, por lo que una compra
         * de $500 debe retornar 0 puntos sin modificar la cuenta del cliente.
         */
        @Test
        @DisplayName("CP-019: Acumular con monto menor a $1.000 debe retornar 0 puntos")
        void acumularPorCompra_montoMenorAUnPunto_debeRetornarCero() {
                int puntosIniciales = useCase.consultarPuntos(1);

                int puntosGanados = useCase.acumularPorCompra(1, 500.0);

                assertEquals(0, puntosGanados, "Una compra menor a $1.000 no debe generar puntos");
                assertEquals(puntosIniciales, useCase.consultarPuntos(1),
                                "El saldo del cliente no debe haber cambiado");
        }

        /*
         * CP-020: Verifica el flujo principal de acumulación. Cliente 1 (Mariana)
         * tiene 120 puntos iniciales. Una compra de $25.000 debe generar 25 puntos
         * adicionales (25.000 / 1.000 = 25), dejando un total de 145 puntos.
         */
        @Test
        @DisplayName("CP-020: Acumular con monto válido debe sumar puntos correctamente")
        void acumularPorCompra_conMontoValido_debeAcumularYPersistir() {
                int puntosIniciales = useCase.consultarPuntos(1);
                double montoCompra = 25000.0;
                int puntosEsperados = (int) (montoCompra / 1000.0);

                int puntosGanados = useCase.acumularPorCompra(1, montoCompra);

                assertEquals(puntosEsperados, puntosGanados,
                                "Debe ganar 1 punto por cada $1.000 de compra");
                assertEquals(puntosIniciales + puntosEsperados, useCase.consultarPuntos(1),
                                "El saldo debe reflejar los puntos acumulados");
        }

        /*
         * CP-021: Verifica que la acumulación de puntos puede cambiar la
         * categoría del cliente. Cliente 1 inicia con 120 puntos (BRONCE).
         * Acumulando $900.000 (= 900 puntos) llega a 1.020 puntos = PLATA.
         */
        @Test
        @DisplayName("CP-021: Acumular puntos suficientes debe cambiar categoría a PLATA")
        void acumularPorCompra_suficientesPuntos_debeCambiarCategoria() {
                assertEquals(CategoriaFidelidad.BRONCE, useCase.consultarCategoria(1),
                                "El cliente debe iniciar como BRONCE");

                useCase.acumularPorCompra(1, 900000.0);

                assertEquals(CategoriaFidelidad.PLATA, useCase.consultarCategoria(1),
                                "Tras acumular 900 puntos, el cliente debe pasar a PLATA");
        }

        // ==================== canjearPuntos ====================

        /*
         * CP-022: Verifica que canjear puntos para un cliente sin cuenta de
         * fidelización lanza excepción.
         */
        @Test
        @DisplayName("CP-022: Canjear puntos de cliente sin cuenta debe lanzar excepción")
        void canjearPuntos_clienteSinCuenta_debeLanzarExcepcion() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> useCase.canjearPuntos(99999, 50));
        }

        /*
         * CP-023: Verifica la validación de saldo insuficiente del aggregate
         * CuentaFidelizacion. Cliente 1 tiene 120 puntos; intentar canjear
         * 200 debe lanzar excepción con mensaje "Puntos insuficientes".
         */
        @Test
        @DisplayName("CP-023: Canjear más puntos de los disponibles debe lanzar excepción")
        void canjearPuntos_saldoInsuficiente_debeLanzarExcepcion() {
                IllegalArgumentException ex = assertThrows(
                                IllegalArgumentException.class,
                                () -> useCase.canjearPuntos(1, 200));
                assertTrue(ex.getMessage().contains("insuficientes"),
                                "El mensaje debe indicar puntos insuficientes");
        }

        /*
         * CP-024: Verifica el flujo principal de canje. Cliente 1 tiene 120
         * puntos; canjear 50 debe dejarlo con 70 y persistir el cambio en BD.
         */
        @Test
        @DisplayName("CP-024: Canjear puntos disponibles debe restar y persistir")
        void canjearPuntos_conSaldoSuficiente_debeRestarYPersistir() {
                int puntosIniciales = useCase.consultarPuntos(1);
                int puntosACanjear = 50;

                useCase.canjearPuntos(1, puntosACanjear);

                assertEquals(puntosIniciales - puntosACanjear, useCase.consultarPuntos(1),
                                "El saldo debe haberse reducido en la cantidad canjeada");
        }

        // ==================== consultarPuntos ====================

        /*
         * CP-025: Verifica que consultar puntos para un cliente sin cuenta
         * lanza excepción.
         */
        @Test
        @DisplayName("CP-025: Consultar puntos de cliente sin cuenta debe lanzar excepción")
        void consultarPuntos_clienteSinCuenta_debeLanzarExcepcion() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> useCase.consultarPuntos(99999));
        }

        // ==================== consultarCategoria ====================

        /*
         * CP-026: Verifica el cálculo de categoría BRONCE (0-999 puntos).
         * Cliente 1 tiene 120 puntos en la BD inicial.
         */
        @Test
        @DisplayName("CP-026: Cliente con 0-999 puntos debe ser categoría BRONCE")
        void consultarCategoria_conPocosPuntos_debeSerBronce() {
                assertEquals(CategoriaFidelidad.BRONCE, useCase.consultarCategoria(1),
                                "Con 120 puntos el cliente debe estar en BRONCE");
        }

        /*
         * CP-027: Verifica el cálculo de categoría PLATA (1000-4999 puntos).
         * Cliente 1 inicia con 120 puntos; tras acumular $1.000.000 llega a
         * 1.120 puntos, entrando al rango de PLATA.
         */
        @Test
        @DisplayName("CP-027: Cliente con 1000-4999 puntos debe ser categoría PLATA")
        void consultarCategoria_conPuntosMedios_debeSerPlata() {
                useCase.acumularPorCompra(1, 1000000.0);

                assertEquals(CategoriaFidelidad.PLATA, useCase.consultarCategoria(1),
                                "Con más de 1000 puntos el cliente debe estar en PLATA");
        }

        /*
         * CP-028: Verifica el cálculo de categoría ORO (5000+ puntos).
         * Cliente 1 inicia con 120 puntos; tras acumular $5.000.000 llega a
         * 5.120 puntos, entrando al rango de ORO.
         */
        @Test
        @DisplayName("CP-028: Cliente con 5000+ puntos debe ser categoría ORO")
        void consultarCategoria_conMuchosPuntos_debeSerOro() {
                useCase.acumularPorCompra(1, 5000000.0);

                assertEquals(CategoriaFidelidad.ORO, useCase.consultarCategoria(1),
                                "Con más de 5000 puntos el cliente debe estar en ORO");
        }

        /*
         * CP-029: Verifica que consultar la categoría de fidelidad para un cliente
         * sin cuenta lanza excepción.
         */
        @Test
        @DisplayName("CP-029: Consultar categoría de cliente sin cuenta debe lanzar excepción")
        void consultarCategoria_clienteSinCuenta_debeLanzarExcepcion() {
                assertThrows(
                                IllegalArgumentException.class,
                                () -> useCase.consultarCategoria(99999));
        }
}