@echo off
chcp 65001 >nul
echo ========================================
echo 🔍 验证安全配置修复
echo ========================================
echo.

echo 📋 检查修改的文件...
echo.

echo 1️⃣ 检查 InterceptorConfig.java
findstr /C:"implements WebMvcConfigurer" src\main\java\com\farmland\intel\config\InterceptorConfig.java >nul
if %errorlevel% equ 0 (
    echo    ✅ InterceptorConfig 已修复
) else (
    echo    ❌ InterceptorConfig 未修复
)

echo 2️⃣ 检查 SwaggerConfig.java
findstr /C:"implements WebMvcConfigurer" src\main\java\com\farmland\intel\config\SwaggerConfig.java >nul
if %errorlevel% equ 0 (
    echo    ✅ SwaggerConfig 已修复
) else (
    echo    ❌ SwaggerConfig 未修复
)

echo.
echo ========================================
echo 📝 修复说明
echo ========================================
echo.
echo 问题原因：
echo   多个配置类继承 WebMvcConfigurationSupport
echo   导致配置冲突，拦截器不生效
echo.
echo 解决方案：
echo   改为实现 WebMvcConfigurer 接口
echo   避免配置冲突
echo.
echo ========================================
echo 🚀 下一步操作
echo ========================================
echo.
echo 1. 重新编译项目
echo    mvn clean compile
echo.
echo 2. 重启SpringBoot应用
echo    mvn spring-boot:run
echo.
echo 3. 测试登录接口（无需Token）
echo    curl -X POST http://localhost:9090/user/login ^
echo      -H "Content-Type: application/json" ^
echo      -d "{\"username\":\"admin\",\"password\":\"admin\"}"
echo.
echo 详细说明请查看: SECURITY_FIX.md
echo ========================================
echo.

pause
