package System.trinexon;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FinanceChartUtil {
    private static final Logger LOGGER = Logger.getLogger(FinanceChartUtil.class.getName());

    /**
     * Minden hónapra egy adatpont – havi bevételek trendje.
     */
    public static void updateProfitTrendChart(LineChart<String, Number> chart,
                                              LocalDate from,
                                              LocalDate to,
                                              String category,
                                              String project) {
        chart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Havi bevétel");

        String sql = """
            SELECT EXTRACT(YEAR_MONTH FROM r.date) AS ym,
                   MONTH(r.date) AS month,
                   YEAR(r.date) AS year,
                   SUM(r.amount) AS amount
            FROM revenues r
            JOIN projects p ON r.project_id = p.id
            JOIN categories c ON r.category_id = c.id
            WHERE r.date BETWEEN ? AND ?
        """ + (isFiltered(category) ? "AND c.name = ? " : "") +
               (isFiltered(project) ? "AND p.name = ? " : "") +
               "GROUP BY ym, year, month ORDER BY year, month";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            int i = 3;
            if (isFiltered(category)) ps.setString(i++, category);
            if (isFiltered(project)) ps.setString(i, project);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int month = rs.getInt("month");
                    int year = rs.getInt("year");
                    double amount = rs.getDouble("amount");

                    String label = getMonthName(month) + " " + year;
                    series.getData().add(new XYChart.Data<>(label, amount));
                }
            }

            chart.getData().add(series);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a havi bevétel lekérdezésénél", e);
        }
    }

    /**
     * Kiadás kategóriák szerint – PieChart frissítése.
     */
    public static void updateExpenseBreakdownChart(PieChart chart,
                                                   LocalDate from,
                                                   LocalDate to,
                                                   String category,
                                                   String project) {
        chart.getData().clear();

        String sql = """
            SELECT e.category AS category, SUM(e.amount) AS total
            FROM expenses e
            JOIN projects p ON e.project_id = p.id
            WHERE e.date BETWEEN ? AND ?
        """ + (isFiltered(category) ? "AND e.category = ? " : "") +
               (isFiltered(project) ? "AND p.name = ? " : "") +
               "GROUP BY e.category";

        try (Connection conn = Database.connect();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            int i = 3;
            if (isFiltered(category)) ps.setString(i++, category);
            if (isFiltered(project)) ps.setString(i, project);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String cat = rs.getString("category");
                    double total = rs.getDouble("total");
                    chart.getData().add(new PieChart.Data(cat, total));
                }
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Hiba a kiadások diagramhoz", e);
        }
    }

    // Segédfüggvények
    private static boolean isFiltered(String value) {
        return value != null && !value.equals("Összes kategória") && !value.equals("Összes projekt");
    }

    private static String getMonthName(int month) {
        return java.time.Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault());
    }
}