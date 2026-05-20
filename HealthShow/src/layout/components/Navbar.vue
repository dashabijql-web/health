<template>
  <div class="navbar">
    <div class="navbar__left">
      <hamburger :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />
      <breadcrumb class="breadcrumb-container" />
    </div>

    <div class="right-menu">
      <div v-if="canSwitchDataSource" class="source-switch">
        <span class="source-switch__label">数据源</span>
        <el-select v-model="dataSource" size="small" class="source-switch__select" @change="handleDataSourceChange">
          <el-option label="新库" value="new" />
          <el-option label="老库" value="old" />
        </el-select>
      </div>

      <div class="warning-badge-btn" @click="$router.push('/alert-management/notifications')" title="点击查看消息通知中心">
        <el-badge :value="pendingWarnings" :hidden="pendingWarnings === 0" :max="99" type="danger">
          <span class="warn-icon">
            <el-icon><Bell /></el-icon>
          </span>
        </el-badge>
        <span v-if="pendingWarnings > 0" class="warn-label">{{ pendingWarnings }} 条待处理</span>
      </div>

      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper">
          <span class="user-name">{{ name }}</span>
          <img
            :src="avatarUrl"
            class="user-avatar"
            @error="handleAvatarError"
          >
          <el-icon class="avatar-arrow"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="user-dropdown">
            <router-link to="/">
              <el-dropdown-item>首页</el-dropdown-item>
            </router-link>
            <el-dropdown-item divided @click="logout">
              <span style="display:block;">退出</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import { ArrowDown, Bell } from '@element-plus/icons-vue'
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import Hamburger from '@/components/Hamburger/index.vue'
import request from '@/utils/request'
import { getDataSource, setDataSource } from '@/utils/data-source'
import { createIntervalTask } from '@/utils/task-timer'

export default {
  components: {
    ArrowDown,
    Bell,
    Breadcrumb,
    Hamburger
  },
  data() {
    return {
      pendingWarnings: 0,
      dataSource: getDataSource(),
      defaultAvatar: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4MCIgaGVpZ2h0PSI4MCIgdmlld0JveD0iMCAwIDgwIDgwIj48cmVjdCB3aWR0aD0iODAiIGhlaWdodD0iODAiIGZpbGw9IiMxYTRkOGYiLz48Y2lyY2xlIGN4PSI0MCIgY3k9IjI4IiByPSIxMiIgZmlsbD0iIzAwZDRmZiIvPjxwYXRoIGQ9Ik0yMCA1NnEwLTE2IDE2LTE2aDE2cTE2IDAgMTYgMTZ6IiBmaWxsPSIjMDBkNGZmIi8+PC9zdmc+',
      avatarError: false
    }
  },
  mounted() {
    this.fetchPendingWarnings()
    this._warningPollTask = createIntervalTask(() => this.fetchPendingWarnings(), 30000)
    this._warningPollTask.start()
  },
  beforeUnmount() {
    this._warningPollTask?.stop()
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'name',
      'canSwitchDataSource'
    ]),
    avatarUrl() {
      if (this.avatarError) {
        return this.defaultAvatar
      }
      if (this.avatar) {
        if (this.avatar.startsWith('http')) {
          return this.avatar
        }
        return `${import.meta.env.VITE_BASE_API || ''}${this.avatar}`
      }
      return this.defaultAvatar
    }
  },
  methods: {
    async fetchPendingWarnings() {
      try {
        const res = await request({ url: '/risk-warning/list', method: 'get', params: { page: 1, size: 1, handled: false } })
        if (res.code === 200) {
          this.pendingWarnings = res.data?.total || 0
        }
      } catch (e) { /* silent */ }
    },
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    handleDataSourceChange(value) {
      setDataSource(value)
      window.location.reload()
    },
    async logout() {
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login?redirect=${this.$route.fullPath}`)
    },
    handleAvatarError() {
      this.avatarError = true
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 50px;
  padding: 0 18px 0 6px;
  overflow: hidden;
  position: relative;
  background: rgba(8, 18, 32, 0.86);
  border-bottom: 1px solid rgba(133, 175, 220, 0.12);
  box-shadow: 0 10px 34px rgba(2, 8, 20, 0.24);
  backdrop-filter: blur(18px);
}

.navbar__left {
  min-width: 0;
  display: flex;
  align-items: center;
  flex: 1;
}

.hamburger-container {
  line-height: 46px;
  height: 100%;
  cursor: pointer;
  transition: background .3s;
  -webkit-tap-highlight-color: transparent;

  &:hover {
    background: rgba(62, 183, 255, 0.08);
  }
}

.breadcrumb-container {
  min-width: 0;
}

.right-menu {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 100%;

  &:focus {
    outline: none;
  }
}

.warning-badge-btn,
.source-switch,
.avatar-wrapper {
  border: 1px solid rgba(133, 175, 220, 0.12);
  background: rgba(13, 28, 47, 0.7);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.04);
}

.warning-badge-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 0 12px;
  height: 36px;
  border-radius: 999px;
  transition: background .3s, border-color .3s;

  &:hover {
    background: rgba(248, 113, 113, 0.12);
    border-color: rgba(248, 113, 113, 0.24);
  }
}

.warn-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #ff9c9c;
  font-size: 18px;
}

.warn-label {
  color: #ffb4b4;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.source-switch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  padding: 0 10px 0 12px;
  border-radius: 999px;

  &__label {
    color: var(--text-secondary);
    font-size: 12px;
    white-space: nowrap;
  }

  &__select {
    width: 96px;
  }
}

.avatar-container {
  height: 100%;
}

.avatar-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
  height: 36px;
  margin-top: 7px;
  padding: 0 12px 0 10px;
  border-radius: 999px;
}

.user-name {
  color: #dff2ff;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.user-avatar {
  cursor: pointer;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid rgba(95, 189, 255, 0.22);
  transition: all 0.3s ease;
  object-fit: cover;
  background-color: #1a4d8f;

  &:hover {
    border-color: rgba(95, 189, 255, 0.5);
  }
}

.avatar-arrow {
  color: var(--text-secondary);
  font-size: 12px;
}

@media (max-width: 768px) {
  .navbar {
    padding-right: 12px;
  }

  :deep(.app-breadcrumb.breadcrumb-container) {
    display: none !important;
  }

  .breadcrumb-container,
  .warn-label,
  .source-switch__label,
  .user-name {
    display: none;
  }

  .right-menu {
    gap: 8px;
  }

  .warning-badge-btn,
  .source-switch,
  .avatar-wrapper {
    padding-left: 10px;
    padding-right: 10px;
  }
}
</style>
