import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementPlus, { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/gloable.css'
import './assets/green-theme.css'
import request from '@/utils/request'

const app = createApp(App)

app.use(router)
app.use(store)
app.use(ElementPlus, { size: 'small' })

app.config.globalProperties.request = request
app.config.globalProperties.$message = ElMessage
app.config.globalProperties.$notify = ElNotification
app.config.globalProperties.$alert = ElMessageBox.alert
app.config.globalProperties.$confirm = ElMessageBox.confirm
app.config.globalProperties.$prompt = ElMessageBox.prompt
app.config.globalProperties.$set = (target, key, value) => {
  if (target && typeof target === 'object') {
    target[key] = value
  }
  return value
}
app.config.globalProperties.$delete = (target, key) => {
  if (target && typeof target === 'object') {
    delete target[key]
  }
}

app.mixin({
  beforeUnmount() {
    const legacyHook = this.$options.beforeDestroy
    if (typeof legacyHook === 'function') {
      legacyHook.call(this)
    } else if (Array.isArray(legacyHook)) {
      legacyHook.forEach(hook => hook.call(this))
    }
  },
  unmounted() {
    const legacyHook = this.$options.destroyed
    if (typeof legacyHook === 'function') {
      legacyHook.call(this)
    } else if (Array.isArray(legacyHook)) {
      legacyHook.forEach(hook => hook.call(this))
    }
  }
})

app.mount('#app')
