package adapters.iot;

public class SimulatedStockSensorAdapter implements StockSensorPort {

    @Override
    public Integer obtenerStockTiempoReal(String codigoProducto) {
        System.out.println("Sensor IoT no conectado: simulación activa");
        return null;
    }
}
