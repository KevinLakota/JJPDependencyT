    package App;

    import Service.ProjectAnalyzerRunner;
    import Service.Repository.DatabaseManager;


    public class Main {
        public static void main(String[] args) {
            DatabaseManager.initializeDatabase();
            ProjectAnalyzerRunner.run();

        }
    }



