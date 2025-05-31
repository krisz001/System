package System.trinexon;

import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;

public class FinanceController {

    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> projectComboBox;

    @FXML private TableView<?> incomeStatementTable;
    @FXML private TableColumn<?, ?> incomeCategoryColumn;
    @FXML private TableColumn<?, ?> incomeAmountColumn;
    @FXML private Label netProfitLabel;

    @FXML private TableView<?> balanceSheetTable;
    @FXML private TableColumn<?, ?> balanceItemColumn;
    @FXML private TableColumn<?, ?> balanceAmountColumn;

    @FXML private TableView<?> cashFlowTable;
    @FXML private TableColumn<?, ?> cashActivityColumn;
    @FXML private TableColumn<?, ?> cashAmountColumn;

    @FXML private LineChart<String, Number> lineChart;
    @FXML private PieChart expensePieChart;

    @FXML private Label summaryLabel;

    public void initialize() {
        // inicializálás, feltöltés stb.
    }

    @FXML
    private void onFilter() {
        // szűrés logikája
    }

    @FXML
    private void onExportPdf() {
        // exportálás PDF-be
    }

    @FXML
    private void onExportExcel() {
        // exportálás Excelbe
    }
}