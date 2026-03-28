# 智慧农场管理系统 - 鸿蒙版

## 版本信息

| 组件 | 版本 |
|------|------|
| HarmonyOS API | 20 |
| ArkTS | 6.0.0 |
| Hvigor | 6.0.0 |
| modelVersion | 6.0.0 |
| 最低兼容SDK | 20 |
| 目标SDK | 20 |

## 项目简介

本项目是智慧农场管理系统的鸿蒙原生版本，使用 ArkTS + ArkUI 开发，适配 HarmonyOS NEXT (API 20)。

## 开发环境要求

- **DevEco Studio**: 最新版本 (支持 API 20)
- **HarmonyOS SDK**: API 20
- **Node.js**: 14.x 或更高版本
- **JDK**: 11 或更高版本

## 项目结构

```
HongMeng/
├── AppScope/                       # 应用全局配置
│   └── app.json5                   # 应用配置
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
│   │   │   │   ├── AlertCard.ets   # 预警卡片
│   │   │   │   ├── StatusBadge.ets # 状态标签
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
│   │       │   │   ├── color.json  # 颜色定义
│   │       │   │   └── string.json # 字符串定义
│   │       │   └── profile/        # 配置文件
│   │       │       └── main_pages.json # 路由配置
│   │       ├── zh_CN/              # 中文资源
│   │       └── en_US/              # 英文资源
│   ├── build-profile.json5         # 模块构建配置
│   ├── module.json5                # 模块配置
│   ├── oh-package.json5            # 模块依赖
│   └── hvigorfile.ts               # 模块构建脚本
├── hvigor/                         # Hvigor构建工具
│   ├── hvigor-config.json5         # Hvigor配置
│   └── hvigor-wrapper.js           # Hvigor包装器
├── build-profile.json5             # 项目构建配置
├── oh-package.json5                # 项目依赖
├── hvigorfile.ts                   # 项目构建脚本
└── README.md                       # 说明文档
```

## 页面功能说明

### 1. 登录页 (LoginPage)
- **路径**: `pages/LoginPage`
- **功能**: 用户登录/注册
- **界面元素**:
  - Logo区域（农场图标 + 标题）
  - 用户名输入框
  - 密码输入框
  - 登录/注册按钮
  - 错误提示
- **API调用**:
  - POST `/user/login` - 登录
  - POST `/user/register` - 注册

### 2. 主页 (MainPage)
- **路径**: `pages/MainPage`
- **功能**: 应用主界面，包含5个Tab页
- **Tab页面**:
  1. **首页** - 欢迎信息、数据卡片、快捷操作、今日预警
  2. **农田** - 农田搜索、列表展示
  3. **库存** - 库存管理（开发中）
  4. **识别** - 果蔬成熟度识别入口
  5. **助手** - AI农业助手入口

### 3. 数据大屏 (DashboardPage)
- **路径**: `pages/DashboardPage`
- **功能**: 数据可视化展示
- **界面元素**:
  - 统计卡片（农田总数、总面积、存栏量、状态正常）
  - 温度分布图
  - 湿度分布图
  - 农田状态列表

### 4. 农田列表 (FarmlandPage)
- **路径**: `pages/FarmlandPage`
- **功能**: 农田搜索和列表展示
- **界面元素**:
  - 搜索栏
  - 农田卡片（名称、地址、作物、环境数据）
  - 分页加载

### 5. 农田详情 (FarmlandDetailPage)
- **路径**: `pages/FarmlandDetailPage`
- **功能**: 农田信息编辑
- **界面元素**:
  - 基本信息表单（名称、面积、地址、作物、数量）
  - 环境参数表单（温度、湿度、pH、CO2、光照）
  - 状态选择（正常、良好、一般、异常）
  - 保存/删除按钮

### 6. 库存管理 (InventoryPage)
- **路径**: `pages/InventoryPage`
- **功能**: 库存列表展示
- **界面元素**:
  - 搜索栏
  - 库存卡片（物资名称、仓库、管理员、数量）
  - 库存状态标识（充足/不足）

### 7. 果蔬识别 (FruitDetectPage)
- **路径**: `pages/FruitDetectPage`
- **功能**: AI果蔬成熟度识别
- **界面元素**:
  - 图片选择区域
  - 操作按钮（选择图片、开始识别）
  - 识别结果展示（类别、置信度、成熟度）
  - 使用说明
- **API调用**: POST `/fruit-detect/analyze`
- **依赖**: Python YOLO检测服务

### 8. AI助手 (AIAssistantPage)
- **路径**: `pages/AIAssistantPage`
- **功能**: 智能问答对话
- **界面元素**:
  - 消息列表（用户/AI消息气泡）
  - 快捷问题推荐
  - 输入框和发送按钮
- **API调用**: POST `/api/chat/ask`

### 9. 预警中心 (AlertPage)
- **路径**: `pages/AlertPage`
- **功能**: 预警任务管理
- **界面元素**:
  - Tab切换（今日任务/全部预警）
  - 预警卡片（类型、级别、消息、时间）
  - 处理按钮
- **API调用**:
  - GET `/alert/today/tasks`
  - GET `/alert/pending`
  - POST `/alert/{id}/process`

### 10. 健康指数 (HealthPage)
- **路径**: `pages/HealthPage`
- **功能**: 农田健康评分展示
- **界面元素**:
  - 统计卡片（优秀、良好、一般、较差数量）
  - 健康指数列表（农田名称、评分、等级）
  - 环形进度条
- **API调用**: GET `/health-index/calculate/all`

