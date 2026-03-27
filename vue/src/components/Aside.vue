<template>
  <div class="aside-container">
    <div class="logo-box">
      <img src="../assets/logo2.png" alt="" class="logo-img" v-if="true">
      <transition name="fade">
        <div class="logo-text-group" v-show="!isCollapse">
          <span class="logo-title">帮帮农</span>
          <span class="logo-sub">Bang Bang Agro</span>
        </div>
      </transition>
    </div>

    <el-menu
      :default-openeds="defaultOpeneds"
      style="border-right: none"
      :default-active="$route.path"
      :collapse="isCollapse"
      :collapse-transition="false"
      background-color="#f8faf9"
      text-color="#475569"
      active-text-color="#059669"
      router
      class="custom-menu"
    >
      <el-menu-item index="/home">
        <i class="el-icon-s-home"></i>
        <span>总控驾驶舱</span>
      </el-menu-item>

      <el-sub-menu index="visual">
        <template #title>
          <i class="el-icon-monitor"></i>
          <span>数字孪生视界</span>
        </template>
        <el-menu-item index="/bigscreen">
          <i class="el-icon-data-board"></i> 全局可视化大屏
        </el-menu-item>
        <el-menu-item index="/farmmap3d">
          <i class="el-icon-office-building"></i> 农场 3D 沙盘
        </el-menu-item>
        <el-menu-item index="/farm-map-gaode">
          <i class="el-icon-map-location"></i> GIS 指挥地图
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="iot">
        <template #title>
          <i class="el-icon-cpu"></i>
          <span>物联感知智控</span>
        </template>
        <el-menu-item index="/aether-monitor">
          <i class="el-icon-view"></i> AI 视觉识别监控
        </el-menu-item>
        <el-menu-item index="/dashbordnew">
          <i class="el-icon-odometer"></i> 环境传感报表
        </el-menu-item>
        <el-menu-item index="/fruit-detect">
          <i class="el-icon-picture-outline"></i> 果蔬双检分析
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="data">
        <template #title>
          <i class="el-icon-s-data"></i>
          <span>AI 经营决策</span>
        </template>
        <el-menu-item index="/business-analysis">
          <i class="el-icon-wallet"></i> 经营效益分析
        </el-menu-item>
        <el-menu-item index="/statistic">
          <i class="el-icon-pie-chart"></i> 农田数据统计
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="operation">
        <template #title>
          <i class="el-icon-s-operation"></i>
          <span>生产资产运营</span>
        </template>
        <el-menu-item index="/farmland">
          <i class="el-icon-place"></i> 农田地块管理
        </el-menu-item>
        <el-menu-item index="/inventory">
          <i class="el-icon-box"></i> 智能仓储库存
        </el-menu-item>
        <el-menu-item index="/purchase">
          <i class="el-icon-shopping-cart-full"></i> 物资采购
        </el-menu-item>
        <el-menu-item index="/sales">
          <i class="el-icon-sell"></i> 出售账单
        </el-menu-item>
        <el-menu-item index="/online-sale">
          <i class="el-icon-shopping-cart-2"></i> 农作物在线销售
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu index="system" v-if="isAdmin">
        <template #title>
          <i class="el-icon-setting"></i>
          <span>系统支撑中心</span>
        </template>
        <el-menu-item index="/user">
          <i class="el-icon-user"></i> 用户与权限
        </el-menu-item>
        <el-menu-item index="/role">
          <i class="el-icon-lock"></i> 角色管理
        </el-menu-item>
        <el-menu-item index="/notice">
          <i class="el-icon-bell"></i> 系统公告
        </el-menu-item>
      </el-sub-menu>
    </el-menu>

    <div class="aside-footer" v-show="!isCollapse">
      <span class="version">Bang Bang Agro v1.0</span>
    </div>
  </div>
</template>

