package System.trinexon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    @FXML private TableView<IncomeRecord> incomeStatementTable;
    @FXML private TableColumn<IncomeRecord, String> incomeProjectColumn;
    @FXML private TableColumn<IncomeRecord, String> incomeTypeColumn;
    @FXML private TableColumn<IncomeRecord, String> incomeCategoryColumn;
    @FXML private TableColumn<IncomeRecord, Double> incomeAmountColumn;

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

        String catQuery = "SELECT DISTINCT category FROM expenses ORDER BY category";
        String projQuery = "SELECT DISTINCT project_name FROM projects ORDER BY project_name";

        try (Connection conn = Database.connect();
             Statement stmt = conn.createStatement()) {

            try (ResultSet rsCat = stmt.executeQuery(catQuery)) {
                while (rsCat.next()) {
                    categories.add(rsCat.getString("category"));
                }
            }

            try (ResultSet rsProj = stmt.executeQuery(projQuery)) {
                while (rsProj.next()) {
                    projects.add(rsProj.getString("project_name"));
                }
            }

            categoryComboBox.getItems().clear();
            projectComboBox.getItems().clear();

            categoryComboBox.getItems().add("Összes kategória");
            categoryComboBox.getItems().addAll(categories);

            projectComboBox.getItems().add("Összes projekt");
            projectComboBox.getItems().addAll(projects);

            categoryComboBox.getSelectionModel().selectFirst();
            projectComboBox.getSelectionModel().selectFirst();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a kategóriák vagy projektek betöltésekor", e);
        }
    }

    @FXML
    private void onFilter() {
        loadData();
    }

    private void loadData() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();

        if (from == null) {
            from = LocalDate.now().minusMonths(1);
            fromDatePicker.setValue(from);
        }
        if (to == null) {
            to = LocalDate.now();
            toDatePicker.setValue(to);
        }

        String selectedCategory = categoryComboBox.getSelectionModel().getSelectedItem();
        String selectedProject = projectComboBox.getSelectionModel().getSelectedItem();

        loadIncomeStatement(from, to, selectedCategory, selectedProject);
        updateNetProfit();
        updateCharts(from, to, selectedCategory, selectedProject);
        updateSummaryLabel(from, to, selectedCategory, selectedProject);
    }

    private void loadIncomeStatement(LocalDate from, LocalDate to, String category, String project) {
        ObservableList<IncomeRecord> data = FXCollections.observableArrayList();

        StringBuilder query = new StringBuilder("SELECT p.project_name AS project, 'Bevétel' AS type, c.category_name AS category, SUM(r.amount) AS amount " +
                "FROM revenues r " +
                "JOIN projects p ON r.project_id = p.id " +
                "JOIN categories c ON r.category_id = c.id " +
                "WHERE r.date BETWEEN ? AND ? ");

        if (category != null && !category.equals("Összes kategória")) {
            query.append("AND c.category_name = ? ");
        }
        if (project != null && !project.equals("Összes projekt")) {
            query.append("AND p.project_name = ? ");
        }
        query.append("GROUP BY p.project_name, c.category_name ");

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(query.toString())) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));

            int paramIndex = 3;
            if (category != null && !category.equals("Összes kategória")) {
                ps.setString(paramIndex++, category);
            }
            if (project != null && !project.equals("Összes projekt")) {
                ps.setString(paramIndex, project);
            }

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                data.add(new IncomeRecord(
                        rs.getString("project"),
                        rs.getString("type"),
                        rs.getString("category"),
                        rs.getDouble("amount")
                ));
            }

            incomeStatementTable.setItems(data);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba az eredménykimutatás betöltésekor", e);
        }
    }

    private void updateNetProfit() {
        double totalIncome = 0;
        for (IncomeRecord rec : incomeStatementTable.getItems()) {
            if (rec.getType().equals("Bevétel")) {
                totalIncome += rec.getAmount();
            }
        }
        // Hasonlóan a kiadások, például később lehet bővíteni

        netProfitLabel.setText(String.format("Nettó eredmény: %.0f Ft", totalIncome)); 
    }

    private void updateCharts(LocalDate from, LocalDate to, String category, String project) {
        updateProfitTrendChart(from, to, category, project);
        updateExpenseBreakdownChart(from, to, category, project);
    }

    private void updateProfitTrendChart(LocalDate from, LocalDate to, String category, String project) {
        profitTrendChart.getData().clear();

        XYChart.Series<String, Number> incomeSeries = new XYChart.Series<>();
        incomeSeries.setName("Bevétel");

        // Dummy data: pl. havi bontás
        // Itt érdemes SQL-lekérdezést írni, ami havi összesítést ad vissza
        // Példa (dummy értékek):
        incomeSeries.getData().add(new XYChart.Data<>("2025-01", 150000));
        incomeSeries.getData().add(new XYChart.Data<>("2025-02", 180000));
        incomeSeries.getData().add(new XYChart.Data<>("2025-03", 210000));

        profitTrendChart.getData().add(incomeSeries);
    }

    private void updateExpenseBreakdownChart(LocalDate from, LocalDate to, String category, String project) {
        expenseBreakdownChart.getData().clear();

        // Dummy adatok kategóriánként
        expenseBreakdownChart.getData().add(new PieChart.Data("Bérköltség", 300000));
        expenseBreakdownChart.getData().add(new PieChart.Data("Anyagköltség", 150000));
        expenseBreakdownChart.getData().add(new PieChart.Data("Szállítás", 50000));
    }

    private void updateSummaryLabel(LocalDate from, LocalDate to, String category, String project) {
        // Itt összegezhetsz bevételeket és kiadásokat

        double totalRevenue = 350000; // dummy
        double totalExpense = 200000; // dummy

        summaryLabel.setText(String.format("Összes bevétel: %.0f Ft | Összes kiadás: %.0f Ft", totalRevenue, totalExpense));
    }

    @FXML
    private void onExportPdf() {
        // Export PDF funkció ide jön
        System.out.println("PDF export...");
    }

    @FXML
    private void onExportExcel() {
        // Export Excel funkció ide jön
        System.out.println("Excel export...");
    }

    // Egyszerű adatmodell osztályok
    public static class IncomeRecord {
        private final String project;
        private final String type;
        private final String category;
        private final double amount;

        public IncomeRecord(String project, String type, String category, double amount) {
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

    // Itt hasonlóan létrehozhatsz ExpenseRecord vagy más modelleket, ha kell
}
