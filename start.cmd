@echo off
del /S /Q *.class > nul 2>&1

javac Program.java && java Program
echo Press ENTER to exit.
pause
