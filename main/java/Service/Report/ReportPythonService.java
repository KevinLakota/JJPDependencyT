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
import Utillity.forPython.PipDependencyTreeFetcher;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ReportPythonService {

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
        <title>Bezpečnostný Report pre Python</title>
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
    """);

        String projectName = project != null ? project.getName() : "Neznámy projekt";
        html.append("<h1>Bezpečnostný report pre Python projekt: %s</h1>".formatted(projectName));

        // --- Vytvoríme risk profile a zoradíme podľa rizikovosti ---
        List<DependencyRiskProfile> riskProfiles = dependencies.stream()
                .filter(dep -> {
                    String key = dep.getName() + ":" + dep.getVersion();
                    return vulnMap.containsKey(key);
                })
                .map(dep -> {
                    String key = dep.getName() + ":" + dep.getVersion();
                    List<Vulnerability> vulns = vulnMap.getOrDefault(key, new ArrayList<>());
                    long frequency = frequencyMap.getOrDefault(dep.getName(), 1L);
                    return DependencyRiskProfileBuilder.build(dep, vulns, frequency);
                })
                .sorted()
                .collect(Collectors.toList());

        List<Dependency> safeDeps = dependencies.stream()
                .filter(dep -> {
                    String key = dep.getName() + ":" + dep.getVersion();
                    return !vulnMap.containsKey(key);
                })
                .collect(Collectors.toList());

        // --- Zraniteľné závislosti ---
        html.append("<h2>Zraniteľné závislosti</h2>");
        for (DependencyRiskProfile profile : riskProfiles) {
            String key = profile.getName() + ":" + profile.getVersion();
            List<Vulnerability> vulns = vulnMap.getOrDefault(key, new ArrayList<>());
            long frequency = profile.getFrequency();

            html.append("<div class='dependency vulnerable'>")
                    .append("<strong>").append(profile.getName()).append(":</strong> ").append(profile.getVersion())
                    .append(" — <em>Výskyt: ").append(frequency).append("×</em>");

            html.append("<ul>");
            for (Vulnerability v : vulns) {
                String displayedCveId = v.getCveId();
                if (displayedCveId != null && displayedCveId.startsWith("GHSA-")) {
                    displayedCveId = GitHubAdvisoryClient.getCveIdFromGhsa(displayedCveId);
                }

                String osvLink = displayedCveId != null ? "https://osv.dev/vulnerability/" + displayedCveId : "#";

                StringBuilder scores = new StringBuilder();
                if (v.getSeverity() != null) {
                    String[] parts = v.getSeverity().split(",");
                    for (String part : parts) {
                        part = part.trim();
                        if (!part.isEmpty()) {
                            String weighted = CvssScoreCalculator.calculateScore(part);
                            String version = part.startsWith("CVSS:4.0") ? "4.0" : "3.1";
                            String scoreClass = getScoreCssClass(weighted);
                            scores.append("<div class='score ").append(scoreClass).append("'>")
                                    .append("CVSS ").append(version).append(": ").append(weighted)
                                    .append("</div><br/>");
                        }
                    }
                }

                html.append("<li class='vuln'>")
                        .append("<strong>").append(displayedCveId).append("</strong><br/>")
                        .append(scores)
                        .append(v.getDescription()).append("<br/>")
                        .append("<a href='").append(osvLink).append("' target='_blank'>Zobraziť detail v OSV</a>")
                        .append("</li>");
            }
            html.append("</ul></div>");
        }

        // --- Bezpečné závislosti ---
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

        html.append("</body></html>");

        try {
            Files.writeString(Paths.get(report.getAbsolutePath(), "reportPY.html"), html.toString());
            System.out.println("HTML report pre Python bol úspešne vygenerovaný.");
        } catch (IOException e) {
            System.err.println("Nepodarilo sa uložiť HTML report pre Python: " + e.getMessage());
        }

        generateDependencyGraphHtmlFile(project.getPath(), report);
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

    public static void generateDependencyGraphHtmlFile(String projectPath, File report) {
        Map<String, List<String>> graph = PipDependencyTreeFetcher.buildDependencyGraph(projectPath);

        Function<String, String> safeId = s -> s.replaceAll("[^a-zA-Z0-9]", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "")
                .toLowerCase();

        Map<String, String> idMap = new HashMap<>();
        for (String node : graph.keySet()) {
            idMap.put(node, safeId.apply(node));
        }

        Map<String, Set<String>> reverseGraph = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : graph.entrySet()) {
            String parent = entry.getKey();
            for (String child : entry.getValue()) {
                reverseGraph.computeIfAbsent(child, k -> new LinkedHashSet<>()).add(parent);
            }
        }

        Set<String> allNodes = new HashSet<>();
        allNodes.addAll(graph.keySet());
        allNodes.addAll(reverseGraph.keySet());

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
                    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
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
            <h1>Graf závislostí a predchodcov pre Python projekt</h1>
    """);

        for (String node : allNodes) {
            String nodeId = idMap.getOrDefault(node, safeId.apply(node));
            Set<String> dependencies = graph.getOrDefault(node, new ArrayList<>())
                    .stream().collect(Collectors.toCollection(LinkedHashSet::new));
            Set<String> dependents = reverseGraph.getOrDefault(node, new LinkedHashSet<>());

            html.append("<div class='dependency-card' id='").append(nodeId).append("'>")
                    .append("<h3>").append(node).append("</h3>")
                    .append("<div class='dependency-relations'>");

            // závisí od
            html.append("<div><strong>závisí od:</strong>");
            if (!dependencies.isEmpty()) {
                html.append("<ul>");
                for (String dep : dependencies) {
                    String depId = idMap.getOrDefault(dep, safeId.apply(dep));
                    html.append("<li><a href='#").append(depId).append("'>").append(dep).append("</a></li>");
                }
                html.append("</ul>");
            } else {
                html.append("<p>Žiadne závislosti.</p>");
            }
            html.append("</div>");

            // je potrebný pre
            html.append("<div><strong>je potrebný pre:</strong>");
            if (!dependents.isEmpty()) {
                html.append("<ul>");
                for (String dep : dependents) {
                    String depId = idMap.getOrDefault(dep, safeId.apply(dep));
                    html.append("<li><a href='#").append(depId).append("'>").append(dep).append("</a></li>");
                }
                html.append("</ul>");
            } else {
                html.append("<p>Žiadni závislí predchodcovia.</p>");
            }

            html.append("</div></div></div>\n");
        }

        // JS na zvýraznenie cieľa pri kliknutí
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
            Files.writeString(Paths.get(report.getAbsolutePath(), "dependency-graph-python.html"), html.toString());
            System.out.println("HTML graf závislostí + predchodcov pre Python bol úspešne vygenerovaný.");
        } catch (IOException e) {
            System.err.println("Nepodarilo sa uložiť HTML graf pre Python: " + e.getMessage());
        }
    }
}
