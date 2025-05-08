package Service.Repository;

import Model.Project;

import java.sql.*;

public class ProjectRepository {

    public static Project findByName(String name) {
        String sql = "SELECT * FROM projects WHERE name = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Project(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("path"),
                        rs.getString("language")
                );
            }

        } catch (SQLException e) {
            System.err.println("Chyba pri hľadaní projektu: " + e.getMessage());
        }

        return null;
    }

    public static Project createOrUpdate(String name, String path, String language) {
        Project existing = findByName(name);
        if (existing != null) {
            // Môžeš aktualizovať cestu alebo jazyk, ak chceš
            return existing;
        }

        String sql = "INSERT INTO projects (name, path, language) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, name);
            stmt.setString(2, path);
            stmt.setString(3, language);
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                int id = keys.getInt(1);
                return new Project(id, name, path, language);
            }

        } catch (SQLException e) {
            System.err.println("Chyba pri vytváraní projektu: " + e.getMessage());
        }

        return null;
    }
    public static Project findById(int id) {
        String sql = "SELECT * FROM projects WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Project(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("path"),
                        rs.getString("language")
                );
            }

        } catch (SQLException e) {
            System.err.println("Chyba pri hľadaní projektu podľa ID: " + e.getMessage());
        }

        return null;
    }

}
