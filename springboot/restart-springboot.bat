@echo off
chcp 65001 >nul
echo ====================================
echo   SpringBoot 自动重启脚本
echo ====================================
echo.

echo [1] 正在停止占用9090端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9090 ^| findstr LISTENING') do (
    echo 发现进程 PID: %%a
    taskkill /F /PID %%a 2>nul
)
echo.

echo [2] 等待端口释放...
timeout /t 2 /nobreak >nul
echo.

echo [3] 正在启动SpringBoot应用...
cd /d "%~dp0"
start "SpringBoot服务" cmd /k "mvn spring-boot:run"

echo.
echo ====================================
echo SpringBoot正在启动中...
echo 请查看新打开的窗口查看启动日志
echo ====================================
pause
