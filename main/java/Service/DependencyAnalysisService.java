package Service;

import App.ProjectAnalyzer;
import Model.Dependency;
import Model.Project;
import Service.Report.ReportComparisonService;
import Service.Report.ReportJSService;
import Service.Report.ReportJavaService;
import Service.Report.ReportPythonService;
import Service.Repository.VulnerabilityRepository;
import Utillity.forJava.DependencyTreeMapper;
import Utillity.forJava.PomDependencyParser;
import Utillity.forJava.TransitiveDependencyFetcher;
import Utillity.forJavaScript.JsDependencyExtractor;
import Utillity.forJavaScript.JsTransitiveDependencyFetcher;
import Utillity.forPython.PipDependencyTreeFetcher;
import Utillity.forPython.PythonVirtualEnvManager;

import java.io.File;
import java.util.*;

public class DependencyAnalysisService {
    public static void analyzeJavaProject(String projectPath, Project project, File report) {
        File projectDir = new File(projectPath);

        File pomFile = ProjectAnalyzer.findPomXml(projectDir);
        if (pomFile == null) {
            System.out.println("Nepodarilo sa nájsť pom.xml.");
            return;
        }

        Map<String, String> mainDeps = PomDependencyParser.parsePom(pomFile.getAbsolutePath());
        List<String> transitives = TransitiveDependencyFetcher.fetchDependencies(projectDir.getAbsolutePath());
        Map<String, String> resolvedMainDeps = PomDependencyParser.assignVersionsFromTransitives(mainDeps, transitives);

        System.out.println("\n--- Hlavné závislosti s priradenými verziami ---");
        System.out.println(resolvedMainDeps.size() + " nájdených:");
        resolvedMainDeps.forEach((key, version) -> System.out.println("- " + key + ":" + version));

        Set<String> allDepsCombined = new HashSet<>(transitives);
        List<Dependency> allDeps = new ArrayList<>();

        for (String dep : allDepsCombined) {
            String[] parts = dep.split(":");
            if (parts.length >= 5) {
                String name = parts[0] + ":" + parts[1];
                String version = parts[3];
                String scope = parts[4];
                allDeps.add(new Dependency(name, version, "java", scope));
            }
        }

        if (DependencyPersisterService.updateProjectDependencies(project.getId(), allDeps)) {
            VulnerabilityScannerService.scanProjectForVulnerabilities(project.getId());
        }

        Map<String, List<String>> dependencyGraph = DependencyTreeMapper.buildDependencyGraph(projectDir.getAbsolutePath());

        System.out.println("\n--- Mapa závislostí (kto používa koho) ---");
        for (Map.Entry<String, List<String>> entry : dependencyGraph.entrySet()) {
            System.out.println("- " + entry.getKey() + " závisí od:");
            for (String dep : entry.getValue()) {
                System.out.println("    → " + dep);
            }
        }

        Map<String, Integer> usageCount = DependencyTreeMapper.countDependencyOccurrences(dependencyGraph);

        System.out.println("\n--- Frekvencia použitia závislostí ---");
        usageCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry ->
                        System.out.println("- " + entry.getKey() + " → použitá " + entry.getValue() + "×")
                );

