<template>
  <div v-if="!item.hidden">
    <template v-if="hasOneShowingChild(item.children,item) && (!onlyOneChild.children||onlyOneChild.noShowingChildren)&&!item.alwaysShow">
      <app-link v-if="onlyOneChild.meta" :to="resolvePath(onlyOneChild.path)">
        <el-menu-item :index="resolvePath(onlyOneChild.path)" :class="{'submenu-title-noDropdown':!isNest}">
          <item :icon="onlyOneChild.meta.icon||(item.meta&&item.meta.icon)" :title="onlyOneChild.meta.title" />
        </el-menu-item>
      </app-link>
    </template>

    <el-sub-menu v-else ref="subMenu" :index="resolvePath(item.path)" popper-append-to-body>
      <template #title>
        <!-- 收缩时只显示图标（不用Item组件，直接渲染避免命名冲突） -->
        <el-icon v-if="isCollapse && item.meta && item.meta.icon" style="font-size:18px;">
          <component :is="item.meta.icon" />
        </el-icon>
        <item v-else-if="item.meta" :icon="item.meta && item.meta.icon" :title="item.meta.title" />
      </template>
      <sidebar-item
        v-for="child in item.children"
        :key="child.path"
        :is-nest="true"
        :item="child"
        :base-path="resolvePath(child.path)"
        :is-collapse="isCollapse"
        class="nest-menu"
      />
    </el-sub-menu>
  </div>
</template>

<script>
import { isExternal } from '@/utils/validate'
import Item from './Item.vue'
import AppLink from './Link.vue'
import FixiOSBug from './FixiOSBug.js'

function resolvePath(basePath, routePath) {
  if (!routePath) return basePath
  if (routePath.startsWith('/')) return routePath
  const base = basePath.endsWith('/') ? basePath : basePath + '/'
  return base + routePath
}

export default {
  name: 'SidebarItem',
  components: { Item, AppLink },
  mixins: [FixiOSBug],
  props: {
    item:       { type: Object,  required: true },
    isNest:     { type: Boolean, default: false },
    basePath:   { type: String,  default: '' },
    isCollapse: { type: Boolean, default: false }
  },
  data() {
    // Vue2写法：直接挂实例，不放进响应式return，避免循环引用TDZ
    this.onlyOneChild = null
    return {}
  },
  methods: {
    hasOneShowingChild(children = [], parent) {
      const showingChildren = children.filter(item => {
        if (item.hidden) return false
        this.onlyOneChild = item
        return true
      })
      if (showingChildren.length === 1) return true
      if (showingChildren.length === 0) {
        this.onlyOneChild = { ...parent, path: '', noShowingChildren: true }
        return true
      }
      return false
    },
    resolvePath(routePath) {
      if (isExternal(routePath)) return routePath
      if (isExternal(this.basePath)) return this.basePath
      return resolvePath(this.basePath, routePath)
    }
  }
}
</script>