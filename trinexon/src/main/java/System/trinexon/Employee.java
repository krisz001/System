package System.trinexon;

import java.time.LocalDate;

public class Employee {
    private int id;
    private String name;
    private String position;
    private String department;
    private LocalDate hireDate;
    private String status;
    private String email;
    private String phone;

    public Employee(int id, String name, String position, String department,
                    LocalDate hireDate, String status, String email, String phone) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.department = department;
        this.hireDate = hireDate;
        this.status = status;
        this.email = email;
        this.phone = phone;
    }

    // --- Getters ---
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPosition() {
        return position;
    }

    public String getDepartment() {
        return department;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public String getStatus() {
        return status;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    // --- Setters ---
    public void setName(String name) {
        this.name = name;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    // --- toString (ComboBox-hoz hasznos) ---
    @Override
    public String toString() {
        return name + " (" + position + ")";
    }
}