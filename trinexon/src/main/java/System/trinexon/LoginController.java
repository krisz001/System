package System.trinexon;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;
import org.mindrot.jbcrypt.BCrypt;
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
            // TODO: Itt töltheted be a következő képernyőt vagy főoldalt
            System.out.println("Belépés sikeres, irány a dashboard!");
        } else {
            showAlert(AlertType.ERROR, "Sikertelen belépés", "Hibás felhasználónév vagy jelszó!");
        }
    }

    private boolean checkCredentials(String username, String password) {
        String sql = "SELECT password FROM users WHERE username = ?";

        try (Connection conn = Database.connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashedPassword = rs.getString("password");
                return BCrypt.checkpw(password, hashedPassword);
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
