package services.clientes;

import aggregates.Cliente;
import entities.CuentaFidelizacion;
import repositories.ClienteRepository;
import repositories.CuentaFidelizacionRepository;

import java.time.LocalDate;
import java.util.concurrent.ThreadLocalRandom;

public class RegistrarClienteUseCase {

    private final ClienteRepository clienteRepository;
    private final CuentaFidelizacionRepository cuentaRepository;

    public RegistrarClienteUseCase(ClienteRepository clienteRepository,
                                   CuentaFidelizacionRepository cuentaRepository) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
    }

    /**
     * Registra un cliente nuevo y le crea automáticamente su cuenta de fidelización
     * con 0 puntos iniciales y un número de tarjeta generado aleatoriamente.
     */
    public Cliente ejecutar(String nombre, String apellido, String correo,
                            String telefono, String direccion) {

        Cliente nuevo = new Cliente(
                0,
                nombre,
                apellido,
                correo,
                telefono,
                direccion,
                LocalDate.now(),
                true
        );

        Cliente guardado = clienteRepository.guardar(nuevo);

        // Crear automáticamente la cuenta de fidelización
        int numeroTarjeta = generarNumeroTarjeta();
        CuentaFidelizacion cuenta = new CuentaFidelizacion(
                0,
                guardado.getId(),
                numeroTarjeta,
                0,
                LocalDate.now(),
                true
        );
        cuentaRepository.guardar(cuenta);

        return guardado;
    }

    private int generarNumeroTarjeta() {
        // Genera un número de 8 dígitos entre 10000000 y 99999999
        return ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000);
    }
}