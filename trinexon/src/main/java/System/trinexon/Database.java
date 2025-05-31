package System.trinexon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

public class Database {

    private static final String URL = "jdbc:mysql://localhost:3306/trinexon";
    private static final String USER = "root";
    private static final String PASSWORD = "KrisztiaN12";

    static {
        try {
            // Régebbi JDBC verzióknál kellhet a driver regisztráció
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL driver nem található: " + e.getMessage());
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * Lekérdezés futtatása, és a ResultSet feldolgozása a processor lambda segítségével.
     * A kapcsolat, statement és resultset automatikusan bezáródik.
     *
     * @param query SQL SELECT lekérdezés
     * @param processor A ResultSet-et feldolgozó funkció
     */
    public static void executeQuery(String query, ResultSetProcessor processor) {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            processor.process(rs);

        } catch (SQLException e) {
            System.err.println("Hiba a lekérdezés futtatása során: " + e.getMessage());
        }
    }

    /**
     * Adatmódosító műveletek (INSERT, UPDATE, DELETE) futtatása.
     * Visszaadja az érintett sorok számát.
     *
     * @param sql SQL utasítás
     * @return érintett sorok száma, vagy -1 hiba esetén
     */
    public static int executeUpdate(String sql) {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            return stmt.executeUpdate(sql);

        } catch (SQLException e) {
            System.err.println("Hiba az adatmódosítás során: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Egyszerű lekérdezés konzolra írással, főként teszteléshez.
     *
     * @param query SQL SELECT lekérdezés
     */
    public static void queryDatabase(String query) {
        executeQuery(query, rs -> {
            while (rs.next()) {
                System.out.println("Eredmény: " + rs.getString(1));
            }
        });
    }

    @FunctionalInterface
    public interface ResultSetProcessor {
        void process(ResultSet rs) throws SQLException;
    }
}
