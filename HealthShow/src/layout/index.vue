<template>
  <div :class="classObj" class="app-wrapper">
    <div class="app-wrapper__bg"></div>
    <div v-if="device==='mobile'&&sidebar.opened" class="drawer-bg" @click="handleClickOutside" />
    <sidebar class="sidebar-container" />
    <div class="main-container">
      <div :class="{'fixed-header':fixedHeader}">
        <navbar />
      </div>
      <app-main />
    </div>
    <mobile-bottom-nav />
  </div>
</template>

<script>
import { Navbar, Sidebar, AppMain, MobileBottomNav } from './components'
import ResizeMixin from './mixin/ResizeHandler'

export default {
  name: 'Layout',
  components: {
    Navbar,
    Sidebar,
    AppMain,
    MobileBottomNav
  },
  mixins: [ResizeMixin],
  computed: {
    sidebar() {
      return this.$store.state.app.sidebar
    },
    device() {
      return this.$store.state.app.device
    },
    fixedHeader() {
      return this.$store.state.settings.fixedHeader
    },
    classObj() {
      return {
        hideSidebar: !this.sidebar.opened,
        openSidebar: this.sidebar.opened,
        withoutAnimation: this.sidebar.withoutAnimation,
        mobile: this.device === 'mobile'
      }
    }
  },
  methods: {
    handleClickOutside() {
      this.$store.dispatch('app/closeSideBar', { withoutAnimation: false })
    }
  }
}
</script>

<style lang="scss" scoped>
@import "@/styles/mixin.scss";
@import "@/styles/variables.scss";

.app-wrapper {
  @include clearfix;
  position: relative;
  min-height: 100%;
  width: 100%;

  &.mobile.openSidebar {
    position: fixed;
    top: 0;
  }
}

.app-wrapper__bg {
  position: fixed;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(circle at top left, rgba(62, 183, 255, 0.08), transparent 22%),
    radial-gradient(circle at bottom right, rgba(54, 211, 153, 0.06), transparent 26%);
}

.drawer-bg {
  background: rgba(3, 8, 18, 0.62);
  backdrop-filter: blur(6px);
  width: 100%;
  top: 0;
  height: 100%;
  position: absolute;
  z-index: 999;
}

.main-container {
  position: relative;
  z-index: 1;
}

.fixed-header {
  position: fixed;
  top: 0;
  right: 0;
  z-index: 9;
  width: calc(100% - #{$sideBarWidth});
  transition: width 0.28s;
}

.hideSidebar .fixed-header {
  width: calc(100% - 36px);
}

.mobile .fixed-header {
  width: 100%;
}
</style>
