package Model;

public class Project {
    private int id;
    private String name;
    private String path;
    private String language;

    public Project(int id, String name, String path, String language) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.language = language;
    }

    public Project(String name, String path, String language) {
        this(-1, name, path, language);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + name + " (" + language + ") → " + path;
    }
}