        System.out.println("\nZávislostný graf bol vygenerovaný (deps.dot).");
        ReportJavaService.generateHtmlReportForProject(project.getId(), report);
        if (VulnerabilityRepository.hasHistoryForProject(project.getId())) {
            File comparisonReport = new File(report, "report-comparison-project-" + project.getId() + ".html");
            ReportComparisonService.generateComparisonReport(project.getId(), comparisonReport);
        }

    }

    public static void analyzePythonProject(String projectPath, Project project, File report) {
        System.out.println("\nAnalyzujem Python projekt: " + projectPath);

        if (!PythonVirtualEnvManager.setupVirtualEnv(projectPath)) {
            System.err.println("Nepodarilo sa pripraviť prostredie (venv).");
            return;
        }

        if (!PythonVirtualEnvManager.runPipdeptreeInVenv(projectPath)) {
            System.err.println("Nepodarilo sa spustiť pipdeptree.");
            return;
        }

        Set<String> allDeps = PipDependencyTreeFetcher.parseDependencyTree(projectPath);

        System.out.println("\n--- Všetky závislosti vrátane tranzitívnych ---");
        allDeps.forEach(dep -> System.out.println("- " + dep));
        System.out.println("Celkový počet všetkých závislostí: " + allDeps.size());

        List<Dependency> newDeps = new ArrayList<>();
        for (String dep : allDeps) {
            String[] parts = dep.split("==");
            if (parts.length == 2) {
                newDeps.add(new Dependency(parts[0], parts[1], "python", "unknown"));
            }
        }

        if (DependencyPersisterService.updateProjectDependencies(project.getId(), newDeps)) {
            VulnerabilityScannerService.scanProjectForVulnerabilities(project.getId());
        }

        Map<String, Integer> freqMap = PipDependencyTreeFetcher.getDependencyFrequencyMap(projectPath);
        System.out.println("\n--- Početnosť výskytu závislostí ---");
        freqMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry -> System.out.printf("- %s → %dx\n", entry.getKey(), entry.getValue()));
        PipDependencyTreeFetcher.printAllDependencyEdges(projectPath);
        ReportPythonService.generateHtmlReportForProject(project.getId(), report);
        if (VulnerabilityRepository.hasHistoryForProject(project.getId())) {
            File comparisonReport = new File(report, "report-comparison-project-" + project.getId() + ".html");
            ReportComparisonService.generateComparisonReport(project.getId(), comparisonReport);
        }

    }

    public static void analyzeJsProject(String projectPath, Project project, File report) {
        System.out.println("\nAnalyzujem JavaScript projekt: " + projectPath);

        JsTransitiveDependencyFetcher.prepareDependencyTrees(projectPath);
        Map<String, String> mainDeps = JsDependencyExtractor.extractDependencies(projectPath);

        if (mainDeps.isEmpty()) {
            System.out.println("Nenašli sa žiadne závislosti v package.json súboroch.");
            return;
        }

        System.out.println("\n--- Hlavné závislosti z package.json ---");
        mainDeps.forEach((name, version) -> System.out.printf("- %s : %s\n", name, version));
        System.out.println("Počet hlavných závislostí: " + mainDeps.size());

        Map<String, List<String>> graph = JsTransitiveDependencyFetcher.buildDependencyGraph(projectPath);

        Set<String> allDepsCombined = new HashSet<>();
        graph.forEach((parent, children) -> {
            allDepsCombined.add(parent);
            allDepsCombined.addAll(children);
        });

        List<Dependency> allDeps = new ArrayList<>();
        for (String fullDep : allDepsCombined) {
            String name = DependencyUtils.stripJsVersion(fullDep);
            String version = DependencyUtils.extractJsVersion(fullDep);
            allDeps.add(new Dependency(name, version, "javascript", "unknown"));
        }

        if (DependencyPersisterService.updateProjectDependencies(project.getId(), allDeps)) {
            VulnerabilityScannerService.scanProjectForVulnerabilities(project.getId());
        }

        System.out.println("\n--- Závislostný graf (kto používa koho) ---");
        graph.forEach((parent, children) -> {
            System.out.println("- " + parent + " závisí od:");
            for (String child : children) {
                System.out.println("    → " + child);
            }
        });

        System.out.println("\nCelkový počet závislostí (vrátane tranzitívnych): " + allDepsCombined.size());

        Map<String, Integer> usageCount = new HashMap<>();
        for (List<String> dependencies : graph.values()) {
            Set<String> uniqueDeps = new HashSet<>(dependencies);
            for (String dep : uniqueDeps) {
                String key = DependencyUtils.stripJsVersion(dep);
                usageCount.put(key, usageCount.getOrDefault(key, 0) + 1);
            }
        }

        System.out.println("\n--- Frekvencia použitia závislostí ---");
        usageCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(entry ->
                        System.out.println("- " + entry.getKey() + " → použitá " + entry.getValue() + "×")
                );
        ReportJSService.generateHtmlReportForProject(project.getId(), report);
        if (VulnerabilityRepository.hasHistoryForProject(project.getId())) {
            File comparisonReport = new File(report, "report-comparison-project-" + project.getId() + ".html");
            ReportComparisonService.generateComparisonReport(project.getId(), comparisonReport);
        }
    }
}
