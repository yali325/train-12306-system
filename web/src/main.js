import './assets/main.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'

import Antd, { notification } from 'ant-design-vue'
import App from './App.vue'
import router from './router'
import 'ant-design-vue/dist/reset.css';
import axios from 'axios';
import * as Icons from '@ant-design/icons-vue';
import { useMemberStore } from '@/stores/member'


const app = createApp(App)

const icons = Icons;
for (const i in icons) {
  app.component(i, icons[i]);
}

//Vite环境变量
axios.defaults.baseURL = import.meta.env.VITE_SERVER

console.log('环境：', import.meta.env.MODE)
console.log('服务端：', import.meta.env.VITE_SERVER)

/**
 * axios拦截器
 */
axios.interceptors.request.use(function (config) {
  console.log('请求参数：', config);
  const memberStore = useMemberStore()
  const _token = memberStore.token
  if (_token) {
    config.headers.token = _token;
    console.log("请求headers增加token:", _token);
  }
  return config;
}, error => {
  return Promise.reject(error);
});
axios.interceptors.response.use(function (response) {
  console.log('返回结果：', response);
  return response;
}, error => {
  console.log('返回错误：', error);
  const status = error.response?.status;
  if (status === 401) {
    // 判断状态码是401 跳转到登录页
    console.log("未登录或登录超时，跳到登录页");
    const memberStore = useMemberStore()
    memberStore.clearMember()
    notification.error({ description: "未登录或登录超时" });
    router.push('/login');
  }
  return Promise.reject(error);
});

app.use(createPinia())
app.use(router)
app.use(Antd)

app.mount('#app')
