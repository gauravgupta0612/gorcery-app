@echo off
REM Loads backend\.env and starts the Gola Grocery API on http://localhost:8095
cd /d "%~dp0"
if not exist ".env" ( echo [ERROR] backend\.env not found & pause & exit /b 1 )
for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do set "%%A=%%B"
where mvn >nul 2>&1 || ( echo [ERROR] Maven ^(mvn^) not found on PATH. Install Maven 3.9+ and JDK 21. & pause & exit /b 1 )
echo Starting Gola Grocery backend (first start creates tables and loads the catalogue)...
mvn spring-boot:run
pause
