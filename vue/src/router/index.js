import Vue from 'vue'
import VueRouter from 'vue-router'
import store from "@/store";

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/404',
    name: '404',
    component: () => import('../views/404.vue')
  },
  {
    path: '/big-screen-3d',
    name: 'BigScreen',
    component: () => import('../views/BigScreen.vue')
  },
]

const router = new VueRouter({
  mode: 'history',
  routes
})

export const resetRouter = () => {
  router.matcher = new VueRouter({
    mode: 'history',
    routes
  }).matcher
}

export const setRoutes = () => {
  const storeMenus = localStorage.getItem("menus");
  if (storeMenus) {
    const manageRoute = { 
      path: '/', 
      name: 'Manage', 
      component: () => import('../views/Manage.vue'), 
      redirect: "/home", 
      children: [
        { path: 'person', name: '个人信息', component: () => import('../views/Person.vue')},
        { path: 'password', name: '修改密码', component: () => import('../views/Password.vue')},
        { path: 'home', name: '农场主页通告', component: () => import('../views/Home.vue')},
        { path: 'aether-monitor', name: 'Aether数据监控', component: () => import('../views/AetherMonitor.vue')},
        { path: 'dashbordnew', name: '农场环境监测', component: () => import('../views/DashbordNew.vue')},
        { path: 'bigscreen', name: '全局可视化大屏', component: () => import('../views/BigScreen.vue')},
        { path: 'farmmap3d', name: '农场3D地图', component: () => import('../views/FarmMap3D.vue')},
        { path: 'farm-map-gaode', name: '农场地理地图', component: () => import('../views/FarmMapGaode.vue')},
        { path: 'statistic', name: '农田信息', component: () => import('../views/Statistic.vue')},
        { path: 'business-analysis', name: '经营分析', component: () => import('../views/BusinessAnalysis.vue')},
        { path: 'farmland', name: '农田管理', component: () => import('../views/Farmland.vue')},
        { path: 'purchase', name: '物资采购', component: () => import('../views/Purchase.vue')},
        { path: 'inventory', name: '物资库存', component: () => import('../views/Inventory.vue')},
        { path: 'sales', name: '出售账单', component: () => import('../views/Sales.vue')},
        { path: 'online-sale', name: '农作物在线销售', component: () => import('../views/OnlineSale.vue')},
        { path: 'notice', name: '系统公告管理', component: () => import('../views/Notice.vue')},
        { path: 'user', name: '农场员工管理', component: () => import('../views/User.vue')},
        { path: 'role', name: '系统角色管理', component: () => import('../views/Role.vue')},
        { path: 'fruit-detect', name: '果蔬双检分析', component: () => import('../views/FruitDetect.vue')},
      ] 
    }
    
    let menus = [];
    try {
      menus = JSON.parse(storeMenus);
    } catch (e) {
      console.error('解析菜单数据失败:', e);
      return;
    }

    // 获取已存在的路由路径和名称，避免重复添加
    const existingPaths = manageRoute.children.map(c => c.path);
    const existingNames = manageRoute.children.map(c => c.name);
    
    menus.forEach(item => {
      if (item.path) {
        const routePath = item.path.replace("/", "");
        // 检查路由的 path 和 name 是否已存在
        if (!existingPaths.includes(routePath) && !existingNames.includes(item.name)) {
          let itemMenu = { 
            path: routePath, 
            name: item.name, 
            component: () => import('../views/' + item.pagePath + '.vue') 
          }
          manageRoute.children.push(itemMenu)
          existingPaths.push(routePath)
          existingNames.push(item.name)
        }
      } else if(item.children && item.children.length) {
        item.children.forEach(subItem => {
          if (subItem.path) {
            const routePath = subItem.path.replace("/", "");
            // 检查路由的 path 和 name 是否已存在
            if (!existingPaths.includes(routePath) && !existingNames.includes(subItem.name)) {
              let itemMenu = { 
                path: routePath, 
                name: subItem.name, 
                component: () => import('../views/' + subItem.pagePath + '.vue') 
              }
              manageRoute.children.push(itemMenu)
              existingPaths.push(routePath)
              existingNames.push(subItem.name)
            }
          }
        })
      }
    })

    const currentRouteNames = router.getRoutes().map(v => v.name)
    if (!currentRouteNames.includes('Manage')) {
      router.addRoute(manageRoute)
    }
  }
}

setRoutes()

router.beforeEach((to, from, next) => {
  localStorage.setItem("currentPathName", to.name)
  store.commit("setPath")
  if (!to.matched.length) {
    const menus = localStorage.getItem("menus")
    if (!menus) {
      next("/login")
    } else {
      next("/404")
    }
  } else {
    next()
  }
})

export default router