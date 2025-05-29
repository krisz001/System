package System.trinexon;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // FXML betöltése
        	FXMLLoader loader = new FXMLLoader(getClass().getResource("/loginform.fxml"));
        	Parent root = loader.load();

            // Scene létrehozása
            Scene scene = new Scene(root);

            // Ablak beállításai
            primaryStage.setTitle("Trinexon v1.0");
            primaryStage.setScene(scene);
            primaryStage.setWidth(434);
            primaryStage.setHeight(210);
            primaryStage.setResizable(false);
            primaryStage.show();

            // Itt például egyszer lefuttathatod az adatbázis lekérdezést
            Database.queryDatabase("SELECT * FROM users");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);  // Ez elindítja a JavaFX alkalmazást
    }
}