<script>
export default {
  name: "Aside",
  props: {
    isCollapse: Boolean
  },
  computed: {
    isAdmin() {
      const user = localStorage.getItem('user')
      if (user) {
        try {
          const userObj = JSON.parse(user)
          return userObj.role === 'ROLE_ADMIN'
        } catch (e) {
          return false
        }
      }
      return false
    },
    defaultOpeneds() {
      const base = ['visual', 'iot', 'data', 'operation']
      if (this.isAdmin) {
        base.push('system')
      }
      return base
    }
  }
}
</script>

<style scoped>
.aside-container {
  height: 100%;
  background: #f8faf9;
  position: relative;
  overflow: hidden;
  transition: width 0.3s;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e2e8f0 !important;
  box-shadow: none !important;
  outline: none !important;
  border-radius: 0 !important;
}

.logo-box {
  min-height: 80px;
  display: flex;
  align-items: center;
  padding: 20px 16px 20px 16px;
  background: #f8faf9;
  border-bottom: 1px solid #e8ebe8;
  flex-shrink: 0;
}

.logo-img {
  width: 52px;
  height: 52px;
  border-radius: 12px;
}

.logo-text-group {
  margin-left: 14px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.logo-title {
  color: #1e293b;
  font-weight: 700;
  font-size: 22px;
  letter-spacing: 0.5px;
}

.logo-sub {
  color: #64748b;
  font-size: 12px;
  font-weight: 500;
  letter-spacing: 0.5px;
  margin-top: 3px;
}

.custom-menu {
  border: none !important;
  padding: 10px 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.custom-menu::-webkit-scrollbar {
  display: none;
  width: 0 !important;
}

::v-deep .el-submenu__title,
::v-deep .el-menu-item {
  height: 44px;
  line-height: 44px;
  margin: 2px 8px;
  background-color: transparent !important;
  color: #475569 !important;
  font-size: 14px;
  font-weight: 500;
  border-radius: 8px;
  padding: 0 12px !important;
  position: relative;
}

::v-deep .el-menu-item i,
::v-deep .el-submenu__title i {
  color: #94a3b8 !important;
  margin-right: 5px;
  font-size: 18px;
  transition: color 0.3s;
}

::v-deep .el-submenu__title .el-submenu__icon-arrow {
  position: static !important;
  margin-left: auto !important;
  margin-top: 0 !important;
  color: #cbd5e1 !important;
  font-size: 12px;
}

::v-deep .el-submenu__title {
  display: flex;
  align-items: center;
}

::v-deep .el-submenu__title:hover,
::v-deep .el-menu-item:hover {
  background: linear-gradient(to right, #f0fdf4, transparent) !important;
  color: #059669 !important;
}

::v-deep .el-submenu__title:hover i,
::v-deep .el-menu-item:hover i {
  color: #059669 !important;
}

::v-deep .el-menu > .el-menu-item.is-active {
  background: #ffffff !important;
  color: #059669 !important;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-left: 3px solid #059669 !important;
}

::v-deep .el-menu > .el-menu-item.is-active i {
  color: #059669 !important;
}

::v-deep .el-submenu.is-opened > .el-submenu__title {
  background: #ffffff !important;
  color: #059669 !important;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  border-left: 3px solid #059669 !important;
}

::v-deep .el-submenu.is-opened > .el-submenu__title i {
  color: #059669 !important;
}

::v-deep .el-submenu .el-menu-item {
  padding-left: 20px !important;
  height: 40px;
  line-height: 40px;
  font-size: 13px !important;
}

::v-deep .el-submenu .el-menu-item i {
  font-size: 15px !important;
  margin-right: 5px;
}

::v-deep .el-submenu .el-menu-item.is-active {
  background: transparent !important;
  color: #059669 !important;
  font-weight: 600;
  box-shadow: none !important;
  border-left: none !important;
  border: none !important;
}

::v-deep .el-submenu .el-menu-item.is-active i {
  color: #059669 !important;
}

.aside-footer {
  height: 48px;
  line-height: 48px;
  text-align: center;
  border-top: 1px solid #e8ebe8;
  font-size: 11px;
  color: #94a3b8;
  background: #f8faf9;
  flex-shrink: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s;
}

.fade-enter,
.fade-leave-to {
  opacity: 0;
}
</style>
