package Service;

import App.ProjectAnalyzer;
import App.ProjectAnalyzer.ProjectType;
import Model.Project;
import Service.Repository.ProjectRepository;

import java.io.File;
import java.util.function.BiConsumer;

public class ProjectAnalyzerRunner {

    public static Project run() {
        // Vyber projektový adresár
        File projectDir = ProjectAnalyzer.chooseProjectDirectory();
        if (projectDir == null) {
            System.out.println("Žiadny projekt nebol vybraný.");
            return null;
        }
        File reportDir = ProjectAnalyzer.chooseReportDirectory();
        if (reportDir == null) {
            System.out.println("Žiadny projekt nebol vybraný.");
            return null;
        }
        // Detekuj typ projektu
        ProjectType type = ProjectAnalyzer.detectProjectType(projectDir);
        System.out.println("Zistený typ projektu: " + type);

        // Priprav údaje pre databázu
        String projectName = projectDir.getName();
        String path = projectDir.getAbsolutePath();
        String language = type.name().toLowerCase(); // java / python / javascript

        // Zápis do DB
        Project project = ProjectRepository.createOrUpdate(projectName, path, language);
        if (project == null) {
            System.err.println("Nepodarilo sa vytvoriť alebo načítať projekt.");
            return null;
        }
        // Spusti analýzu podľa typu
        switch (type) {
            case JAVA -> DependencyAnalysisService.analyzeJavaProject(path, project, reportDir);
            case PYTHON -> DependencyAnalysisService.analyzePythonProject(path, project, reportDir);
            case JAVASCRIPT -> DependencyAnalysisService.analyzeJsProject(path, project, reportDir);
            default -> System.out.println("Nepodarilo sa identifikovať typ projektu.");
        }

        return project;
    }

    public static void runWithProgress(File projectDirectory, File outputDirectory, BiConsumer<Double, String> progressCallback) {
        progressCallback.accept(0.35, "Detekujem typ projektu...");
        ProjectAnalyzer.ProjectType type = ProjectAnalyzer.detectProjectType(projectDirectory);

        String projectName = projectDirectory.getName();
        String path = projectDirectory.getAbsolutePath();
        String language = type.name().toLowerCase();

        Project project = ProjectRepository.createOrUpdate(projectName, path, language);
        if (project == null) {
            throw new RuntimeException("Nepodarilo sa vytvoriť alebo načítať projekt.");
        }

        progressCallback.accept(0.55, "Analyzujem závislosti...");

        switch (type) {
            case JAVA -> DependencyAnalysisService.analyzeJavaProject(path, project, outputDirectory);
            case PYTHON -> DependencyAnalysisService.analyzePythonProject(path, project, outputDirectory);
            case JAVASCRIPT -> DependencyAnalysisService.analyzeJsProject(path, project, outputDirectory);
            default -> throw new IllegalArgumentException("Nepodporovaný typ projektu: " + type);
        }

        progressCallback.accept(0.90, "Generujem report...");
        // (Generovanie prebieha už v analyzačných službách)

        progressCallback.accept(1.0, "Hotovo.");
    }


}
