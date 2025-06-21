package System.trinexon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;

import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;

public class ProjectController {

    @FXML private TableView<Project> projectTable;
    @FXML private TableColumn<Project, String> colName;
    @FXML private TableColumn<Project, LocalDate> colStartDate;
    @FXML private TableColumn<Project, LocalDate> colEndDate;
    @FXML private TableColumn<Project, String> colStatus;
    @FXML private TableColumn<Project, String> colManager;
    @FXML private TableColumn<Project, Double> colBudget;

    @FXML private TextField searchField;
    @FXML private TextField detailName;
    @FXML private TextArea detailDescription;
    @FXML private DatePicker detailStartDate;
    @FXML private DatePicker detailEndDate;
    @FXML private ComboBox<String> detailStatus;
    @FXML private TextField detailManager;
    @FXML private TextField detailBudget;

    @FXML private Label statusLabel;

    private ObservableList<Project> projectList = FXCollections.observableArrayList();
    private Connection conn;

    @FXML
    public void initialize() {
        if (!connectToDatabase()) {
            showErrorDialog("Nem sikerült kapcsolódni az adatbázishoz.");
            return;
        }
        initTable();
        loadProjects();

        detailStatus.setItems(FXCollections.observableArrayList("Folyamatban", "Kész", "Felfüggesztve"));
        projectTable.setOnMouseClicked(this::onProjectSelected);
    }

