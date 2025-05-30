package System.trinexon;

import javafx.fxml.FXML;
import javafx.scene.text.Text;

public class DashboardController {

    @FXML
    private Text felLabel;

    public void setUsername(String username) {
        felLabel.setText("Bejelentkezett: " + username);
    }
}
