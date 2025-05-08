package Service;

import Model.Dependency;
import Model.DependencyRiskProfile;
import Model.Vulnerability;

import java.util.List;

public class DependencyRiskProfileBuilder {

    public static DependencyRiskProfile build(Dependency dep, List<Vulnerability> vulnerabilities, long frequency) {
        int criticalCount = 0;
        int highCount = 0;
        int mediumCount = 0;
        double highestCriticalScore = 0.0;
        double highestHighScore = 0.0;
        double highestMediumScore = 0.0;

        for (Vulnerability vuln : vulnerabilities) {
            String severity = vuln.getSeverity();
            if (severity == null || severity.isBlank()) continue;

            String[] vectors = severity.split(",");
            for (String raw : vectors) {
                raw = raw.trim();
                if (raw.isEmpty()) continue;

                double score = 0.0;
                try {
                    score = Double.parseDouble(Service.External.CvssScoreCalculator.calculateScore(raw));
                } catch (Exception e) {
                    // Ak je problém pri výpočte skóre, preskočíme
                    continue;
                }

                if (score >= 9.0) {
                    criticalCount++;
                    if (score > highestCriticalScore) {
                        highestCriticalScore = score;
                    }
                } else if (score >= 7.0) {
                    highCount++;
                    if (score > highestHighScore) {
                        highestHighScore = score;
                    }
                } else if (score >= 4.0) {
                    mediumCount++;
                    if (score > highestMediumScore) {
                        highestMediumScore = score;
                    }
                }
                // Low zraniteľnosti ignorujeme
            }
        }

        return new DependencyRiskProfile(
                dep.getName(),
                dep.getVersion(),
                criticalCount,
                highCount,
                mediumCount,
                highestCriticalScore,
                highestHighScore,
                highestMediumScore,
                frequency
        );
    }
}
