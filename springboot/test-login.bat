@echo off
chcp 65001 >nul
echo ========================================
echo 🧪 测试登录接口
echo ========================================
echo.

set BASE_URL=http://localhost:9090

echo 📝 测试信息：
echo URL: %BASE_URL%/user/login
echo 用户名: admin
echo 密码: 12345
echo.
echo ----------------------------------------
echo 🚀 发送请求中...
echo ----------------------------------------
echo.

curl -X POST %BASE_URL%/user/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"12345\"}"

echo.
echo.
echo ----------------------------------------
echo 📋 结果说明：
echo ----------------------------------------
echo.
echo ✅ 如果返回 code: "200" 和 token，说明：
echo    - 登录成功
echo    - 安全配置生效
echo    - 接口无需Token验证
echo.
echo ❌ 如果返回 code: "401"，说明：
echo    - 安全配置未生效
echo    - 需要检查配置并重启服务
echo.
echo ❌ 如果返回 code: "600" 或其他错误，说明：
echo    - 用户名或密码错误
echo    - 数据库连接问题
echo.
echo ========================================

pause
