package Utillity.forJavaScript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class JsDependencyExtractor {

    public static Map<String, String> extractDependencies(String projectPath) {
        Map<String, String> deps = new HashMap<>();
        List<File> packageJsonFiles = new ArrayList<>();
        findPackageJsonFiles(new File(projectPath), packageJsonFiles);

        for (File pkgJson : packageJsonFiles) {
            //System.out.println("\nNačítavam závislosti z: " + pkgJson.getPath());

            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(pkgJson);

                JsonNode dependencies = root.path("dependencies");
                JsonNode devDependencies = root.path("devDependencies");

                int before = deps.size();
                addDepsFromNode("dependencies", dependencies, deps);
                addDepsFromNode("devDependencies", devDependencies, deps);
                int added = deps.size() - before;

                //System.out.println(" Pridané: " + added + " závislostí");

            } catch (IOException e) {
                System.out.println(" Chyba pri čítaní " + pkgJson + ": " + e.getMessage());
            }
        }

        System.out.println("\n Celkový počet unikátnych závislostí: " + deps.size());
        return deps;
    }

    public static void findPackageJsonFiles(File dir, List<File> results) {
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                // Preskoč priečinky, ktoré nemajú byť skenované
                String name = f.getName();
                if (name.equals("node_modules") || name.startsWith(".")) continue;

                findPackageJsonFiles(f, results);
            } else if (f.getName().equals("package.json")) {
                File parent = f.getParentFile();
                // Extra ochrana: ak je to vo vnútri node_modules, preskoč
                if (parent.getAbsolutePath().contains(File.separator + "node_modules")) continue;

                results.add(f);
            }
        }
    }



    private static void addDepsFromNode(String label, JsonNode node, Map<String, String> deps) {
        if (!node.isMissingNode()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                deps.put(entry.getKey(), entry.getValue().asText());
                //System.out.printf("- [%s] %s : %s%n", label, entry.getKey(), entry.getValue().asText());
            }
        }
    }
}
