package System.trinexon;

import javafx.scene.chart.LineChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TabPane;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

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
    	    this.file = Objects.requireNonNull(file, "file must not be null");
    	    this.fromDate = Objects.requireNonNull(fromDate, "fromDate must not be null");
    	    this.toDate = Objects.requireNonNull(toDate, "toDate must not be null");
    	    this.selectedProject = selectedProject;      // ezek lehetnek null-ok is, ha nincs szűrés
    	    this.selectedCategory = selectedCategory;
    	    this.netProfitText = Objects.requireNonNull(netProfitText, "netProfitText must not be null");
    	    this.summaryText = Objects.requireNonNull(summaryText, "summaryText must not be null");
    	    this.activeTable = activeTable;              // null esetén a szolgáltatás kezelni tudja az üres táblát
    	    this.tableMap = Objects.requireNonNull(tableMap, "tableMap must not be null");
    	    this.chart = chart;
    	    this.tabPane = Objects.requireNonNull(tabPane, "tabPane must not be null");
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