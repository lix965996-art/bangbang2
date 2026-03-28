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
      <div v-show="!isCollapse" class="menu-caption">核心主线</div>
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

  </div>
</template>

<script>
const operationsGroup = {
  index: 'operations-support',
  title: '产业链路',
  icon: 'el-icon-collection-tag',
  children: [
    { path: '/farmland', icon: 'el-icon-location-outline', title: '地块资产图谱' },
    { path: '/inventory', icon: 'el-icon-box', title: '仓储雷达' },
    { path: '/purchase', icon: 'el-icon-shopping-cart-full', title: '供应协作网' },
    { path: '/sales', icon: 'el-icon-s-order', title: '订单流向' },
    { path: '/online-sale', icon: 'el-icon-sell', title: '现货联销' }
  ]
}

export default {
  name: 'Aside',
  props: {
    isCollapse: Boolean
  },
  computed: {
    isCompetitionMode() {
      return process.env.VUE_APP_SHOWCASE_MODE === 'competition'
    },
    supportCaption() {
      return this.isCompetitionMode ? '业务延展' : '扩展与后台'
    },
    secondaryGroups() {
      if (this.isCompetitionMode) {
        return [operationsGroup]
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
            { path: '/bigscreen', icon: 'el-icon-data-board', title: '全域态势大屏' },
            { path: '/dashbordnew', icon: 'el-icon-odometer', title: '农情推演舱' },
            { path: '/statistic', icon: 'el-icon-pie-chart', title: '农情总览' }
          ]
        },
        operationsGroup,
        {
          index: 'system-support',
          title: '后台运维',
          icon: 'el-icon-setting',
          children: [
            { path: '/user', icon: 'el-icon-user', title: '用户管理' },
            { path: '/role', icon: 'el-icon-lock', title: '角色管理' },
            { path: '/notice', icon: 'el-icon-bell', title: '通知中心' }
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

::v-deep .el-menu-item:hover,
::v-deep .el-submenu__title:hover {
  color: #0f766e !important;
  background: rgba(15, 118, 110, 0.06) !important;
}

::v-deep .el-menu-item.is-active {
  color: #059669 !important;
  background: rgba(5, 150, 105, 0.08) !important;
  font-weight: 600;
}

::v-deep .el-menu-item.is-active::before {
  content: '';
  position: absolute;
  left: -8px;
  top: 8px;
  bottom: 8px;
  width: 3px;
  border-radius: 999px;
  background: linear-gradient(180deg, #059669, #34d399);
}

::v-deep .el-menu-item i,
::v-deep .el-submenu__title i {
  margin-right: 10px;
  color: inherit !important;
}

</style>
