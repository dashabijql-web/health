<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-page">
        <component :is="Component" :key="route.path" />
      </transition>
    </router-view>
  </section>
</template>

<script>
export default {
  name: 'AppMain'
}
</script>

<style scoped>
.app-main {
  min-height: calc(100vh - 50px);
  width: 100%;
  position: relative;
  overflow-x: hidden;
  overflow-y: auto;
}
@media (max-width: 768px) {
  .app-main {
    /* 54px 底部导航 + iPhone 安全区域 */
    padding-bottom: calc(54px + env(safe-area-inset-bottom, 8px));
  }
}
.fixed-header + .app-main {
  padding-top: 50px;
}

.fade-page-enter-active {
  transition: opacity 0.2s ease;
}
.fade-page-leave-active {
  transition: opacity 0.15s ease;
  position: absolute;
  width: 100%;
}
.fade-page-enter-from,
.fade-page-leave-to {
  opacity: 0;
}
</style>

<style lang="scss">
.el-popup-parent--hidden {
  .fixed-header {
    padding-right: 15px;
  }
}
</style>