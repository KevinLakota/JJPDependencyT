package Service.External;

import java.io.*;
import java.nio.file.*;

public class PythonScriptExtractor {

    private static final String SCRIPT_NAME = "cvss_calc.py";
    private static final String TARGET_DIR = System.getProperty("java.io.tmpdir");

    public static void extractIfNeeded() throws IOException {
        Path targetPath = Paths.get(TARGET_DIR, SCRIPT_NAME);

        if (Files.exists(targetPath)) {
            return; // Skript už existuje, nič netreba robiť
        }

        try (InputStream in = PythonScriptExtractor.class.getResourceAsStream("/Service/External/" + SCRIPT_NAME)) {
            if (in == null) {
                throw new FileNotFoundException("Python skript cvss_calc.py sa nenašiel v JARe.");
            }
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Extrahovaný Python skript do: " + targetPath.toAbsolutePath());
        }
    }

    public static String getExtractedScriptPath() {
        return Paths.get(TARGET_DIR, SCRIPT_NAME).toAbsolutePath().toString();
    }
}
