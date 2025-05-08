package Utillity.forJavaScript;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.*;
import java.util.stream.Collectors;

public class JsTransitiveDependencyFetcher {

    public static void prepareDependencyTrees(String projectPath) {
        String npmPath = "C:\\Program Files\\nodejs\\npm.cmd"; // uprav podľa potreby
        List<File> packageJsonFiles = new ArrayList<>();
        JsDependencyExtractor.findPackageJsonFiles(new File(projectPath), packageJsonFiles);

        // ⇩ Sem pridaj výpis
        System.out.println("\nSpracované súbory package.json:");
        packageJsonFiles.forEach(f -> System.out.println("- " + f.getAbsolutePath()));

        for (File pkgJson : packageJsonFiles) {
            File dir = pkgJson.getParentFile();
            File nodeModules = new File(dir, "node_modules");
            File lockFile = new File(dir, "package-lock.json");

            if (!lockFile.exists() || !nodeModules.exists()) {
                try {
                    System.out.println("Spúšťam `npm install` v: " + dir.getAbsolutePath());
                    ProcessBuilder installPB = new ProcessBuilder(npmPath, "install", "--ignore-scripts");
                    installPB.directory(dir);
                    installPB.inheritIO();
                    Process installProcess = installPB.start();
                    int code = installProcess.waitFor();

                    if (code != 0) {
                        System.out.println("Zlyhalo `npm install` v: " + dir.getAbsolutePath());
                    }
                } catch (Exception e) {
                    System.out.println("Chyba pri spustení `npm install` v: " + dir.getAbsolutePath() + " → " + e.getMessage());
                }
            } else {
                System.out.println("Preskakujem `npm install` v: " + dir.getAbsolutePath() + " (už existuje node_modules a package-lock.json)");
            }
        }
    }


    public static Map<String, List<String>> buildDependencyGraph(String projectPath) {
        Map<String, List<String>> graph = new HashMap<>();
        String npmPath = "C:\\Program Files\\nodejs\\npm.cmd";
        List<File> packageJsonFiles = new ArrayList<>();
        JsDependencyExtractor.findPackageJsonFiles(new File(projectPath), packageJsonFiles);

        for (File pkgJson : packageJsonFiles) {
            File dir = pkgJson.getParentFile();
            System.out.println("Spracovávam package-lock.json v: " + dir.getAbsolutePath());
            try {
                ProcessBuilder pb = new ProcessBuilder(npmPath, "ls", "--all", "--json");
                pb.directory(dir);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String output = reader.lines().collect(Collectors.joining());
                int exitCode = process.waitFor();

                if (exitCode != 0 || output.isBlank()) {
                    System.out.println("Zlyhalo `npm ls` v: " + dir.getAbsolutePath());
                    continue;
                }

                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(output);

                String key = dir.getName();
                collectDependencies(key, root.path("dependencies"), graph);

            } catch (Exception e) {
                System.out.println("Chyba pri spustení `npm ls` v: " + dir.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        return graph;
    }

    private static void collectDependencies(String parent, JsonNode depsNode, Map<String, List<String>> graph) {
        if (depsNode == null || !depsNode.isObject()) return;

        for (Iterator<String> it = depsNode.fieldNames(); it.hasNext(); ) {
            String depName = it.next();
            JsonNode depNode = depsNode.get(depName);

            String version = depNode.has("version") ? depNode.get("version").asText() : "unknown";
            String fullDep = depName + "@" + version;

            // Pridaj iba ak parent nie je null – tým vynecháš koreňový balík
            if (parent != null) {
                graph.computeIfAbsent(parent, k -> new ArrayList<>()).add(fullDep);
            }

            // Rekurzívne pre ďalšie závislosti
            collectDependencies(fullDep, depNode.path("dependencies"), graph);
        }
    }

}
