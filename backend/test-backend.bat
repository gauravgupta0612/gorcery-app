@echo off
REM Runs the backend unit tests and saves the full output to backend\test-output.txt
cd /d "%~dp0"
echo Running backend tests... (output: backend\test-output.txt)
call mvn -B test > test-output.txt 2>&1
type test-output.txt | findstr /R /C:"Tests run" /C:"FAIL" /C:"ERROR" /C:"BUILD"
pause
