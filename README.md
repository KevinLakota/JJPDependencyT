
# JJPDependencyT

**JJPDependencyT** is a standalone Java application for analyzing software dependencies and detecting known vulnerabilities using open-source data sources.

## Key Features

- Dependency analysis for Java, Python, and JavaScript projects
- Vulnerability lookup using the OSV (Open Source Vulnerabilities) database
- CVSS score calculation for vulnerabilities using a Python-based script
- HTML report generation with dependency statistics and risk profiling
- Built-in database for storing scan results
- JavaFX GUI for easy interaction

## Requirements

- Java 17+
- Python (for CVSS calculation)
- JavaFX SDK (included in `/target/`)

## Running the App

Use the provided `Start.bat` file located in the `target/` folder to run the application. No manual setup is required.

```bash
./Start.bat
```

---

## 🧰 Installation (Windows)

To run the application correctly, it's necessary to install a few prerequisite tools and libraries, especially for working with Java, Python, and JavaScript ecosystems.

We recommend using [Chocolatey](https://chocolatey.org/) – a package manager for Windows – to simplify the installation process.

### 1. Open PowerShell as Administrator

Right-click the Windows icon and select **“Windows PowerShell (Admin)”**.

> All commands below must be executed with administrator privileges.

### 2. Install Chocolatey

Run the following command in PowerShell:

```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force; `
[System.Net.ServicePointManager]::SecurityProtocol = `
[System.Net.ServicePointManager]::SecurityProtocol -bor 3072; `
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

Restart PowerShell after installation.

### 3. Install Required Tools

Use Chocolatey to install all necessary tools:

```powershell
choco install git -y
choco install python -y
choco install wget -y
choco install maven -y
choco install nodejs -y
```

After installation, verify commands such as `mvn -v`, `git --version`, `python --version`, and `npm -v`.

### 4. Install Required Python Packages

Some components use Python scripts (e.g., CVSS score calculation). Install the needed libraries:

```bash
pip install cvss
pip install graphviz
```

### 5. Install Visual C++ Build Tools (if needed)

Some Python packages require native C/C++ compilation. If installation fails, download:

[https://visualstudio.microsoft.com/visual-cpp-build-tools/](https://visualstudio.microsoft.com/visual-cpp-build-tools/)

During setup, select the **“C++ build tools”** workload.

---

Once all steps are complete, you can launch the application using the provided `Start.bat` script.

## License

This project is intended for educational and research purposes.
