@echo off

echo Compilation...
C:\dev\jdk\jdk-24.0.1\bin\javac -classpath .\lwjgl\* -d .\out .\src\com\company\Main.java

echo Running...
_launcher.cmd
