<template>
  <div :class="{'has-logo':showLogo}">
    <logo v-if="showLogo" :collapse="isCollapse" />
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="variables.menuBg"
        :text-color="variables.menuText"
        :unique-opened="false"
        :active-text-color="variables.menuActiveText"
        :collapse-transition="false"
        mode="vertical"
      >
        <sidebar-item
          v-for="route in routes"
          :key="route.path"
          :item="route"
          :base-path="route.path"
          :is-collapse="isCollapse"
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import Logo from './Logo.vue'
import SidebarItem from './SidebarItem.vue'
import { buildGroupedMenuRoutes } from '@/layout/menu/navigation.mjs'

const menuVariables = {
  menuBg: '#304156',
  menuText: '#bfcbd9',
  menuActiveText: '#409EFF'
}

export default {
  components: { SidebarItem, Logo },
  computed: {
    ...mapGetters(['sidebar']),
    routes() {
      return buildGroupedMenuRoutes(this.$store.state.user.resultAllRoutes)
    },
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      if (meta.activeMenu) return meta.activeMenu
      return path
    },
    showLogo() {
      return this.$store.state.settings.sidebarLogo
    },
    variables() {
      return menuVariables
    },
    isCollapse() {
      return !this.sidebar.opened
    }
  }
}
</script>

<style lang="scss" scoped>
:deep(.el-menu--vertical) {
  /* overflow: hidden removed — allow el-scrollbar to scroll */
}

/* 收缩时：只隐藏文字标签，保留图标 */
:deep(.el-menu--collapse) {
  .menu-title {
    display: none !important;
  }

  .el-submenu__icon-arrow,
  .el-sub-menu__icon-arrow {
    display: none !important;
  }

  .el-menu-item,
  .el-submenu__title,
  .el-sub-menu__title {
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    padding: 0 !important;
    width: 36px !important;

    .sub-el-icon, .svg-icon, svg {
      margin: 0 !important;
      font-size: 18px;
    }
  }
}

:deep(.el-menu--vertical.el-menu--collapse) {
  .el-submenu > .el-submenu__title,
  .el-sub-menu > .el-sub-menu__title {
    overflow: hidden;
    white-space: nowrap;
  }
}
</style>
