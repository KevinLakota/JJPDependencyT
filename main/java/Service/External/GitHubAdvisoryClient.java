package Service.External;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GitHubAdvisoryClient {

    private static final String GITHUB_ADVISORY_API_URL = "https://api.github.com/advisories/";

    public static String getCveIdFromGhsa(String ghsaId) {
        try {
            String url = GITHUB_ADVISORY_API_URL + ghsaId;

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.toString());

            return root.has("cve_id") && !root.get("cve_id").isNull()
                    ? root.get("cve_id").asText()
                    : ghsaId;

        } catch (Exception e) {
            System.err.println("Nepodarilo sa získať CVE pre GHSA ID " + ghsaId + ": " + e.getMessage());
            return ghsaId; // fallback
        }
    }
}
