@echo off
chcp 65001 >nul
cd /d "%~dp0"
where javac >nul 2>nul || (echo No se encontro Java JDK 17 o superior. & pause & exit /b 1)
if not exist servidor-java\build\classes mkdir servidor-java\build\classes
dir /s /b servidor-java\src\*.java > servidor-java\build\fuentes.txt
javac -encoding UTF-8 -d servidor-java\build\classes @servidor-java\build\fuentes.txt || (pause & exit /b 1)
echo Para detener el servidor cierra esta ventana.
java -cp servidor-java\build\classes servidor.Principal
pause
