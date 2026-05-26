package adapters.ml;

public class DemandPredictionService {

    private DemandPredictionPort predictor;

    public DemandPredictionService(DemandPredictionPort predictor) {
        this.predictor = predictor;
    }

    public void sugerirCompra(
            String producto) {
        Double prediccion = predictor.predecirDemanda(producto, 7);

        if (prediccion == -1) {
            System.out.println("Predicción no disponible");
        }

        if (prediccion > 0) {
            System.out.println("Demanda esperada: " + prediccion);
        }
    }
}
