package Utillity.forJava;

import java.io.*;
import java.util.*;

public class TransitiveDependencyFetcher {

    public static List<String> fetchDependencies(String projectDir) {
        ensureMavenWrapper(projectDir); // pridané

        List<String> dependencies = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c", "mvnw.cmd", "dependency:tree",
                    "-DoutputFile=target/deps.txt"
            );
            pb.directory(new File(projectDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (reader.readLine() != null); // ignoruj konzolový výstup
            process.waitFor();

            File depsFile = new File(projectDir + "/target/deps.txt");
            if (depsFile.exists()) {
                BufferedReader depsReader = new BufferedReader(new FileReader(depsFile));
                String line;
                boolean skippedRoot = false;

                while ((line = depsReader.readLine()) != null) {
                    line = line.trim();

                    if (line.matches("^[+\\\\\\-\\| ]*[^\\s]+:[^\\s]+:jar:[^\\s]+(:[a-z]+)?$")) {
                        // Odstráň prefixy ako "+- ", "|  ", atď.
                        String cleaned = line.replaceAll("^[+\\\\\\-\\| ]+", "");

                        // Prvý riadok je root projekt, preskočíme ho
                        if (!skippedRoot) {
                            skippedRoot = true;
                            continue;
                        }

                        dependencies.add(cleaned);
                    }
                }
                depsReader.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dependencies;
    }

    private static void ensureMavenWrapper(String projectDirPath) {
        File projectDir = new File(projectDirPath);
        File mvnw = new File(projectDir, "mvnw.cmd");
        File mvnDir = new File(projectDir, ".mvn");

        if (!mvnw.exists() || !mvnDir.exists()) {
            System.out.println("Maven Wrapper nebol nájdený – pokúšam sa ho vytvoriť...");

            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "mvn", "-N", "io.takari:maven:wrapper");
            pb.directory(projectDir);
            pb.inheritIO(); // výstup do konzoly

            try {
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    System.out.println("Maven Wrapper bol úspešne vytvorený.");
                } else {
                    System.err.println("Nepodarilo sa vytvoriť Maven Wrapper. Skontroluj, či máš nainštalovaný Maven.");
                }
            } catch (Exception e) {
                System.err.println("Chyba pri vytváraní Maven Wrappera: " + e.getMessage());
            }
        } else {
            System.out.println("Maven Wrapper už existuje – pokračujem.");
        }
    }
}
