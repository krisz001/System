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
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class FinanceController {
    private static final Logger LOGGER = Logger.getLogger(FinanceController.class.getName());

    // --- FXML elemek ---
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> projectComboBox;

    @FXML private TableView<FinanceRecord> incomeStatementTable;
    @FXML private TableColumn<FinanceRecord, String> incomeProjectColumn;
    @FXML private TableColumn<FinanceRecord, String> incomeTypeColumn;
    @FXML private TableColumn<FinanceRecord, String> incomeCategoryColumn;
    @FXML private TableColumn<FinanceRecord, Double> incomeAmountColumn;

    @FXML private TableView<FinanceRecord> balanceSheetTable;
    @FXML private TableView<FinanceRecord> cashFlowTable;
    @FXML private TableView<FinanceRecord> yearlySummaryTable;

    @FXML private Label netProfitLabel;
    @FXML private Label summaryLabel;

    @FXML private LineChart<String, Number> profitTrendChart;
    @FXML private PieChart expenseBreakdownChart;
    @FXML private TabPane financeTabPane;

    // --- Szolgáltatások ---
    private final FinanceService financeService       = new FinanceService();
    private final FinanceExportService exportService  = new FinanceExportService();

    /** Inicializálás FXML betöltésekor */
    @FXML
    public void initialize() {
        setupTableColumns();
        financeService.setupRowStyle(incomeStatementTable);
        financeService.loadComboBoxes(categoryComboBox, projectComboBox);
        setDefaultDates();
        loadData();
    }

    /** Beállítja az oszlopokat és az összegek formázását */
    private void setupTableColumns() {
        incomeProjectColumn .setCellValueFactory(new PropertyValueFactory<>("project"));
        incomeTypeColumn    .setCellValueFactory(new PropertyValueFactory<>("type"));
        incomeCategoryColumn.setCellValueFactory(new PropertyValueFactory<>("category"));
        incomeAmountColumn  .setCellValueFactory(new PropertyValueFactory<>("amount"));

        NumberFormat nf = NumberFormat.getInstance(new Locale("hu", "HU"));
        nf.setMaximumFractionDigits(0);
        nf.setGroupingUsed(true);

        incomeAmountColumn.setCellFactory(tc -> new TableCell<FinanceRecord, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText("");
                } else {
                    setText(nf.format(amount) + " Ft");
                }
            }
        });
    }

    /** Ha üres, az év elejére és mára állítja a dátumpickereket */
    private void setDefaultDates() {
        if (fromDatePicker.getValue() == null) fromDatePicker.setValue(LocalDate.now().withDayOfYear(1));
        if (toDatePicker.getValue()   == null) toDatePicker.setValue(LocalDate.now());
    }

    /** Betölti az összes táblát és diagramot a szűrők alapján */
    private void loadData() {
        String catFilter  = "Összes kategória".equals(categoryComboBox.getValue()) ? null : categoryComboBox.getValue();
        String projFilter = "Összes projekt" .equals(projectComboBox.getValue())   ? null : projectComboBox.getValue();

        ObservableList<FinanceRecord> records = financeService.loadData(
            fromDatePicker.getValue(),
            toDatePicker.getValue(),
            catFilter,
            projFilter,
            incomeStatementTable,
            netProfitLabel,
            summaryLabel,
            profitTrendChart,
            expenseBreakdownChart
        );

        // ugyanazokat a rekordokat használjuk minden táblához
        incomeStatementTable .setItems(records);
        balanceSheetTable    .setItems(records);
        cashFlowTable        .setItems(records);
        yearlySummaryTable   .setItems(records);
    }

    /** Frissít (F5 gomb vagy gombnyomás) */
    @FXML public void onRefresh(ActionEvent e) {
        loadData();
    }

    /** Szűrők változása esetén is betöltjük az új adatokat */
    @FXML private void onFilter(ActionEvent e) {
        loadData();
    }

    /** PDF export */
    @FXML private void onExportPdf() {
        File file = chooseSaveFile("PDF exportálása", "kimutatas.pdf", "*.pdf", "PDF fájl");
        if (file != null) exportService.exportToPdf(buildExportContext(file));
    }

    /** Excel export */
    @FXML private void onExportExcel() {
        File file = chooseSaveFile("Excel exportálása", "kimutatas.xlsx", "*.xlsx", "Excel fájl");
        if (file != null) exportService.exportToExcel(buildExportContext(file));
    }

    /** Fájl mentés dialógus */
    private File chooseSaveFile(String title, String initialName, String extPattern, String desc) {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.setInitialFileName(initialName);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(desc, extPattern));
        return fc.showSaveDialog(null);
    }

    /** Külső hívásra is elérhető metódus a frissítésre */
    public void refresh() {
        loadData();
    }

    /** Kontextus összeállítása exporthoz */
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
              "Mérleg",           balanceSheetTable,
              "Cash Flow",        cashFlowTable,
              "Éves összesítés",  yearlySummaryTable
            ),
            profitTrendChart,
            financeTabPane
        );
    }

    /** A kiválasztott tab-hoz tartozó táblázat */
    private TableView<FinanceRecord> getActiveTableView() {
        return switch (financeTabPane.getSelectionModel().getSelectedItem().getText()) {
            case "Mérleg"          -> balanceSheetTable;
            case "Cash Flow"       -> cashFlowTable;
            case "Éves összesítés" -> yearlySummaryTable;
            default                -> incomeStatementTable;
        };
    }
}