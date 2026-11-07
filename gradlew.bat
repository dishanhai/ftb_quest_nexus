@rem Author: dishanhai
@echo off
setlocal
set "APP_HOME=%~dp0"
set "BUILD_BOTH="
if "%~1"=="" goto defaultBuild
for %%A in (%*) do if "%%~A"=="build" set "BUILD_BOTH=1"
for %%A in (%*) do if "%%~A"==":build" set "BUILD_BOTH=1"
goto runGradle

:defaultBuild
set "BUILD_BOTH=1"

:runGradle
if defined JAVA_HOME (set "JAVA_EXE=%JAVA_HOME%\bin\java.exe") else (set "JAVA_EXE=java.exe")
pushd "%APP_HOME%"
if "%~1"=="" (
    "%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain build
) else (
    "%JAVA_EXE%" %JAVA_OPTS% %GRADLE_OPTS% -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
)
set "BUILD_RESULT=%ERRORLEVEL%"
if not "%BUILD_RESULT%"=="0" goto finish
if not defined BUILD_BOTH goto finish
if "%~1"=="" (
    call "%APP_HOME%neoforge\gradlew.bat" -p "%APP_HOME%neoforge" build
) else (
    call "%APP_HOME%neoforge\gradlew.bat" -p "%APP_HOME%neoforge" %*
)
set "BUILD_RESULT=%ERRORLEVEL%"

:finish
popd
exit /b %BUILD_RESULT%
