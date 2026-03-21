# BangBangAgro 智慧农场系统 - 云端部署指南

## 📋 系统架构
- **前端**: Vue 2 + Element UI + ECharts + 高德地图
- **后端**: Spring Boot 2 + MyBatis Plus + MySQL
- **部署端口**: 前端 8080 / 后端 9090

---

## 🚀 快速部署步骤

### 一、环境准备

#### 1. 服务器要求
- **操作系统**: Linux (推荐 Ubuntu 20.04 / CentOS 7+) 或 Windows Server
- **内存**: 最低 2GB，推荐 4GB+
- **磁盘**: 最低 10GB 可用空间
- **Java**: JDK 1.8+
- **Node.js**: v14.0+
- **MySQL**: 5.7+ 或 8.0+
- **Nginx**: 用于反向代理和静态资源托管

#### 2. 必需软件安装
```bash
# Ubuntu 示例
sudo apt update
sudo apt install openjdk-8-jdk nodejs npm mysql-server nginx -y

# 验证安装
java -version
node -v
mysql --version
nginx -v
```

---

### 二、数据库配置

#### 1. 创建数据库
```sql
CREATE DATABASE smart_agriculture 
  DEFAULT CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- 创建用户（可选，推荐）
CREATE USER 'agro_user'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON smart_agriculture.* TO 'agro_user'@'%';
FLUSH PRIVILEGES;
```

#### 2. 导入数据结构
```bash
mysql -u root -p smart_agriculture < database/schema.sql
```

---

### 三、后端部署（Spring Boot）

#### 1. 配置环境变量
创建 `/etc/environment` 或使用 `.env` 文件：
```bash
# 数据库配置
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=smart_agriculture
export DB_USERNAME=agro_user
export DB_PASSWORD=your_secure_password

# 文件上传路径
export UPLOAD_PATH=/var/www/uploads

# 服务器端口
export SERVER_PORT=9090
```

#### 2. 构建后端应用
```bash
cd springboot
mvn clean package -DskipTests

# 或使用已提供的jar包
# 生成的文件在 target/smart-agriculture-0.0.1-SNAPSHOT.jar
```

#### 3. 运行后端服务
```bash
# 开发环境直接运行
java -jar target/smart-agriculture-0.0.1-SNAPSHOT.jar

# 生产环境后台运行
nohup java -jar target/smart-agriculture-0.0.1-SNAPSHOT.jar > /var/log/smart-agro-backend.log 2>&1 &

# 或使用 systemd 服务（推荐）
sudo nano /etc/systemd/system/smart-agro.service
```

#### systemd 服务配置示例：
```ini
[Unit]
Description=Smart Agriculture Backend Service
After=network.target mysql.service

[Service]
Type=simple
User=www-data
WorkingDirectory=/opt/smart-agriculture
Environment="DB_HOST=localhost"
Environment="DB_USERNAME=agro_user"
Environment="DB_PASSWORD=your_password"
ExecStart=/usr/bin/java -jar /opt/smart-agriculture/smart-agriculture.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl start smart-agro
sudo systemctl enable smart-agro
sudo systemctl status smart-agro
```

---

### 四、前端部署（Vue）

#### 1. 配置生产环境API地址
创建 `vue/.env.production`：
```env
VUE_APP_API_BASE_URL=http://your-server-ip:9090
# 或使用域名
VUE_APP_API_BASE_URL=https://api.yourdomain.com
```

#### 2. 构建前端静态文件
```bash
cd vue
npm install
npm run build

# 构建完成后，dist/ 目录包含所有静态文件
```

#### 3. 配置 Nginx
创建 `/etc/nginx/sites-available/smart-agro`：
```nginx
server {
    listen 80;
    server_name your-domain.com;  # 改为你的域名或IP

    # 前端静态文件
    root /var/www/smart-agro/dist;
    index index.html;

    # 支持Vue Router的history模式
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 反向代理后端API
    location /api/ {
        proxy_pass http://localhost:9090/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    # 文件上传目录
    location /uploads/ {
        alias /var/www/uploads/;
        autoindex off;
    }

    # Gzip压缩
    gzip on;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml;
    gzip_min_length 1000;
}
```

启用站点并重启Nginx：
```bash
sudo ln -s /etc/nginx/sites-available/smart-agro /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

---

### 五、HTTPS配置（可选但推荐）

使用 Let's Encrypt 免费证书：
```bash
sudo apt install certbot python3-certbot-nginx -y
sudo certbot --nginx -d your-domain.com
sudo systemctl reload nginx
```

---

## ✅ 部署检查清单

### 前端检查
- [ ] `vue/.env.production` 中的API地址已更新
- [ ] `npm run build` 构建成功
- [ ] dist文件已上传到服务器 `/var/www/smart-agro/dist`
- [ ] Nginx配置正确
- [ ] 浏览器访问正常，无控制台错误

### 后端检查
- [ ] 数据库已创建并导入表结构
- [ ] `application.yml` 环境变量已配置
- [ ] jar包构建成功
- [ ] 后端服务启动成功（检查端口9090）
- [ ] API接口可访问：`curl http://localhost:9090/health`

### 响应式布局检查
- [ ] PC端（1920x1080）显示正常
- [ ] 平板端（768-1024px）布局适配
- [ ] 移动端（<768px）侧边栏可收起
- [ ] 关键页面（Farmland、BigScreen）自适应

### 安全配置
- [ ] MySQL密码已修改
- [ ] 上传目录权限正确（755）
- [ ] 防火墙已配置
- [ ] CORS跨域正确配置
- [ ] API密钥安全存储

---

## 🔧 常见问题排查

### 1. 前端无法连接后端
- 检查 `.env.production` 中的API地址
- 检查防火墙是否开放9090端口
- 检查Nginx反向代理配置
- 查看浏览器控制台Network错误

### 2. 数据库连接失败
```bash
# 检查MySQL服务状态
sudo systemctl status mysql

# 检查环境变量
echo $DB_HOST $DB_USERNAME

# 查看后端日志
tail -f /var/log/smart-agro-backend.log
```

### 3. 文件上传失败
```bash
# 确保上传目录存在且有写权限
sudo mkdir -p /var/www/uploads
sudo chown -R www-data:www-data /var/www/uploads
sudo chmod -R 755 /var/www/uploads
```

### 4. 移动端显示异常
- 检查 `Manage.vue` 响应式CSS是否正确
- 清除浏览器缓存
- 使用Chrome DevTools移动设备模拟器测试

---

## 📊 性能优化建议

1. **启用Gzip压缩**（已在Nginx配置中）
2. **开启浏览器缓存**
3. **使用CDN加速静态资源**
4. **数据库索引优化**
5. **Redis缓存热点数据**

---

## 🔐 安全加固建议

1. 修改默认端口
2. 配置HTTPS（必须）
3. 启用防火墙规则
4. 定期备份数据库
5. API密钥使用环境变量管理
6. 设置Nginx速率限制

---

## 📞 技术支持

- **系统版本**: v1.0
- **更新日期**: 2025-12-20
- **联系方式**: [根据实际填写]

---

**部署完成后，请访问：**
- 前端页面：`http://your-domain.com`
- 后端健康检查：`http://your-domain.com/api/health`

**默认管理员账号**（首次登录后请立即修改）：
- 用户名：admin
- 密码：[根据实际系统设置]
