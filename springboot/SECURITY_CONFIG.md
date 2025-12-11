# 🔐 SpringBoot 安全配置说明

## 📋 JWT拦截器配置

项目使用 **JWT拦截器** 进行身份验证，配置文件位于：
```
src/main/java/com/farmland/intel/config/InterceptorConfig.java
```

---

## ✅ 无需Token验证的接口（白名单）

### 1. **用户认证相关**
```
/user/login       - 用户登录
/user/register    - 用户注册
```

### 2. **文件操作**
```
/**/export        - 数据导出
/**/import        - 数据导入
/file/**          - 文件上传下载
```

### 3. **业务模块**（根据需要调整）
```
/inventory/**     - 库存管理
/purchase/**      - 采购管理
/sales/**         - 销售管理
/order/**         - 订单管理
/onlineSale/**    - 在线销售
```

### 4. **API文档**
```
/swagger**/**     - Swagger UI
/webjars/**       - Swagger资源
/v3/**            - Swagger API
/doc.html         - Knife4j文档
```

### 5. **物联网和天气服务**
```
/aether/**        - 物联网设备
/amap/**          - 高德地图API
```

### 6. **AI服务**
```
/api/chat/**      - AI聊天
/fruit-detect/**  - 果蔬检测
```

### 7. **健康检查和监控**
```
/health           - 健康检查
/actuator/**      - Spring Actuator
/agri-report/**   - 农情日报
```

---

## 🔒 需要Token验证的接口

除了上述白名单接口外，所有其他接口都需要在请求头中携带有效的JWT Token：

```http
GET /api/some-protected-endpoint
Headers:
  token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

---

## 🛠️ JWT拦截器工作流程

1. **请求到达** → 检查路径是否在白名单中
2. **白名单路径** → 直接放行
3. **非白名单路径** → 检查Token
   - Token为空 → 返回401错误
   - Token无效 → 返回401错误
   - Token有效 → 放行请求

---

## 📝 如何添加新的白名单接口

在 `InterceptorConfig.java` 中添加：

```java
@Override
protected void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(jwtInterceptor())
            .addPathPatterns("/**")
            // 添加你的新接口
            .excludePathPatterns("/your-new-api/**");
    super.addInterceptors(registry);
}
```

---

## 🔍 使用 @AuthAccess 注解

如果某个特定方法需要跳过Token验证，可以使用 `@AuthAccess` 注解：

```java
@RestController
@RequestMapping("/api")
public class YourController {
    
    @AuthAccess  // 跳过Token验证
    @GetMapping("/public-endpoint")
    public Result publicMethod() {
        return Result.success("无需登录即可访问");
    }
    
    @GetMapping("/protected-endpoint")
    public Result protectedMethod() {
        return Result.success("需要登录才能访问");
    }
}
```

---

## ⚠️ 安全注意事项

### 1. **生产环境配置**
- 不要将敏感业务接口加入白名单
- 定期审查白名单配置
- 使用强密码生成JWT密钥

### 2. **Token管理**
- Token有效期建议设置为2-24小时
- 实现Token刷新机制
- 登出时清除客户端Token

### 3. **CORS配置**
确保CORS配置正确，避免跨域安全问题：

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:8080", "http://47.108.93.78")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

---

## 🧪 测试接口访问

### 测试登录接口（无需Token）
```bash
curl -X POST http://localhost:9090/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

### 测试健康检查（无需Token）
```bash
curl http://localhost:9090/health
```

### 测试受保护接口（需要Token）
```bash
curl http://localhost:9090/user/info \
  -H "token: YOUR_JWT_TOKEN"
```

---

## 📊 当前配置总结

| 接口类型 | 路径模式 | 是否需要Token |
|---------|---------|--------------|
| 用户登录 | `/user/login` | ❌ 否 |
| 用户注册 | `/user/register` | ❌ 否 |
| 健康检查 | `/health` | ❌ 否 |
| 农情日报 | `/agri-report/**` | ❌ 否 |
| 果蔬检测 | `/fruit-detect/**` | ❌ 否 |
| 物联网 | `/aether/**` | ❌ 否 |
| 用户信息 | `/user/info` | ✅ 是 |
| 农田管理 | `/farmland/**` | ✅ 是 |
| 其他业务 | 其他路径 | ✅ 是 |

---

## 🔄 配置更新记录

### 2025-12-04
- ✅ 添加 `/agri-report/**` 到白名单（农情日报）
- ✅ 添加 `/fruit-detect/**` 到白名单（果蔬检测）
- ✅ 添加 `/health` 和 `/actuator/**` 到白名单（健康检查）
- ✅ 优化配置注释，提高可读性

---

## 📞 问题排查

### Q1: 登录接口返回401
**A:** 检查 `/user/login` 是否在白名单中

### Q2: 其他接口无法访问
**A:** 检查请求头是否携带了有效的Token

### Q3: Token验证失败
**A:** 检查Token是否过期，或用户密码是否已修改

### Q4: 跨域问题
**A:** 检查CORS配置，确保允许前端域名访问
