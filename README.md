# 智慧农场管理系统

一个面向农场经营与数字化管理的全栈项目，包含 Spring Boot 后端和 Vue 2 前端。项目覆盖农田管理、库存采购、经营分析、环境监测、地理地图、果蔬识别和 AI 助手等模块，适合课程设计、毕业设计和本地演示。

## 项目定位

这个项目的核心目标是把农场日常经营中分散的数据和流程整合到一个系统里，提供：

- 农田与作物信息管理
- 采购、库存、销售台账管理
- 环境监测与可视化大屏
- 高德地图农场地理展示
- 果蔬识别与检测分析
- 基于通义千问的问答与分析能力

## 技术栈

- 前端：Vue 2、Vue Router、Vuex、Element UI、ECharts、Three.js、高德地图 JS API
- 后端：Spring Boot 2.5、MyBatis Plus、MySQL、WebSocket、Actuator
- AI 与外部服务：通义千问、Amap、Python 检测服务、OneNET

## 目录结构

```text
.
├─springboot/                后端工程
│  ├─src/main/java/          Java 业务代码
│  ├─src/main/resources/     配置、Mapper、静态资源
│  ├─sql/                    初始化 SQL
│  └─pom.xml                 Maven 配置
├─vue/                       前端工程
│  ├─src/                    页面、组件、路由、状态管理
│  ├─public/                 静态入口文件
│  └─package.json            npm 依赖与脚本
├─check-env.ps1              环境变量检查脚本
├─环境变量配置指南.md         环境变量说明
├─简单启动指南.md             快速启动说明
└─项目简介.md                项目中文介绍
```

## 主要功能

- 用户、角色、公告等后台管理
- 农田档案、种植信息和经营统计
- 采购、库存、销售等业务模块
- 农场环境监测与可视化大屏
- 农场二维地理地图与三维地图展示
- 果蔬识别、检测结果分析
- AI 助手问答、天气与位置相关能力

## 运行环境

- JDK 8
- Maven 3.6+
- Node.js 14 或 16
- MySQL 5.7/8.0

## 环境变量

当前项目已经整理为通过环境变量读取通义千问和高德地图配置。

后端至少需要：

- `QWEN_API_KEY`
- `AMAP_JS_KEY` 或 `AMAP_WEB_KEY` 至少一个
- `AMAP_JS_SECURITY_KEY` 建议配置

前端地图页面需要：

- `VUE_APP_AMAP_JS_KEY`
- `VUE_APP_AMAP_SECURITY_CODE`

可直接参考：

- `./.env.example`
- `./vue/.env.example`
- `./环境变量配置指南.md`

## 快速启动

### 1. 初始化数据库

- 创建数据库：`smart-agriculture`
- 执行脚本：`springboot/sql/init_map_coordinates.sql`

### 2. 启动后端

```bash
cd springboot
mvn spring-boot:run
```

默认地址：

```text
http://localhost:9090
```

### 3. 启动前端

```bash
cd vue
npm install
npm run serve
```

默认地址：

```text
http://localhost:8080
```

## 已整理的配置说明

- `QWEN` 和 `AMAP` 相关真实密钥不再保留在源码中
- 地图前端与后端均改为读取环境变量
- OneNET 固定 `author-key` 目前保留原状，未迁移到环境变量

## 验证情况

- 后端已验证可通过环境变量正常启动
- 前端高德地图页面已验证可正常渲染
- 前后端构建已通过

## 适用场景

- 智慧农业课程设计
- 毕业设计项目整理与展示
- 农业信息化系统原型演示
- 个人全栈项目作品集

## 相关文档

- `简单启动指南.md`
- `环境变量配置指南.md`
- `编译成功指南.md`
- `改进方案.md`
- `项目简介.md`
