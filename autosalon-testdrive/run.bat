@echo off
rem Start the console app. Requires Java 17+ in PATH and a built jar (mvn clean package).
cd /d "%~dp0"
if not exist "target\autosalon-testdrive.jar" (
    echo target\autosalon-testdrive.jar not found. Build the project first: mvn clean package
    pause
    exit /b 1
)
java -jar target\autosalon-testdrive.jar
pause
