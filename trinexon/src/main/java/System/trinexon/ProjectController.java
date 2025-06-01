package System.trinexon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import java.sql.*;
import java.time.LocalDate;

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
        connectToDatabase();
        initTable();
        loadProjects();

        detailStatus.setItems(FXCollections.observableArrayList("Folyamatban", "Kész", "Felfüggesztve"));

        projectTable.setOnMouseClicked(this::onProjectSelected);
    }

    private void connectToDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/trinexon", "root", "KrisztiaN12");
        } catch (SQLException e) {
            statusLabel.setText("Adatbázis hiba: " + e.getMessage());
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
                Project p = new Project(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        rs.getString("status"),
                        rs.getString("manager"),
                        rs.getDouble("budget")
                );
                projectList.add(p);
            }
        } catch (SQLException e) {
            statusLabel.setText("Hiba a projektek betöltésekor.");
            e.printStackTrace();
        }
    }

    private void onProjectSelected(MouseEvent event) {
        Project selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            detailName.setText(selected.getName());
            detailDescription.setText(selected.getDescription());
            detailStartDate.setValue(selected.getStartDate());
            detailEndDate.setValue(selected.getEndDate());
            detailStatus.setValue(selected.getStatus());
            detailManager.setText(selected.getManager());
            detailBudget.setText(String.valueOf(selected.getBudget()));
        }
    }

    @FXML
    private void handleAddProject() {
        detailName.clear();
        detailDescription.clear();
        detailStartDate.setValue(null);
        detailEndDate.setValue(null);
        detailStatus.setValue(null);
        detailManager.clear();
        detailBudget.clear();
        projectTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleSaveProject() {
        String name = detailName.getText();
        String description = detailDescription.getText();
        LocalDate start = detailStartDate.getValue();
        LocalDate end = detailEndDate.getValue();
        String status = detailStatus.getValue();
        String manager = detailManager.getText();
        double budget = Double.parseDouble(detailBudget.getText());

        Project selected = projectTable.getSelectionModel().getSelectedItem();

        try {
            if (selected == null) {
                String insert = "INSERT INTO projects (name, description, start_date, end_date, status, manager, budget) VALUES (?, ?, ?, ?, ?, ?, ?)";
                PreparedStatement ps = conn.prepareStatement(insert);
                ps.setString(1, name);
                ps.setString(2, description);
                ps.setDate(3, Date.valueOf(start));
                ps.setDate(4, Date.valueOf(end));
                ps.setString(5, status);
                ps.setString(6, manager);
                ps.setDouble(7, budget);
                ps.executeUpdate();
                statusLabel.setText("Projekt hozzáadva.");
            } else {
                String update = "UPDATE projects SET name=?, description=?, start_date=?, end_date=?, status=?, manager=?, budget=? WHERE id=?";
                PreparedStatement ps = conn.prepareStatement(update);
                ps.setString(1, name);
                ps.setString(2, description);
                ps.setDate(3, Date.valueOf(start));
                ps.setDate(4, Date.valueOf(end));
                ps.setString(5, status);
                ps.setString(6, manager);
                ps.setDouble(7, budget);
                ps.setInt(8, selected.getId());
                ps.executeUpdate();
                statusLabel.setText("Projekt frissítve.");
            }

            loadProjects();
        } catch (SQLException e) {
            statusLabel.setText("Mentési hiba: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteProject() {
        Project selected = projectTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            String delete = "DELETE FROM projects WHERE id=?";
            PreparedStatement ps = conn.prepareStatement(delete);
            ps.setInt(1, selected.getId());
            ps.executeUpdate();
            statusLabel.setText("Projekt törölve.");
            loadProjects();
        } catch (SQLException e) {
            statusLabel.setText("Törlési hiba: " + e.getMessage());
        }
    }
}
