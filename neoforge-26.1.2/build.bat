@echo off
setlocal
rem Author: dishanhai
if defined JAVA25_HOME set "JAVA_HOME=%JAVA25_HOME%"
set "BUILD_EXIT=1"

call "%~dp0..\neoforge\gradlew.bat" -p "%~dp0." build
set "BUILD_EXIT=%ERRORLEVEL%"
if not "%BUILD_EXIT%"=="0" goto finish
echo NeoForge 26.1.2 JARs:
dir /b "%~dp0build\libs\*.jar"

:finish
if not "%BUILD_EXIT%"=="0" echo Build failed. See the output above.
if /i not "%~1"=="--no-pause" pause
exit /b %BUILD_EXIT%
