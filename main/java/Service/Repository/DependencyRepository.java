package Service.Repository;

import Model.Dependency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.sql.ResultSet;

public class DependencyRepository {

    public static void replaceForProject(int projectId, List<Dependency> dependencies) {
        String deleteSql = "DELETE FROM dependencies WHERE project_id = ?";
        String insertSql = "INSERT INTO dependencies (project_id, name, version, language, scope) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // dávkové spracovanie

            // Zmaž staré závislosti
            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                deleteStmt.setInt(1, projectId);
                deleteStmt.executeUpdate();
            }

            // Vlož nové
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                for (Dependency dep : dependencies) {
                    insertStmt.setInt(1, projectId);
                    insertStmt.setString(2, dep.getName());
                    insertStmt.setString(3, dep.getVersion());
                    insertStmt.setString(4, dep.getLanguage());
                    insertStmt.setString(5, dep.getScope());
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            conn.commit();
            System.out.println("Závislosti uložené pre projekt ID " + projectId);
            System.out.println("Počet nových závislostí na uloženie: " + dependencies.size());

        } catch (SQLException e) {
            System.err.println("Chyba pri ukladaní závislostí: " + e.getMessage());
        }
    }

    public static List<Dependency> getForProject(int projectId) {
        List<Dependency> result = new ArrayList<>();
        String sql = "SELECT name, version, language, scope FROM dependencies WHERE project_id = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, projectId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                result.add(new Dependency(
                        rs.getString("name"),
                        rs.getString("version"),
                        rs.getString("language"),
                        rs.getString("scope")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Chyba pri načítaní závislostí: " + e.getMessage());
        }

        return result;
    }

}
