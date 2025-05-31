package System.trinexon;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class DashboardController {

    @FXML
    private Text felLabel;

    @FXML
    private ImageView teamImage;

    public void setUsername(String username) {
        if (felLabel != null) {
            felLabel.setText("Bejelentkezett: " + username);
        }
    }

    @FXML
    public void initialize() {
        if (teamImage != null) {
            teamImage.setOnMouseClicked(this::openWorkerWindow);
        }
    }

    @FXML
    public void openWorkerWindow(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/workerView.fxml"));
            Parent root = loader.load(); // AnchorPane -> Parent

            Stage stage = new Stage();
            stage.setTitle("Munkavállalói nyilvántartás");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // modal ablak
            stage.showAndWait(); // várja az ablak bezárását
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
