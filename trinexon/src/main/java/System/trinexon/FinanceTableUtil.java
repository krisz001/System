package System.trinexon;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class FinanceTableUtil {

    /**
     * Rekurzívan kigyűjti az összes oszlop fejlécét, beleértve a nested column-okat is.
     */
    public static List<String> getColumnHeaders(TableView<?> table) {
        if (table == null || table.getColumns() == null) {
            return Collections.emptyList();
        }
        List<String> headers = new ArrayList<>();
        collectHeaders(table.getColumns(), headers);
        return headers;
    }

    private static void collectHeaders(List<? extends TableColumn<?, ?>> cols, List<String> out) {
        if (cols == null || out == null) return;
        for (TableColumn<?, ?> col : cols) {
            out.add(col.getText());
            if (col.getColumns() != null && !col.getColumns().isEmpty()) {
                collectHeaders(col.getColumns(), out);
            }
        }
    }

    /**
     * Kigyűjti az összes sort és cellaértéket, alapból a toString() alapján.
     */
    public static List<List<String>> getTableData(TableView<?> table) {
        return getTableData(table, obj -> obj == null ? "" : obj.toString());
    }

    /**
     * Kigyűjti az összes sort és cellaértéket, a megadott converter alapján.
     *
     * @param table     a TableView
     * @param converter a cellaobjektum -> String konverter
     */
    public static List<List<String>> getTableData(TableView<?> table,
                                                  Function<Object, String> converter) {
        if (table == null || table.getColumns() == null || table.getItems() == null) {
            return Collections.emptyList();
        }
        Objects.requireNonNull(converter, "Converter must not be null");
        List<List<String>> rows = new ArrayList<>();
        for (Object item : table.getItems()) {
            List<String> row = new ArrayList<>();
            collectRowData(item, table.getColumns(), row, converter);
            rows.add(row);
        }
        return rows;
    }

    private static void collectRowData(Object item,
                                       List<? extends TableColumn<?, ?>> cols,
                                       List<String> out,
                                       Function<Object, String> converter) {
        if (cols == null || out == null) return;
        for (TableColumn<?, ?> rawCol : cols) {
            @SuppressWarnings("unchecked")
            TableColumn<Object, Object> col = (TableColumn<Object, Object>) rawCol;
            if (col.getColumns() == null || col.getColumns().isEmpty()) {
                // valódi cella-oszlop
                ObservableValue<?> obs = col.getCellObservableValue(item);
                Object value = obs == null ? null : obs.getValue();
                out.add(converter.apply(value));
            } else {
                // beágyazott aloszlopok
                collectRowData(item, col.getColumns(), out, converter);
            }
        }
    }
}