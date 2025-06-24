package System.trinexon;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SalaryController {

    @FXML private ComboBox<Employee> employeeCombo;
    @FXML private TextField salaryField, monthField;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        loadEmployees();  // 🔄 Dolgozók betöltése
    }

    private void loadEmployees() {
        try {
            String json = HttpUtil.get("http://localhost:5000/api/employees");

            if (json == null || json.startsWith("Hiba") || !json.trim().startsWith("[")) {
                throw new RuntimeException("Nem JSON tömb: " + json);
            }

            JSONArray jsonArray = new JSONArray(json);
            List<Employee> employees = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE; // "yyyy-MM-dd"

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);

                LocalDate hireDate = LocalDate.parse(obj.getString("hire_date"), formatter);

                Employee emp = new Employee(
                        obj.getInt("id"),
                        obj.getString("name"),
                        obj.getString("position"),
                        obj.getString("department"),
                        hireDate,
                        obj.getString("status"),
                        obj.optString("email", ""),
                        obj.optString("phone", "")
                );
                employees.add(emp);
            }

            employeeCombo.setItems(FXCollections.observableArrayList(employees));

        } catch (Exception e) {
            e.printStackTrace();
            if (statusLabel != null) {
                statusLabel.setText("❌ Hiba dolgozók betöltésénél.");
            }
        }
    }

    @FXML
    private void onSaveSalary() {
        Employee emp = employeeCombo.getValue();
        String salary = salaryField.getText().trim();
        String month = monthField.getText().trim();

        if (emp == null || salary.isEmpty() || month.isEmpty()) {
            statusLabel.setText("❗ Minden mezőt ki kell tölteni.");
            return;
        }

        try {
            double parsedSalary = Double.parseDouble(salary);
            if (parsedSalary <= 0) throw new NumberFormatException();

            String json = new JSONObject()
                    .put("employee_id", emp.getId())
                    .put("salary", parsedSalary)
                    .put("month", month)
                    .toString();

            String response = HttpUtil.post("http://localhost:5000/api/salary", json);
            statusLabel.setText(response.contains("Sikeres") ? "✅ Mentés sikeres" : "❌ Hiba: " + response);

        } catch (NumberFormatException e) {
            statusLabel.setText("⚠️ Hibás fizetés érték.");
        }
    }

    @FXML
    private void onGetRecommendation() {
        Employee emp = employeeCombo.getValue();
        if (emp == null) {
            statusLabel.setText("❗ Válassz ki egy dolgozót.");
            return;
        }

        try {
            String url = "http://localhost:5000/api/salary/recommend?department=" + emp.getDepartment();
            System.out.println("🔎 Lekérdezés URL: " + url);  // 🔍 LOG

            String response = HttpUtil.get(url);
            System.out.println("📥 AI válasz: " + response);  // 🔍 LOG

            if (response == null || !response.trim().startsWith("{")) {
                statusLabel.setText("❌ Hibás válasz az AI-tól: " + response);
                System.out.println("⚠️ Válasz nem JSON objektum: " + response);
                return;
            }

            JSONObject json = new JSONObject(response);
            if (json.has("recommended_salary")) {
                salaryField.setText(String.valueOf(json.getDouble("recommended_salary")));
                statusLabel.setText("🤖 AI javaslat beillesztve.");
            } else {
                statusLabel.setText("⚠️ Nincs AI adat ehhez az osztályhoz.");
            }

        } catch (Exception e) {
            statusLabel.setText("❌ Hiba AI javaslat lekérdezésekor.");
            e.printStackTrace();
        }
    }
}