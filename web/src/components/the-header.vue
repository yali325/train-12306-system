<template>
  <a-layout-header class="header">
    <div class="logo">
      <router-link to="/welcome" style="color: white; font-size: 18px">
        甲蛙12306
      </router-link>
    </div>

    <div style="float: right; color: white">
      您好：{{ member.mobile }} &nbsp;&nbsp;
      <router-link to="/login" style="color: white">
        退出登录
      </router-link>
    </div>

    <a-menu
      v-model:selectedKeys="selectedKeys"
      theme="dark"
      mode="horizontal"
      :style="{ lineHeight: '64px' }"
    >
      <a-menu-item key="/welcome">
        <router-link to="/welcome">
          <coffee-outlined /> &nbsp; 欢迎
        </router-link>
      </a-menu-item>

      <a-menu-item key="/passenger">
        <router-link to="/passenger">
          <user-outlined /> &nbsp; 乘车人管理
        </router-link>
      </a-menu-item>

      <a-menu-item key="/ticket">
        <router-link to="/ticket">
          <user-outlined /> &nbsp; 余票查询
        </router-link>
      </a-menu-item>
    </a-menu>
  </a-layout-header>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'
// import store from '@/store'

defineOptions({
  name: 'TheHeaderView',
})

const route = useRoute()

// const member = store.state.member
const member = {
  mobile: '13000000000',
  token: 'test-token',
}
const selectedKeys = ref([])

watch(
  () => route.path,
  newValue => {
    console.log('watch', newValue)
    selectedKeys.value = [newValue]
  },
  { immediate: true },
)
</script>

<style scoped>
.logo {
  float: left;
  height: 31px;
  width: 150px;
  color: white;
  font-size: 20px;
}
</style>
