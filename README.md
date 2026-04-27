智慧农业综合管理平台
====================

基于 Spring Boot + Vue + Python AI 的现代农业管理系统，集成物联网设备监控、人工智能检测与智能 Agent 辅助决策。

项目概述
--------

本项目是一个面向农业生产全流程的综合管理平台，涵盖农田管理、环境监测、果实 AI 检测、库存管理、市场分析等业务模块，并通过 OneNET 物联网平台连接 STM32 硬件设备实现实时数据采集与自动灌溉控制，同时接入通义千问大模型提供智能问答与建议。

**核心亮点**

- **AI 果实检测**：基于 YOLOv8 的果实成熟度识别与分类
- **物联网联动**：对接 OneNET 平台，实现传感器数据上报与水泵自动控制
- **智能 Agent**：接入通义千问大模型，提供农业问答与决策建议
- **3D 农场可视化**：Three.js 构建 3D 农场场景与高德地图集成

技术栈
------

| 层次 | 技术选型 |
| :--- | :--- |
| 后端 | Spring Boot 2.5.12、MyBatis-Plus 3.5.1、MySQL 8.0 |
| 前端 | Vue 2.6、Element UI、ECharts、Three.js、Vue Router |
| AI 检测 | Python 3.x、PyTorch 1.9、YOLOv8 (Ultralytics)、OpenCV |
| AI 对话 | 通义千问 Qwen-Max（DashScope API） |
| 认证鉴权 | JWT、Spring Interceptor |
| IoT 平台 | 移动 OneNET（HTTP API） |
| 其他 | Hutool、Apache POI、Swagger、WebSocket、HikariCP |

功能模块
--------

| 模块 | 说明 |
| :--- | :--- |
| 用户管理 | 注册、登录、密码修改、角色权限分配（RBAC） |
| 农田管理 | 农田信息维护、作物配置、生长阶段记录 |
| 智能监控 | 实时采集 OneNET 传感器数据，WebSocket 推送 |
| 果实检测 | 上传作物图像，AI 模型自动识别成熟度并返回结果 |
| 库存管理 | 农产品入库/出库操作、出入库记录查询 |
| 市场中心 | 价格行情展示、供需分析图表 |
| 供应链管理 | 供应商与采购商信息管理 |
| 销售管理 | 订单录入、销售统计 |
| 采购管理 | 采购申请、采购单管理 |
| 在线售卖 | 农产品上架、在售商品管理 |
| 经营分析 | 多维度数据统计与 ECharts 可视化报表 |
| 系统设置 | 菜单管理、公告发布、参数配置 |
| 自动灌溉 | 根据土壤/空气湿度、温度阈值自动控制水泵启停 |
| 智能 Agent | 基于通义千问的农业领域智能问答 |

项目结构
--------

```text
bang-bang-agro-master/
├── springboot/                  # Spring Boot 后端服务
│   ├── src/main/java/com/farmland/intel/
│   │   ├── agent/              # Agent 智能体定义（动作、计划、结果）
│   │   ├── common/             # 公共常量、枚举、统一响应
│   │   ├── config/             # 跨域、JWT拦截器、MyBatis、Swagger 等
│   │   ├── controller/         # REST API 控制器
│   │   ├── entity/             # 数据库实体类
│   │   ├── exception/          # 全局异常处理
│   │   ├── mapper/             # MyBatis Mapper 接口
│   │   ├── service/            # 业务逻辑层
│   │   └── utils/              # 工具类（Token、日志等）
│   ├── src/main/resources/     # 配置文件（application.yml 等）
│   ├── sql/init_map_coordinates.sql  # 地图坐标初始化脚本
│   └── pom.xml                 # Maven 依赖
├── vue/                         # Vue 前端应用
│   ├── src/
│   │   ├── components/         # 公共组件（Aside 侧边栏、GlobalLoading 等）
│   │   ├── config/             # 环境变量配置
│   │   ├── router/             # 路由定义
│   │   ├── views/              # 页面组件（各功能模块视图）
│   │   └── App.vue             # 根组件
│   ├── public/                 # 静态资源
│   ├── package.json            # 前端依赖
│   └── vue.config.js           # Vue CLI 配置
├── TomatoDetection/             # Python AI 图像检测模块
│   ├── api_server.py           # Flask HTTP 服务入口
│   ├── detect_tools.py         # 图像检测工具
│   ├── models/                 # AI 模型权重文件
│   ├── requirements.txt        # Python 依赖列表
│   └── MainProgram.py          # 主程序入口
├── HongMeng/                    # HarmonyOS 适配版本（可选）
├── logs/                        # 运行日志（已在 .gitignore 中排除）
├── .gitignore                   # Git 忽略规则
└── README.md                    # 本文件
```

