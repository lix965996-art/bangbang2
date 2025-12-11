@echo off
chcp 65001 >nul
echo ========================================
echo 🧪 API接口测试脚本
echo ========================================
echo.

set BASE_URL=http://localhost:9090

echo 📋 测试无需Token的接口（白名单）
echo ----------------------------------------
echo.

echo 1️⃣ 测试登录接口...
curl -X POST %BASE_URL%/user/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin\"}"
echo.
echo.

echo 2️⃣ 测试健康检查接口...
curl %BASE_URL%/health
echo.
echo.

echo 3️⃣ 测试农情日报接口...
curl %BASE_URL%/agri-report/daily
echo.
echo.

echo 4️⃣ 测试果蔬检测健康检查...
curl %BASE_URL%/fruit-detect/health
echo.
echo.

echo ========================================
echo ✅ 测试完成！
echo ========================================
echo.
echo 💡 提示：
echo - 如果接口返回401，说明需要Token验证
echo - 如果接口正常返回数据，说明在白名单中
echo.

pause
