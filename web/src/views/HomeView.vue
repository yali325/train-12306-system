<template>
  <a-layout id="components-layout-demo-top-side-2">
    <TheHeaderView />

    <a-layout>
      <TheSiderView />

      <a-layout-content
        :style="{
          background: '#fff',
          padding: '24px',
          margin: 0,
          minHeight: '280px',
        }"
      >
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import TheHeaderView from '@/components/the-header.vue'
import TheSiderView from '@/components/the-sider.vue'

defineOptions({
  name: 'HomeView',
})
</script>

<style scoped></style>
