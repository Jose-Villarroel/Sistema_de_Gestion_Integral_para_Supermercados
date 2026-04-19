package infrastructure.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
//se quito la parte de data base initializer ya que se va a usar la bd y no los scrpits
public class MainApp extends Application {

    private static Stage stageActual;

    @Override
    public void start(Stage stage) throws Exception {
        stageActual = stage;

        navegarA("/infrastructure/ui/supervisor/inventario.fxml",
                "MasterMarket - Inventario", 1200, 700);
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