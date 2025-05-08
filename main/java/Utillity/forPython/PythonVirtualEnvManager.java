package Utillity.forPython;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public class PythonVirtualEnvManager {

    public static boolean setupVirtualEnv(String projectPath) {
        try {
            Path venvDir = Paths.get(projectPath, "venv");

            // 1. Vytvor venv, ak ešte neexistuje
            if (!Files.exists(venvDir)) {
                System.out.println("Vytváram virtuálne prostredie...");
                ProcessBuilder createVenv = new ProcessBuilder("py", "-m", "venv", "venv");
                createVenv.directory(new File(projectPath));
                createVenv.inheritIO();
                Process p = createVenv.start();
                if (p.waitFor() != 0) {
                    System.err.println("Nepodarilo sa vytvoriť venv.");
                    return false;
                }
            }

            // 2. Inštaluj balíčky jednotlivo
            return installPackagesIndividually(projectPath);

        } catch (IOException | InterruptedException e) {
            System.err.println("Chyba počas vytvárania alebo inštalácie venv: " + e.getMessage());
            return false;
        }
    }

    private static boolean installPackagesIndividually(String projectPath) {
        Path venvDir = Paths.get(projectPath, "venv");
        String pythonPath = venvDir.resolve("Scripts").resolve("python.exe").toString();

        // 🔽 DETEKCIA REQUIREMENTS ZDROJA
        Path reqFile;
        Path defaultReq = Paths.get(projectPath, "requirements.txt");
        Path reqFolder = Paths.get(projectPath, "requirements");

        if (Files.exists(defaultReq)) {
            reqFile = defaultReq;
        } else if (Files.exists(reqFolder) && Files.isDirectory(reqFolder)) {
            Path merged = Paths.get(projectPath, "merged_requirements.txt");

            try (BufferedWriter writer = Files.newBufferedWriter(merged)) {
                DirectoryStream<Path> stream = Files.newDirectoryStream(reqFolder, "*.txt");
                for (Path file : stream) {
                    List<String> lines = Files.readAllLines(file);
                    for (String line : lines) {
                        line = line.strip();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            writer.write(line);
                            writer.newLine();
                        }
                    }
                }
            } catch (IOException e) {
                System.err.println("Chyba pri spájaní súborov v requirements/: " + e.getMessage());
                return false;
            }

            reqFile = merged;
        } else {
            System.err.println("Nebolo nájdené requirements.txt ani priečinok requirements/");
            return false;
        }

        // INŠTALÁCIA BALÍKOV ZO ZISTENÉHO SÚBORU
        try {
            List<String> lines = Files.readAllLines(reqFile);
            for (String line : lines) {
                String pkg = line.strip();
                if (pkg.isEmpty() || pkg.startsWith("#")) continue;

                System.out.println("Inštalujem: " + pkg);
                ProcessBuilder pipInstall = new ProcessBuilder(pythonPath, "-m", "pip", "install", pkg);
                pipInstall.directory(new File(projectPath));
                pipInstall.inheritIO();
                Process p = pipInstall.start();
                int code = p.waitFor();

                if (code != 0) {
                    System.out.println("⚠️ Nepodarilo sa nainštalovať: " + pkg + " – preskakujem.");
                }
            }

            // Nakoniec pipdeptree
            System.out.println("Inštalujem: pipdeptree (na analýzu)...");
            ProcessBuilder pipdeptree = new ProcessBuilder(pythonPath, "-m", "pip", "install", "pipdeptree");
            pipdeptree.directory(new File(projectPath));
            pipdeptree.inheritIO();
            Process p = pipdeptree.start();
            if (p.waitFor() != 0) {
                System.err.println("⚠️ Nepodarilo sa nainštalovať pipdeptree.");
                return false;
            }

            return true;

        } catch (IOException | InterruptedException e) {
            System.err.println("Chyba počas inštalácie balíkov: " + e.getMessage());
            return false;
        }
    }

    public static boolean runPipdeptreeInVenv(String projectPath) {
        try {
            Path venvDir = Paths.get(projectPath, "venv");
            String pythonPath = venvDir.resolve("Scripts").resolve("python.exe").toString();

            ProcessBuilder pb = new ProcessBuilder(pythonPath, "-m", "pipdeptree", "--json-tree");
            pb.directory(new File(projectPath));
            pb.redirectOutput(new File(projectPath, "dependencies.json"));
            pb.redirectErrorStream(true);
            Process p = pb.start();
            return p.waitFor() == 0;

        } catch (IOException | InterruptedException e) {
            System.err.println("Chyba pri spúšťaní pipdeptree vo venv: " + e.getMessage());
            return false;
        }
    }
}
