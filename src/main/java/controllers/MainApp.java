package controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage stageActual;
    private static final AppDependencies DEPENDENCIES = AppDependencies.getInstance();

    @Override
    public void start(Stage stage) throws Exception {
        stageActual = stage;

        navegarA("/infrastructure/ui/autenticacion/login.fxml",
                "MasterMarket - Login", 420, 550);
    }

    public static void navegarA(String rutaFxml, String titulo, double ancho, double alto) {
        try {
            FXMLLoader loader = crearLoader(rutaFxml);
            Scene scene = new Scene(loader.load());
            stageActual.setTitle(titulo);
            stageActual.setScene(scene);
            stageActual.setWidth(ancho);
            stageActual.setHeight(alto);
            stageActual.centerOnScreen();
            stageActual.setResizable(false);
            stageActual.show();
        } catch (Exception e) {
            System.err.println("Error al navegar a: " + rutaFxml);
            e.printStackTrace();
        }
    }

    public static FXMLLoader crearLoader(String rutaFxml) {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(rutaFxml));
        loader.setControllerFactory(DEPENDENCIES::createController);
        return loader;
    }

    public static void main(String[] args) {
        launch();
    }
}