import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import Antd, { notification } from 'ant-design-vue'
import 'ant-design-vue/dist/reset.css'
import * as Icons from '@ant-design/icons-vue'
import axios from 'axios'
import './assets/js/session-storage'
import './assets/js/tool'
import './assets/js/enums'
import { useMemberStore } from '@/stores/member'

const app = createApp(App)

// 全局注册 Ant Design 图标
const icons = Icons
for (const i in icons) {
  app.component(i, icons[i])
}

/**
 * axios 拦截器
 */
axios.interceptors.request.use(function (config) {
  console.log('请求参数：', config)
  const memberStore = useMemberStore()
  const token = memberStore.token
  if (token) {
    config.headers.token = token
    console.log('请求 headers 增加 token:', token)
  }
  return config
}, error => {
  return Promise.reject(error)
})
axios.interceptors.response.use(function (response) {
  console.log('返回结果：', response)
  return response
}, error => {
  console.log('返回错误：', error)
  const status = error.response?.status
  if (status === 401) {
    console.log('未登录或登录超时，跳到登录页')
    const memberStore = useMemberStore()
    memberStore.clearMember()
    notification.error({ description: '未登录或登录超时' })
    router.push('/login')
  }
  return Promise.reject(error)
})
axios.defaults.baseURL = import.meta.env.VITE_SERVER
console.log('环境：', import.meta.env.MODE)
console.log('服务端：', import.meta.env.VITE_SERVER)

app.use(Antd).use(pinia).use(router).mount('#app')
