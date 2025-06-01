package System.trinexon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;

public class ProjectsOverviewController {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Project> projectsTable;

    @FXML
    private TableColumn<Project, String> nameColumn;

    @FXML
    private TableColumn<Project, String> descriptionColumn;

    @FXML
    private TableColumn<Project, LocalDate> startDateColumn;

    @FXML
    private TableColumn<Project, LocalDate> endDateColumn;

    @FXML
    private TableColumn<Project, Void> actionsColumn;

    @FXML
    private Label totalProjectsLabel;

    private ObservableList<Project> projects = FXCollections.observableArrayList();

    // Adatbázis kapcsolat
    private final String DB_URL = "jdbc:mysql://localhost:3306/trinexon?useSSL=false&serverTimezone=UTC";
    private final String DB_USER = "root";
    private final String DB_PASS = "KrisztiaN12";

    @FXML
    public void initialize() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));

        addActionsToTable();
        loadProjectsFromDB();
        updateStatistics();
    }

    private void addActionsToTable() {
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Törlés");

            {
                deleteBtn.setOnAction(event -> {
                    Project project = getTableView().getItems().get(getIndex());
                    deleteProject(project);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });
    }
    private void loadProjectsFromDB() {
        projects.clear();
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM projects ORDER BY start_date DESC")) {

            while (rs.next()) {
                LocalDate endDate = rs.getDate("end_date") != null ? rs.getDate("end_date").toLocalDate() : null;
                String status = rs.getString("status");
                String manager = rs.getString("manager");
                double budget = rs.getObject("budget") != null ? rs.getDouble("budget") : 0.0;

                Project p = new Project(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getDate("start_date").toLocalDate(),
                    endDate,
                    status,
                    manager,
                    budget
                );
                projects.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Adatbázis hiba", "Nem sikerült betölteni a projekteket.");
        }

        projectsTable.setItems(projects);
    }

    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim().toLowerCase();

        if (keyword.isEmpty()) {
            projectsTable.setItems(projects);
            updateStatistics();
            return;
        }

        ObservableList<Project> filtered = FXCollections.observableArrayList();
        for (Project p : projects) {
            if (p.getName().toLowerCase().contains(keyword) ||
                (p.getDescription() != null && p.getDescription().toLowerCase().contains(keyword))) {
                filtered.add(p);
            }
        }
        projectsTable.setItems(filtered);
        updateStatistics(filtered);
    }

    @FXML
    private void handleAddProject() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO projects (name, description, start_date, end_date) VALUES (?, ?, ?, ?)")) {

            ps.setString(1, "Új Projekt");
            ps.setString(2, "Leírás");
            ps.setDate(3, Date.valueOf(LocalDate.now()));
            ps.setDate(4, Date.valueOf(LocalDate.now().plusMonths(3)));

            int inserted = ps.executeUpdate();
            if (inserted > 0) {
                loadProjectsFromDB();
                updateStatistics();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Adatbázis hiba", "Nem sikerült hozzáadni az új projektet.");
        }
    }

    private void deleteProject(Project project) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM projects WHERE id = ?")) {

            ps.setInt(1, project.getId());
            int deleted = ps.executeUpdate();
            if (deleted > 0) {
                projects.remove(project);
                projectsTable.refresh();
                updateStatistics();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Adatbázis hiba", "Nem sikerült törölni a projektet.");
        }
    }

    private void updateStatistics() {
        totalProjectsLabel.setText("Összes projekt: " + projects.size());
    }

    private void updateStatistics(ObservableList<Project> currentProjects) {
        totalProjectsLabel.setText("Összes projekt: " + currentProjects.size());
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
