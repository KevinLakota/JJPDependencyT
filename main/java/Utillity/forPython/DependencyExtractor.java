package Utillity.forPython;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

public class DependencyExtractor {

    public static Map<String, String> extractMainDependencies(String projectPath) {
        Map<String, String> deps = new HashMap<>();

        // 1. Klasický requirements.txt
        Path mainReq = Paths.get(projectPath, "requirements.txt");
        if (Files.exists(mainReq)) {
            readRequirementsFile(mainReq, deps);
        }

        // 2. Priečinok requirements/
        Path reqDir = Paths.get(projectPath, "requirements");
        if (Files.exists(reqDir) && Files.isDirectory(reqDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(reqDir, "*.txt")) {
                for (Path file : stream) {
                    readRequirementsFile(file, deps);
                }
            } catch (IOException e) {
                System.out.println("Chyba pri čítaní priečinka requirements/: " + e.getMessage());
            }
        }

        System.out.println("Celkový počet unikátnych závislostí: " + deps.size());
        return deps;
    }

    private static void readRequirementsFile(Path path, Map<String, String> deps) {
        int before = deps.size();

        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#")) continue;

                if (line.contains("==")) {
                    String[] parts = line.split("==");
                    if (parts.length == 2) {
                        deps.put(parts[0].strip(), parts[1].strip());
                    }
                } else {
                    deps.put(line, "unknown");
                }
            }
        } catch (IOException e) {
            System.out.println("Chyba pri čítaní súboru " + path + ": " + e.getMessage());
        }

        int added = deps.size() - before;
    }
}
