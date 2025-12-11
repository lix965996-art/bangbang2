<template>
  <div class="green-header">
    <div class="header-left">
      <span :class="collapseBtnClass" class="collapse-btn" @click="collapse"></span>

      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item :to="'/'">
          <i class="el-icon-s-home"></i>
          首页
        </el-breadcrumb-item>
        <el-breadcrumb-item>{{ currentPathName }}</el-breadcrumb-item>
      </el-breadcrumb>
    </div>
    
  
    <el-dropdown class="user-dropdown">
      <div class="user-info">
        <img src="@/assets/mo.png" alt="" class="user-avatar">
        <span class="user-name">{{ user.nickname }}</span>
        <i class="el-icon-arrow-down"></i>
      </div>
      <el-dropdown-menu slot="dropdown" class="user-menu">
        <el-dropdown-item>
          <router-link to="/person" class="menu-link">
            <i class="el-icon-user"></i>
            个人信息
          </router-link>
        </el-dropdown-item>
        <el-dropdown-item>
          <router-link to="/password" class="menu-link">
            <i class="el-icon-key"></i>
            修改密码
          </router-link>
        </el-dropdown-item>
        <el-dropdown-item divided>
          <div class="menu-link logout-btn" @click="logout">
            <i class="el-icon-switch-button"></i>
            退出登录
          </div>
        </el-dropdown-item>
      </el-dropdown-menu>
    </el-dropdown>
  </div>
</template>

<script>
export default {
  name: "Header",
  props: {
    collapseBtnClass: String,
    user: Object
  },
  computed: {
    currentPathName () {
      return this.$store.state.currentPathName;　　//需要监听的数据
    }
  },
  data() {
    return {

    }
  },
  methods: {
    collapse() {
      // this.$parent.$parent.$parent.$parent.collapse()  // 通过4个 $parent 找到父组件，从而调用其折叠方法
      this.$emit("asideCollapse")
    },
    logout() {
      this.$store.commit("logout")
      this.$message.success("退出成功")
    }
  }
}
</script>

<style scoped>
/* 顶部导航 - 悬浮白色 (最上层) */
.green-header {
  height: 60px;
  display: flex;
  align-items: center;
  background: #ffffff;
  border-bottom: 1px solid #e8ebe8;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  flex: 1;
}

.collapse-btn {
  cursor: pointer;
  font-size: 20px;
  color: #4caf50;
  padding: 8px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.collapse-btn:hover {
  background: rgba(76, 175, 80, 0.1);
}

.breadcrumb {
  margin-left: 15px;
}

:deep(.breadcrumb .el-breadcrumb__item) {
  font-weight: 500;
}

:deep(.breadcrumb .el-breadcrumb__inner) {
  color: #2c5530;
}

:deep(.breadcrumb .el-breadcrumb__inner:hover) {
  color: #4caf50;
}

.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
}

.system-title {
  font-size: 20px;
  font-weight: 600;
  color: #2c5530;
  display: flex;
  align-items: center;
  gap: 8px;
}

.system-title i {
  font-size: 24px;
  color: #4caf50;
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 20px;
  background: transparent;
  border: none;
  transition: all 0.3s ease;
}

.user-info:hover {
  background: rgba(76, 175, 80, 0.1);
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 2px solid #6dac50ff;
  object-fit: cover;
  background: #ffffff;
  padding: 4px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-name {
  font-weight: 600;
  color: #2c5530;
  font-size: 14px;
}

.user-info i {
  color: #4caf50;
  font-size: 12px;
}

/* 下拉菜单 */
.user-menu {
  border: 2px solid #4caf50;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(76, 175, 80, 0.2);
}

.menu-link {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #2c5530;
  text-decoration: none;
  font-weight: 500;
  transition: all 0.3s ease;
}

.menu-link:hover {
  color: #4caf50;
}

.menu-link i {
  font-size: 16px;
}

.logout-btn {
  cursor: pointer;
}

.logout-btn:hover {
  color: #f56c6c !important;
}
</style>
