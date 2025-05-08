package Service.Report;

import Model.Vulnerability;
import Model.Project;
import Service.Repository.VulnerabilityRepository;
import Service.Repository.ProjectRepository;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.io.File;

public class ReportComparisonService {

    public static void generateComparisonReport(int projectId, File outputFile) {
        List<Vulnerability> current = VulnerabilityRepository.getForProject(projectId);
        List<Vulnerability> history = VulnerabilityRepository.getHistoryForProject(projectId);
        Project project = ProjectRepository.findById(projectId);

        Set<String> currentSet = new HashSet<>();
        Set<String> historySet = new HashSet<>();

        Map<String, Vulnerability> currentMap = new HashMap<>();
        Map<String, Vulnerability> historyMap = new HashMap<>();

        for (Vulnerability v : current) {
            String key = generateKey(v);
            currentSet.add(key);
            currentMap.put(key, v);
        }

        for (Vulnerability v : history) {
            String key = generateKey(v);
            historySet.add(key);
            historyMap.put(key, v);
        }

        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(currentSet);
        allKeys.addAll(historySet);

        String projectName = project != null ? project.getName() : ("Projekt ID " + projectId);

        int newCount = 0;
        int removedCount = 0;
        int unchangedCount = 0;

        class Row {
            String key;
            String change;
            String rowClass;
            Vulnerability v;

            Row(String key, String change, String rowClass, Vulnerability v) {
                this.key = key;
                this.change = change;
                this.rowClass = rowClass;
                this.v = v;
            }
        }

        List<Row> rows = new ArrayList<>();

        for (String key : allKeys) {
            Vulnerability c = currentMap.get(key);
            Vulnerability h = historyMap.get(key);
            Vulnerability v = c != null ? c : h;

            if (c != null && h != null) {
                rows.add(new Row(key, "Zostáva", "unchanged", v));
                unchangedCount++;
            } else if (c != null) {
                rows.add(new Row(key, "Nová", "new", v));
                newCount++;
            } else {
                rows.add(new Row(key, "Odstránená", "removed", v));
                removedCount++;
            }
        }

        // Zoradenie podľa názvu balíka, potom podľa poradia: Odstránená, Zostáva, Nová
        Map<String, Integer> changeOrder = Map.of(
                "Odstránená", 0,
                "Zostáva", 1,
                "Nová", 2
        );

        rows.sort(Comparator
                .comparing((Row r) -> r.v.getPackageName())
                .thenComparing(r -> changeOrder.getOrDefault(r.change, 3)));

        StringBuilder html = new StringBuilder();
        html.append("""
        <!DOCTYPE html>
        <html lang="sk">
        <head>
            <meta charset="UTF-8">
            <title>Porovnanie zraniteľností</title>
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
                table {
                    width: 100%;
                    border-collapse: collapse;
                    background: #fff;
                    box-shadow: 0 2px 8px rgba(0,0,0,0.05);
                    margin-bottom: 30px;
                    border-radius: 8px;
                    overflow: hidden;
                }
                th, td {
                    padding: 12px 15px;
                    text-align: left;
                    border-bottom: 1px solid #ddd;
                    vertical-align: top;
                }
                th {
                    background-color: #ecf0f1;
                    font-weight: bold;
                }
                td.cve {
                    white-space: nowrap;
                    font-family: monospace;
                    font-size: 14px;
                }
                tr.new {
                    background-color: #fdecea;
                }
                tr.removed {
                    background-color: #eafaf1;
                }
                tr.unchanged {
                    background-color: #fff9e6;
                }
                ul {
                    background: #fff;
                    padding: 20px;
                    border-radius: 8px;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.05);
                }
                li {
                    margin-bottom: 8px;
                }
                .legend {
                    margin-top: 30px;
                    padding: 15px;
                    background: #fff;
                    border-radius: 8px;
                    box-shadow: 0 2px 6px rgba(0,0,0,0.05);
                }
                .legend-item {
                    display: flex;
                    align-items: center;
                    margin-bottom: 10px;
                }
                .color-box {
                    width: 20px;
                    height: 20px;
                    margin-right: 10px;
                    border-radius: 4px;
                    border: 1px solid #ccc;
                }
                .new-box { background-color: #fdecea; }
                .removed-box { background-color: #eafaf1; }
                .unchanged-box { background-color: #fff9e6; }
            </style>
        </head>
        <body>
        """);

        html.append("<h1>Porovnanie zraniteľností pre projekt: <strong>").append(projectName).append("</strong></h1>");

        html.append("<table>");
        html.append("<tr>")
                .append("<th>Balík</th>")
                .append("<th>Verzia</th>")
                .append("<th>CVE</th>")
                .append("<th>Zmena</th>")
                .append("<th>Závažnosť</th>")
                .append("<th>Popis</th>")
                .append("</tr>");

        for (Row row : rows) {
            Vulnerability v = row.v;
            html.append("<tr class='").append(row.rowClass).append("'>")
                    .append("<td>").append(v.getPackageName()).append("</td>")
                    .append("<td>").append(v.getPackageVersion()).append("</td>")
                    .append("<td class='cve'>").append(v.getCveId()).append("</td>")
                    .append("<td>").append(row.change).append("</td>")
                    .append("<td>").append(v.getSeverity()).append("</td>")
                    .append("<td>").append(v.getDescription()).append("</td>")
                    .append("</tr>");
        }

        html.append("</table>");

        html.append("<h2>Zhrnutie zmien</h2>");
        html.append("<ul>");
        html.append("<li><strong>Nové zraniteľnosti:</strong> ").append(newCount).append("</li>");
        html.append("<li><strong>Odstránené zraniteľnosti:</strong> ").append(removedCount).append("</li>");
        html.append("<li><strong>Zostávajúce zraniteľnosti:</strong> ").append(unchangedCount).append("</li>");
        html.append("</ul>");

        html.append("""
        <div class="legend">
            <h2>Legenda:</h2>
            <div class="legend-item"><div class="color-box new-box"></div> Nová zraniteľnosť (pridaná od poslednej analýzy)</div>
            <div class="legend-item"><div class="color-box removed-box"></div> Odstránená zraniteľnosť (už sa nevyskytuje)</div>
            <div class="legend-item"><div class="color-box unchanged-box"></div> Zostávajúca zraniteľnosť (stále prítomná)</div>
        </div>
        """);

        html.append("</body></html>");

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html.toString());
            System.out.println("Porovnávací HTML report bol úspešne vygenerovaný: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Chyba pri zápise HTML reportu: " + e.getMessage());
        }
    }

    private static String generateKey(Vulnerability v) {
        return v.getPackageName() + ":" + v.getPackageVersion() + ":" + v.getCveId();
    }
}
