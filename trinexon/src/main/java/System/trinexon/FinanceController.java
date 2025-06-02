package System.trinexon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FinanceController {

    private static final Logger LOGGER = Logger.getLogger(FinanceController.class.getName());

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> projectComboBox;

    @FXML private TableView<FinanceRecord> incomeStatementTable;
    @FXML private TableColumn<FinanceRecord, String> incomeProjectColumn;
    @FXML private TableColumn<FinanceRecord, String> incomeTypeColumn;
    @FXML private TableColumn<FinanceRecord, String> incomeCategoryColumn;
    @FXML private TableColumn<FinanceRecord, Double> incomeAmountColumn;

    @FXML private Label netProfitLabel;
    @FXML private Label summaryLabel;

    @FXML private LineChart<String, Number> profitTrendChart;
    @FXML private PieChart expenseBreakdownChart;

    public void initialize() {
        setupColumns();
        loadComboBoxes();
        loadData();
    }

    private void setupColumns() {
        incomeProjectColumn.setCellValueFactory(new PropertyValueFactory<>("project"));
        incomeTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        incomeCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        incomeAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
    }

    private void loadComboBoxes() {
        ObservableList<String> categories = FXCollections.observableArrayList();
        ObservableList<String> projects = FXCollections.observableArrayList();

        try (Connection conn = Database.connect()) {
            try (Statement stmt = conn.createStatement()) {
                ResultSet rsCat = stmt.executeQuery("SELECT DISTINCT category FROM expenses ORDER BY category");
                while (rsCat.next()) {
                    categories.add(rsCat.getString("category"));
                }

                ResultSet rsProj = stmt.executeQuery("SELECT DISTINCT project_name FROM projects ORDER BY project_name");
                while (rsProj.next()) {
                    projects.add(rsProj.getString("project_name"));
                }
            }

            categoryComboBox.setItems(FXCollections.observableArrayList("Összes kategória"));
            categoryComboBox.getItems().addAll(categories);
            categoryComboBox.getSelectionModel().selectFirst();

            projectComboBox.setItems(FXCollections.observableArrayList("Összes projekt"));
            projectComboBox.getItems().addAll(projects);
            projectComboBox.getSelectionModel().selectFirst();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a ComboBox adatok betöltésekor", e);
        }
    }

    private void loadData() {
        LocalDate from = fromDatePicker.getValue() != null ? fromDatePicker.getValue() : LocalDate.now().minusMonths(1);
        LocalDate to = toDatePicker.getValue() != null ? toDatePicker.getValue() : LocalDate.now();
        fromDatePicker.setValue(from);
        toDatePicker.setValue(to);

        String category = categoryComboBox.getValue();
        String project = projectComboBox.getValue();

        ObservableList<FinanceRecord> records = FXCollections.observableArrayList();
        double totalIncome = 0;
        double totalExpense = 0;

        String revenueQuery = """
            SELECT p.project_name AS project, 'Bevétel' AS type, c.category_name AS category, SUM(r.amount) AS amount
            FROM revenues r
            JOIN projects p ON r.project_id = p.id
            JOIN categories c ON r.category_id = c.id
            WHERE r.date BETWEEN ? AND ?
            """ + (category != null && !category.equals("Összes kategória") ? "AND c.category_name = ? " : "") +
                (project != null && !project.equals("Összes projekt") ? "AND p.project_name = ? " : "") +
                "GROUP BY p.project_name, c.category_name";

        String expenseQuery = """
            SELECT p.project_name AS project, 'Kiadás' AS type, e.category AS category, SUM(e.amount) AS amount
            FROM expenses e
            JOIN projects p ON e.project_id = p.id
            WHERE e.date BETWEEN ? AND ?
            """ + (category != null && !category.equals("Összes kategória") ? "AND e.category = ? " : "") +
                (project != null && !project.equals("Összes projekt") ? "AND p.project_name = ? " : "") +
                "GROUP BY p.project_name, e.category";

        try (Connection conn = Database.connect()) {

            try (PreparedStatement ps = conn.prepareStatement(revenueQuery)) {
                setQueryParams(ps, from, to, category, project);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double amount = rs.getDouble("amount");
                        totalIncome += amount;
                        records.add(new FinanceRecord(rs.getString("project"), "Bevétel", rs.getString("category"), amount));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(expenseQuery)) {
                setQueryParams(ps, from, to, category, project);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double amount = rs.getDouble("amount");
                        totalExpense += amount;
                        records.add(new FinanceRecord(rs.getString("project"), "Kiadás", rs.getString("category"), amount));
                    }
                }
            }

            incomeStatementTable.setItems(records);
            netProfitLabel.setText(String.format("Nettó eredmény: %.0f Ft", totalIncome - totalExpense));
            summaryLabel.setText(String.format("Összes bevétel: %.0f Ft | Összes kiadás: %.0f Ft", totalIncome, totalExpense));

            updateProfitTrendChart(from, to, category, project);
            updateExpenseBreakdownChart(from, to, category, project);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba az adatok betöltésekor", e);
        }
    }

    private void setQueryParams(PreparedStatement ps, LocalDate from, LocalDate to, String category, String project) throws SQLException {
        ps.setDate(1, Date.valueOf(from));
        ps.setDate(2, Date.valueOf(to));
        int index = 3;
        if (category != null && !category.equals("Összes kategória")) {
            ps.setString(index++, category);
        }
        if (project != null && !project.equals("Összes projekt")) {
            ps.setString(index, project);
        }
    }

    private void updateProfitTrendChart(LocalDate from, LocalDate to, String category, String project) {
        profitTrendChart.getData().clear();
        XYChart.Series<String, Number> revenueSeries = new XYChart.Series<>();
        revenueSeries.setName("Havi bevétel");

        String query = """
            SELECT strftime('%Y-%m', r.date) AS month, SUM(r.amount) AS total
            FROM revenues r
            JOIN projects p ON r.project_id = p.id
            JOIN categories c ON r.category_id = c.id
            WHERE r.date BETWEEN ? AND ?
            """ + (category != null && !category.equals("Összes kategória") ? "AND c.category_name = ? " : "") +
                (project != null && !project.equals("Összes projekt") ? "AND p.project_name = ? " : "") +
            "GROUP BY month ORDER BY month";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            int index = 3;
            if (category != null && !category.equals("Összes kategória")) {
                ps.setString(index++, category);
            }
            if (project != null && !project.equals("Összes projekt")) {
                ps.setString(index, project);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String month = rs.getString("month");
                    double total = rs.getDouble("total");
                    revenueSeries.getData().add(new XYChart.Data<>(month, total));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a trend chart frissítésekor", e);
        }

        profitTrendChart.getData().add(revenueSeries);
    }

    private void updateExpenseBreakdownChart(LocalDate from, LocalDate to, String category, String project) {
        expenseBreakdownChart.getData().clear();

        String query = """
            SELECT e.category, SUM(e.amount) AS total
            FROM expenses e
            JOIN projects p ON e.project_id = p.id
            WHERE e.date BETWEEN ? AND ?
            """ + (category != null && !category.equals("Összes kategória") ? "AND e.category = ? " : "") +
                (project != null && !project.equals("Összes projekt") ? "AND p.project_name = ? " : "") +
                "GROUP BY e.category";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(query)) {

            setQueryParams(ps, from, to, category, project);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cat = rs.getString("category");
                    double total = rs.getDouble("total");
                    expenseBreakdownChart.getData().add(new PieChart.Data(cat, total));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a PieChart frissítésekor", e);
        }
    }

    @FXML
    public void onRefresh(ActionEvent event) {
        // Frissítés logika
        loadData();
        System.out.println("Refresh button clicked!");
    }

    @FXML
    private void onFilter(ActionEvent event) {
        System.out.println("Szűrés gomb megnyomva");
        loadData();
    }

    @FXML
    private void onExportPdf() {
        System.out.println("PDF export...");
        // PDF generálás itt implementálható
    }

    @FXML
    private void onExportExcel() {
        System.out.println("Excel export...");
        // Excel export itt implementálható
    }

    public static class FinanceRecord {
        private final String project;
        private final String type;
        private final String category;
        private final double amount;

        public FinanceRecord(String project, String type, String category, double amount) {
            this.project = project;
            this.type = type;
            this.category = category;
            this.amount = amount;
        }

        public String getProject() { return project; }
        public String getType() { return type; }
        public String getCategory() { return category; }
        public double getAmount() { return amount; }
    }
}
