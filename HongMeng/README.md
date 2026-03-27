# 智慧农场管理系统 - 鸿蒙版

## 项目简介

本项目是智慧农场管理系统的鸿蒙原生版本，使用 ArkTS + ArkUI 开发，用于参加服务创新设计大赛。

## 技术栈

- **开发语言**: ArkTS (TypeScript 超集)
- **UI框架**: ArkUI (鸿蒙原生UI框架)
- **最低SDK版本**: API 9
- **目标设备**: 手机、平板

## 项目结构

```
HongMeng/
├── entry/                          # 主入口模块
│   ├── src/main/
│   │   ├── ets/                    # ArkTS 源码
│   │   │   ├── common/             # 公共模块
│   │   │   │   ├── ApiConfig.ets   # API配置
│   │   │   │   ├── HttpClient.ets  # HTTP请求工具
│   │   │   │   ├── StorageUtil.ets # 本地存储工具
│   │   │   │   └── CommonStyles.ets# 通用样式
│   │   │   ├── components/         # 自定义组件
│   │   │   │   ├── HeaderBar.ets   # 标题栏
│   │   │   │   ├── TabBar.ets      # 底部导航
│   │   │   │   ├── DataCard.ets    # 数据卡片
│   │   │   │   ├── StatusBadge.ets # 状态标签
│   │   │   │   ├── AlertCard.ets   # 预警卡片
│   │   │   │   └── LoadingDialog.ets# 加载对话框
│   │   │   ├── pages/              # 页面
│   │   │   │   ├── LoginPage.ets   # 登录页
│   │   │   │   ├── MainPage.ets    # 主页(含Tab)
│   │   │   │   ├── DashboardPage.ets# 数据大屏
│   │   │   │   ├── FarmlandPage.ets# 农田列表
│   │   │   │   ├── FarmlandDetailPage.ets# 农田详情
│   │   │   │   ├── InventoryPage.ets# 库存管理
│   │   │   │   ├── FruitDetectPage.ets# 果蔬识别
│   │   │   │   ├── AIAssistantPage.ets# AI助手
│   │   │   │   ├── AlertPage.ets   # 预警中心
│   │   │   │   ├── HealthPage.ets  # 健康指数
│   │   │   │   └── SettingsPage.ets# 设置页
│   │   │   └── entryability/
│   │   │       └── EntryAbility.ets# 应用入口
│   │   └── resources/              # 资源文件
│   │       ├── base/
│   │       │   ├── element/        # 颜色、字符串
│   │       │   └── profile/        # 配置文件
│   │       ├── zh_CN/              # 中文资源
│   │       └── en_US/              # 英文资源
│   └── module.json5                # 模块配置
├── build-profile.json5             # 构建配置
├── oh-package.json5               # 依赖配置
└── hvigorfile.ts                  # 构建脚本
```

## 功能模块

### 1. 用户认证
- 用户登录/注册
- Token 管理
- 自动登录

### 2. 首页
- 数据概览（农田总数、面积、存栏等）
- 今日预警提醒
- 快捷操作入口

### 3. 农田管理
- 农田列表展示
- 农田详情查看/编辑
- 环境参数显示（温度、湿度、光照等）

### 4. 数据大屏
- 实时环境监测
- 温度/湿度分布图
- 农田状态一览

### 5. 库存管理
- 库存列表
- 库存预警
- 安全库存提示

### 6. 果蔬识别
- 图片选择/拍照
- AI 识别果蔬成熟度
- 识别结果展示

### 7. AI 助手
- 智能问答
- 农业知识咨询
- 实时环境数据获取

### 8. 预警中心
- 今日任务
- 全部预警
- 预警处理

### 9. 健康指数
- 农田健康评分
- 等级分类展示

## 后端 API 对接

应用需要连接 SpringBoot 后端服务，默认配置：
- 模拟器: `http://10.0.2.2:9090`
- 真机: `http://[电脑IP]:9090`

修改服务器地址请编辑 `ets/common/ApiConfig.ets`

## 开发环境

1. 安装 DevEco Studio
2. 打开 HongMeng 目录
3. 同步项目依赖
4. 连接鸿蒙设备或启动模拟器
5. 点击运行

## 注意事项

1. **网络权限**: 应用已在 module.json5 中声明 INTERNET 权限
2. **图片选择**: 需要真机测试图片选择功能
3. **后端服务**: 确保 SpringBoot 服务已启动
4. **Python 服务**: 果蔬识别功能需要启动 Python YOLO 检测服务

## 参赛信息

- **项目名称**: 智慧农场管理系统
- **技术特点**: 鸿蒙原生应用、AI识别、智能问答
- **适用场景**: 智慧农业、农田管理

## 版本历史

- v1.0.0 - 初始版本，完成所有基础功能
