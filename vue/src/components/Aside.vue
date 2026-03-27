<template>
  <div class="aside-container">
    <div class="logo-box">
      <img src="../assets/logo2.png" alt="" class="logo-img">
      <transition name="fade">
        <div v-show="!isCollapse" class="logo-text-group">
          <span class="logo-title">帮帮农</span>
          <span class="logo-sub">Smart Farm Command</span>
        </div>
      </transition>
    </div>

    <el-menu
      style="border-right: none"
      :default-active="$route.path"
      :default-openeds="defaultOpeneds"
      :collapse="isCollapse"
      :collapse-transition="false"
      background-color="#f8faf9"
      text-color="#475569"
      active-text-color="#059669"
      router
      class="custom-menu"
    >
      <div v-show="!isCollapse" class="menu-caption">核心能力</div>
      <el-menu-item
        v-for="item in primaryMenus"
        :key="item.path"
        :index="item.path"
      >
        <i :class="item.icon"></i>
        <span slot="title">{{ item.title }}</span>
      </el-menu-item>

      <div
        v-show="!isCollapse && secondaryGroups.length"
        class="menu-caption support-caption"
      >
        {{ supportCaption }}
      </div>
      <el-submenu
        v-for="group in secondaryGroups"
        :key="group.index"
        :index="group.index"
        class="support-submenu"
      >
        <template slot="title">
          <i :class="group.icon"></i>
          <span>{{ group.title }}</span>
        </template>
        <el-menu-item
          v-for="item in group.children"
          :key="item.path"
          :index="item.path"
        >
          <i :class="item.icon"></i>
          <span slot="title">{{ item.title }}</span>
        </el-menu-item>
      </el-submenu>
    </el-menu>

    <div v-show="!isCollapse" class="aside-footer">
      <span class="version">{{ footerText }}</span>
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
    isCompetitionMode() {
      return process.env.VUE_APP_SHOWCASE_MODE === 'competition'
    },
    supportCaption() {
      return '扩展与管理'
    },
    footerText() {
      return this.isCompetitionMode ? 'Bang Bang Agro · 国赛展示版' : 'Bang Bang Agro · 完整版'
    },
    secondaryGroups() {
      if (this.isCompetitionMode) {
        return []
      }
      return this.fullSecondaryGroups
    },
    defaultOpeneds() {
      const currentPath = this.$route.path
      return this.secondaryGroups
        .filter(group => group.children.some(item => item.path === currentPath))
        .map(group => group.index)
    }
  },
  data() {
    return {
      primaryMenus: [
        { path: '/home', icon: 'el-icon-s-home', title: '总控驾驶舱' },
        { path: '/farm-map-gaode', icon: 'el-icon-map-location', title: 'GIS 指挥地图' },
        { path: '/farmmap3d', icon: 'el-icon-office-building', title: '农场 3D 孪生' },
        { path: '/aether-monitor', icon: 'el-icon-cpu', title: '物联感知与联动' },
        { path: '/fruit-detect', icon: 'el-icon-picture-outline', title: '作物智能诊断' },
        { path: '/business-analysis', icon: 'el-icon-data-analysis', title: '决策预警中心' }
      ],
      fullSecondaryGroups: [
        {
          index: 'extended-showcase',
          title: '扩展展示',
          icon: 'el-icon-monitor',
          children: [
            { path: '/bigscreen', icon: 'el-icon-data-board', title: '全局态势大屏' },
            { path: '/dashbordnew', icon: 'el-icon-odometer', title: '农情监测推演' },
            { path: '/statistic', icon: 'el-icon-pie-chart', title: '农情数据总览' }
          ]
        },
        {
          index: 'operations-support',
          title: '运营补充',
          icon: 'el-icon-collection-tag',
          children: [
            { path: '/farmland', icon: 'el-icon-location-outline', title: '农田管理' },
            { path: '/inventory', icon: 'el-icon-box', title: '物资库存' },
            { path: '/purchase', icon: 'el-icon-shopping-cart-full', title: '物资采购' },
            { path: '/sales', icon: 'el-icon-s-order', title: '出售账单' },
            { path: '/online-sale', icon: 'el-icon-sell', title: '在线销售' }
          ]
        },
        {
          index: 'system-support',
          title: '系统维护',
          icon: 'el-icon-setting',
          children: [
            { path: '/user', icon: 'el-icon-user', title: '用户管理' },
            { path: '/role', icon: 'el-icon-lock', title: '角色管理' },
            { path: '/notice', icon: 'el-icon-bell', title: '系统公告' }
          ]
        }
      ]
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
  padding: 20px 16px;
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

.menu-caption {
  margin: 12px 20px 6px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.support-caption {
  margin-top: 18px;
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

::v-deep .support-submenu > .el-submenu__title {
  height: 40px;
  line-height: 40px;
  margin-top: 6px;
  color: #64748b !important;
  font-size: 13px;
  font-weight: 600;
  opacity: 0.88;
}

::v-deep .support-submenu .el-menu-item {
  color: #64748b !important;
  font-size: 13px !important;
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
