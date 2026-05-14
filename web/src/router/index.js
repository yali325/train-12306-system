import { createRouter, createWebHistory } from 'vue-router'
import { notification } from 'ant-design-vue'
import { useMemberStore } from '@/stores/member'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login',
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/home',
      name: 'home',
      component: () => import('../views/HomeView.vue'),
      meta: {
        loginRequire: true,
      },
    },
  ],
})

/**
 * 路由登录拦截
 */
router.beforeEach((to, from) => {
  const needLogin = to.matched.some(item => {
    console.log(item, '是否需要登录校验：', item.meta.loginRequire || false)
    return item.meta.loginRequire
  })

  if (needLogin) {
    const memberStore = useMemberStore()
    const member = memberStore.member

    console.log('页面登录校验开始：', member)

    if (!member.token) {
      console.log('用户未登录或登录超时！')
      notification.error({ description: '未登录或登录超时' })

      return '/login'
    }
  }

  return true
})

export default router
