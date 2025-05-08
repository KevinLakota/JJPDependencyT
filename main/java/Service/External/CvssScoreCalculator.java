package Service.External;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class CvssScoreCalculator {

    public static String calculateScore(String cvssVector) {
        try {
            PythonScriptExtractor.extractIfNeeded();
            String scriptPath = PythonScriptExtractor.getExtractedScriptPath();

            ProcessBuilder pb = new ProcessBuilder("python", scriptPath, cvssVector);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String result = reader.readLine();

            process.waitFor();

            if (result == null || result.contains("Error")) {
                return result;
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return "N/A";
        }
    }

}
