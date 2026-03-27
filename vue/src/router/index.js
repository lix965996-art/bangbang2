import { createRouter, createWebHistory } from 'vue-router'
import store from '@/store'

const MANAGE_ROUTE_NAME = 'Manage'

const routes = [
  {
    path: '/login',
    name: '登录',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: '注册',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/404',
    name: '404',
    component: () => import('../views/404.vue')
  },
  {
    path: '/big-screen-3d',
    name: '全局大屏',
    component: () => import('../views/BigScreen.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

const routeComponentMap = {
  Person: () => import('../views/Person.vue'),
  Password: () => import('../views/Password.vue'),
  Home: () => import('../views/Home.vue'),
  AetherMonitor: () => import('../views/AetherMonitor.vue'),
  Dashbord: () => import('../views/DashbordNew.vue'),
  DashbordNew: () => import('../views/DashbordNew.vue'),
  BigScreen: () => import('../views/BigScreen.vue'),
  BigScreen3D: () => import('../views/BigScreen.vue'),
  FarmMap3D: () => import('../views/FarmMap3D.vue'),
  FarmMapGaode: () => import('../views/FarmMapGaode.vue'),
  Statistic: () => import('../views/Statistic.vue'),
  BusinessAnalysis: () => import('../views/BusinessAnalysis.vue'),
  Farmland: () => import('../views/Farmland.vue'),
  Purchase: () => import('../views/Purchase.vue'),
  Inventory: () => import('../views/Inventory.vue'),
  Sales: () => import('../views/Sales.vue'),
  OnlineSale: () => import('../views/OnlineSale.vue'),
  Notice: () => import('../views/Notice.vue'),
  User: () => import('../views/User.vue'),
  Role: () => import('../views/Role.vue'),
  FruitDetect: () => import('../views/FruitDetect.vue'),
  AIAnalysis: () => import('../views/AIAnalysis.vue'),
  Prediction: () => import('../views/Prediction.vue')
}

const baseManageChildren = [
  { path: 'person', name: '个人信息', component: routeComponentMap.Person },
  { path: 'password', name: '修改密码', component: routeComponentMap.Password },
  { path: 'home', name: '农场主页通告', component: routeComponentMap.Home },
  { path: 'aether-monitor', name: 'Aether数据监控', component: routeComponentMap.AetherMonitor },
  { path: 'aethermonitor', redirect: '/aether-monitor' },
  { path: 'dashbordnew', name: '农场环境监测', component: routeComponentMap.DashbordNew },
  { path: 'dashbord', redirect: '/dashbordnew' },
  { path: 'bigscreen', name: '全局可视化大屏', component: routeComponentMap.BigScreen },
  { path: 'farmmap3d', name: '农场3D地图', component: routeComponentMap.FarmMap3D },
  { path: 'farm-map-gaode', name: '农场地理地图', component: routeComponentMap.FarmMapGaode },
  { path: 'statistic', name: '农田信息', component: routeComponentMap.Statistic },
  { path: 'business-analysis', name: '经营分析', component: routeComponentMap.BusinessAnalysis },
  { path: 'farmland', name: '农田管理', component: routeComponentMap.Farmland },
  { path: 'purchase', name: '物资采购', component: routeComponentMap.Purchase },
  { path: 'inventory', name: '物资库存', component: routeComponentMap.Inventory },
  { path: 'sales', name: '出售账单', component: routeComponentMap.Sales },
  { path: 'online-sale', name: '农作物在线销售', component: routeComponentMap.OnlineSale },
  { path: 'notice', name: '系统公告管理', component: routeComponentMap.Notice },
  { path: 'user', name: '农场员工管理', component: routeComponentMap.User },
  { path: 'role', name: '系统角色管理', component: routeComponentMap.Role },
  { path: 'fruit-detect', name: '果蔬双检分析', component: routeComponentMap.FruitDetect },
  { path: 'ai-analysis', name: 'AI智能分析', component: routeComponentMap.AIAnalysis },
  { path: 'prediction', name: '产量预测', component: routeComponentMap.Prediction }
]

const buildManageRoute = () => ({
  path: '/',
  name: MANAGE_ROUTE_NAME,
  component: () => import('../views/Manage.vue'),
  redirect: '/home',
  children: [...baseManageChildren]
})

const normalizeRoutePath = (path) => String(path || '').replace(/^\/+/, '')

const resolveDynamicComponent = (pagePath) => routeComponentMap[pagePath] || null

const appendMenuRoute = (manageRoute, menuItem, existingPaths, existingNames) => {
  const routePath = normalizeRoutePath(menuItem?.path)
  const routeName = menuItem?.name
  const component = resolveDynamicComponent(menuItem?.pagePath)

  if (!routePath || existingPaths.has(routePath) || (routeName && existingNames.has(routeName))) {
    return
  }

  if (!component) {
    console.warn(`Skip dynamic route "${routePath}": unresolved pagePath "${menuItem?.pagePath || ''}"`)
    return
  }

  manageRoute.children.push({
    path: routePath,
    name: routeName || routePath,
    component
  })
  existingPaths.add(routePath)
  if (routeName) {
    existingNames.add(routeName)
  }
}

export const resetRouter = () => {
  if (router.hasRoute(MANAGE_ROUTE_NAME)) {
    router.removeRoute(MANAGE_ROUTE_NAME)
  }
}

export const setRoutes = () => {
  const storeMenus = localStorage.getItem('menus')
  if (!storeMenus) {
    return
  }

  let menus = []
  try {
    menus = JSON.parse(storeMenus)
  } catch (error) {
    console.error('解析菜单数据失败:', error)
    return
  }

  const manageRoute = buildManageRoute()
  const existingPaths = new Set(manageRoute.children.map(route => route.path))
  const existingNames = new Set(
    manageRoute.children
      .map(route => route.name)
      .filter(Boolean)
  )

  menus.forEach(item => {
    appendMenuRoute(manageRoute, item, existingPaths, existingNames)
    if (Array.isArray(item?.children)) {
      item.children.forEach(child => appendMenuRoute(manageRoute, child, existingPaths, existingNames))
    }
  })

  if (!router.hasRoute(MANAGE_ROUTE_NAME)) {
    router.addRoute(manageRoute)
  }
}

setRoutes()

router.beforeEach((to, from, next) => {
  localStorage.setItem('currentPathName', String(to.name || ''))
  store.commit('setPath')

  const whiteList = ['/login', '/register', '/404']
  const user = localStorage.getItem('user')
  if (!whiteList.includes(to.path) && !user) {
    next('/login')
    return
  }

  if (!to.matched.length) {
    const menus = localStorage.getItem('menus')
    next(menus ? '/404' : '/login')
    return
  }

  next()
})

export default router
