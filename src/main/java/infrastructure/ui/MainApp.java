package infrastructure.ui;

import infrastructure.persistence.DatabaseInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        DatabaseInitializer.init();

        // Ruta completa con / al inicio
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

    /**
     * Navega a otra vista FXML usando el mismo Stage principal.
     */
    public static void navegarA(String fxmlPath, String titulo, double width, double height) {
        if (primaryStage == null) {
            throw new IllegalStateException("El Stage principal aún no ha sido inicializado.");
        }

        try {
            var url = MainApp.class.getResource(fxmlPath);
            if (url == null) {
                throw new IllegalArgumentException("No se encontró el recurso FXML en la ruta: " + fxmlPath);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Scene scene = new Scene(loader.load());

            primaryStage.setTitle(titulo);
            primaryStage.setScene(scene);
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.centerOnScreen();
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Error al cargar la vista FXML: " + fxmlPath, e);
        }
    }

    public static void main(String[] args) {
        launch();
    }
}