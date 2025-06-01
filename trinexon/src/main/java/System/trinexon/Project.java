package System.trinexon;

import javafx.beans.property.*;

import java.time.LocalDate;

public class Project {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final ObjectProperty<LocalDate> startDate = new SimpleObjectProperty<>();
    private final ObjectProperty<LocalDate> endDate = new SimpleObjectProperty<>();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty manager = new SimpleStringProperty();
    private final DoubleProperty budget = new SimpleDoubleProperty();

    public Project(int id, String name, String description, LocalDate startDate, LocalDate endDate, String status, String manager, double budget) {
        this.id.set(id);
        this.name.set(name);
        this.description.set(description);
        this.startDate.set(startDate);
        this.endDate.set(endDate);
        this.status.set(status);
        this.manager.set(manager);
        this.budget.set(budget);
    }

    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getDescription() { return description.get(); }
    public LocalDate getStartDate() { return startDate.get(); }
    public LocalDate getEndDate() { return endDate.get(); }
    public String getStatus() { return status.get(); }
    public String getManager() { return manager.get(); }
    public double getBudget() { return budget.get(); }

    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
    public ObjectProperty<LocalDate> endDateProperty() { return endDate; }
    public StringProperty statusProperty() { return status; }
    public StringProperty managerProperty() { return manager; }
    public DoubleProperty budgetProperty() { return budget; }
}
