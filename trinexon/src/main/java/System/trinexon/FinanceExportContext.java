package System.trinexon;

import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;

public class FinanceExportContext {

    private final File file;
    private final LocalDate fromDate;
    private final LocalDate toDate;
    private final String selectedProject;
    private final String selectedCategory;
    private final String netProfitText;
    private final String summaryText;

    private final TableView<?> activeTable;
    private final Map<String, TableView<?>> tableMap;

    private final LineChart<String, Number> chart;
    private final TabPane tabPane;

    public FinanceExportContext(
            File file,
            LocalDate fromDate,
            LocalDate toDate,
            String selectedProject,
            String selectedCategory,
            String netProfitText,
            String summaryText,
            TableView<?> activeTable,
            Map<String, TableView<?>> tableMap,
            LineChart<String, Number> chart,
            TabPane tabPane
    ) {
        this.file = file;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.selectedProject = selectedProject;
        this.selectedCategory = selectedCategory;
        this.netProfitText = netProfitText;
        this.summaryText = summaryText;
        this.activeTable = activeTable;
        this.tableMap = tableMap;
        this.chart = chart;
        this.tabPane = tabPane;
    }

    // --- Getters ---

    public File getFile() {
        return file;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public String getSelectedProject() {
        return selectedProject;
    }

    public String getSelectedCategory() {
        return selectedCategory;
    }

    public String getNetProfitText() {
        return netProfitText;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public TableView<?> getActiveTable() {
        return activeTable;
    }

    public Map<String, TableView<?>> getTableMap() {
        return tableMap;
    }

    public LineChart<String, Number> getChart() {
        return chart;
    }

    public TabPane getTabPane() {
        return tabPane;
    }
}