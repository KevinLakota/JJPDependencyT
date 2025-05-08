package Service.External;

import Model.Vulnerability;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class OsvClient {

    private static final String API_URL = "https://api.osv.dev/v1/query";

    public static List<Vulnerability> fetchVulnerabilities(String ecosystemInput, String name, String version) {
        List<Vulnerability> vulnList = new ArrayList<>();

        try {
            String osvEcosystem = mapEcosystem(ecosystemInput);

            String jsonPayload = String.format("""
                {
                    "package": {
                        "name": "%s",
                        "ecosystem": "%s"
                    },
                    "version": "%s"
                }
                """, name, osvEcosystem, version);

            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.err.println("Chyba API volania: " + responseCode);
                try (Scanner errScanner = new Scanner(conn.getErrorStream()).useDelimiter("\\A")) {
                    System.err.println("Odpoveď servera: " + (errScanner.hasNext() ? errScanner.next() : ""));
                }
                return vulnList;
            }

            Scanner scanner = new Scanner(conn.getInputStream()).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);
            JsonNode vulns = root.path("vulns");

            System.out.println("Zraniteľnosti pre " + name + "@" + version + " (" + osvEcosystem + "):");

            if (vulns.isEmpty()) {
                System.out.println("  Žiadne zraniteľnosti nenájdené.");
            } else {
                for (JsonNode vuln : vulns) {
                    String id = vuln.path("id").asText();
                    String summary = vuln.path("summary").asText();
                    String firstDetail = vuln.has("details") ? vuln.get("details").asText().split("\\n")[0] : "";
                    String severity = "";

                    if (vuln.has("severity")) {
                        for (JsonNode sev : vuln.path("severity")) {
                            String score = sev.path("score").asText();
                            severity += (severity.isEmpty() ? "" : ", ") + score;
                        }
                    }

                    vulnList.add(new Vulnerability(
                            name,
                            version,
                            "OSV",
                            id,
                            severity.isEmpty() ? "unknown" : severity,
                            firstDetail
                    ));

                    System.out.println("  - [" + id + "] " + summary);
                    if (!firstDetail.isEmpty()) System.out.println("    → " + firstDetail);
                    if (!severity.isEmpty()) System.out.println("    CVSS: " + severity);
                }
            }

        } catch (Exception e) {
            System.err.println("Chyba pri volaní OSV API: " + e.getMessage());
        }

        return vulnList;
    }

    private static String mapEcosystem(String input) {
        return switch (input.toLowerCase()) {
            case "npm", "javascript"      -> "npm";
            case "pypi", "python"         -> "PyPI";
            case "maven", "java"          -> "Maven";
            case "go"                     -> "Go";
            case "nuget"                  -> "NuGet";
            case "cargo", "crates"        -> "crates.io";
            case "rubygems", "ruby"       -> "RubyGems";
            case "composer", "php"        -> "Packagist";
            default -> throw new IllegalArgumentException("Nepodporovaný ekosystém: " + input);
        };
    }
}