环境要求
--------

| 环境 | 版本要求 |
| :--- | :--- |
| JDK | 1.8+ |
| Node.js | 14+ |
| Python | 3.7+ |
| MySQL | 8.0+ |
| Maven | 3.6+ |

快速开始
--------

**1. 数据库初始化**

创建数据库并导入初始化脚本：

```sql
CREATE DATABASE IF NOT EXISTS smart_agriculture DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

执行 `springboot/sql/init_map_coordinates.sql` 中的 SQL 语句完成表结构初始化。

**2. 后端启动**

```bash
cd springboot
mvn clean package -DskipTests
mvn spring-boot:run
```

后端默认运行在 `http://localhost:9090`。

**3. 前端启动**

```bash
cd vue
npm install
npm run serve
```

前端默认运行在 `http://localhost:8080`，会自动代理请求到后端。

**4. AI 检测服务启动**

```bash
cd TomatoDetection
pip install -r requirements.txt
python integrated_api_server.py
```

AI 检测服务默认运行在 `http://localhost:5000`，提供成熟度检测、病虫害检测、双检分析三个接口。

> 必须先启动 AI 检测服务，再启动后端，否则成熟度检测功能无法使用。

**5. 配置说明**

所有外部配置均通过环境变量注入，参考 `springboot/src/main/resources/application.yml` 中的占位符格式配置以下变量：

| 变量名 | 说明 | 示例值 |
| :--- | :--- | :--- |
| `DB_HOST` | 数据库地址 | `localhost` |
| `DB_PORT` | 数据库端口 | `3306` |
| `DB_NAME` | 数据库名 | `smart-agriculture` |
| `DB_USERNAME` | 数据库用户名 | `root` |
| `DB_PASSWORD` | 数据库密码 | `123456` |
| `QWEN_API_KEY` | 通义千问 API Key | （必填） |
| `AMAP_JS_KEY` | 高德地图 JS API Key | （选填） |
| `ONENET_NEW_PRODUCT_ID` | OneNET 新设备 ProductId | （IoT 部署时必填） |

API 文档
--------

后端集成了 Swagger，启动后可通过 `http://localhost:9090/swagger-ui.html` 查看和测试全部接口。

主要接口概览：

| 路径前缀 | 模块 |
| :--- | :--- |
| `/api/user` | 用户认证与管理 |
| `/api/farmland` | 农田管理 |
| `/api/monitor` | 设备与传感器监控 |
| `/api/detection` | AI 果实检测 |
| `/api/inventory` | 库存管理 |
| `/api/market` | 市场价格信息 |
| `/api/supply` / `/api/sales` / `/api/purchase` | 供应链相关 |
| `/api/statistic` | 数据统计分析 |
| `/api/chat` | 智能 Agent 问答 |
| `/api/notenet` | OneNET 物联网设备 |

架构设计
--------

**后端分层**

```text
Controller（REST 接口层）
    -> Service（业务逻辑层，接口 + 实现分离）
        -> Mapper（数据访问层，MyBatis-Plus）
            -> Entity（数据库映射）
```

**前后端交互**

```text
Vue 页面 -> Axios 封装（统一拦截 Token / 错误）-> Spring Boot Controller
                                    <- Result{code, data, message} <-
```

**AI 检测流程**

```text
前端上传图片 -> Spring Boot 转发 -> Flask API -> YOLOv8 推理 -> JSON 结果返回
```

注意事项
--------

- 配置文件中的敏感信息（数据库密码、API Key）应通过环境变量注入，不要直接提交明文
- AI 模型权重文件体积较大，建议使用 Git LFS 或单独分发
- 日志文件位于 `logs/` 目录，默认按日期滚动存储，单文件最大 10MB，保留 30 天
- WebSocket 用于实时推送传感器数据，请确保浏览器兼容
