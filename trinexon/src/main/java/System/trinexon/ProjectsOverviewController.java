package System.trinexon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.sql.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProjectsOverviewController {

    private static final Logger LOGGER = Logger.getLogger(ProjectsOverviewController.class.getName());

    // --- FXML mezők ---
    @FXML private TextField searchField;
    @FXML private TableView<Project> projectsTable;
    @FXML private TableColumn<Project, String> nameColumn;
    @FXML private TableColumn<Project, LocalDate> startDateColumn;
    @FXML private TableColumn<Project, LocalDate> endDateColumn;
    @FXML private TableColumn<Project, String> statusColumn;
    @FXML private TableColumn<Project, String> managerColumn;
    @FXML private TableColumn<Project, Double> budgetColumn;
    @FXML private TableColumn<Project, String> categoryColumn;
    @FXML private TableColumn<Project, Void> actionsColumn;
    @FXML private Label totalProjectsLabel;

    // detail panel
    @FXML private TextField detailName;
    @FXML private TextArea detailDescription;
    @FXML private DatePicker detailStartDate;
    @FXML private DatePicker detailEndDate;
    @FXML private ComboBox<String> detailStatus;
    @FXML private TextField detailManager;
    @FXML private ComboBox<String> detailCategory;
    @FXML private TextField detailBudget;
    @FXML private Label statusLabel;

    // Ha van include-olva, injektálódik; egyébként null
    @FXML private FinanceController financeController;

    private final ObservableList<Project> projects = FXCollections.observableArrayList();

    private static final String DB_URL  = "jdbc:mysql://localhost:3306/trinexon?useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "KrisztiaN12";

    @FXML
    public void initialize() {
        setupColumns();
        addActionsToTable();
        loadProjectsFromDB();
        updateStatistics();
        setupListeners();

        detailStatus.setItems(FXCollections.observableArrayList("Függőben", "Folyamatban", "Kész"));
        detailCategory.setItems(FXCollections.observableArrayList("Alapértelmezett", "Termékértékesítés", "Szolgáltatás"));
    }

    private void setupColumns() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        startDateColumn.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        endDateColumn.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        managerColumn.setCellValueFactory(new PropertyValueFactory<>("manager"));
        budgetColumn.setCellValueFactory(new PropertyValueFactory<>("budget"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));

        NumberFormat nf = NumberFormat.getInstance(new Locale("hu","HU"));
        nf.setMaximumFractionDigits(0);
        nf.setGroupingUsed(true);
        budgetColumn.setCellFactory(col -> new TableCell<Project, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "N/A" : nf.format(item) + " Ft");
            }
        });

        endDateColumn.setCellFactory(col -> new TableCell<Project, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "N/A" : item.toString());
            }
        });
    }

    private void addActionsToTable() {
        actionsColumn.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Project, Void> call(TableColumn<Project, Void> param) {
                return new TableCell<>() {
                    private final Button editBtn   = new Button("Szerkesztés");
                    private final Button deleteBtn = new Button("Törlés");
                    private final HBox pane = new HBox(5, editBtn, deleteBtn);
                    {
                        pane.setPadding(new Insets(0,0,0,5));
                        editBtn.setOnAction(e -> fillDetails(getTableView().getItems().get(getIndex())));
                        deleteBtn.setOnAction(e -> deleteProject(getTableView().getItems().get(getIndex())));
                    }
                    @Override protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        });
    }

    private void setupListeners() {
        searchField.setOnKeyReleased(e -> handleSearch());
        projectsTable.getSelectionModel().selectedItemProperty().addListener((obs,o,n) -> {
            if (n != null) fillDetails(n);
        });
    }

    private void fillDetails(Project p) {
        detailName.setText(p.getName());
        detailDescription.setText(p.getDescription());
        detailStartDate.setValue(p.getStartDate());
        detailEndDate.setValue(p.getEndDate());
        detailStatus.setValue(p.getStatus());
        detailManager.setText(p.getManager());
        detailCategory.setValue(p.getCategory());
        detailBudget.setText(String.valueOf(p.getBudget()));
    }

    private void loadProjectsFromDB() {
        projects.clear();
        String sql = "SELECT * FROM projects ORDER BY start_date DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                LocalDate start = rs.getDate("start_date").toLocalDate();
                LocalDate end   = rs.getDate("end_date") != null
                                  ? rs.getDate("end_date").toLocalDate() : null;
                projects.add(new Project(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    start, end,
                    rs.getString("status"),
                    rs.getString("manager"),
                    rs.getDouble("budget"),
                    rs.getString("category")
                ));
            }
            projectsTable.setItems(projects);

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Nem sikerült betölteni a projekteket.", ex);
            showAlert(Alert.AlertType.ERROR, "Adatbázis hiba", "Nem sikerült betölteni a projekteket.");
        }
    }

    @FXML
    private void handleSearch() {
        String kw = searchField.getText().trim().toLowerCase();
        if (kw.isEmpty()) {
            projectsTable.setItems(projects);
        } else {
            ObservableList<Project> filtered = FXCollections.observableArrayList();
            for (Project p : projects) {
                if (p.getName().toLowerCase().contains(kw)
                 || (p.getDescription()!=null && p.getDescription().toLowerCase().contains(kw))
                 || (p.getStatus()     !=null && p.getStatus().toLowerCase().contains(kw))
                 || (p.getManager()    !=null && p.getManager().toLowerCase().contains(kw))
                ) filtered.add(p);
            }
            projectsTable.setItems(filtered);
        }
        updateStatistics();
    }

    @FXML
    private void handleUpsertProject(ActionEvent evt) {
        if (!validateInput()) return;

        Project sel      = projectsTable.getSelectionModel().getSelectedItem();
        String oldStatus = sel != null ? sel.getStatus() : null;

        String name      = detailName.getText().trim();
        String desc      = detailDescription.getText().trim();
        LocalDate start  = detailStartDate.getValue();
        LocalDate end    = detailEndDate.getValue();
        String newStatus = detailStatus.getValue();
        String mgr       = detailManager.getText().trim();
        String cat       = detailCategory.getValue();
        double budget;
        try {
            budget = Double.parseDouble(detailBudget.getText().trim());
        } catch (NumberFormatException ex) {
            statusLabel.setText("Érvénytelen költségvetés formátum.");
            return;
        }

        String sql = sel==null
          ? "INSERT INTO projects(name,description,start_date,end_date,status,manager,budget,category) VALUES(?,?,?,?,?,?,?,?)"
          : "UPDATE projects SET name=?,description=?,start_date=?,end_date=?,status=?,manager=?,budget=?,category=? WHERE id=?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(
                 sql,
                 sel==null ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS
             )) {

            ps.setString(1, name);
            ps.setString(2, desc);
            ps.setDate(3, Date.valueOf(start));
            if (end != null) ps.setDate(4, Date.valueOf(end));
            else             ps.setNull(4, Types.DATE);
            ps.setString(5, newStatus);
            ps.setString(6, mgr);
            ps.setDouble(7, budget);
            ps.setString(8, cat);
            if (sel != null) ps.setInt(9, sel.getId());

            int affected = ps.executeUpdate();
            if (affected == 0) return;

            int projectId;
            if (sel == null) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    projectId = keys.getInt(1);
                    sel = new Project(projectId, name, desc, start, end, newStatus, mgr, budget, cat);
                    projects.add(0, sel);
                    projectsTable.getSelectionModel().select(sel);
                    statusLabel.setText("Új projekt beszúrva.");
                }
            } else {
                projectId = sel.getId();
                sel.setName(name); sel.setDescription(desc);
                sel.setStartDate(start); sel.setEndDate(end);
                sel.setStatus(newStatus); sel.setManager(mgr);
                sel.setBudget(budget); sel.setCategory(cat);
                projectsTable.refresh();
                statusLabel.setText("Projekt frissítve.");
            }

            // ha státusz → „Kész”, bevétel beszúrása
            if (!"Kész".equals(oldStatus) && "Kész".equals(newStatus)) {
                insertRevenue(conn, projectId, cat, budget);
                statusLabel.setText(statusLabel.getText() + " Bevétel hozzáadva.");
            }

            clearDetails();
            updateStatistics();

            // csak ha valóban injektálva van:
            if (financeController != null) {
                financeController.refresh();
            }

        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Adatbázis hiba", ex);
            showAlert(Alert.AlertType.ERROR, "Adatbázis hiba", ex.getMessage());
        }
    }

    @FXML
    private void handleDeleteProject(ActionEvent evt) {
        Project sel = projectsTable.getSelectionModel().getSelectedItem();
        if (sel == null) {
            showAlert(Alert.AlertType.WARNING, "Nincs kiválasztva", "Válassz ki egy projektet!");
            return;
        }
        deleteProject(sel);
    }

    private void deleteProject(Project p) {
        String sql = "DELETE FROM projects WHERE id=?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, p.getId());
            if (ps.executeUpdate()>0) {
                projects.remove(p);
                updateStatistics();
                statusLabel.setText("Projekt törölve.");
                if (financeController != null) {
                    financeController.refresh();
                }
            }
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "Adatbázis hiba", ex);
            showAlert(Alert.AlertType.ERROR, "Adatbázis hiba", ex.getMessage());
        }
    }

    private void insertRevenue(Connection conn, int projectId, String category, double amount) throws SQLException {
        String revSql = "INSERT INTO revenues(project_id, category_id, amount, date) VALUES(?,?,?,?)";
        try (PreparedStatement revPs = conn.prepareStatement(revSql)) {
            revPs.setInt(1, projectId);
            int catId = 1;
            try (PreparedStatement catPs = conn.prepareStatement("SELECT id FROM categories WHERE name=?")) {
                catPs.setString(1, category);
                try (ResultSet crs = catPs.executeQuery()) {
                    if (crs.next()) catId = crs.getInt(1);
                }
            }
            revPs.setInt(2, catId);
            revPs.setDouble(3, amount);
            revPs.setDate(4, Date.valueOf(LocalDate.now()));
            revPs.executeUpdate();
        }
    }

    private boolean validateInput() {
        if (detailName.getText().trim().isEmpty())     { statusLabel.setText("Név kötelező."); return false;}
        if (detailStartDate.getValue()==null)         { statusLabel.setText("Kezdés dátuma kötelező."); return false;}
        if (detailStatus.getValue()==null)            { statusLabel.setText("Státusz kötelező."); return false;}
        if (detailManager.getText().trim().isEmpty()) { statusLabel.setText("Projektvezető kötelező."); return false;}
        if (detailBudget.getText().trim().isEmpty())  { statusLabel.setText("Költségvetés kötelező."); return false;}
        if (detailCategory.getValue()==null)          { statusLabel.setText("Kategória kötelező."); return false;}
        return true;
    }

    private void clearDetails() {
        detailName.clear(); detailDescription.clear();
        detailStartDate.setValue(null); detailEndDate.setValue(null);
        detailStatus.setValue(null); detailManager.clear();
        detailBudget.clear(); detailCategory.setValue(null);
        projectsTable.getSelectionModel().clearSelection();
    }

    private void updateStatistics() {
        totalProjectsLabel.setText("Összes projekt: " + projects.size());
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.setTitle(title);
        a.showAndWait();
    }
}