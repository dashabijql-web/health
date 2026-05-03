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

const menuVariables = {
  menuBg: '#304156',
  menuText: '#bfcbd9',
  menuActiveText: '#409EFF'
}

const NAV_GROUPS = [
  { key: 'command', title: '指挥中心', icon: 'HomeFilled', order: 1, path: '/__nav__/command' },
  { key: 'monitor', title: '监测中心', icon: 'DataAnalysis', order: 2, path: '/__nav__/monitor' },
  { key: 'warning', title: '预警中心', icon: 'Bell', order: 3, path: '/__nav__/warning' },
  { key: 'people', title: '人员中心', icon: 'UserFilled', order: 4, path: '/__nav__/people' },
  { key: 'report', title: '报告与AI', icon: 'Document', order: 5, path: '/__nav__/report' },
  { key: 'admin', title: '系统管理', icon: 'Setting', order: 6, path: '/__nav__/admin' }
]

function resolvePath(basePath, routePath = '') {
  if (!routePath) return basePath || '/'
  if (routePath.startsWith('/')) return routePath
  if (!basePath) return routePath.startsWith('/') ? routePath : `/${routePath}`
  return `${basePath.replace(/\/$/, '')}/${routePath}`
}

function collectLeafRoutes(routes, basePath = '') {
  const leaves = []

  for (const route of routes || []) {
    const fullPath = resolvePath(basePath, route.path)

    if (route.children && route.children.length > 0) {
      leaves.push(...collectLeafRoutes(route.children, fullPath))
      continue
    }

    if (route.hidden || !route.meta?.navGroup) continue

    leaves.push({
      ...route,
      path: fullPath,
      children: undefined
    })
  }

  return leaves
}

function buildGroupedRoutes(routes) {
  const leaves = collectLeafRoutes(routes)

  return NAV_GROUPS
    .map((group) => {
      const children = leaves
        .filter((route) => route.meta?.navGroup === group.key)
        .sort((a, b) => (a.meta?.navOrder || 999) - (b.meta?.navOrder || 999))
        .map((route) => ({
          ...route,
          path: route.path.startsWith('/') ? route.path : `/${route.path}`
        }))

      return {
        path: group.path,
        name: `NavGroup${group.key}`,
        alwaysShow: true,
        meta: {
          title: group.title,
          icon: group.icon
        },
        children
      }
    })
    .filter((group) => group.children.length > 0)
}

export default {
  components: { SidebarItem, Logo },
  computed: {
    ...mapGetters(['sidebar']),
    routes() {
      return buildGroupedRoutes(this.$store.state.user.resultAllRoutes)
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