### 11. 设置页 (SettingsPage)
- **路径**: `pages/SettingsPage`
- **功能**: 应用设置
- **界面元素**:
  - 用户信息卡片
  - 设置项列表（服务器地址、版本号、帮助、协议）
  - 退出登录按钮

## 自定义组件说明

### HeaderBar - 顶部导航栏
```
+------------------------------------------+
| ←  标题                          图标    |
+------------------------------------------+
```
- Props: title, showBack, rightIcon
- Events: onBackClick, onRightClick

### TabBar - 底部标签栏
```
+------------------------------------------+
|  🏠首页  |  🌾农田  |  📦库存  |  🍎识别  |  🤖助手  |
+------------------------------------------+
```
- Props: currentTab (双向绑定)

### DataCard - 数据卡片
```
+------------------+
| 🌾 农田总数      |
|                  |
| 128 块           |
+------------------+
```
- Props: title, value, unit, icon, color

### AlertCard - 预警卡片
```
+------------------------------------------+
| 🌡️  温度过高，请及时处理        [处理]   |
|      2024-03-27 10:30                    |
| [高] [温度]                              |
+------------------------------------------+
```
- Props: alertType, alertLevel, message, createTime, status
- Events: onProcess

## 配置文件说明

### oh-package.json5 (项目级)
```json5
{
  "modelVersion": "6.0.0",
  "name": "smartfarm",
  "version": "1.0.0",
  "dependencies": {
    "@ohos/hvigor-ohos-plugin": "6.0.0",
    "@ohos/hvigor": "6.0.0"
  }
}
```

### build-profile.json5 (项目级)
```json5
{
  "app": {
    "products": [{
      "name": "default",
      "compileSdkVersion": 20,
      "compatibleSdkVersion": 20,
      "targetSdkVersion": 20
    }]
  }
}
```

### hvigor-config.json5
```json5
{
  "modelVersion": "6.0.0"
}
```

### module.json5
```json5
{
  "module": {
    "name": "entry",
    "type": "entry",
    "deviceTypes": ["default", "tablet"],
    "requestPermissions": [{
      "name": "ohos.permission.INTERNET",
      "reason": "$string:internet_permission_reason"
    }]
  }
}
```

## 运行步骤

### 1. 环境准备
1. 下载并安装 [DevEco Studio](https://developer.huawei.com/consumer/cn/deveco-studio/)
2. 安装 HarmonyOS SDK API 20
3. 配置 Node.js 环境

### 2. 项目导入
1. 打开 DevEco Studio
2. 选择 `File → Open`
3. 选择 `HongMeng` 目录
4. 等待项目同步完成

### 3. 运行项目
1. 连接鸿蒙真机或启动模拟器
2. 点击工具栏的运行按钮
3. 等待编译和安装完成

### 4. 预览页面
1. 打开 `entry/src/main/ets/pages/` 下的任意 `.ets` 文件
2. 右侧会显示 Previewer 面板
3. 可实时预览UI效果

## 后端服务配置

### API地址配置
修改 `entry/src/main/ets/common/ApiConfig.ets`:
```typescript
export class ApiConfig {
  // 模拟器使用
  static BASE_URL: string = 'http://10.0.2.2:9090';

  // 真机使用（改为电脑IP）
  // static BASE_URL: string = 'http://192.168.1.100:9090';
}
```

### 需要的后端服务
1. **SpringBoot服务** (端口9090)
   - 用户认证
   - 农田管理
   - 库存管理
   - 预警管理

2. **Python YOLO服务** (果蔬识别)
   - 端点: `/fruit-detect/analyze`
   - 支持图片上传和识别

## ArkTS 语法注意事项

### 组件定义
```typescript
// 正确：使用 struct
@Component
export default struct MyComponent {
  @Prop title: string = '';
  build() { ... }
}

// 错误：不能使用 class
@Component
export default class MyComponent { ... }
```

### 类型定义
```typescript
// 正确：使用接口定义对象类型
interface UserInfo {
  name: string;
  age: number;
}
const user: UserInfo = { name: 'Tom', age: 20 };

// 错误：不能使用 any/unknown
const data: any = JSON.parse(str);
```

### 装饰器
```typescript
// 正确：回调函数不加装饰器
export default struct HeaderBar {
  onBackClick: () => void = () => {};
}

// 错误：@Event 不存在
@Event onBackClick: () => void = () => {};
```

### 双向绑定
```typescript
// 正确：使用 $$ 语法
TextInput({ text: $$inputText })

// 或使用 onChange
TextInput({ text: inputText })
  .onChange((value) => { inputText = value; })
```

## 常见问题

### Q1: Hvigor版本不一致
**错误**: `ohpm的开发态配置版本为5.0.0，hvigor的开发态配置版本未填写`

**解决**: 确保 `oh-package.json5` 和 `hvigor-config.json5` 中的 `modelVersion` 一致，都为 `6.0.0`

### Q2: 组件语法错误
**错误**: `The '@Component' decorator can only be used with 'struct'`

**解决**: 将 `class` 改为 `struct`

### Q3: 对象字面量错误
**错误**: `Object literal must correspond to some explicitly declared class or interface`

**解决**: 先定义接口，再使用接口类型
```typescript
interface Params { id: number; name: string; }
const params: Params = { id: 1, name: 'test' };
```

### Q4: 图片选择器导入错误
**错误**: `Cannot find module '@ohos.multimedia.imagePicker'`

**解决**: 使用新的导入方式
```typescript
import { picker } from '@kit.CoreFileKit';
// 使用: new picker.PhotoViewPicker()
```

## 许可证

ISC License

## 联系方式

如有问题，请联系开发者。
