package System.trinexon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Logger;

public class EmployeeOverviewController {
    private static final Logger LOGGER = Logger.getLogger(EmployeeOverviewController.class.getName());

    @FXML private TableView<Employee> employeeTable;
    @FXML private TableColumn<Employee, Integer> colId;
    @FXML private TableColumn<Employee, String>  colName;
    @FXML private TableColumn<Employee, String>  colPosition;
    @FXML private TableColumn<Employee, String>  colDept;
    @FXML private TableColumn<Employee, LocalDate> colHireDate;
    @FXML private TableColumn<Employee, String>  colStatus;
    @FXML private TableColumn<Employee, Void>    colActions;

    @FXML private ListView<String> departmentsList;
    @FXML private TextField searchField;

    @FXML private TextField detailName;
    @FXML private TextField detailPosition;
    @FXML private ComboBox<String> detailDept;
    @FXML private DatePicker detailHireDate;
    @FXML private ComboBox<String> detailStatus;
    @FXML private TextField detailEmail;
    @FXML private TextField detailPhone;

    @FXML private Label statusLabel;
    @FXML private Label totalEmployees;

    private final ObservableList<Employee> employees = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        loadDepartments();
        loadEmployees();
        setupListeners();

        detailDept.setItems(FXCollections.observableArrayList("Fejlesztés", "HR", "Értékesítés", "Pénzügy"));
        detailStatus.setItems(FXCollections.observableArrayList("Aktív", "Szabadságon", "Kilépett"));
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPosition.setCellValueFactory(new PropertyValueFactory<>("position"));
        colDept.setCellValueFactory(new PropertyValueFactory<>("department"));
        colHireDate.setCellValueFactory(new PropertyValueFactory<>("hireDate"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colActions.setCellFactory(new Callback<>() {
            @Override
            public TableCell<Employee, Void> call(TableColumn<Employee, Void> param) {
                return new TableCell<>() {
                    private final Button edit = new Button("✏️");
                    private final Button del = new Button("🗑️");
                    private final HBox pane = new HBox(5, edit, del);

                    {
                        pane.setPadding(new Insets(0, 0, 0, 5));
                        edit.setOnAction(e -> {
                            Employee emp = getTableView().getItems().get(getIndex());
                            editEmployee(emp);
                        });
                        del.setOnAction(e -> {
                            Employee emp = getTableView().getItems().get(getIndex());
                            removeEmployee(emp);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        setGraphic(empty ? null : pane);
                    }
                };
            }
        });
    }

    private void loadDepartments() {
        departmentsList.getItems().setAll("Összes osztály", "Fejlesztés", "HR", "Értékesítés", "Pénzügy");
        departmentsList.getSelectionModel().selectFirst();
    }

    private void loadEmployees() {
        employees.clear();
        employees.addAll(EmployeeDAO.findAll());
        filterEmployees();
    }

    private void setupListeners() {
        searchField.setOnKeyReleased(e -> filterEmployees());
        departmentsList.getSelectionModel().selectedItemProperty().addListener((o, oldV, newV) -> filterEmployees());
    }

    private void filterEmployees() {
        String kw = searchField.getText().toLowerCase().trim();
        String dept = departmentsList.getSelectionModel().getSelectedItem();
        ObservableList<Employee> filtered = FXCollections.observableArrayList();
        for (Employee emp : employees) {
            boolean matchesDept = "Összes osztály".equals(dept) || emp.getDepartment().equals(dept);
            boolean matchesKw = emp.getName().toLowerCase().contains(kw) || emp.getPosition().toLowerCase().contains(kw);
            if (matchesDept && matchesKw) filtered.add(emp);
        }
        employeeTable.setItems(filtered);
        updateStatistics(filtered.size());
    }

    @FXML
    private void onAddEmployee(ActionEvent e) {
        employeeTable.getSelectionModel().clearSelection();
        clearDetails();
        statusLabel.setText("➕ Új dolgozó rögzítése…");
    }

    private void editEmployee(Employee emp) {
        employeeTable.getSelectionModel().select(emp);
        detailName.setText(emp.getName());
        detailPosition.setText(emp.getPosition());
        detailDept.setValue(emp.getDepartment());
        detailHireDate.setValue(emp.getHireDate());
        detailStatus.setValue(emp.getStatus());
        detailEmail.setText(emp.getEmail());
        detailPhone.setText(emp.getPhone());
        statusLabel.setText("✏️ Adatok szerkesztése…");
    }

    @FXML
    private void onSaveEmployee(ActionEvent e) {
        Employee sel = employeeTable.getSelectionModel().getSelectedItem();
        if (detailName.getText().trim().isEmpty()) {
            statusLabel.setText("⚠️ A név megadása kötelező.");
            return;
        }

        if (sel == null) {
            Employee ne = new Employee(
                0,
                detailName.getText(),
                detailPosition.getText(),
                detailDept.getValue(),
                detailHireDate.getValue(),
                detailStatus.getValue(),
                detailEmail.getText(),
                detailPhone.getText()
            );
            if (EmployeeDAO.insertEmployee(ne)) {
                statusLabel.setText("✅ Új dolgozó elmentve.");
                loadEmployees();
            } else {
                statusLabel.setText("❌ Hiba a mentés során.");
            }
        } else {
            sel.setName(detailName.getText());
            sel.setPosition(detailPosition.getText());
            sel.setDepartment(detailDept.getValue());
            sel.setHireDate(detailHireDate.getValue());
            sel.setStatus(detailStatus.getValue());
            sel.setEmail(detailEmail.getText());
            sel.setPhone(detailPhone.getText());

            if (EmployeeDAO.updateEmployee(sel)) {
                statusLabel.setText("✅ Dolgozó frissítve.");
                loadEmployees();
            } else {
                statusLabel.setText("❌ Hiba a frissítés során.");
            }
        }

        clearDetails();
    }

    @FXML
    private void onDeleteEmployee(ActionEvent e) {
        Employee sel = employeeTable.getSelectionModel().getSelectedItem();
        if (sel != null) {
            removeEmployee(sel);
        } else {
            statusLabel.setText("⚠️ Nincs kiválasztott dolgozó.");
        }
    }

    private void removeEmployee(Employee emp) {
        if (emp == null) {
            statusLabel.setText("⚠️ Nincs kiválasztott dolgozó a törléshez.");
            return;
        }

        boolean confirmed = confirm("Biztosan törölni szeretnéd „" + emp.getName() + "” dolgozót?");
        if (!confirmed) {
            statusLabel.setText("❎ Törlés megszakítva.");
            return;
        }

        if (EmployeeDAO.deleteEmployeeSecure(emp.getId())) {
            statusLabel.setText("🗑️ Dolgozó törölve.");
            loadEmployees();
            clearDetails();
        } else {
            statusLabel.setText("❌ Hiba történt a törlés során.");
        }
    }

    private boolean confirm(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.setTitle("Megerősítés");
        alert.setHeaderText(null);
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    @FXML
    private void onOpenSalary() {
        try {
            startFlaskServer();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/SalaryView.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("AI Fizetéskezelés – Trinexon");
            stage.setScene(new Scene(root));

            // 🛑 Ha bezárják az ablakot, állítsuk le a Flask szervert
            stage.setOnCloseRequest(event -> {
                stopFlaskServer();
                System.out.println("🔒 SalaryView.fxml ablak bezárva.");
            });

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            statusLabel.setText("❌ Hiba: Nem sikerült megnyitni a fizetéskezelőt.");
        }
    }

    private static boolean flaskRunning = false;
    private static Process flaskProcess = null;

    private void startFlaskServer() {
        if (flaskRunning) {
            System.out.println("ℹ️ Flask már fut.");
            return;
        }

        try {
            String pythonPath = "/Users/bogdankrisztian/anaconda3/bin/python3";
            String scriptPath = "/Users/bogdankrisztian/eclipse-workspace/System/trinexon/ai_backend/app.py";

            ProcessBuilder pb = new ProcessBuilder(pythonPath, scriptPath);
            pb.inheritIO();
            pb.directory(new File(System.getProperty("user.dir")));

            flaskProcess = pb.start();
            flaskRunning = true;

            System.out.println("✅ Flask backend elindítva: " + scriptPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("❌ Flask backend indítása sikertelen.");
        }
    }

    private void stopFlaskServer() {
        if (flaskProcess != null && flaskProcess.isAlive()) {
            flaskProcess.destroy();
            flaskRunning = false;
            System.out.println("🛑 Flask backend sikeresen leállítva.");
        } else {
            System.out.println("⚠️ Nincs futó Flask backend a leállításhoz.");
        }
    }
    private void clearDetails() {
        detailName.clear();
        detailPosition.clear();
        detailDept.getSelectionModel().clearSelection();
        detailHireDate.setValue(null);
        detailStatus.getSelectionModel().clearSelection();
        detailEmail.clear();
        detailPhone.clear();
    }

    private void updateStatistics(int count) {
        totalEmployees.setText("Összesen: " + count);
    }
}