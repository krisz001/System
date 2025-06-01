package System.trinexon;

import java.io.IOException;
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
        // Mivel az onMouseClicked eseményeket FXML-ben állítottad be, nincs szükség itt explicit listenerekre.
    }

    @FXML
    public void openWorkerWindow(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/workerView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Munkavállalói nyilvántartás");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void openFinanceWindow(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FinanceView.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Pénzügyi Kimutatások");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Új metódus a GitHub kép kattintás kezelésére
    @FXML
    public void openProjectWindow(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProjectsOverview.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Projektek áttekintése");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
