package App;

import javax.swing.*;
import java.io.File;

public class ProjectAnalyzer {

    public enum ProjectType {
        JAVA, PYTHON, JAVASCRIPT, UNKNOWN
    }

    public static File findPomXml(File root) {
        File pom = new File(root, "pom.xml");
        return pom.exists() ? pom : null;
    }

    public static File findRequirementsTxt(File root) {
        File req = new File(root, "requirements.txt");
        return req.exists() ? req : null;
    }

    public static boolean hasRequirementsFolderWithTxt(File root) {
        File requirementsDir = new File(root, "requirements");
        if (requirementsDir.exists() && requirementsDir.isDirectory()) {
            File[] txtFiles = requirementsDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
            return txtFiles != null && txtFiles.length > 0;
        }
        return false;
    }

    public static ProjectType detectProjectType(File root) {
        if (findPomXml(root) != null) {
            return ProjectType.JAVA;
        } else if (findRequirementsTxt(root) != null || hasRequirementsFolderWithTxt(root)) {
            return ProjectType.PYTHON;
        } else if (hasPackageJson(root)) {
            return ProjectType.JAVASCRIPT;
        } else {
            return ProjectType.UNKNOWN;
        }
    }

    public static File chooseProjectDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Vyber projektový adresár");
        return getFile(chooser);
    }

    public static File chooseReportDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Vyber adresár pre HTML reporty");
        return getFile(chooser);
    }

    public static File getFile(JFileChooser chooser) {
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {
            System.out.println("Výber adresára zrušený používateľom.");
            return null;
        }
    }


    public static boolean hasPackageJson(File root) {
        return containsFileRecursively(root, "package.json");
    }

    private static boolean containsFileRecursively(File dir, String fileName) {
        if (!dir.isDirectory()) return false;
        File[] files = dir.listFiles();
        if (files == null) return false;

        for (File f : files) {
            if (f.isDirectory()) {
                if (containsFileRecursively(f, fileName)) return true;
            } else if (f.getName().equalsIgnoreCase(fileName)) {
                return true;
            }
        }
        return false;
    }

}
