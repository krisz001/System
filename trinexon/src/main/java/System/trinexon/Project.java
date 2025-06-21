package System.trinexon;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

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
        setId(id);
        setName(name);
        setDescription(description);
        setStartDate(startDate);
        setEndDate(endDate);
        setStatus(status);
        setManager(manager);
        setBudget(budget);
    }

    // Getterek
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getDescription() { return description.get(); }
    public LocalDate getStartDate() { return startDate.get(); }
    public LocalDate getEndDate() { return endDate.get(); }
    public String getStatus() { return status.get(); }
    public String getManager() { return manager.get(); }
    public double getBudget() { return budget.get(); }

    // Setterek validációval
    public void setId(int id) {
        if (id < 0) throw new IllegalArgumentException("ID nem lehet negatív");
        this.id.set(id);
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Név nem lehet üres");
        this.name.set(name);
    }

    public void setDescription(String description) {
        this.description.set(description != null ? description : "");
    }

    public void setStartDate(LocalDate startDate) {
        if (startDate == null) throw new IllegalArgumentException("Kezdő dátum nem lehet null");
        if (getEndDate() != null && startDate.isAfter(getEndDate()))
            throw new IllegalArgumentException("Kezdő dátum nem lehet későbbi, mint a záró dátum");
        this.startDate.set(startDate);
    }

    public void setEndDate(LocalDate endDate) {
        // Nem dob hibát, ha null — opcionális mezőként kezeljük
        this.endDate.set(endDate);
    }

    public void setStatus(String status) {
        if (status == null || status.isBlank()) throw new IllegalArgumentException("Státusz nem lehet üres");
        this.status.set(status);
    }

    public void setManager(String manager) {
        this.manager.set(manager != null ? manager : "");
    }

    public void setBudget(double budget) {
        if (budget < 0) throw new IllegalArgumentException("Költségvetés nem lehet negatív");
        this.budget.set(budget);
    }

    // Property-k (JavaFX bindinghez)
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty descriptionProperty() { return description; }
    public ObjectProperty<LocalDate> startDateProperty() { return startDate; }
    public ObjectProperty<LocalDate> endDateProperty() { return endDate; }
    public StringProperty statusProperty() { return status; }
    public StringProperty managerProperty() { return manager; }
    public DoubleProperty budgetProperty() { return budget; }

    // Segítő metódusok
    public long getDurationDays() {
        if (getStartDate() == null || getEndDate() == null) return 0;
        return ChronoUnit.DAYS.between(getStartDate(), getEndDate()) + 1;
    }

    public boolean isActive() {
        LocalDate today = LocalDate.now();
        return (getStartDate() != null && !today.isBefore(getStartDate())) &&
               (getEndDate() != null && !today.isAfter(getEndDate()));
    }

    @Override
    public String toString() {
        return "Project{" +
               "id=" + getId() +
               ", name='" + getName() + '\'' +
               ", status='" + getStatus() + '\'' +
               ", manager='" + getManager() + '\'' +
               ", budget=" + getBudget() +
               ", startDate=" + getStartDate() +
               ", endDate=" + getEndDate() +
               '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;
        Project project = (Project) o;
        return getId() == project.getId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
