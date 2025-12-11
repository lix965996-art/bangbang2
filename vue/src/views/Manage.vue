<template>
  <el-container class="green-theme-container">

    <el-aside :width="sideWidth + 'px'" class="green-sidebar">
      <Aside :isCollapse="isCollapse" :logoTextShow="logoTextShow" />
    </el-aside>

    <el-container :class="['main-container', { 'collapsed': isCollapse }]">
      <el-header class="green-header-wrapper">
        <Header :collapseBtnClass="collapseBtnClass" @asideCollapse="collapse" :user="user" />
      </el-header>

      <el-main class="green-main">
        <router-view @refreshUser="getUser" />
      </el-main>

    </el-container>
  </el-container>
</template>

<script>

import Aside from "@/components/Aside";
import Header from "@/components/Header";

export default {
  name: 'Home',
  data() {
    return {
      collapseBtnClass: 'el-icon-s-fold',
      isCollapse: false,
      sideWidth: 240,
      logoTextShow: true,
      user: {}
    }
  },
  components: {
    Aside,
    Header
  },
  created() {
    // 从后台获取最新的User数据
    this.getUser()
  },
  methods: {
    collapse() {  // 点击收缩按钮触发
      this.isCollapse = !this.isCollapse
      if (this.isCollapse) {  // 收缩
        this.sideWidth = 64
        this.collapseBtnClass = 'el-icon-s-unfold'
        this.logoTextShow = false
      } else {   // 展开
        this.sideWidth = 220
        this.collapseBtnClass = 'el-icon-s-fold'
        this.logoTextShow = true
      }
    },
    getUser() {
      let username = "";
      try {
        const userStr = localStorage.getItem("user");
        const user = userStr ? JSON.parse(userStr) : null;
        username = user ? user.username : "";
      } catch (e) {
        console.error('解析用户信息失败:', e);
        username = "";
      }
      
      if (username) {
        // 从后台获取User数据
        this.request.get("/user/username/" + username).then(res => {
          // 重新赋值后台的最新User数据
          this.user = res.data
        })
      }
    }
  }
}
</script>

<style scoped>
/* 清新绿主题布局 - 像素级修复 */
.green-theme-container {
  min-height: 100vh;
  background: #f1f5f9; /* 淡灰蓝背景 (最底层) */
  display: flex;
  gap: 0 !important;
}

.green-sidebar {
  height: 100vh;
  position: fixed !important;
  left: 0;
  top: 0;
  z-index: 1000;
  /* 强制移除所有边框、阴影、轮廓、圆角 */
  box-shadow: none !important;
  border: none !important;
  border-right: none !important;
  border-left: none !important;
  outline: none !important;
  border-radius: 0 !important; /* 移除圆角 */
  background: #f8faf9; /* 淡薄荷灰 (中间层) */
  overflow: hidden;
  margin: 0 !important;
  padding: 0 !important;
  flex-shrink: 0;
}

/* 深度穿透：确保 Element UI 的 el-aside 也无边框 */
:deep(.el-aside) {
  border: none !important;
  box-shadow: none !important;
  outline: none !important;
  border-radius: 0 !important;
}

.main-container {
  background: transparent;
  margin: 0 !important;
  margin-left: 240px !important; /* 为固定侧边栏留出空间 */
  padding: 0;
  flex: 1;
  min-width: 0;
  box-shadow: none !important;
  border: none !important;
  transition: margin-left 0.3s ease;
}

.main-container.collapsed {
  margin-left: 64px !important; /* 收缩时的左边距 */
}

.green-header-wrapper {
  border-bottom: none;
  padding: 0;
}

.green-main {
  background: #f1f5f9; /* 淡灰蓝 - 让卡片突出 */
  padding: 0;
  height: calc(100vh - 60px);
  overflow-y: auto;
  overflow-x: hidden;
}

.green-main::-webkit-scrollbar {
  width: 8px;
}

.green-main::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.green-main::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
}

.green-main::-webkit-scrollbar-thumb:hover {
  background: #a1a1a1;
}

/* 全局卡片样式 - 中性灰色阴影 */
:deep(.el-card) {
  background: #ffffff !important;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06); /* 中性灰色阴影 */
  transition: none;
}

:deep(.el-card:hover) {
  transform: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border-color: #e2e8f0;
}

:deep(.el-card__header) {
  background: linear-gradient(135deg, rgba(76, 175, 80, 0.1) 0%, rgba(129, 199, 132, 0.1) 100%);
  border-bottom: 2px solid rgba(76, 175, 80, 0.2);
  font-weight: 600;
  color: #2c5530;
}

:deep(.el-card__body) {
  transition: none !important;
  transform: none !important;
  animation: none !important;
}

:deep(.el-card__body:hover) {
  border-color: inherit !important;
  box-shadow: inherit !important;
  transform: none !important;
  background: inherit !important;
}

:deep(.el-card__body:active) {
  border-color: inherit !important;
  box-shadow: inherit !important;
  transform: none !important;
}

:deep(.el-card__body:focus) {
  border-color: inherit !important;
  box-shadow: inherit !important;
  outline: none !important;
  transform: none !important;
}

:deep(.el-card__body *) {
  transition: none !important;
}

/* 按钮样式 */
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%);
  border: none;
  box-shadow: 0 2px 8px rgba(76, 175, 80, 0.3);
  transition: all 0.3s ease;
}

:deep(.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(76, 175, 80, 0.4);
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #66bb6a 0%, #81c784 100%);
  border: none;
}

:deep(.el-button--warning) {
  background: linear-gradient(135deg, #ff9800 0%, #ffb74d 100%);
  border: none;
}

/* 表格样式 */
:deep(.el-table) {
  background: rgba(255, 255, 255, 0.9);
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-table th) {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  color: #2c5530;
  font-weight: 600;
}

:deep(.el-table--striped .el-table__body tr.el-table__row--striped td) {
  background: rgba(76, 175, 80, 0.05);
}

/* 输入框样式 */
:deep(.el-input__inner) {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  transition: none;
}

:deep(.el-input__inner:focus) {
  border-color: #dcdfe6;
  box-shadow: none;
}

/* 标签样式 */
:deep(.el-tag--success) {
  background: rgba(76, 175, 80, 0.1);
  border-color: #4caf50;
  color: #2c5530;
}
</style>

