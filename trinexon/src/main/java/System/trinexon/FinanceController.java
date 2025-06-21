package System.trinexon;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;
import java.util.logging.Logger;

public class FinanceController {
    private static final Logger LOGGER = Logger.getLogger(FinanceController.class.getName());

    // --- FXML komponensek ---
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> projectComboBox;

    @FXML private TableView<FinanceRecord> incomeStatementTable;
    @FXML private TableView<FinanceRecord> yearlySummaryTable;
    @FXML private TableView<FinanceRecord> balanceSheetTable;
    @FXML private TableView<FinanceRecord> cashFlowTable;

    @FXML private TableColumn<FinanceRecord, String> incomeProjectColumn;
    @FXML private TableColumn<FinanceRecord, String> incomeTypeColumn;
    @FXML private TableColumn<FinanceRecord, String> incomeCategoryColumn;
    @FXML private TableColumn<FinanceRecord, Double> incomeAmountColumn;

    @FXML private Label netProfitLabel;
    @FXML private Label summaryLabel;

    @FXML private LineChart<String, Number> profitTrendChart;
    @FXML private PieChart expenseBreakdownChart;
    @FXML private TabPane financeTabPane;

    // --- Szolgáltatások ---
    private final FinanceService financeService = new FinanceService();
    private final FinanceExportService exportService = new FinanceExportService();

    // --- Inicializálás ---
    public void initialize() {
        setupColumns();
        financeService.setupRowStyle(incomeStatementTable);
        financeService.loadComboBoxes(categoryComboBox, projectComboBox);
        setDefaultDates();
        loadData();
    }

    private void setupColumns() {
        incomeProjectColumn.setCellValueFactory(new PropertyValueFactory<>("project"));
        incomeTypeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        incomeCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        incomeAmountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
    }

    private void setDefaultDates() {
        if (fromDatePicker.getValue() == null) fromDatePicker.setValue(LocalDate.of(2024, 1, 1));
        if (toDatePicker.getValue() == null) toDatePicker.setValue(LocalDate.of(2026, 12, 31));
    }

    private void loadData() {
        ObservableList<FinanceRecord> records = financeService.loadData(
                fromDatePicker.getValue(),
                toDatePicker.getValue(),
                categoryComboBox.getValue(),
                projectComboBox.getValue(),
                incomeStatementTable,
                netProfitLabel,
                summaryLabel,
                profitTrendChart,
                expenseBreakdownChart
        );
        incomeStatementTable.setItems(records);
    }

    // --- Eseménykezelők ---
    @FXML
    private void onRefresh(ActionEvent event) {
        loadData();
    }

    @FXML
    private void onFilter(ActionEvent event) {
        loadData();
    }

    @FXML
    private void onExportPdf() {
        File file = chooseFile("PDF exportálása", "kimutatas.pdf", "*.pdf", "PDF fájl");
        if (file != null) {
            FinanceExportContext context = buildExportContext(file);
            exportService.exportToPdf(context);
        }
    }

    @FXML
    private void onExportExcel() {
        File file = chooseFile("Excel exportálása", "kimutatas.xlsx", "*.xlsx", "Excel fájl");
        if (file != null) {
            FinanceExportContext context = buildExportContext(file);
            exportService.exportToExcel(context);
        }
    }

    // --- Segédfüggvények ---
    private File chooseFile(String title, String defaultName, String extension, String description) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        fileChooser.setInitialFileName(defaultName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, extension));
        return fileChooser.showSaveDialog(null);
    }

    private FinanceExportContext buildExportContext(File file) {
        return new FinanceExportContext(
                file,
                fromDatePicker.getValue(),
                toDatePicker.getValue(),
                projectComboBox.getValue(),
                categoryComboBox.getValue(),
                netProfitLabel.getText(),
                summaryLabel.getText(),
                getActiveTableView(),
                Map.of(
                        "Eredménykimutatás", incomeStatementTable,
                        "Mérleg", balanceSheetTable,
                        "Cash Flow", cashFlowTable,
                        "Éves összesítés", yearlySummaryTable
                ),
                profitTrendChart,
                financeTabPane
        );
    }

    private TableView<FinanceRecord> getActiveTableView() {
        Tab selectedTab = financeTabPane.getSelectionModel().getSelectedItem();
        if (selectedTab == null) return null;

        return switch (selectedTab.getText()) {
            case "Eredménykimutatás" -> incomeStatementTable;
            case "Mérleg" -> balanceSheetTable;
            case "Cash Flow" -> cashFlowTable;
            case "Éves összesítés" -> yearlySummaryTable;
            default -> null;
        };
    }
}