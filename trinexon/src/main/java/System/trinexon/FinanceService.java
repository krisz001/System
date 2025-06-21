package System.trinexon;

import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FinanceService {
    private static final Logger LOGGER = Logger.getLogger(FinanceService.class.getName());

    /**
     * Táblázat oszlopainak beállítása (külön is hívható).
     */
    public void setupIncomeStatementColumns(
            TableColumn<FinanceRecord, String> projectCol,
            TableColumn<FinanceRecord, String> typeCol,
            TableColumn<FinanceRecord, String> categoryCol,
            TableColumn<FinanceRecord, Double> amountCol
    ) {
        projectCol.setCellValueFactory(new PropertyValueFactory<>("project"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
    }

    /**
     * Táblázat sorstílus beállítása típustól függően.
     */
    public void setupRowStyle(TableView<FinanceRecord> table) {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(FinanceRecord record, boolean empty) {
                super.updateItem(record, empty);
                if (record == null || empty) {
                    setStyle("");
                } else if ("Várható bevétel".equals(record.getType())) {
                    setStyle("-fx-background-color: #003c30;");
                } else if ("Felfüggesztve".equals(record.getType())) {
                    setStyle("-fx-background-color: #4b000f;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    /**
     * ComboBox mezők feltöltése az adatbázisból.
     */
    public void loadComboBoxes(ComboBox<String> categoryBox, ComboBox<String> projectBox) {
        ObservableList<String> categories = FXCollections.observableArrayList();
        ObservableList<String> projects = FXCollections.observableArrayList();

        try (Connection conn = Database.connect(); Statement stmt = conn.createStatement()) {
            ResultSet rsCat = stmt.executeQuery("SELECT name FROM categories ORDER BY name");
            while (rsCat.next()) categories.add(rsCat.getString("name"));
            rsCat.close();

            ResultSet rsProj = stmt.executeQuery("SELECT name FROM projects ORDER BY name");
            while (rsProj.next()) projects.add(rsProj.getString("name"));
            rsProj.close();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a ComboBox adatok betöltése során", e);
        }

        categoryBox.setItems(FXCollections.observableArrayList("Összes kategória"));
        categoryBox.getItems().addAll(categories);
        categoryBox.getSelectionModel().selectFirst();

        projectBox.setItems(FXCollections.observableArrayList("Összes projekt"));
        projectBox.getItems().addAll(projects);
        projectBox.getSelectionModel().selectFirst();
    }

    /**
     * Adatok betöltése a kiválasztott szűrők alapján.
     */
    public ObservableList<FinanceRecord> loadData(
            LocalDate from,
            LocalDate to,
            String category,
            String project,
            TableView<FinanceRecord> table,
            Label netProfitLabel,
            Label summaryLabel,
            LineChart<String, Number> lineChart,
            PieChart pieChart
    ) {
        ObservableList<FinanceRecord> records = FXCollections.observableArrayList();
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
        """ + (isFiltered(category) ? "AND c.name = ? " : "") +
               (isFiltered(project) ? "AND p.name = ? " : "") +
               "GROUP BY p.name, p.status, c.name";

        String expenseQuery = """
            SELECT p.name AS project, 'Kiadás' AS type, e.category AS category, SUM(e.amount) AS amount
            FROM expenses e
            JOIN projects p ON e.project_id = p.id
            WHERE e.date BETWEEN ? AND ?
        """ + (isFiltered(category) ? "AND e.category = ? " : "") +
               (isFiltered(project) ? "AND p.name = ? " : "") +
               "GROUP BY p.name, e.category";

        try (Connection conn = Database.connect()) {
            try (PreparedStatement ps = conn.prepareStatement(revenueQuery)) {
                setQueryParams(ps, from, to, category, project);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String type = rs.getString("type");
                    double amount = rs.getDouble("amount");
                    if ("Bevétel".equals(type)) totalIncome += amount;
                    records.add(new FinanceRecord(rs.getString("project"), type, rs.getString("category"), amount));
                }
                rs.close();
            }

            try (PreparedStatement ps = conn.prepareStatement(expenseQuery)) {
                setQueryParams(ps, from, to, category, project);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    double amount = rs.getDouble("amount");
                    totalExpense += amount;
                    records.add(new FinanceRecord(rs.getString("project"), "Kiadás", rs.getString("category"), amount));
                }
                rs.close();
            }

            netProfitLabel.setText(String.format("Nettó eredmény: %.0f Ft", totalIncome - totalExpense));
            summaryLabel.setText(String.format("Összes bevétel: %.0f Ft | Összes kiadás: %.0f Ft", totalIncome, totalExpense));

            FinanceChartUtil.updateProfitTrendChart(lineChart, from, to, category, project);
            FinanceChartUtil.updateExpenseBreakdownChart(pieChart, from, to, category, project);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba az adatok betöltésekor", e);
        }

        return records;
    }

    private boolean isFiltered(String value) {
        return value != null && !value.equals("Összes kategória") && !value.equals("Összes projekt");
    }

    private void setQueryParams(PreparedStatement ps, LocalDate from, LocalDate to, String category, String project) throws SQLException {
        ps.setDate(1, Date.valueOf(from));
        ps.setDate(2, Date.valueOf(to));
        int index = 3;
        if (isFiltered(category)) ps.setString(index++, category);
        if (isFiltered(project)) ps.setString(index, project);
    }

    /**
     * Profit trend frissítése (LineChart) – később implementálandó.
     */
    public void updateProfitTrendChart(LineChart<String, Number> chart, LocalDate from, LocalDate to, String category, String project) {
        chart.getData().clear();
        // TODO: implementáld a havi bevétel logikát
    }

    /**
     * Kiadások bontása (PieChart) – később implementálandó.
     */
    public void updateExpenseBreakdownChart(PieChart chart, LocalDate from, LocalDate to, String category, String project) {
        chart.getData().clear();
        // TODO: implementáld a kategória szerinti kiadásmegoszlást
    }
}