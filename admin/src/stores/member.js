import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useMemberStore = defineStore('member', () => {
  // 从 sessionStorage 里恢复登录信息，防止刷新页面后丢失
  const member = ref(JSON.parse(sessionStorage.getItem('member') || '{}'))

  // token 从 member 里取
  const token = computed(() => member.value.token)

  // 是否已登录
  const isLogin = computed(() => !!member.value.token)

  // 保存登录用户信息
  function setMember(data) {
    member.value = data || {}
    sessionStorage.setItem('member', JSON.stringify(member.value))
  }

  // 退出登录 / 清空登录信息
  function clearMember() {
    member.value = {}
    sessionStorage.removeItem('member')
  }

  return {
    member,
    token,
    isLogin,
    setMember,
    clearMember,
  }
})
