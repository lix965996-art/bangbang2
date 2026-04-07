import axios from 'axios'
import router from "@/router";

const request = axios.create({
    baseURL: process.env.VUE_APP_API_BASE_URL || 'http://localhost:9090',
    timeout: 60000  // 60秒超时，AI接口响应较慢
})

// request 拦截器
// 可以自请求发送前对请求做一些处理
// 比如统一加token，对请求参数统一加密
request.interceptors.request.use(config => {
    if (!(config.data instanceof FormData)) {
        config.headers['Content-Type'] = 'application/json;charset=utf-8';
    } else {
        delete config.headers['Content-Type'];
    }
    let user = null;
    try {
        const userStr = localStorage.getItem("user");
        user = userStr ? JSON.parse(userStr) : null;
    } catch (e) {
        console.error('解析用户信息失败:', e);
        localStorage.removeItem("user"); // 清除损坏的数据
        user = null;
    }
    
    if (user && user.token) {
        config.headers['token'] = user.token;  // 设置请求头
    }

    return config
}, error => {
    return Promise.reject(error)
});

// response 拦截器
// 可以在接口响应后统一处理结果
request.interceptors.response.use(
    response => {
        let res = response.data;
        // 如果是返回的文件
        if (response.config.responseType === 'blob') {
            return res
        }
        // 兼容服务端返回的字符串数据
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res
        }
        // 当权限验证不通过的时候给出提示
        if (res.code === '401') {
            console.warn('Token验证失败，跳转到登录页');
            // 清除无效的用户信息
            localStorage.removeItem("user");
            localStorage.removeItem("menus");
            router.push("/login").catch(err => {
                // 忽略路由重复跳转错误
                if (err.name !== 'NavigationDuplicated') {
                    console.error(err);
                }
            });
        }
        return res;
    },
    error => {
        console.error('请求错误:', error);
        
        // 网络错误处理
        if (!error.response) {
            console.error('网络连接失败');
            return Promise.reject(new Error('网络连接失败，请检查网络设置'));
        }
        
        // HTTP状态码错误处理
        const status = error.response.status;
        switch (status) {
            case 401:
                console.warn('未授权访问，跳转到登录页');
                localStorage.removeItem("user");
                localStorage.removeItem("menus");
                router.push("/login").catch(err => {
                    // 忽略路由重复跳转错误
                    if (err.name !== 'NavigationDuplicated') {
                        console.error(err);
                    }
                });
                break;
            case 403:
                console.warn('权限不足');
                break;
            case 404:
                console.warn('请求的资源不存在');
                break;
            case 500:
                console.error('服务器内部错误');
                break;
            default:
                console.error(`请求失败，状态码: ${status}`);
        }
        
        return Promise.reject(error);
    }
)


export default request
