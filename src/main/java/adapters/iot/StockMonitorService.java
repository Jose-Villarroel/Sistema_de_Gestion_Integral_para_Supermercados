package adapters.iot;

public class StockMonitorService {

    private final StockSensorPort sensor;

    public StockMonitorService(StockSensorPort sensor) {
        this.sensor = sensor;
    }

    public void verificarStock(
            String codigoProducto) {
        Integer stock = sensor.obtenerStockTiempoReal(codigoProducto);

        if (stock == null) {
            System.out.println("Stock IoT no disponible");
            return;
        }

        if (stock < 10) {
            System.out.println("Alerta futura IoT: stock crítico");
        }
    }
}
