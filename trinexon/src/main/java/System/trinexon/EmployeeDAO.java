package System.trinexon;

import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EmployeeDAO {

    public static List<Employee> findAll() {
        List<Employee> list = new ArrayList<>();
        System.out.println("➡️ EmployeeDAO.findAll() elindult");

        Database.executeQuery("SELECT * FROM employees", rs -> {
            while (rs.next()) {
                System.out.println("👤 Betöltött dolgozó: " + rs.getString("name"));
                Employee emp = new Employee(
                    rs.getInt("id"),
                    rs.getString("name"),
                    rs.getString("position"),
                    rs.getString("department"),
                    rs.getDate("hire_date") != null ? rs.getDate("hire_date").toLocalDate() : null,
                    rs.getString("status"),
                    rs.getString("email"),
                    rs.getString("phone")
                );
                list.add(emp);
            }
        });

        return list;
    }

    public static boolean insertEmployee(Employee emp) {
        String sql = String.format("""
            INSERT INTO employees (name, position, department, hire_date, status, email, phone)
            VALUES ('%s', '%s', '%s', '%s', '%s', '%s', '%s')
        """, emp.getName(), emp.getPosition(), emp.getDepartment(),
             emp.getHireDate(), emp.getStatus(), emp.getEmail(), emp.getPhone());

        System.out.println("🟢 SQL beszúrás: " + sql);
        return Database.executeUpdate(sql) > 0;
    }

    public static boolean updateEmployee(Employee emp) {
        String sql = String.format("""
            UPDATE employees SET 
                name = '%s',
                position = '%s',
                department = '%s',
                hire_date = '%s',
                status = '%s',
                email = '%s',
                phone = '%s'
            WHERE id = %d
        """, emp.getName(), emp.getPosition(), emp.getDepartment(),
             emp.getHireDate(), emp.getStatus(), emp.getEmail(), emp.getPhone(),
             emp.getId());

        System.out.println("🟡 SQL frissítés: " + sql);
        return Database.executeUpdate(sql) > 0;
    }

    public static boolean deleteEmployeeSecure(int id) {
        String sql = "DELETE FROM employees WHERE id = ?";
        try (Connection conn = Database.connect();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;

        } catch (Exception e) {
            System.err.println("❌ Törlés hiba: " + e.getMessage());
            return false;
        }
    }
}