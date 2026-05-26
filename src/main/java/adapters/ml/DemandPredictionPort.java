package adapters.ml;

public interface DemandPredictionPort {

    Double predecirDemanda(
            String producto,
            Integer dias
    );
}
