package Service.Repository;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:identifier.sqlite"; // SQLite súbor
    private static Connection connection;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection()) {
            StringBuilder schemaSql = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(DatabaseManager.class.getClassLoader().getResourceAsStream("schema.sql")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    schemaSql.append(line).append("\n");
                }
            }

            Statement stmt = conn.createStatement();
            stmt.executeUpdate(schemaSql.toString());
            stmt.close();

            System.out.println("Databáza inicializovaná.");
        } catch (Exception e) {
            System.err.println("Chyba pri inicializácii databázy: " + e.getMessage());
        }
    }
}

