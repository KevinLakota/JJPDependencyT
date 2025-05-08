package Utillity.forJava;

import Service.DependencyUtils;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class DependencyTreeMapper {

    public static Map<String, List<String>> buildDependencyGraph(String projectDir) {
        Map<String, List<String>> dependencyMap = new HashMap<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c", "mvnw.cmd", "dependency:tree",
                    "-DoutputType=dot",
                    "-DoutputFile=target/deps.dot"
            );
            pb.directory(new File(projectDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor();

            File dotFile = new File(projectDir + "/target/deps.dot");
            if (!dotFile.exists()) {
                System.out.println("deps.dot nebol nájdený.");
                return dependencyMap;
            }

            dependencyMap = parseDotFile(dotFile.getAbsolutePath());

        } catch (Exception e) {
            System.out.println("Chyba pri mapovaní závislostí: " + e.getMessage());
            e.printStackTrace();
        }

        return dependencyMap;
    }

    public static Map<String, List<String>> parseDotFile(String dotFilePath) {
        Map<String, List<String>> dependencyMap = new HashMap<>();

        try {
            List<String> lines = Files.readAllLines(new File(dotFilePath).toPath());
            for (String line : lines) {
                line = line.trim();
                if (line.matches("^\".+\" -> \".+\"(\\s*;)?$")) {
                    String[] parts = line.replace("\"", "").replace(";", "").split(" -> ");
                    if (parts.length == 2) {
                        String parent = parts[0];
                        String child = parts[1];

                        dependencyMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(child);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Chyba pri čítaní deps.dot: " + e.getMessage());
            e.printStackTrace();
        }

        return dependencyMap;
    }

    public static Map<String, Integer> countDependencyOccurrences(Map<String, List<String>> graph) {
        Map<String, Integer> usageCount = new HashMap<>();

        for (List<String> dependencies : graph.values()) {
            Set<String> uniqueDeps = new HashSet<>(dependencies);
            for (String dep : uniqueDeps) {
                String key = DependencyUtils.stripMavenVersion(dep);
                usageCount.put(key, usageCount.getOrDefault(key, 0) + 1);
            }
        }

        return usageCount;
    }

}
