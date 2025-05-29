package System.trinexon;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

public class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/trinexon"; // Az adatbázis URL-je
    private static final String USER = "root"; // A felhasználó neve
    private static final String PASSWORD = "KrisztiaN12"; // A felhasználó jelszava

    // Kapcsolat létrehozása
    public static Connection connect() {
        try {
            // Csatlakozás a MySQL adatbázishoz
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Sikeres kapcsolódás az adatbázishoz!");
            return conn;
        } catch (SQLException e) {
            System.err.println("Hiba az adatbázishoz való kapcsolódás során: " + e.getMessage());
            return null;
        }
    }

    // Lekérdezés futtatása
    public static void queryDatabase(String query) {
        Connection conn = connect();  // Kapcsolódás létrehozása

        if (conn != null) {
            try (Statement stmt = conn.createStatement()) {
                // SQL lekérdezés futtatása
                ResultSet rs = stmt.executeQuery(query);

                // Eredmények kiíratása
                while (rs.next()) {
                    System.out.println("Eredmény: " + rs.getString(1));  // Kiírjuk az első oszlopot
                }
            } catch (SQLException e) {
                System.err.println("Hiba a lekérdezés futtatása során: " + e.getMessage());
            } finally {
                try {
                    conn.close();  // Kapcsolat lezárása
                } catch (SQLException e) {
                    System.err.println("Hiba a kapcsolat lezárása során: " + e.getMessage());
                }
            }
        }
    }
}
