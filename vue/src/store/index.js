import { createStore } from 'vuex'
import router, {resetRouter} from "@/router";

const store = createStore({
    state: {
        currentPathName: ''
    },
    mutations: {
        setPath (state) {
            state.currentPathName = localStorage.getItem("currentPathName")
        },
        logout() {
            // 清空缓存
            localStorage.removeItem("user")
            localStorage.removeItem("menus")
            router.push("/login")
            // 重置路由
            resetRouter()
        }
    }
})

export default store
