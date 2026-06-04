# BangBangAgro 帮帮农智慧农业综合管理平台

BangBangAgro 是一个面向现代农业生产、设施农业管理和农产品经营场景的综合管理系统。项目将农田管理、IoT 设备接入、AI 作物识别、无人农场巡检、供应链业务、即时通讯和数据可视化整合到同一套 Web 平台中，适合用于课程设计、竞赛演示、农业信息化原型和二次开发学习。

项目目前由三部分组成：

- `springboot/`：Spring Boot 后端服务，提供业务 API、权限控制、WebSocket、AI 配置、IoT 设备控制和数据持久化能力。
- `vue/`：Vue 3 前端管理端，包含首页大屏、农田管理、无人农场、3D 数字孪生、聊天、供应链和系统管理等页面。
- `TomatoDetection/`：Python Flask AI 检测服务，提供番茄成熟度识别和多作物病害识别接口。

## 功能亮点

### 智慧农场驾驶舱

- 首页汇总农田数量、设备状态、待处理任务、预警事件和业务数据。
- 支持农田健康指数、环境趋势、近期任务和供应链指标可视化。
- 前端基于 Element Plus、ECharts、ECharts GL 等组件构建，适合后台管理与展示场景。

### 无人农场自主巡检

- 提供“无人农场总控仪表盘”和“自主巡检中心”。
- 支持按固定间隔采集传感器状态，结合规则和 AI Agent 生成巡检建议。
- 可记录巡检日志、待审批任务、自动执行操作和人工确认动作。
- 硬件尚未完全稳定时，可通过演示模式跑通“感知、判断、执行、留痕”的软件闭环；硬件联调完成后可切换到真实设备控制。

### AI 模型自由配置

- 系统支持在页面中配置不同 AI 服务提供方、模型名称、API 地址和密钥。
- 可按用户保存 AI 配置，避免把密钥硬编码到仓库。
- 支持通义千问、DeepSeek 或兼容 OpenAI Chat Completions 协议的模型服务。
- AI 能力用于农业问答、知识库检索、Agent 决策建议、巡检报告生成等场景。

### IoT 与设备控制

- 支持 OneNET / STM32 设备接入边界。
- 后端封装设备状态查询、传感器数据读取和水泵、补光、风扇等设备控制接口。
- 自动灌溉、巡检任务、传感器预警和人工审批可以联动。
- 关键控制操作建议在真实部署时增加角色授权、设备白名单和操作审计。

### 3D 数字孪生农场

- 基于 Three.js 构建 3D 农场场景。
- 支持农田地块、作物模型、天气效果、传感器状态和设备控制入口。
- 可展示作物生长阶段、环境状态和 AI 建议。
- 页面已加入资源释放逻辑，减少重复进入页面时的 GPU 和内存占用风险。

### 作物 AI 检测

- Python 检测服务基于 Flask 和 Ultralytics YOLO。
- 支持图片上传检测、结果图片返回、成熟度识别和多作物病害识别。
- 检测服务与后端/前端解耦，可单独启动或替换模型。

### 通讯中心

- 支持好友关系、私聊、群聊、消息列表和 WebSocket 实时消息。
- WebSocket 连接采用登录后认证消息方式，避免在 URL 中直接传递 token。
- 聊天页面处理了快速切换会话时的分页请求竞争问题。

### 供应链与业务管理

- 覆盖库存、采购、销售、线上销售、公告、统计分析等常见农业经营模块。
- Excel 导入、数据查询、状态流转和权限控制适合进一步扩展为完整业务后台。
- 部分高风险接口已补充事务、数据权限、导入校验和 404 处理。

## 技术栈

### 前端

- Vue 3
- Vite
- Vue Router
- Pinia
- Element Plus
- Axios
- ECharts / ECharts GL
- Leaflet / 高德地图
- Three.js
- Electron 打包支持

### 后端

- Java 17
- Spring Boot 3
- MyBatis-Plus
- MySQL
- Springdoc OpenAPI
- WebSocket
- JWT
- Apache POI
- Hutool
- Spring AI / Spring AI Alibaba 相关依赖

### AI 服务

- Python 3
- Flask
- Flask-Cors
- Ultralytics YOLO
- Pillow / OpenCV 相关图像处理依赖

## 目录结构

```text
.
├── README.md
├── PROJECT.md
├── springboot/
│   ├── pom.xml
│   ├── sql/
│   └── src/
│       ├── main/java/com/farmland/intel/
│       └── main/resources/
├── vue/
│   ├── package.json
│   ├── vite.config.js
│   ├── electron/
│   ├── public/
│   └── src/
└── TomatoDetection/
    ├── api_requirements.txt
    ├── integrated_api_server.py
    ├── models/              # 本地模型目录，公开仓库默认不提交
    └── disease_weights/     # 本地病害模型目录，公开仓库默认不提交
```

## 大型资源说明

为了控制仓库体积并避免把本地演示材料误传到公开仓库，以下内容默认不提交到 GitHub：

