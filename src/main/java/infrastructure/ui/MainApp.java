package infrastructure.ui;

import infrastructure.persistence.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseInitializer.init();

        // 👇 Ruta completa con / al inicio
        var url = getClass().getResource("/infrastructure/ui/autenticacion/login.fxml");
        System.out.println("URL encontrada: " + url); // diagnóstico temporal

        FXMLLoader loader = new FXMLLoader(url);
        Scene scene = new Scene(loader.load());
        stage.setTitle("Sistema de Supermercado");
        stage.setScene(scene);

        stage.setWidth(420);
        stage.setHeight(550);
        stage.centerOnScreen();
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}