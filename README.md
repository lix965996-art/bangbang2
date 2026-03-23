# BangBangAgro

本项目分为两个有效子工程：

- `springboot/`：后端服务，默认端口 `9094`
- `vue/`：前端项目，默认开发端口 `8080`

## 运行

后端：

```bash
cd springboot
mvn spring-boot:run
```

前端：

```bash
cd vue
npm install
npm run serve
```

## 关键环境变量

- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USERNAME`
- `DB_PASSWORD`
- `AMAP_JS_KEY`
- `AMAP_WEB_KEY`
- `QWEN_API_KEY`
- `ONENET_AUTHOR_KEY`

## 说明

- 根目录不再作为独立 Node 工程使用。
- 前端构建、依赖和脚本均以 `vue/` 目录为准。
- 后端文件上传目录通过 `UPLOAD_PATH` 控制，默认是 `./uploads`。
