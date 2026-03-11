<template>
  <div class="navbar">
    <hamburger :is-active="sidebar.opened" class="hamburger-container" @toggleClick="toggleSideBar" />

    <breadcrumb class="breadcrumb-container" />

    <div class="right-menu">
      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper">
          <span class="user-name">{{ name }}</span>
          <img
            :src="avatarUrl"
            class="user-avatar"
            @error="handleAvatarError"
          >
          <i class="el-icon-caret-bottom" />
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
import Breadcrumb from '@/components/Breadcrumb/index.vue'
import Hamburger from '@/components/Hamburger/index.vue'

export default {
  components: {
    Breadcrumb,
    Hamburger
  },
  data() {
    return {
      // 默认头像（SVG格式）
      defaultAvatar: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI4MCIgaGVpZ2h0PSI4MCIgdmlld0JveD0iMCAwIDgwIDgwIj48cmVjdCB3aWR0aD0iODAiIGhlaWdodD0iODAiIGZpbGw9IiMxYTRkOGYiLz48Y2lyY2xlIGN4PSI0MCIgY3k9IjI4IiByPSIxMiIgZmlsbD0iIzAwZDRmZiIvPjxwYXRoIGQ9Ik0yMCA1NnEwLTE2IDE2LTE2aDE2cTE2IDAgMTYgMTZ6IiBmaWxsPSIjMDBkNGZmIi8+PC9zdmc+',
      avatarError: false
    }
  },
  computed: {
    ...mapGetters([
      'sidebar',
      'avatar',
      'name'
    ]),
    avatarUrl() {
      // 如果头像加载失败，使用默认头像
      if (this.avatarError) {
        return this.defaultAvatar
      }
      // 如果有用户头像，使用用户头像
      if (this.avatar) {
        // 如果是完整URL，直接使用
        if (this.avatar.startsWith('http')) {
          return this.avatar
        }
        // 如果是相对路径，添加基础路径
        return `${import.meta.env.VITE_BASE_API || ''}${this.avatar}`
      }
      // 否则使用默认头像
      return this.defaultAvatar
    }
  },
  methods: {
    toggleSideBar() {
      this.$store.dispatch('app/toggleSideBar')
    },
    async logout() {
      await this.$store.dispatch('user/logout')
      this.$router.push(`/login?redirect=${this.$route.fullPath}`)
    },
    // 头像加载失败时的处理
    handleAvatarError() {
      this.avatarError = true
    }
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #0d2847;
  border-bottom: 1px solid #1a4d8f;
  box-shadow: 0 2px 8px rgba(0, 212, 255, 0.1);

  .hamburger-container {
    line-height: 46px;
    height: 100%;
    float: left;
    cursor: pointer;
    transition: background .3s;
    -webkit-tap-highlight-color:transparent;

    &:hover {
      background: rgba(0, 212, 255, 0.1);
    }
  }

  .breadcrumb-container {
    float: left;
  }

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;

    &:focus {
      outline: none;
    }

    .right-menu-item {
      display: inline-block;
      padding: 0 8px;
      height: 100%;
      font-size: 18px;
      color: #8ba6c8;
      vertical-align: text-bottom;

      &.hover-effect {
        cursor: pointer;
        transition: background .3s;

        &:hover {
          background: rgba(0, 212, 255, 0.1);
        }
      }
    }

    .avatar-container {
      margin-right: 30px;

      .avatar-wrapper {
        margin-top: 5px;
        position: relative;
        display: flex;
        align-items: center;
        gap: 8px;

        .user-name {
          color: #00d4ff;
          font-size: 14px;
          font-weight: 500;
          letter-spacing: 0.5px;
          white-space: nowrap;
        }

        .user-avatar {
          cursor: pointer;
          width: 40px;
          height: 40px;
          border-radius: 10px;
          border: 2px solid #1a4d8f;
          transition: all 0.3s ease;
          /* 🔧 确保图片正确显示 */
          object-fit: cover;
          background-color: #1a4d8f;

          &:hover {
            border-color: #00d4ff;
            box-shadow: 0 0 10px rgba(0, 212, 255, 0.3);
          }
        }

        .el-icon-caret-bottom {
          cursor: pointer;
          position: absolute;
          right: -20px;
          top: 25px;
          font-size: 12px;
          color: #8ba6c8;
        }
      }
    }
  }
}

</style>