- `TomatoDetection/models/`：成熟度检测模型权重。
- `TomatoDetection/disease_weights/`：多作物病害检测模型权重。
- `TomatoDetection/Font/`：本地字体资源。
- `vue/public/models/`：3D 数字孪生使用的 `.glb` 模型。
- `vue/public/textures/`：3D 场景贴图、天空盒和天气效果资源。
- 本地 `.env`、`application-local.yml`、日志、构建产物、PPT、PDF、Word 文档和压缩包。

如果需要完整演示 AI 检测或 3D 数字孪生，请在本地保留这些资源，或从可信位置下载后放回对应目录。源码本身不依赖这些文件完成编译，但缺少资源时相关页面或检测接口只能作为代码结构参考，不能完整展示最终效果。

## 环境要求

| 组件 | 建议版本 |
| --- | --- |
| JDK | 17 或以上 |
| Maven | 3.8 或以上 |
| Node.js | 18 或以上 |
| npm | 9 或以上 |
| Python | 3.9 或以上 |
| MySQL | 8.0 或以上 |

## 本地启动

### 1. 准备数据库

创建 MySQL 数据库：

```sql
CREATE DATABASE `smart-agriculture` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后根据实际需要导入 `springboot/sql/` 目录下的初始化脚本。不同分支和开发阶段的 SQL 文件可能对应不同模块，建议先导入基础表，再导入业务模块表。

### 2. 配置后端环境

复制后端本地配置示例：

```bash
cp springboot/src/main/resources/application-local.yml.example springboot/src/main/resources/application-local.yml
```

修改 `application-local.yml` 或设置环境变量：

| 变量 | 说明 |
| --- | --- |
| `DB_HOST` | MySQL 地址 |
| `DB_PORT` | MySQL 端口 |
| `DB_NAME` | 数据库名 |
| `DB_USERNAME` | 数据库用户名 |
| `DB_PASSWORD` | 数据库密码 |
| `JWT_SECRET` | JWT 签名密钥，建议至少 32 位随机字符串 |
| `AMAP_JS_KEY` | 高德地图 Web JS Key |
| `AMAP_WEB_KEY` | 高德地图 Web 服务 Key |
| `AMAP_JS_SECURITY_KEY` | 高德地图安全密钥 |
| `PYTHON_API_URL` | Python AI 检测服务地址 |
| `ONENET_AUTHOR_KEY` | OneNET 鉴权 Key |
| `ONENET_PRODUCT_ID` | OneNET 产品 ID |
| `ONENET_DEVICE_NAME` | OneNET 设备名称 |
| `IOT_CALLBACK_SECRET` | IoT 回调共享密钥 |

不要把真实的 `application-local.yml`、数据库密码、地图 Key、OneNET Key 或 AI Key 提交到 GitHub。

### 3. 启动后端

```bash
cd springboot
mvn spring-boot:run
```

默认地址：

- API 服务：`http://localhost:9090`
- Swagger UI：`http://localhost:9090/swagger-ui/index.html`

### 4. 配置前端环境

复制前端环境变量示例：

```bash
cd vue
cp .env.example .env
```

常用配置：

| 变量 | 说明 |
| --- | --- |
| `VUE_APP_API_BASE_URL` | 后端 API 地址 |
| `VUE_APP_AMAP_JS_KEY` | 高德地图 JS Key |
| `VUE_APP_AMAP_SECURITY_CODE` | 高德地图安全密钥 |
| `VUE_APP_SHOWCASE_MODE` | 展示模式，可用于控制菜单范围 |

### 5. 启动前端

```bash
cd vue
npm install
npm run dev
```

Vite 会输出实际访问地址，通常是：

```text
http://localhost:8080
```

### 6. 启动 Python AI 检测服务

```bash
cd TomatoDetection
pip install -r api_requirements.txt
python integrated_api_server.py
```

默认地址：

```text
http://localhost:5000
```

## 常用命令

### 后端编译

```bash
cd springboot
mvn -q -DskipTests compile
```

### 前端构建

```bash
cd vue
npm run build
```

### Python 语法检查

```bash
python -m py_compile TomatoDetection/integrated_api_server.py
```

## 安全说明

- 本仓库不应包含任何真实账号、手机号、身份证件、数据库密码、地图 Key、AI Key、OneNET Key 或本地路径。
- `.env`、`.env.production`、`application-local.yml`、日志、构建产物、比赛文档、PPT、PDF、Word 文档和压缩包默认不会提交。
- 生产环境必须设置强随机 `JWT_SECRET`，并为数据库、IoT 回调和设备控制接口配置权限边界。
- 设备控制类接口建议只允许管理员或授权角色调用。
- AI 模型密钥建议由用户在系统内单独配置，并在数据库层面做好加密或脱敏展示。

## 开发状态

这是一个持续迭代中的智慧农业综合项目。当前代码重点覆盖演示闭环和核心业务链路，部分硬件能力可通过演示模式联调，真实设备接入需要根据具体 OneNET 产品、STM32 固件和传感器协议继续配置。

## 许可

本项目仅用于学习、课程设计、竞赛展示和技术交流。若用于生产环境，请自行完成安全加固、权限审计、压力测试和合规评估。
