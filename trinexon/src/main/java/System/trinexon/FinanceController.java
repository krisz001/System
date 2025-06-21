package System.trinexon;

// JavaFX
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;

// Java SE
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

// Apache POI – Excel exporthoz
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFFont;

// iTextPDF – PDF exporthoz
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
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
    @FXML private TableView<FinanceRecord> yearlySummaryTable;

    @FXML private Label netProfitLabel;
    @FXML private Label summaryLabel;

    @FXML private LineChart<String, Number> profitTrendChart;
    @FXML private PieChart expenseBreakdownChart;
    @FXML private TabPane financeTabPane;
    @FXML private TableView<FinanceRecord> balanceSheetTable;
    @FXML private TableView<FinanceRecord> cashFlowTable;

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
        // TODO: Query and fill series
        profitTrendChart.getData().add(series);
    }

    private void updateExpenseBreakdownChart(LocalDate from, LocalDate to, String category, String project) {
        expenseBreakdownChart.getData().clear();
        // TODO: Query and fill pie chart
    }
    @SuppressWarnings("unchecked")
    private TableView<FinanceRecord> getActiveTableView() {
        Tab selectedTab = financeTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return null;

        return switch (selectedTab.getText()) {
            case "Eredménykimutatás" -> incomeStatementTable;
            case "Mérleg" -> balanceSheetTable;
            case "Cash Flow" -> cashFlowTable;
            case "Éves összesítés" -> yearlySummaryTable;
            case "Diagramok" -> null; // Ez szándékosan null, nincs táblázat
            default -> null;
        };
    }
    @FXML private void onRefresh(ActionEvent event) { loadData(); }
    @FXML private void onFilter(ActionEvent event) { loadData(); }
    
    @FXML
    private void onExportPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("PDF exportálása");
        fileChooser.setInitialFileName("kimutatas.pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF fájl", "*.pdf"));
        File file = fileChooser.showSaveDialog(null);

        if (file == null) return;

        try {
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Betűtípusok
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, BaseColor.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12, BaseColor.DARK_GRAY);

            // Cím és metaadatok
            document.addTitle("Pénzügyi kimutatás");
            document.addAuthor("Trinexon Rendszer");
            document.add(new Paragraph("Pénzügyi kimutatás", titleFont));
            document.add(Chunk.NEWLINE);

            // Alapadatok
            document.add(new Paragraph("Dátumtartomány: " + fromDatePicker.getValue() + " - " + toDatePicker.getValue(), normalFont));
            document.add(new Paragraph("Projekt: " + projectComboBox.getValue(), normalFont));
            document.add(new Paragraph("Kategória: " + categoryComboBox.getValue(), normalFont));
            document.add(new Paragraph("Nettó eredmény: " + netProfitLabel.getText(), normalFont));
            document.add(new Paragraph("Összegzés: " + summaryLabel.getText(), normalFont));
            document.add(Chunk.NEWLINE);

            // Aktív fül alapján döntés
            Tab selectedTab = financeTabPane.getSelectionModel().getSelectedItem();
            String tabName = selectedTab != null ? selectedTab.getText() : "";

            if ("Diagramok".equals(tabName)) {
                // Diagram mentése és beillesztése
                WritableImage chartImage = profitTrendChart.snapshot(new SnapshotParameters(), null);
                File tempImageFile = new File("chart_snapshot.png");

                try {
                    ImageIO.write(SwingFXUtils.fromFXImage(chartImage, null), "png", tempImageFile);
                    Image chart = Image.getInstance(tempImageFile.getAbsolutePath());
                    chart.scaleToFit(500, 300);
                    chart.setAlignment(Element.ALIGN_CENTER);
                    document.add(chart);
                    document.add(Chunk.NEWLINE);
                    tempImageFile.delete();
                } catch (Exception ex) {
                    document.add(new Paragraph("A diagram képet nem sikerült betölteni.", normalFont));
                }

            } else {
                // Táblázat exportálás
                TableView<?> table = getActiveTableView();

                if (table == null) {
                    document.add(new Paragraph("Ehhez a nézethez nem tartozik táblázat.", normalFont));
                } else if (table.getItems().isEmpty()) {
                    document.add(new Paragraph("A táblázat nem tartalmaz adatot.", normalFont));
                } else {
                    PdfPTable pdfTable = new PdfPTable(table.getColumns().size());
                    pdfTable.setWidthPercentage(100);

                    // Fejlécek
                    for (TableColumn<?, ?> col : table.getColumns()) {
                        PdfPCell header = new PdfPCell(new Phrase(col.getText(), titleFont));
                        header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                        header.setHorizontalAlignment(Element.ALIGN_CENTER);
                        pdfTable.addCell(header);
                    }

                    // Adatsorok
                    for (Object row : table.getItems()) {
                        for (TableColumn<?, ?> column : table.getColumns()) {
                            @SuppressWarnings("unchecked")
                            TableColumn<Object, ?> col = (TableColumn<Object, ?>) column;
                            Object value = col.getCellObservableValue(row).getValue();
                            String cellText = value != null ? value.toString() : "";
                            PdfPCell cell = new PdfPCell(new Phrase(cellText, normalFont));
                            pdfTable.addCell(cell);
                        }
                    }

                    document.add(pdfTable);
                }
            }

            document.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Sikeres exportálás");
            alert.setContentText("PDF fájl elmentve: " + file.getAbsolutePath());
            alert.showAndWait();

        } catch (Exception e) {
            showError("PDF exportálás sikertelen", e);
        }
    }

    @FXML
    private void onExportExcel() {
        // 1. Adatok frissítése
        loadData();

        // 2. Fájl kiválasztása
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Excel exportálása");
        fileChooser.setInitialFileName("kimutatas.xlsx");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel fájl", "*.xlsx"));
        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            // 3. Stílusok
            CellStyle titleStyle = workbook.createCellStyle();
            XSSFFont titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 4. Összegző lap// 1. Összegző lap
            XSSFSheet infoSheet = workbook.createSheet("Összegzés");
            int rowCount = 0;

            Row titleRow = infoSheet.createRow(rowCount++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Pénzügyi kimutatás");
            titleCell.setCellStyle(titleStyle);

            infoSheet.createRow(rowCount++).createCell(0)
                .setCellValue("Dátumtartomány: " + fromDatePicker.getValue() + " - " + toDatePicker.getValue());
            infoSheet.createRow(rowCount++).createCell(0)
                .setCellValue("Projekt: " + projectComboBox.getValue());
            infoSheet.createRow(rowCount++).createCell(0)
                .setCellValue("Kategória: " + categoryComboBox.getValue());

            // Tisztított szöveg, hogy ne legyen duplikált prefix
            String netProfitValue = netProfitLabel.getText().replace("Nettó eredmény: ", "");
            String summaryValue = summaryLabel.getText().replace("Összegzés: ", "");

            infoSheet.createRow(rowCount++).createCell(0)
                .setCellValue("Nettó eredmény: " + netProfitValue);
            infoSheet.createRow(rowCount++).createCell(0)
                .setCellValue("Összegzés: " + summaryValue);

            // 5. Táblázatok külön lapokra
            Map<String, TableView<?>> tableMap = Map.of(
                "Eredménykimutatás", incomeStatementTable,
                "Mérleg", balanceSheetTable,
                "Cash Flow", cashFlowTable,
                "Éves összesítés", yearlySummaryTable
            );

            for (Map.Entry<String, TableView<?>> entry : tableMap.entrySet()) {
                String sheetName = entry.getKey();
                TableView<?> rawTable = entry.getValue();

                if (rawTable == null || rawTable.getItems().isEmpty()) continue;

                @SuppressWarnings("unchecked")
                TableView<Object> table = (TableView<Object>) rawTable;
                XSSFSheet sheet = workbook.createSheet(sheetName);

                int sheetRow = 0;
                Row headerRow = sheet.createRow(sheetRow++);

                int colIndex = 0;
                for (TableColumn<?, ?> column : table.getColumns()) {
                    Cell cell = headerRow.createCell(colIndex++);
                    cell.setCellValue(column.getText());
                    cell.setCellStyle(headerStyle);
                }

                for (Object item : table.getItems()) {
                    Row row = sheet.createRow(sheetRow++);
                    int cellIndex = 0;

                    for (TableColumn<?, ?> column : table.getColumns()) {
                        @SuppressWarnings("unchecked")
                        TableColumn<Object, ?> col = (TableColumn<Object, ?>) column;
                        ObservableValue<?> obs = col.getCellObservableValue(item);
                        Object value = obs != null ? obs.getValue() : "";
                        row.createCell(cellIndex++).setCellValue(value != null ? value.toString() : "");
                    }
                }

                for (int i = 0; i < table.getColumns().size(); i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            // 6. Fájl mentése
            try (FileOutputStream out = new FileOutputStream(file)) {
                workbook.write(out);
            }

            // 7. Sikeres export értesítés
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("Sikeres exportálás");
            alert.setContentText("Excel fájl elmentve: " + file.getAbsolutePath());
            alert.showAndWait();

        } catch (Exception e) {
            showError("Excel exportálás sikertelen", e);
        }
    }
    private int writeMetaData(XSSFSheet sheet, int rowCount) {
        sheet.createRow(rowCount++).createCell(0).setCellValue("Pénzügyi kimutatás");
        sheet.createRow(rowCount++).createCell(0).setCellValue("Dátum: " + fromDatePicker.getValue() + " - " + toDatePicker.getValue());
        sheet.createRow(rowCount++).createCell(0).setCellValue("Projekt: " + projectComboBox.getValue());
        sheet.createRow(rowCount++).createCell(0).setCellValue("Kategória: " + categoryComboBox.getValue());
        sheet.createRow(rowCount++).createCell(0).setCellValue("Nettó eredmény: " + netProfitLabel.getText());
        sheet.createRow(rowCount++).createCell(0).setCellValue("Összegzés: " + summaryLabel.getText());
        return rowCount + 1;
    }

    private void writeTableHeaders(Row headerRow, TableView<?> table) {
        int colCount = 0;
        for (TableColumn<?, ?> column : table.getColumns()) {
            headerRow.createCell(colCount++).setCellValue(column.getText());
        }
    }

    private void writeTableContent(XSSFSheet sheet, TableView<FinanceRecord> table, int startRow) {
        int rowCount = startRow;
        for (FinanceRecord item : table.getItems()) {
            Row row = sheet.createRow(rowCount++);
            int cellCount = 0;
            for (TableColumn<FinanceRecord, ?> column : table.getColumns()) {
                ObservableValue<?> observable = column.getCellObservableValue(item);
                Object value = observable != null ? observable.getValue() : null;
                row.createCell(cellCount++).setCellValue(value != null ? value.toString() : "");
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
