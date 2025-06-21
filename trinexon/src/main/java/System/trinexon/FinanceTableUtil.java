package System.trinexon;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.ArrayList;
import java.util.List;

public class FinanceTableUtil {

    // Fejlécek kigyűjtése
    public static List<String> getColumnHeaders(TableView<?> table) {
        List<String> headers = new ArrayList<>();
        for (TableColumn<?, ?> column : table.getColumns()) {
            headers.add(column.getText());
        }
        return headers;
    }

    // Adatsorok kigyűjtése
    public static List<List<String>> getTableData(TableView<?> table) {
        List<List<String>> rows = new ArrayList<>();

        for (Object item : table.getItems()) {
            List<String> row = new ArrayList<>();
            for (TableColumn<?, ?> col : table.getColumns()) {
                @SuppressWarnings("unchecked")
                TableColumn<Object, ?> column = (TableColumn<Object, ?>) col;
                ObservableValue<?> observable = column.getCellObservableValue(item);
                String value = observable != null && observable.getValue() != null
                        ? observable.getValue().toString() : "";
                row.add(value);
            }
            rows.add(row);
        }

        return rows;
    }
}