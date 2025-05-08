package App;

import Service.External.PythonScriptExtractor;
import Service.Repository.DatabaseManager;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

public class MainWindow extends Application {

    private File projectDirectory;
    private File outputDirectory;
    private ProgressBar progressBar;
    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Report Generator");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Button selectProjectButton = new Button("Vybrať projektový adresár");
        Label projectPathLabel = new Label("Nie je vybratý projekt.");

        Button selectOutputButton = new Button("Vybrať výstupný adresár");
        Label outputPathLabel = new Label("Nie je vybraný výstup.");

        Button startButton = new Button("Spustiť generovanie reportov");

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(400);
        progressBar.setVisible(false);

        statusLabel = new Label("");

        selectProjectButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Vyber projektový adresár");
            File selected = chooser.showDialog(primaryStage);
            if (selected != null) {
                projectDirectory = selected;
                projectPathLabel.setText("Projekt: " + projectDirectory.getAbsolutePath());
            }
        });

        selectOutputButton.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Vyber výstupný adresár");
            File selected = chooser.showDialog(primaryStage);
            if (selected != null) {
                outputDirectory = selected;
                outputPathLabel.setText("Výstup: " + outputDirectory.getAbsolutePath());
            }
        });

        startButton.setOnAction(e -> {
            if (projectDirectory == null || outputDirectory == null) {
                showAlert("Vyberte projekt a výstupný adresár.");
                return;
            }
            generateReports();
        });

        root.getChildren().addAll(
                selectProjectButton, projectPathLabel,
                selectOutputButton, outputPathLabel,
                startButton,
                progressBar,
                statusLabel
        );

        primaryStage.setScene(new Scene(root, 500, 400));
        primaryStage.show();
    }

    private void generateReports() {
        progressBar.setProgress(0);
        progressBar.setVisible(true);
        statusLabel.setText("Prebieha generovanie reportu...");

        new Thread(() -> {
            try {
                updateProgress(0.05, "Extrahujem Python skript...");
                PythonScriptExtractor.extractIfNeeded();

                updateProgress(0.15, "Inicializujem databázu...");
                DatabaseManager.initializeDatabase();

                updateProgress(0.30, "Analyzujem projekt...");
                Service.ProjectAnalyzerRunner.runWithProgress(projectDirectory, outputDirectory, this::updateProgress);

                updateUIAfterSuccess();
            } catch (Exception e) {
                updateUIAfterError(e.getMessage());
            }
        }).start();
    }

    private void updateProgress(double progress, String message) {
        javafx.application.Platform.runLater(() -> {
            progressBar.setProgress(progress);
            statusLabel.setText(message);
        });
    }


    private void updateUIAfterSuccess() {
        javafx.application.Platform.runLater(() -> {
            progressBar.setProgress(1.0);
            statusLabel.setText("Report úspešne vygenerovaný!");
        });
    }

    private void updateUIAfterError(String error) {
        javafx.application.Platform.runLater(() -> {
            progressBar.setVisible(false);
            statusLabel.setText("Chyba: " + error);
            showAlert(error);
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Chyba");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
