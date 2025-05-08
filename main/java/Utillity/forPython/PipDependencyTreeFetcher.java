package Utillity.forPython;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

public class PipDependencyTreeFetcher {

    public static Set<String> parseDependencyTree(String projectPath) {
        Set<String> dependencies = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();

        File jsonFile = Paths.get(projectPath, "dependencies.json").toFile();

        if (!jsonFile.exists()) {
            System.err.println("Súbor dependencies.json neexistuje.");
            return dependencies;
        }

        try {
            JsonNode root = mapper.readTree(jsonFile);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    extractDependenciesRecursively(node, dependencies);
                }
            } else {
                System.err.println("Očakával som pole v JSON výstupe pipdeptree.");
            }

        } catch (IOException e) {
            System.err.println("Chyba pri parsovaní dependencies.json: " + e.getMessage());
        }

        return dependencies;
    }

    private static void extractDependenciesRecursively(JsonNode node, Set<String> deps) {
        if (node.has("package_name")) {
            String name = node.get("package_name").asText();
            String version = node.has("installed_version") ? node.get("installed_version").asText() : "unknown";
            deps.add(name + "==" + version);
        }

        if (node.has("dependencies")) {
            for (JsonNode child : node.get("dependencies")) {
                extractDependenciesRecursively(child, deps);
            }
        }
    }

    public static Map<String, Integer> getDependencyFrequencyMap(String projectPath) {
        Map<String, Integer> frequencyMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();

        File jsonFile = Paths.get(projectPath, "dependencies.json").toFile();

        if (!jsonFile.exists()) {
            System.err.println("Súbor dependencies.json neexistuje.");
            return frequencyMap;
        }

        try {
            JsonNode root = mapper.readTree(jsonFile);

            if (root.isArray()) {
                for (JsonNode node : root) {
                    countDependenciesRecursively(node, frequencyMap);
                }
            } else {
                System.err.println("Očakával som pole v JSON výstupe pipdeptree.");
            }

        } catch (IOException e) {
            System.err.println("Chyba pri parsovaní dependencies.json: " + e.getMessage());
        }

        return frequencyMap;
    }

    private static void countDependenciesRecursively(JsonNode node, Map<String, Integer> freqMap) {
        if (node.has("package_name")) {
            String name = node.get("package_name").asText();
            freqMap.put(name, freqMap.getOrDefault(name, 0) + 1);
        }

        if (node.has("dependencies")) {
            for (JsonNode child : node.get("dependencies")) {
                countDependenciesRecursively(child, freqMap);
            }
        }
    }

    public static void printAllDependencyEdges(String projectPath) {
        File jsonFile = Paths.get(projectPath, "dependencies.json").toFile();
        if (!jsonFile.exists()) {
            System.err.println("dependencies.json neexistuje.");
            return;
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(jsonFile);
            if (!root.isArray()) {
                System.err.println("Očakával som pole v JSON výstupe pipdeptree.");
                return;
            }

            System.out.println("\n--- Vzťahy závislostí (A -> B) ---");

            for (JsonNode node : root) {
                printEdgesRecursively(node);
            }

        } catch (IOException e) {
            System.err.println("Chyba pri čítaní dependencies.json: " + e.getMessage());
        }
    }

    private static void printEdgesRecursively(JsonNode node) {
        if (!node.has("package_name") || !node.has("installed_version")) return;

        String parent = node.get("package_name").asText();
        String parentVersion = node.get("installed_version").asText();
        String parentFull = parent + "==" + parentVersion;

        if (node.has("dependencies")) {
            for (JsonNode child : node.get("dependencies")) {
                if (child.has("package_name") && child.has("installed_version")) {
                    String childName = child.get("package_name").asText();
                    String childVersion = child.get("installed_version").asText();
                    String childFull = childName + "==" + childVersion;

                    System.out.println(parentFull + " -> " + childFull);
                    printEdgesRecursively(child);
                }
            }
        }
    }
    public static Map<String, List<String>> buildDependencyGraph(String projectPath) {
        Map<String, List<String>> graph = new HashMap<>();
        File jsonFile = Paths.get(projectPath, "dependencies.json").toFile();

        if (!jsonFile.exists()) {
            System.err.println("dependencies.json neexistuje.");
            return graph;
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(jsonFile);
            if (!root.isArray()) {
                System.err.println("Očakával som pole v JSON výstupe pipdeptree.");
                return graph;
            }

            for (JsonNode node : root) {
                buildEdgesRecursively(node, graph);
            }

        } catch (IOException e) {
            System.err.println("Chyba pri čítaní dependencies.json: " + e.getMessage());
        }

        return graph;
    }

    private static void buildEdgesRecursively(JsonNode node, Map<String, List<String>> graph) {
        if (!node.has("package_name") || !node.has("installed_version")) return;

        String parent = node.get("package_name").asText();
        String parentVersion = node.get("installed_version").asText();
        String parentFull = parent + "==" + parentVersion;

        if (node.has("dependencies")) {
            for (JsonNode child : node.get("dependencies")) {
                if (child.has("package_name") && child.has("installed_version")) {
                    String childName = child.get("package_name").asText();
                    String childVersion = child.get("installed_version").asText();
                    String childFull = childName + "==" + childVersion;

                    graph.computeIfAbsent(parentFull, k -> new ArrayList<>()).add(childFull);
                    buildEdgesRecursively(child, graph);
                }
            }
        }
    }

}
