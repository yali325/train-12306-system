import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useMemberStore = defineStore('member', () => {
  const member = ref(JSON.parse(sessionStorage.getItem('member') || '{}'))

  const token = computed(() => member.value.token)
  const isLogin = computed(() => !!member.value.token)

  function setMember(data) {
    member.value = data || {}
    sessionStorage.setItem('member', JSON.stringify(member.value))
  }

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
