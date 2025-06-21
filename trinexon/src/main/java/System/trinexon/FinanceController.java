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
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

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

    private ObservableList<FinanceRecord> records;

    public void initialize() {
        setupColumns();
        setupRowStyle();
        loadComboBoxes();
        setDefaultDates();
        loadData();
    }

    private void setupColumns() {
        incomeProjectColumn.setCellValueFactory(new PropertyValueFactory<>("project"));
        incomeTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        incomeCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        incomeAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
    }

    private void setupRowStyle() {
        incomeStatementTable.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(FinanceRecord record, boolean empty) {
                super.updateItem(record, empty);
                if (record == null || empty) {
                    setStyle("");
                } else if (record.getType().equals("Várható bevétel")) {
                    setStyle("-fx-background-color: #fff3cd;");
                } else if (record.getType().equals("Felfüggesztve")) {
                    setStyle("-fx-background-color: #f8d7da;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void loadComboBoxes() {
        ObservableList<String> categories = FXCollections.observableArrayList();
        ObservableList<String> projects = FXCollections.observableArrayList();

        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement()) {
            try (ResultSet rsCat = stmt.executeQuery("SELECT name FROM categories ORDER BY name")) {
                while (rsCat.next()) {
                    categories.add(rsCat.getString("name"));
                }
            }

            try (ResultSet rsProj = stmt.executeQuery("SELECT name FROM projects ORDER BY name")) {
                while (rsProj.next()) {
                    projects.add(rsProj.getString("name"));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a ComboBox adatok betoltese soran", e);
        }

        categoryComboBox.setItems(FXCollections.observableArrayList("Összes kategória"));
        categoryComboBox.getItems().addAll(categories);
        categoryComboBox.getSelectionModel().selectFirst();

        projectComboBox.setItems(FXCollections.observableArrayList("Összes projekt"));
        projectComboBox.getItems().addAll(projects);
        projectComboBox.getSelectionModel().selectFirst();
    }

    private void setDefaultDates() {
        if (fromDatePicker.getValue() == null) fromDatePicker.setValue(LocalDate.of(2024, 1, 1));
        if (toDatePicker.getValue() == null) toDatePicker.setValue(LocalDate.of(2026, 12, 31));
    }

    private void loadData() {
        LocalDate from = fromDatePicker.getValue();
        LocalDate to = toDatePicker.getValue();
        String category = categoryComboBox.getValue();
        String project = projectComboBox.getValue();

        records = FXCollections.observableArrayList();
        double totalIncome = 0, totalExpense = 0;

        String revenueQuery = """
            SELECT p.name AS project,
                   CASE LOWER(p.status)
                       WHEN 'kész' THEN 'Bevétel'
                       WHEN 'folyamatban' THEN 'Várható bevétel'
                       ELSE 'Felfüggesztve' END AS type,
                   c.name AS category,
                   SUM(r.amount) AS amount
            FROM revenues r
            JOIN projects p ON r.project_id = p.id
            JOIN categories c ON r.category_id = c.id
            WHERE r.date BETWEEN ? AND ?
        """ + (category != null && !category.equals("Összes kategória") ? "AND c.name = ? " : "") +
               (project != null && !project.equals("Összes projekt") ? "AND p.name = ? " : "") +
               " GROUP BY p.name, p.status, c.name";

        String expenseQuery = """
            SELECT p.name AS project, 'Kiadás' AS type, e.category AS category, SUM(e.amount) AS amount
            FROM expenses e
            JOIN projects p ON e.project_id = p.id
            WHERE e.date BETWEEN ? AND ?
        """ + (category != null && !category.equals("Összes kategória") ? "AND e.category = ? " : "") +
               (project != null && !project.equals("Összes projekt") ? "AND p.name = ? " : "") +
               " GROUP BY p.name, e.category";

        try (Connection conn = Database.connect()) {
            try (PreparedStatement ps = conn.prepareStatement(revenueQuery)) {
                setQueryParams(ps, from, to, category, project);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String type = rs.getString("type");
                        double amount = rs.getDouble("amount");
                        if ("Bevétel".equals(type)) totalIncome += amount;
                        records.add(new FinanceRecord(rs.getString("project"), type, rs.getString("category"), amount));
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
        if (category != null && !category.equals("Összes kategória")) ps.setString(index++, category);
        if (project != null && !project.equals("Összes projekt")) ps.setString(index, project);
    }

    private void updateProfitTrendChart(LocalDate from, LocalDate to, String category, String project) {
        profitTrendChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Havi bevétel");

        String query = """
            SELECT DATE_FORMAT(r.date, '%Y-%m') AS month, SUM(r.amount) AS total
            FROM revenues r
            JOIN projects p ON r.project_id = p.id
            JOIN categories c ON r.category_id = c.id
            WHERE r.date BETWEEN ? AND ?
        """ + (category != null && !category.equals("Összes kategória") ? "AND c.name = ? " : "") +
               (project != null && !project.equals("Összes projekt") ? "AND p.name = ?" : "") +
               " GROUP BY month ORDER BY month";

        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(query)) {
            setQueryParams(ps, from, to, category, project);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    series.getData().add(new XYChart.Data<>(rs.getString("month"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a trend chart frissítésekor", e);
        }

        profitTrendChart.getData().add(series);
    }

    private void updateExpenseBreakdownChart(LocalDate from, LocalDate to, String category, String project) {
        expenseBreakdownChart.getData().clear();

        String query = """
            SELECT e.category, SUM(e.amount) AS total
            FROM expenses e
            JOIN projects p ON e.project_id = p.id
            WHERE e.date BETWEEN ? AND ?
        """ + (category != null && !category.equals("Összes kategória") ? "AND e.category = ? " : "") +
               (project != null && !project.equals("Összes projekt") ? "AND p.name = ? " : "") +
               " GROUP BY e.category";

        try (Connection conn = Database.connect(); PreparedStatement ps = conn.prepareStatement(query)) {
            setQueryParams(ps, from, to, category, project);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    expenseBreakdownChart.getData().add(new PieChart.Data(rs.getString("category"), rs.getDouble("total")));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a PieChart frissítésekor", e);
        }
    }

    @FXML public void onRefresh(ActionEvent event) { loadData(); }
    @FXML private void onFilter(ActionEvent event) { loadData(); }

    @FXML
    private void onExportPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("PDF exportálása");
        fileChooser.setInitialFileName("adatok.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF fájl", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                PdfPTable pdfTable = new PdfPTable(incomeStatementTable.getColumns().size());

                for (TableColumn<FinanceRecord, ?> column : incomeStatementTable.getColumns()) {
                    pdfTable.addCell(new PdfPCell(new Phrase(column.getText())));
                }

                for (FinanceRecord row : incomeStatementTable.getItems()) {
                    for (TableColumn<FinanceRecord, ?> column : incomeStatementTable.getColumns()) {
                        Object cell = column.getCellData(row);
                        pdfTable.addCell(cell != null ? cell.toString() : "");
                    }
                }

                document.add(pdfTable);
                document.close();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Sikeres exportálás");
                alert.setContentText("PDF fájl elmentve:\n" + file.getAbsolutePath());
                alert.showAndWait();

            } catch (Exception e) {
                showError("PDF exportálás sikertelen", e);
            }
        }
    }

    @FXML
    private void onExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Excel exportálása");
        fileChooser.setInitialFileName("adatok.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel fájl", "*.xlsx"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (XSSFWorkbook workbook = new XSSFWorkbook()) {
                XSSFSheet sheet = workbook.createSheet("Adatok");

                int rowCount = 0;
                Row headerRow = sheet.createRow(rowCount++);
                int colCount = 0;
                for (TableColumn<FinanceRecord, ?> column : incomeStatementTable.getColumns()) {
                    org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(colCount++);
                    cell.setCellValue(column.getText());
                }

                for (FinanceRecord item : incomeStatementTable.getItems()) {
                    Row row = sheet.createRow(rowCount++);
                    int cellCount = 0;
                    for (TableColumn<FinanceRecord, ?> column : incomeStatementTable.getColumns()) {
                        Object cellValue = column.getCellData(item);
                        row.createCell(cellCount++).setCellValue(cellValue != null ? cellValue.toString() : "");
                    }
                }

                try (FileOutputStream out = new FileOutputStream(file)) {
                    workbook.write(out);
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Sikeres exportálás");
                alert.setContentText("Excel fájl elmentve:\n" + file.getAbsolutePath());
                alert.showAndWait();

            } catch (Exception e) {
                showError("Excel exportálás sikertelen", e);
            }
        }
    }

    private void showError(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Hiba");
        alert.setHeaderText(message);
        alert.setContentText(e.getMessage());
        alert.showAndWait();
        e.printStackTrace();
    }

    public static class FinanceRecord {
        private final String project, type, category;
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