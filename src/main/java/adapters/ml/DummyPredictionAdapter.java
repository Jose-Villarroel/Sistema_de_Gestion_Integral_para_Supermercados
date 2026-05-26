package adapters.ml;

public class DummyPredictionAdapter implements DemandPredictionPort {

    @Override
    public Double predecirDemanda(String producto, Integer dias) {
        System.out.println("Modelo ML no configurado");
        return -1.0;
    }
}
