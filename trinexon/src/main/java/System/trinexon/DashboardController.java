package System.trinexon;

import java.io.IOException;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DashboardController {

    @FXML
    private Text felLabel;

    @FXML
    private ImageView teamImage;

    @FXML
    private ImageView budgetImage;

    @FXML
    private ImageView githubImage;  // Új ImageView a GitHub képhez

    public void setUsername(String username) {
        if (felLabel != null) {
            felLabel.setText("Bejelentkezett: " + username);
        }
    }

    @FXML
    public void initialize() {
        // Az onMouseClicked események FXML-ben vannak, ezért itt nem kell listenert beállítani
    }

    @FXML
    private void openWebEditor() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/WebEditorView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Weboldal szerkesztő - Trinexon");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @FXML
    public void openWorkerWindow(MouseEvent event) {
        openModalWindow("/workerView.fxml", "Munkavállalói nyilvántartás");
    }

    @FXML
    public void openFinanceWindow(MouseEvent event) {
        openModalWindow("/FinanceView.fxml", "Pénzügyi Kimutatások");
    }

    @FXML
    public void openProjectWindow(MouseEvent event) {
        openModalWindow("/ProjectsOverview.fxml", "Projektek áttekintése");
    }

    /**
     * Segédmetódus modális ablak megnyitásához.
     * 
     * @param fxmlPath Az FXML fájl elérési útja
     * @param title Az ablak címe
     */
    private void openModalWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            // Érdemes UI értesítést is megjeleníteni, ha kell
            Platform.runLater(() -> {
                // pl. alert vagy log üzenet, ha van UI elem erre
            });
        }
    }
}