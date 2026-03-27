@echo off

REM HarmonyOS Hvigor Wrapper Script for Windows

set DEFAULT_HVIGOR_VERSION=4.0.2
set HVIGOR_VERSION=%HVIGOR_VERSION%
if "%HVIGOR_VERSION%"=="" set HVIGOR_VERSION=%DEFAULT_HVIGOR_VERSION%

set HVIGOR_APP_HOME=%~dp0
set HVIGOR_WRAPPER_DIR=%HVIGOR_APP_HOME%hvigor
set HVIGOR_WRAPPER_JAR=%HVIGOR_WRAPPER_DIR%\hvigor-wrapper-%HVIGOR_VERSION%.jar

REM Check if hvigor-wrapper.jar exists
if not exist "%HVIGOR_WRAPPER_JAR%" (
    echo Hvigor wrapper JAR not found.
    echo Please open this project in DevEco Studio and sync the project first.
    echo DevEco Studio will automatically download the required dependencies.
    exit /b 1
)

node "%HVIGOR_WRAPPER_DIR%\hvigor-wrapper.js" %*
