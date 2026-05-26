package adapters.iot;

public interface StockSensorPort {

    Integer obtenerStockTiempoReal(
            String codigoProducto
    );
}
