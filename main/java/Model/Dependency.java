package Model;

import java.util.Objects;

public class Dependency {
    private String name;
    private String version;
    private String language;
    private String scope;

    public Dependency(String name, String version, String language) {
        this(name, version, language, "compile"); // predvolený scope
    }

    public Dependency(String name, String version, String language, String scope) {
        this.name = name;
        this.version = version;
        this.language = language;
        this.scope = scope;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getLanguage() {
        return language;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return name + "@" + version + " [" + language + ", " + scope + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(version, that.version) &&
                Objects.equals(language, that.language) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, version, language, scope);
    }
}
