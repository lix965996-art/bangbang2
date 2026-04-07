<template>
  <div class="aside-container">
    <div class="logo-box">
      <img src="../assets/logo2.png" alt="帮帮农" class="logo-img" />
      <transition name="fade">
        <div v-show="!isCollapse" class="logo-text-group">
          <span class="logo-title">帮帮农</span>
          <span class="logo-sub">设施农业数字化协同系统</span>
        </div>
      </transition>
    </div>

    <el-menu
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
      <el-menu-item
        v-for="item in mainMenus"
        :key="item.path"
        :index="item.path"
      >
        <i :class="item.icon"></i>
        <span slot="title">{{ item.title }}</span>
      </el-menu-item>

      <div v-show="!isCollapse" class="section-divider"></div>

      <el-submenu index="more-features" class="more-submenu">
        <template slot="title">
          <i class="el-icon-more-outline"></i>
          <span>更多功能</span>
        </template>

        <template v-for="group in supportGroups">
          <div :key="`${group.index}-label`" class="submenu-group-label">
            {{ group.title }}
          </div>
          <el-menu-item
            v-for="item in group.children"
            :key="item.path"
            :index="item.path"
            class="secondary-item"
          >
            <i :class="item.icon"></i>
            <span slot="title">{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-submenu>
    </el-menu>
  </div>
</template>

<script>
const mainMenus = [
  { path: '/home', icon: 'el-icon-s-home', title: '首页总览' },
  { path: '/aether-monitor', icon: 'el-icon-cpu', title: '环境监测与联动' },
  { path: '/fruit-detect', icon: 'el-icon-picture-outline-round', title: '视觉巡检与异常识别' },
  { path: '/farm-map-gaode', icon: 'el-icon-map-location', title: 'GIS 地块指挥' },
  { path: '/farmmap3d', icon: 'el-icon-office-building', title: '3D 数字孪生' },
  { path: '/business-analysis', icon: 'el-icon-data-analysis', title: '预警与研判中心' }
]

const supportGroups = [
  {
    index: 'display',
    title: '展示分析',
    children: [
      { path: '/bigscreen', icon: 'el-icon-monitor', title: '态势展示大屏' },
      { path: '/dashbordnew', icon: 'el-icon-data-line', title: '监测分析看板' },
      { path: '/statistic', icon: 'el-icon-pie-chart', title: '地块数据总览' },
      { path: '/farmland', icon: 'el-icon-location-outline', title: '地块资产图谱' }
    ]
  },
  {
    index: 'business',
    title: '业务协同',
    children: [
      { path: '/supply-center', icon: 'el-icon-box', title: '供给协同中心' },
      { path: '/market-center', icon: 'el-icon-sell', title: '产销协同中心' }
    ]
  },
  {
    index: 'system',
    title: '系统后台',
    children: [
      { path: '/user', icon: 'el-icon-user', title: '用户管理' },
      { path: '/role', icon: 'el-icon-lock', title: '角色管理' },
      { path: '/notice', icon: 'el-icon-bell', title: '系统公告' }
    ]
  }
]

export default {
  name: 'Aside',
  props: {
    isCollapse: Boolean
  },
  data() {
    return {
      mainMenus,
      supportGroups
    }
  },
  computed: {
    defaultOpeneds() {
      const currentPath = this.$route.path
      const inSupportMenus = this.supportGroups.some(group =>
        group.children.some(item => item.path === currentPath)
      )
      return inSupportMenus ? ['more-features'] : []
    }
  }
}
</script>

<style scoped>
.aside-container {
  height: 100%;
  background: #f8faf9;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #e2e8f0 !important;
}

.logo-box {
  min-height: 84px;
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
  letter-spacing: 0.2px;
  margin-top: 3px;
}

.custom-menu {
  border: none !important;
  padding: 14px 12px;
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

.section-divider {
  height: 1px;
  margin: 14px 18px 10px;
  background: linear-gradient(90deg, rgba(203, 213, 225, 0), rgba(203, 213, 225, 0.9), rgba(203, 213, 225, 0));
}

.submenu-group-label {
  padding: 12px 20px 6px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

::v-deep .el-menu-item,
::v-deep .el-submenu__title {
  height: 48px;
  line-height: 48px;
  margin: 4px 8px;
  background-color: transparent !important;
  color: #475569 !important;
  font-size: 15px;
  font-weight: 500;
  border-radius: 12px;
  padding: 0 14px !important;
  position: relative;
  transition: all 0.2s ease;
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

::v-deep .more-submenu > .el-submenu__title {
  margin-top: 0;
  color: #64748b !important;
  font-size: 14px;
  font-weight: 500;
}

::v-deep .secondary-item {
  color: #64748b !important;
  font-size: 14px;
}
</style>
