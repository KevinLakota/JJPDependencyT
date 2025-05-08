package Service.Report;

import Model.Dependency;
import Model.DependencyRiskProfile;
import Model.Vulnerability;
import Model.Project;
import Service.DependencyRiskProfileBuilder;
import Service.Repository.DependencyRepository;
import Service.Repository.VulnerabilityRepository;
import Service.Repository.ProjectRepository;
import Service.External.GitHubAdvisoryClient;
import Service.External.CvssScoreCalculator;
import Utillity.forJava.DependencyTreeMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportJavaService {

    public static void generateHtmlReportForProject(int projectId, File report) {
        List<Dependency> dependencies = DependencyRepository.getForProject(projectId);
        List<Vulnerability> vulnerabilities = VulnerabilityRepository.getForProject(projectId);
        Project project = ProjectRepository.findById(projectId);

        Map<String, List<Vulnerability>> vulnMap = vulnerabilities.stream()
                .collect(Collectors.groupingBy(v -> v.getPackageName() + ":" + v.getPackageVersion()));

        Map<String, Long> frequencyMap = dependencies.stream()
                .collect(Collectors.groupingBy(Dependency::getName, Collectors.counting()));

        StringBuilder html = new StringBuilder();
        html.append("""
    <!DOCTYPE html>
    <html lang="sk">
    <head>
        <meta charset="UTF-8">
        <title>Bezpečnostný Report</title>
        <style>
            body {
                font-family: 'Segoe UI', Tahoma, sans-serif;
                background: #f4f6f7;
                padding: 30px;
                line-height: 1.6;
                color: #2c3e50;
            }
            h1, h2 {
                color: #2c3e50;
            }
            .dependency {
                background: #fff;
                margin-bottom: 25px;
                padding: 15px 20px;
                border-radius: 8px;
                box-shadow: 0 2px 6px rgba(0,0,0,0.05);
            }
            .vulnerable {
                border-left: 6px solid #e74c3c;
                background-color: #fef0f0;
            }
            .safe {
                border-left: 6px solid #27ae60;
                background-color: #f0fef4;
            }
            .vuln {
                background: #fff5f5;
                padding: 10px;
                margin-top: 10px;
                border-left: 4px solid #e74c3c;
                border-radius: 5px;
            }
            .score {
                font-weight: bold;
                margin-top: 5px;
                margin-bottom: 10px;
                padding: 6px 10px;
                border-radius: 5px;
                display: inline-block;
                background-color: #ecf0f1;
            }
            .score-critical {
                color: #b30000;
                background-color: #ffe6e6;
            }
            .score-high {
                color: #e67e22;
                background-color: #fff2e0;
            }
            .score-medium {
                color: #f39c12;
                background-color: #fef9e7;
            }
            .score-low {
                color: #27ae60;
                background-color: #eafaf1;
            }
            a {
                color: #2980b9;
                text-decoration: none;
            }
            a:hover {
                text-decoration: underline;
            }
        </style>
    </head>
    <body>
   \s""");

        String projectName = project != null ? project.getName() : "Neznámy projekt";
        html.append("<h1>Bezpečnostný report projektu: ").append(projectName).append("</h1>");

        // 1. Rozdelenie na vulnerabilné a bezpečné závislosti
        List<Dependency> vulnerableDeps = new ArrayList<>();
        List<Dependency> safeDeps = new ArrayList<>();
        for (Dependency dep : dependencies) {
            String key = dep.getName() + ":" + dep.getVersion();
            if (!vulnMap.getOrDefault(key, new ArrayList<>()).isEmpty()) {
                vulnerableDeps.add(dep);
            } else {
                safeDeps.add(dep);
            }
        }

        // 2. Pre konverziu Dependency -> DependencyRiskProfile
        List<DependencyRiskProfile> riskProfiles = vulnerableDeps.stream()
                .map(dep -> {
                    String key = dep.getName() + ":" + dep.getVersion();
                    List<Vulnerability> vulns = vulnMap.getOrDefault(key, new ArrayList<>());
                    long frequency = frequencyMap.getOrDefault(dep.getName(), 1L);
                    return DependencyRiskProfileBuilder.build(dep, vulns, frequency);
                })
                .collect(Collectors.toList());

        // 3. Zoradenie podla rizikovosti
        riskProfiles.sort(null); // použije compareTo()

        // 4. Vypíšeme zraniteľné závislosti
        html.append("<h2>Zraniteľné závislosti</h2>");
        for (DependencyRiskProfile profile : riskProfiles) {
            String key = profile.getName() + ":" + profile.getVersion();
            List<Vulnerability> vulns = vulnMap.getOrDefault(key, new ArrayList<>());

            html.append("<div class='dependency vulnerable'>")
                    .append("<strong>").append(profile.getName()).append(":</strong> ").append(profile.getVersion())
                    .append(" — <em>Výskyt: ").append(profile.getFrequency()).append("×</em>")
                    .append("<ul>");

            for (Vulnerability v : vulns) {
                String displayedCveId = v.getCveId();
                if (displayedCveId != null && displayedCveId.startsWith("GHSA-")) {
                    displayedCveId = GitHubAdvisoryClient.getCveIdFromGhsa(displayedCveId);
                }

                String severity = v.getSeverity();
                String score31 = "N/A";
                String score40 = "N/A";

                if (severity != null && !severity.isBlank()) {
                    String[] parts = severity.split(",");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.startsWith("CVSS:3.1/")) {
                            score31 = CvssScoreCalculator.calculateScore(part);
                        } else if (part.startsWith("CVSS:4.0/")) {
                            score40 = CvssScoreCalculator.calculateScore(part);
                        }
                    }
                }

                String scoreClass31 = getScoreCssClass(score31);
                String scoreClass40 = getScoreCssClass(score40);
                String osvUrl = "https://osv.dev/vulnerability/" + displayedCveId;

                html.append("<li class='vuln'>")
                        .append("<strong>").append(displayedCveId).append("</strong><br/>");

                if (!"N/A".equals(score31))
                    html.append("<div class='score ").append(scoreClass31).append("'>CVSS 3.1: ").append(score31).append("</div><br/>");
                if (!"N/A".equals(score40))
                    html.append("<div class='score ").append(scoreClass40).append("'>CVSS 4.0: ").append(score40).append("</div><br/>");

                html.append(v.getDescription()).append("<br/>")
                        .append("<a href='").append(osvUrl).append("' target='_blank'>Zobraziť detail v OSV</a>")
                        .append("</li>");
            }
            html.append("</ul></div>");
        }

        // 5. Bezpečné závislosti
        html.append("<h2>Bezpečné závislosti</h2>");
        for (Dependency dep : safeDeps) {
            long frequency = frequencyMap.getOrDefault(dep.getName(), 1L);
            html.append("<div class='dependency safe'>")
                    .append("<strong>").append(dep.getName()).append(":</strong> ")
                    .append(dep.getVersion())
                    .append(" — <em>Výskyt: ").append(frequency).append("×</em><br/>")
                    .append("<em>Žiadne známe zraniteľnosti.</em>")
                    .append("</div>");
        }

        Map<String, List<String>> dependencyGraph = DependencyTreeMapper.buildDependencyGraph(project.getPath());
        generateDependencyGraphHtml(dependencyGraph, report);

        html.append("</body></html>");

        try {
            Files.writeString(Paths.get(report.getAbsolutePath(), "reportJ.html"), html.toString());
        } catch (IOException e) {
            System.err.println("Nepodarilo sa uložiť HTML report: " + e.getMessage());
        }
    }

    private static String getScoreCssClass(String scoreStr) {
        try {
            double score = Double.parseDouble(scoreStr);
            if (score >= 9.0) {
                return "score-critical";
            } else if (score >= 7.0) {
                return "score-high";
            } else if (score >= 4.0) {
                return "score-medium";
            } else {
                return "score-low";
            }
        } catch (Exception e) {
            return ""; // Pre "N/A" a iné nečíselné hodnoty
        }
    }
    public static void generateDependencyGraphHtml(Map<String, List<String>> dependencyGraph, File report) {
        StringBuilder html = new StringBuilder();
        html.append("""
        <!DOCTYPE html>
        <html lang="sk">
        <head>
            <meta charset="UTF-8">
            <title>Graf závislostí a predchodcov</title>
            <style>
                body {
                    font-family: 'Segoe UI', Tahoma, sans-serif;
                    background: #f4f6f7;
                    padding: 30px;
                    line-height: 1.6;
                    color: #2c3e50;
                }
                h1 {
                    color: #2c3e50;
                    margin-bottom: 30px;
                }
                .dependency-card {
                    background: #fff;
                    padding: 20px;
                    border-radius: 8px;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.05);
                    margin-bottom: 25px;
                    transition: background-color 0.5s ease;
                }
                .dependency-card h3 {
                    margin-top: 0;
                    font-size: 1.1em;
                    color: #2c3e50;
                }
                .dependency-relations {
                    display: flex;
                    justify-content: space-between;
                    gap: 40px;
                    margin-top: 15px;
                }
                .dependency-relations div {
                    flex: 1;
                }
                ul {
                    padding-left: 20px;
                    margin: 5px 0;
                }
                li {
                    margin: 3px 0;
                }
                a {
                    color: #2980b9;
                    text-decoration: none;
                }
                a:hover {
                    text-decoration: underline;
                }
                @keyframes highlight {
                    0%   { background-color: #fcf8e3; }
                    100% { background-color: #fff; }
                }
                .highlighted {
                    animation: highlight 2s ease;
                }
            </style>
        </head>
        <body>
            <h1>Graf závislostí a predchodcov</h1>
    """);

        Function<String, String> safeId = s -> s.replaceAll("[^a-zA-Z0-9]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "")
                .toLowerCase();

        Map<String, String> idMap = new HashMap<>();
        for (String node : dependencyGraph.keySet()) {
            idMap.put(node, safeId.apply(node));
        }

        Map<String, List<String>> reverseGraph = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : dependencyGraph.entrySet()) {
            String parent = entry.getKey();
            for (String child : entry.getValue()) {
                reverseGraph.computeIfAbsent(child, k -> new ArrayList<>()).add(parent);
            }
        }

        Set<String> allNodes = new HashSet<>();
        allNodes.addAll(dependencyGraph.keySet());
        allNodes.addAll(reverseGraph.keySet());

        for (String node : allNodes) {
            String nodeId = idMap.getOrDefault(node, safeId.apply(node));
            List<String> dependencies = dependencyGraph.getOrDefault(node, new ArrayList<>());
            List<String> dependents = reverseGraph.getOrDefault(node, new ArrayList<>());

            html.append("<div class='dependency-card' id='").append(nodeId).append("'>")
                    .append("<h3>").append(node).append("</h3>")
                    .append("<div class='dependency-relations'>");

            html.append("<div><strong>závisí od:</strong>");
            if (!dependencies.isEmpty()) {
                html.append("<ul>");
                for (String dep : dependencies) {
                    String depId = idMap.getOrDefault(dep, safeId.apply(dep));
                    html.append("<li><a href='#").append(depId).append("'>")
                            .append(dep).append("</a></li>");
                }
                html.append("</ul>");
            } else {
                html.append("<p>Žiadne závislosti.</p>");
            }
            html.append("</div>");

            html.append("<div><strong>je potrebný pre:</strong>");
            if (!dependents.isEmpty()) {
                html.append("<ul>");
                for (String dep : dependents) {
                    String depId = idMap.getOrDefault(dep, safeId.apply(dep));
                    html.append("<li><a href='#").append(depId).append("'>")
                            .append(dep).append("</a></li>");
                }
                html.append("</ul>");
            } else {
                html.append("<p>Žiadni závislí predchodcovia.</p>");
            }
            html.append("</div></div></div>\n"); // koniec dependency-card
        }

        // JavaScript pre zvýraznenie
        html.append("""
        <script>
            window.addEventListener("DOMContentLoaded", () => {
                const hash = window.location.hash;
                if (hash) {
                    const target = document.querySelector(hash);
                    if (target) {
                        target.classList.add("highlighted");
                        setTimeout(() => target.classList.remove("highlighted"), 2000);
                    }
                }

                document.querySelectorAll("a[href^='#']").forEach(link => {
                    link.addEventListener("click", (e) => {
                        const targetId = e.target.getAttribute("href");
                        const target = document.querySelector(targetId);
                        if (target) {
                            target.classList.add("highlighted");
                            setTimeout(() => target.classList.remove("highlighted"), 2000);
                        }
                    });
                });
            });
        </script>
    """);

        html.append("</body></html>");

        try {
            Files.writeString(Paths.get(report.getAbsolutePath(), "dependency-graph.html"), html.toString());
            System.out.println("HTML report závislostí + predchodcov bol úspešne vygenerovaný.");
        } catch (IOException e) {
            System.err.println("Nepodarilo sa uložiť HTML report: " + e.getMessage());
        }
    }
}
