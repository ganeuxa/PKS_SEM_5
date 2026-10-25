@echo off
rem Start the JavaFX app. Requires JDK 21+ (JAVA_HOME or java in PATH). Maven is downloaded by mvnw on first run.
cd /d "%~dp0"
call mvnw.cmd -q javafx:run
if errorlevel 1 pause
