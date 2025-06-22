package System.trinexon;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

public class FinanceRecord {
    private final StringProperty project   = new SimpleStringProperty(this, "project");
    private final StringProperty type      = new SimpleStringProperty(this, "type");
    private final StringProperty category  = new SimpleStringProperty(this, "category");
    private final DoubleProperty amount    = new SimpleDoubleProperty(this, "amount");

    public FinanceRecord(String project, String type, String category, double amount) {
        this.project.set(project);
        this.type.set(type);
        this.category.set(category);
        this.amount.set(amount);
    }

    public StringProperty projectProperty() {
        return project;
    }
    public String getProject() {
        return project.get();
    }
    public void setProject(String project) {
        this.project.set(project);
    }

    public StringProperty typeProperty() {
        return type;
    }
    public String getType() {
        return type.get();
    }
    public void setType(String type) {
        this.type.set(type);
    }

    public StringProperty categoryProperty() {
        return category;
    }
    public String getCategory() {
        return category.get();
    }
    public void setCategory(String category) {
        this.category.set(category);
    }

    public DoubleProperty amountProperty() {
        return amount;
    }
    public double getAmount() {
        return amount.get();
    }
    public void setAmount(double amount) {
        this.amount.set(amount);
    }
}