# 鸿蒙项目迁移指南

## 问题原因
当前项目目录不是由 DevEco Studio 创建的，缺少必要的配置文件和依赖包，导致工程同步失败。

## 解决方案

### 步骤1：创建新项目

1. 打开 DevEco Studio
2. 点击 "Create Project" 创建新项目
3. 选择 "Empty Ability" 模板
4. 配置项目：
   - Project name: `smartfarm`
   - Bundle name: `com.smartfarm.app`
   - Save location: 选择一个新目录（如 `D:\DevEcoProjects\smartfarm`）
   - Compile SDK: API 9 或更高
   - Model: Stage
5. 点击 Finish，等待项目创建和同步完成

### 步骤2：复制代码文件

创建完成后，将以下目录/文件从 `F:\Code-\bang-bang-agro-master\bang-bang-agro-master\HongMeng` 复制到新项目：

#### 复制 ets 源代码
```
源目录: HongMeng\entry\src\main\ets\
目标目录: 新项目\entry\src\main\ets\

复制内容:
- common/ (ApiConfig.ets, HttpClient.ets, StorageUtil.ets, CommonStyles.ets)
- components/ (所有组件文件)
- pages/ (所有页面文件)
```

#### 更新 main_pages.json
将新项目的 `entry\src\main\resources\base\profile\main_pages.json` 替换为：
```json
{
  "src": [
    "pages/LoginPage",
    "pages/MainPage",
    "pages/DashboardPage",
    "pages/FarmlandPage",
    "pages/FarmlandDetailPage",
    "pages/InventoryPage",
    "pages/FruitDetectPage",
    "pages/AIAssistantPage",
    "pages/AlertPage",
    "pages/SettingsPage",
    "pages/HealthPage"
  ]
}
```

#### 复制资源文件
```
源目录: HongMeng\entry\src\main\resources\base\element\color.json
目标目录: 新项目\entry\src\main\resources\base\element\color.json
```

#### 更新 module.json5
在新项目的 `entry\src\main\module.json5` 中添加网络权限：
```json5
"requestPermissions": [
  {
    "name": "ohos.permission.INTERNET",
    "reason": "需要网络访问来获取农场数据",
    "usedScene": {
      "abilities": ["EntryAbility"],
      "when": "inuse"
    }
  }
]
```

### 步骤3：同步和运行

1. 点击 "Sync Now" 或 File > Sync Project
2. 等待同步完成
3. 创建模拟器或使用 Previewer 预览

## API 配置

确保后端服务器运行在 `http://localhost:9090`，API 配置在 `ApiConfig.ets` 中：
- 模拟器访问: `http://10.0.2.2:9090`
- 真机访问: 使用电脑的局域网 IP 地址

## 文件清单

### 公共模块 (common/)
- ApiConfig.ets - API 接口配置
- HttpClient.ets - HTTP 请求封装
- StorageUtil.ets - 本地存储工具
- CommonStyles.ets - 通用样式定义

### 组件 (components/)
- HeaderBar.ets - 顶部导航栏
- TabBar.ets - 底部标签栏
- DataCard.ets - 数据卡片
- AlertCard.ets - 预警卡片
- StatusBadge.ets - 状态标签
- LoadingDialog.ets - 加载对话框

### 页面 (pages/)
- LoginPage.ets - 登录页面
- MainPage.ets - 主页（数据概览）
- DashboardPage.ets - 数据大屏
- FarmlandPage.ets - 农田列表
- FarmlandDetailPage.ets - 农田详情
- InventoryPage.ets - 库存管理
- FruitDetectPage.ets - 水果识别
- AIAssistantPage.ets - AI 助手
- AlertPage.ets - 预警中心
- SettingsPage.ets - 设置页面
- HealthPage.ets - 健康指数