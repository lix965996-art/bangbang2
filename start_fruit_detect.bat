@echo off
setlocal EnableExtensions EnableDelayedExpansion
chcp 65001 >nul
title BangBangAgro Fruit Detect

set "ROOT_DIR=%~dp0"
set "PYTHON_DIR=%ROOT_DIR%TomatoDetection"
set "PYTHON_SCRIPT=%PYTHON_DIR%\integrated_api_server.py"
set "REQUIREMENTS_FILE=%PYTHON_DIR%\api_requirements.txt"
set "HEALTH_URL=http://localhost:5000/api/health"
set "VENV_DIR=%PYTHON_DIR%\.venv"
set "VENV_PYTHON=%VENV_DIR%\Scripts\python.exe"

set "PYTHON_CMD="
py -3.12 -V >nul 2>&1
if not errorlevel 1 set "PYTHON_CMD=py -3.12"

if not defined PYTHON_CMD (
where py >nul 2>&1
if not errorlevel 1 set "PYTHON_CMD=py -3"
)

if not defined PYTHON_CMD (
    where python >nul 2>&1
    if not errorlevel 1 set "PYTHON_CMD=python"
)

echo ====================================
echo BangBangAgro Fruit Detect Launcher
echo ====================================
echo.

if not exist "%PYTHON_SCRIPT%" (
    echo [ERROR] Python service script not found:
    echo %PYTHON_SCRIPT%
    goto :end
)

if not defined PYTHON_CMD (
    echo [ERROR] Python command not found.
    echo Install Python or add py/python to PATH.
    goto :end
)

set "NO_PROXY=*"
set "no_proxy=*"
set "HTTP_PROXY="
set "HTTPS_PROXY="
set "http_proxy="
set "https_proxy="
set "ALL_PROXY="
set "all_proxy="

set "RECREATE_VENV=0"
if exist "%VENV_PYTHON%" (
    "%VENV_PYTHON%" -c "import sys; raise SystemExit(0 if sys.version_info >= (3, 10) else 1)" >nul 2>&1
    if errorlevel 1 set "RECREATE_VENV=1"
) else (
    set "RECREATE_VENV=1"
)

if "%RECREATE_VENV%"=="1" (
    if exist "%VENV_DIR%" (
        echo [1/4] Rebuilding local Python environment...
        rmdir /s /q "%VENV_DIR%"
    ) else (
        echo [1/4] Creating local Python environment...
    )
    %PYTHON_CMD% -m venv "%VENV_DIR%"
    if errorlevel 1 (
        echo [ERROR] Failed to create virtual environment:
        echo %VENV_DIR%
        goto :end
    )
)

echo [2/4] Checking Python dependencies...
"%VENV_PYTHON%" -c "import flask, flask_cors, ultralytics, torch, cv2, numpy" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Installing required packages, this may take a few minutes...
    "%VENV_PYTHON%" -m pip install --upgrade pip
    if errorlevel 1 (
        echo [ERROR] Failed to upgrade pip.
        goto :end
    )
    "%VENV_PYTHON%" -m pip install -r "%REQUIREMENTS_FILE%"
    if errorlevel 1 (
        echo [ERROR] Failed to install Python requirements.
        echo Check Python version, system proxy and network connectivity.
        goto :end
    )
)

echo [3/4] Starting YOLO service...
start "BangBangAgro-Python" cmd /k "cd /d \"%PYTHON_DIR%\" && \"%VENV_PYTHON%\" integrated_api_server.py"

echo [4/4] Waiting for service health check...
set /a WAIT_COUNT=0

:wait_loop
curl -s %HEALTH_URL% >nul 2>&1
if not errorlevel 1 goto :healthy
if %WAIT_COUNT% GEQ 15 goto :health_failed
timeout /t 2 /nobreak >nul
set /a WAIT_COUNT+=1
goto :wait_loop

:health_failed
    echo [WARN] Health check failed:
    echo %HEALTH_URL%
    echo Check the BangBangAgro-Python window for dependency or model errors.
    echo.
    echo Notes:
    echo - fruit-detect page uses Spring Boot ^> /crop-analysis
    echo - backend should be running at http://localhost:9090
    echo - frontend should be running at http://localhost:8080
    goto :after_health

:healthy
    echo [OK] YOLO service is running:
    echo %HEALTH_URL%
    echo.
    echo Open page:
    echo http://localhost:8080/fruit-detect
    start "" "http://localhost:8080/fruit-detect"

:after_health

echo.
echo Current project linkage:
echo - Python: TomatoDetection\integrated_api_server.py
echo - Python requirements: TomatoDetection\api_requirements.txt
echo - Python venv: TomatoDetection\.venv
echo - Backend: /crop-analysis/*
echo - Frontend: /fruit-detect
echo.

:end
pause
endlocal
