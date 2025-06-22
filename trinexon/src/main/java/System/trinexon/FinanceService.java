// FinanceService.java
package System.trinexon;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;

import java.sql.*;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FinanceService {
    private static final Logger LOGGER = Logger.getLogger(FinanceService.class.getName());

    // 1) Combo-boxok feltöltése: "Összes ..." opcióval
    public void loadComboBoxes(ComboBox<String> categoryBox, ComboBox<String> projectBox) {
        ObservableList<String> cats  = FXCollections.observableArrayList("Összes kategória");
        cats.addAll(loadCategories());
        categoryBox.setItems(cats);
        categoryBox.getSelectionModel().selectFirst();

        ObservableList<String> projs = FXCollections.observableArrayList("Összes projekt");
        projs.addAll(loadProjects());
        projectBox.setItems(projs);
        projectBox.getSelectionModel().selectFirst();
    }

    public ObservableList<String> loadCategories() {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = "SELECT name FROM categories ORDER BY name";
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Cannot load categories", e);
        }
        return list;
    }

    public ObservableList<String> loadProjects() {
        ObservableList<String> list = FXCollections.observableArrayList();
        String sql = "SELECT name FROM projects ORDER BY name";
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(rs.getString("name"));
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Cannot load projects", e);
        }
        return list;
    }

    // 2) Sor-stílus beállítása az Eredménykimutatás táblához
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

    // 3) Adatok betöltése projektenként: bevétel–kiadás különbözet, nullás projektek is
    public ObservableList<FinanceRecord> loadData(
            LocalDate from,
            LocalDate to,
            String categoryFilter,
            String projectFilter,
            TableView<FinanceRecord> table,
            Label netProfitLabel,
            Label summaryLabel,
            LineChart<String, Number> lineChart,
            PieChart pieChart
    ) {
        ObservableList<FinanceRecord> records = FXCollections.observableArrayList();
        double totalIncome = 0, totalExpense = 0;

        String sql = """
            SELECT
              p.name AS project,
              CASE
                WHEN COALESCE(r.sum_rev,0) > COALESCE(e.sum_exp,0) THEN 'Bevétel'
                WHEN COALESCE(e.sum_exp,0) > COALESCE(r.sum_rev,0) THEN 'Kiadás'
                ELSE 'Nincs tétel'
              END AS type,
              COALESCE(c_rev.name, e.category, '') AS category,
              COALESCE(r.sum_rev,0) - COALESCE(e.sum_exp,0) AS amount
            FROM projects p
            LEFT JOIN (
              SELECT project_id, category_id, SUM(amount) AS sum_rev
              FROM revenues
              WHERE date BETWEEN ? AND ?
              GROUP BY project_id, category_id
            ) r ON r.project_id = p.id
            LEFT JOIN categories c_rev ON c_rev.id = r.category_id
            LEFT JOIN (
              SELECT project_id, category AS category, SUM(amount) AS sum_exp
              FROM expenses
              WHERE date BETWEEN ? AND ?
              GROUP BY project_id, category
            ) e ON e.project_id = p.id
            WHERE 1=1
        """
        + (categoryFilter  != null ? " AND (c_rev.name = ? OR e.category = ?) " : "")
        + (projectFilter   != null ? " AND p.name = ? " : "")
        + "ORDER BY p.name";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            ps.setDate(3, Date.valueOf(from));
            ps.setDate(4, Date.valueOf(to));
            int idx = 5;
            if (categoryFilter != null) {
                ps.setString(idx++, categoryFilter);
                ps.setString(idx++, categoryFilter);
            }
            if (projectFilter != null) {
                ps.setString(idx, projectFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String proj = rs.getString("project");
                    String type = rs.getString("type");
                    String cat  = rs.getString("category");
                    double amt  = rs.getDouble("amount");

                    if ("Bevétel".equals(type)) totalIncome += amt;
                    if ("Kiadás".equals(type))  totalExpense += amt;

                    records.add(new FinanceRecord(proj, type, cat, amt));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba az adatok betöltésekor", e);
        }

        netProfitLabel.setText(String.format("Nettó eredmény: %.0f Ft", totalIncome - totalExpense));
        summaryLabel.setText(String.format("Összes bevétel: %.0f Ft | Összes kiadás: %.0f Ft", totalIncome, totalExpense));

        FinanceChartUtil.updateProfitTrendChart(lineChart, from, to, categoryFilter, projectFilter);
        FinanceChartUtil.updateExpenseBreakdownChart(pieChart, from, to, categoryFilter, projectFilter);

        return records;
    }
}