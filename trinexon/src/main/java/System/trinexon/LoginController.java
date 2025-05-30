package System.trinexon;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;
import org.mindrot.jbcrypt.BCrypt;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.ERROR, "Hiba", "Minden mezőt ki kell tölteni!");
            return;
        }

        if (checkCredentials(username, password)) {
            showAlert(AlertType.INFORMATION, "Sikeres belépés", "Üdvözöllek, " + username + "!");
            openDashboard(username);  // Itt megnyitjuk az új ablakot a dashboarddal
            System.out.println("Belépés sikeres, irány a dashboard!");
        } else {
            showAlert(AlertType.ERROR, "Sikertelen belépés", "Hibás felhasználónév vagy jelszó!");
        }
    }

    private void openDashboard(String username) {
        System.out.println("Trying to open dashboard for user: " + username);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            Scene scene = new Scene(loader.load());
            DashboardController controller = loader.getController();
            controller.setUsername(username);

            Stage stage = new Stage();
            stage.setScene(scene);
            stage.setTitle("Dashboard");
            stage.show();
            System.out.println("Dashboard should be showing now.");
        } catch (IOException e) {
            System.out.println("Error loading FXML:");
            e.printStackTrace();
        }
    }


    private boolean checkCredentials(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String storedPassword = rs.getString("password");

                if (storedPassword != null) {
                    // Ha bcrypt hashelve van tárolva
                    if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                        return BCrypt.checkpw(password, storedPassword);
                    } else {
                        // Ha sima szövegként van tárolva (átmeneti megoldás)
                        return password.equals(storedPassword);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void showAlert(AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
