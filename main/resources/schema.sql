CREATE TABLE IF NOT EXISTS projects (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        name TEXT NOT NULL UNIQUE,
                                        path TEXT,
                                        language TEXT
);

CREATE TABLE IF NOT EXISTS dependencies (
                                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                                            project_id INTEGER,
                                            name TEXT,
                                            version TEXT,
                                            language TEXT,
                                            scope TEXT DEFAULT 'compile',
                                            FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS vulnerabilities (
                                               id INTEGER PRIMARY KEY AUTOINCREMENT,
                                               project_id INTEGER,
                                               package_name TEXT,
                                               package_version TEXT,
                                               source TEXT,
                                               cve_id TEXT,
                                               severity TEXT,
                                               description TEXT,
                                               FOREIGN KEY (project_id) REFERENCES projects(id)
);

CREATE TABLE IF NOT EXISTS vulnerability_history (
                                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                                     project_id INTEGER NOT NULL,
                                                     package_name TEXT NOT NULL,
                                                     package_version TEXT NOT NULL,
                                                     source TEXT NOT NULL,
                                                     cve_id TEXT NOT NULL,
                                                     severity TEXT,
                                                     description TEXT,
                                                     archived_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


