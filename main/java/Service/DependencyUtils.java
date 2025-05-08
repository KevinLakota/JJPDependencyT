package Service;

public class DependencyUtils {

    /**
     * Odstráni verziu z Maven dependency (napr. groupId:artifactId:version → groupId:artifactId)
     */
    public static String stripMavenVersion(String fullDep) {
        String[] parts = fullDep.split(":");
        if (parts.length >= 2) {
            return parts[0] + ":" + parts[1];
        }
        return fullDep;
    }

    /**
     * Odstráni verziu z NPM dependency (napr. lodash@4.17.21 → lodash, @babel/core@7.0.0 → @babel/core)
     */
    public static String stripJsVersion(String fullDep) {
        if (fullDep == null) return "unknown";

        int atIndex = fullDep.lastIndexOf("@");
        if (atIndex > 0) {
            return fullDep.substring(0, atIndex);
        }

        return fullDep;
    }

    /**
     * Získa verziu z NPM dependency (napr. lodash@4.17.21 → 4.17.21, react@^18.2.0 → ^18.2.0)
     */
    public static String extractJsVersion(String full) {
        if (full == null) return "unknown";

        int atIndex = full.lastIndexOf("@");
        if (atIndex > 0 && atIndex < full.length() - 1) {
            return full.substring(atIndex + 1);
        }

        return "unknown";
    }
}
