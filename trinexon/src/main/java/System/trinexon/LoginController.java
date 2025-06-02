package System.trinexon;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
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
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert(AlertType.ERROR, "Hiba", "Minden mezőt ki kell tölteni!");
            return;
        }

        if (checkCredentials(username, password)) {
            showAlert(AlertType.INFORMATION, "Sikeres belépés", "Üdvözöllek, " + username + "!");
            openDashboard(username);
        } else {
            passwordField.clear();  // Jelszómező törlése sikertelen bejelentkezés után
            showAlert(AlertType.ERROR, "Sikertelen belépés", "Hibás felhasználónév vagy jelszó!");
        }
    }

    private void openDashboard(String username) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Dashboard.fxml"));
            AnchorPane root = loader.load();

            DashboardController controller = loader.getController();
            controller.setUsername(username);

            Scene scene = new Scene(root);

            Stage stage = new Stage();
            stage.setTitle("Dashboard");
            stage.setScene(scene);

            // Méret beállítása az FXML-ben megadott méretre
            double width = root.getPrefWidth();
            double height = root.getPrefHeight();

            stage.setWidth(width);
            stage.setHeight(height);
            stage.setMinWidth(width);
            stage.setMinHeight(height);
            stage.setMaxWidth(width);
            stage.setMaxHeight(height);

            stage.show();

            // Bejelentkező ablak bezárása
            Stage currentStage = (Stage) usernameField.getScene().getWindow();
            currentStage.close();

        } catch (IOException e) {
            System.err.println("Hiba történt a Dashboard betöltésekor:");
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Betöltési hiba", "Nem sikerült megnyitni a Dashboardot.");
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
                    // Ha bcrypt hash van tárolva, ellenőrzés BCrypt-tal
                    if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
                        return BCrypt.checkpw(password, storedPassword);
                    } else {
                        // Ha sima szöveg, közvetlen összehasonlítás (javasolt csak átmenetileg)
                        return password.equals(storedPassword);
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(AlertType.ERROR, "Adatbázis hiba", "Nem sikerült ellenőrizni a felhasználó hitelesítését.");
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
