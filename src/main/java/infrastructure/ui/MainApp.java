package infrastructure.ui;

import infrastructure.persistence.DatabaseConnection;
import infrastructure.persistence.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage stageActual;

    @Override
    public void start(Stage stage) throws Exception {
        stageActual = stage;

        DatabaseConnection db = new DatabaseConnection();
        DatabaseInitializer initializer = new DatabaseInitializer(db);
        initializer.init();

        navegarA("/infrastructure/ui/autenticacion/login.fxml", "MasterMarket - Login", 420, 550);
    }

    public static void navegarA(String rutaFxml, String titulo, double ancho, double alto) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    MainApp.class.getResource(rutaFxml)
            );
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

    public static void main(String[] args) {
        launch();
    }
}