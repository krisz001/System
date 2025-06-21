package System.trinexon;

public class FinanceRecord {
    private final String project;
    private final String type;
    private final String category;
    private final double amount;

    public FinanceRecord(String project, String type, String category, double amount) {
        this.project = project;
        this.type = type;
        this.category = category;
        this.amount = amount;
    }

    public String getProject() {
        return project;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }
}