    private boolean connectToDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/trinexon", "root", "KrisztiaN12");
            statusLabel.setText("Adatbázishoz csatlakozva.");
            return true;
        } catch (SQLException e) {
            statusLabel.setText("Adatbázis hiba: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void initTable() {
        colName.setCellValueFactory(cell -> cell.getValue().nameProperty());
        colStartDate.setCellValueFactory(cell -> cell.getValue().startDateProperty());
        colEndDate.setCellValueFactory(cell -> cell.getValue().endDateProperty());
        colStatus.setCellValueFactory(cell -> cell.getValue().statusProperty());
        colManager.setCellValueFactory(cell -> cell.getValue().managerProperty());
        colBudget.setCellValueFactory(cell -> cell.getValue().budgetProperty().asObject());
        projectTable.setItems(projectList);
    }

    private void loadProjects() {
        projectList.clear();
        String query = "SELECT * FROM projects";

        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                LocalDate start = rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null;
                LocalDate end = rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null;

                Project p = new Project(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description") != null ? rs.getString("description") : "",
                        start,
                        end,
                        rs.getString("status") != null ? rs.getString("status") : "Folyamatban",
                        rs.getString("manager") != null ? rs.getString("manager") : "",
                        rs.getDouble("budget")
                );
                projectList.add(p);
            }
            statusLabel.setText(projectList.size() + " projekt betöltve.");
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    private void onProjectSelected(MouseEvent event) {
        Project selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected != null) fillDetailFields(selected);
    }

    private void fillDetailFields(Project p) {
        detailName.setText(p.getName());
        detailDescription.setText(p.getDescription());
        detailStartDate.setValue(p.getStartDate());
        detailEndDate.setValue(p.getEndDate());
        detailStatus.setValue(p.getStatus());
        detailManager.setText(p.getManager());
        detailBudget.setText(String.valueOf(p.getBudget()));
    }

    @FXML
    private void handleAddProject() {
        clearDetails();
        statusLabel.setText("Új projekt hozzáadása.");
        projectTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleSaveProject() {
        if (!validateInput()) return;

        String name = detailName.getText().trim();
        String description = detailDescription.getText().trim();
        LocalDate start = detailStartDate.getValue();
        LocalDate end = detailEndDate.getValue();
        String status = detailStatus.getValue();
        String manager = detailManager.getText().trim();

        double budget;
        try {
            budget = NumberFormat.getInstance(Locale.US).parse(detailBudget.getText().trim()).doubleValue();
        } catch (Exception e) {
            statusLabel.setText("Érvénytelen költségvetés formátum.");
            return;
        }

        Project selected = projectTable.getSelectionModel().getSelectedItem();

        try {
            if (selected == null) {
                String insert = "INSERT INTO projects (name, description, start_date, end_date, status, manager, budget) VALUES (?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, name);
                    ps.setString(2, description);
                    ps.setDate(3, Date.valueOf(start));
                    ps.setDate(4, Date.valueOf(end));
                    ps.setString(5, status);
                    ps.setString(6, manager);
                    ps.setDouble(7, budget);
                    ps.executeUpdate();

                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            int newId = generatedKeys.getInt(1);
                            Project newProject = new Project(newId, name, description, start, end, status, manager, budget);
                            projectList.add(newProject);
                            projectTable.getSelectionModel().select(newProject);

                            int categoryId = ensureDefaultCategory();
                            addOrUpdateRevenue(newId, categoryId, end, status, budget);
                        }
                    }
                }
                statusLabel.setText("Projekt hozzáadva.");
            } else {
                String update = "UPDATE projects SET name=?, description=?, start_date=?, end_date=?, status=?, manager=?, budget=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, name);
                    ps.setString(2, description);
                    ps.setDate(3, Date.valueOf(start));
                    ps.setDate(4, Date.valueOf(end));
                    ps.setString(5, status);
                    ps.setString(6, manager);
                    ps.setDouble(7, budget);
                    ps.setInt(8, selected.getId());
                    ps.executeUpdate();
                }
                selected.setName(name);
                selected.setDescription(description);
                selected.setStartDate(start);
                selected.setEndDate(end);
                selected.setStatus(status);
                selected.setManager(manager);
                selected.setBudget(budget);
                projectTable.refresh();
                statusLabel.setText("Projekt frissítve.");
            }
            clearDetails();
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    private int ensureDefaultCategory() throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM categories WHERE name = 'Alapértelmezett'");
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt("id");
        }
        try (PreparedStatement insert = conn.prepareStatement("INSERT INTO categories (name) VALUES ('Alapértelmezett')", Statement.RETURN_GENERATED_KEYS)) {
            insert.executeUpdate();
            try (ResultSet rs = insert.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return -1;
    }

    private void addOrUpdateRevenue(int projectId, int categoryId, LocalDate endDate, String status, double budget) throws SQLException {
        try (PreparedStatement psInsert = conn.prepareStatement("INSERT INTO revenues (project_id, category_id, amount, date, note) VALUES (?, ?, ?, ?, ?)");
             PreparedStatement psDelete = conn.prepareStatement("DELETE FROM revenues WHERE project_id = ?")) {
            psDelete.setInt(1, projectId);
            psDelete.executeUpdate();

            psInsert.setInt(1, projectId);
            psInsert.setInt(2, categoryId);
            psInsert.setDouble(3, budget);
            psInsert.setDate(4, Date.valueOf(endDate));
            psInsert.setString(5, switch (status) {
                case "Kész" -> "Bevétel";
                case "Folyamatban" -> "Várható bevétel";
                default -> "Felfüggesztve";
            });
            psInsert.executeUpdate();
        }
    }

    @FXML
    private void handleDeleteProject() {
        Project selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Nincs kiválasztva projekt törléshez.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Projekt törlése");
        alert.setHeaderText(null);
        alert.setContentText("Biztosan törölni szeretnéd a projektet: " + selected.getName() + "?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        try {
            try (PreparedStatement ps1 = conn.prepareStatement("DELETE FROM revenues WHERE project_id=?");
                 PreparedStatement ps2 = conn.prepareStatement("DELETE FROM expenses WHERE project_id=?")) {
                ps1.setInt(1, selected.getId());
                ps2.setInt(1, selected.getId());
                ps1.executeUpdate();
                ps2.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM projects WHERE id=?")) {
                ps.setInt(1, selected.getId());
                ps.executeUpdate();
            }
            projectList.remove(selected);
            statusLabel.setText("Projekt törölve.");
            clearDetails();
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    @FXML
    private void handleSearchProject() {
        String keyword = searchField.getText().trim().toLowerCase();
        if (keyword.isEmpty()) {
            loadProjects();
            statusLabel.setText("Keresési mező üres, összes projekt megjelenítve.");
            return;
        }

        projectList.clear();
        String query = "SELECT * FROM projects WHERE LOWER(name) LIKE ? OR LOWER(description) LIKE ? OR LOWER(manager) LIKE ? OR LOWER(status) LIKE ?";
        String pattern = "%" + keyword + "%";

        try (PreparedStatement ps = conn.prepareStatement(query)) {
            for (int i = 1; i <= 4; i++) ps.setString(i, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Project p = new Project(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null,
                            rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null,
                            rs.getString("status"),
                            rs.getString("manager"),
                            rs.getDouble("budget")
                    );
                    projectList.add(p);
                }
            }
            projectTable.setItems(projectList);
            statusLabel.setText(projectList.size() + " találat a keresésre: '" + keyword + "'");
        } catch (SQLException e) {
            showDatabaseError(e);
        }
    }

    private boolean validateInput() {
        String name = detailName.getText().trim();
        LocalDate start = detailStartDate.getValue();
        LocalDate end = detailEndDate.getValue();
        String status = detailStatus.getValue();
        String manager = detailManager.getText().trim();
        String budgetText = detailBudget.getText().trim();

        if (name.isEmpty() || start == null || end == null || status == null || manager.isEmpty() || budgetText.isEmpty()) {
            statusLabel.setText("Kérlek tölts ki minden mezőt.");
            return false;
        }
        if (end.isBefore(start)) {
            statusLabel.setText("A befejező dátum nem lehet korábbi, mint a kezdő dátum.");
            return false;
        }
        try {
            double budget = Double.parseDouble(budgetText);
            if (budget < 0) {
                statusLabel.setText("A költségvetés nem lehet negatív.");
                return false;
            }
        } catch (NumberFormatException e) {
            statusLabel.setText("A költségvetésnek számnak kell lennie.");
            return false;
        }
        return true;
    }

    private void clearDetails() {
        detailName.clear();
        detailDescription.clear();
        detailStartDate.setValue(null);
        detailEndDate.setValue(null);
        detailStatus.setValue(null);
        detailManager.clear();
        detailBudget.clear();
        projectTable.getSelectionModel().clearSelection();
    }

    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Hiba");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showDatabaseError(SQLException e) {
        showErrorDialog("Adatbázis hiba: " + e.getMessage());
        e.printStackTrace();
    }

    public void shutdown() {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
