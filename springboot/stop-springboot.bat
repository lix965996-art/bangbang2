@echo off
chcp 65001 >nul
echo ====================================
echo   停止SpringBoot服务
echo ====================================
echo.

echo 正在查找占用9090端口的进程...
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :9090 ^| findstr LISTENING') do (
    echo 发现进程 PID: %%a，正在停止...
    taskkill /F /PID %%a
)

echo.
echo ====================================
echo SpringBoot服务已停止
echo ====================================
pause
