<template>
  <!--
    ╔══════════════════════════════════════════════════════════════════╗
    ║              登录页面模板（新手必读）                               ║
    ╚══════════════════════════════════════════════════════════════════╝

    【Vue 单文件组件（SFC）结构说明】

    .vue 文件分三个块：
      <template>：HTML 模板（页面结构）
      <script>：JavaScript 逻辑（数据、方法）
      <style>：CSS 样式（支持 SCSS/Less 等预处理器）

    【页面布局说明】

    整个页面是一个深色背景的登录框，包含：
    - 顶部标题"登录"
    - 用户名输入框（带用户图标）
    - 密码输入框（带密码图标 + 显示/隐藏切换）
    - 登录按钮（点击后发请求，有 loading 效果）
    - 底部提示默认账号密码

    【Element Plus 组件说明】

    el-form：表单容器
      :model="loginForm" → 将表单与 loginForm 数据对象绑定
      :rules="loginRules" → 表单验证规则
      ref="loginForm"     → 给表单设置引用名（通过 this.$refs.loginForm 操作）

    el-form-item：表单项（包裹输入框，显示验证错误消息）
      prop="username" → 指定验证 loginRules 中的哪条规则

    el-input：输入框
      v-model="loginForm.username" → 双向数据绑定（输入框和数据同步）
      :type="passwordType" → 动态 type：'password' 隐藏，'' 显示
      @keyup.enter="handleLogin" → 按 Enter 键触发登录
  -->
  <div class="login-container">
    <!-- 登录表单 -->
    <el-form
      ref="loginForm"
      :model="loginForm"
      :rules="loginRules"
      class="login-form"
      auto-complete="on"
      label-position="left"
    >
      <!-- 标题 -->
      <div class="title-container">
        <h3 class="title">登录</h3>
      </div>

      <!-- 用户名输入框 -->
      <el-form-item prop="username">
        <!-- SVG 图标（用户图标） -->
        <span class="svg-container">
          <svg-icon icon-class="user" />
        </span>
        <el-input
          ref="username"
          v-model="loginForm.username"
          placeholder="Username"
          name="username"
          type="text"
          tabindex="1"
          auto-complete="on"
        />
      </el-form-item>

      <!-- 密码输入框 -->
      <el-form-item prop="password">
        <!-- SVG 图标（密码图标） -->
        <span class="svg-container">
          <svg-icon icon-class="password" />
        </span>
        <!--
          :key="passwordType"：
          当 passwordType 变化时，强制重新渲染输入框
          这是为了触发浏览器自动填充行为（解决 type 切换时的浏览器兼容问题）
        -->
        <el-input
          :key="passwordType"
          ref="password"
          v-model="loginForm.password"
          :type="passwordType"
          placeholder="Password"
          name="password"
          tabindex="2"
          auto-complete="on"
          @keyup.enter="handleLogin"
        />
        <!-- 显示/隐藏密码的眼睛图标按钮 -->
        <span class="show-pwd" @click="showPwd">
          <!-- 根据 passwordType 动态切换图标：关眼睛 / 开眼睛 -->
          <svg-icon :icon-class="passwordType === 'password' ? 'eye' : 'eye-open'" />
        </span>
      </el-form-item>

      <!--
        登录按钮
        :loading="loading" → loading=true 时按钮显示旋转动画，防止重复点击
        @click.prevent → 阻止 form 默认提交行为（防止页面刷新）
      -->
      <el-button
        :loading="loading"
        type="primary"
        style="width:100%;margin-bottom:30px;"
        @click.prevent="handleLogin"
      >
        登录
      </el-button>

      <!-- 开发环境测试账号提示 -->
      <div class="tips" @click="showHint=!showHint" style="cursor:pointer;user-select:none">
        <span style="color:#8ba6c8;font-size:12px">{{ showHint ? '▲ 隐藏账号提示' : '▼ 测试账号提示' }}</span>
        <span v-if="showHint" style="margin-left:16px;color:#aac4e0">admin / admin123</span>
      </div>

    </el-form>
  </div>
</template>

<script>
/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║              登录页面逻辑（新手必读）                               ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * 【Options API vs Composition API】
 *
 * 本组件使用 Vue 的 Options API（export default { data(), methods() }）
 * Vue 3 还支持更新的 Composition API（setup() 函数风格）
 * 两者功能等价，本项目用 Options API（更接近 Vue 2，上手简单）
 *
 * 【登录流程】
 *
 * 用户点击登录按钮 → handleLogin()
 *   ↓ 表单验证
 * this.$refs.loginForm.validate()
 *   ↓ 验证通过
 * loading = true（显示加载动画）
 *   ↓
 * this.$store.dispatch('user/login', loginForm)
 *   ↓ Vuex action：调用 POST /auth/login
 *   ↓ 成功：Token 存入 Cookie + store
 *   ↓ 跳转到目标页（或首页）
 * loading = false
 *
 * 【redirect 参数说明】
 *
 * 当未登录用户访问 /health-monitor/dashboard 时，
 * permission.js 会将其重定向到：
 *   /login?redirect=%2Fhealth-monitor%2Fdashboard
 *
 * 登录成功后，handleLogin 读取 this.redirect 参数，
 * 跳回用户原本想访问的页面（而不是首页）。
 *
 * 安全过滤：
 *   不能跳到 /404 或 /login（这两个路径跳转没意义，改为跳首页）
 *
 * 【$nextTick 说明】
 *
 * showPwd() 方法切换密码显示后，调用：
 *   this.$nextTick(() => { this.$refs.password.focus() })
 *
 * nextTick：等待 Vue 完成当前的 DOM 更新后再执行回调。
 * 因为 DOM 更新是异步的（批量更新），立即 focus() 可能操作到旧 DOM。
 * nextTick 确保操作的是新渲染的 DOM。
 */
import { validUsername } from '@/utils/validate'

export default {
  name: 'Login',

  /**
   * data()：定义组件的响应式数据
   * 函数返回对象（而不是直接写对象）是为了确保多实例时数据隔离
   */
  data() {
    // ─── 自定义验证函数 ──────────────────────────────────────────

    /**
     * 用户名验证函数（目前已被注释掉，任意输入都能过）
     * @param {Object} rule - 验证规则对象
     * @param {string} value - 当前输入值
     * @param {Function} callback - 验证结果回调：callback() = 通过，callback(new Error) = 失败
     */
    const validateUsername = (rule, value, callback) => {
      if (!validUsername(value)) {
        callback(new Error('Please enter the correct user name'))
      } else {
        callback()
      }
    }

    /**
     * 密码验证函数（目前已被注释掉）
     * 密码长度至少6位
     */
    const validatePassword = (rule, value, callback) => {
      if (value.length < 6) {
        callback(new Error('The password can not be less than 6 digits'))
      } else {
        callback()
      }
    }

    return {
      showHint: false,
      /**
       * 表单数据对象
       * 通过 v-model 与输入框双向绑定
       * 默认填入 admin 账号便于开发时快速登录（生产环境应清空）
       */
      loginForm: {
        username: 'admin',
        password: 'admin123'
      },

      /**
       * 表单验证规则
       * 目前注释掉了验证规则（登录任意输入都可以尝试，由后端验证）
       * 如需启用前端验证，取消注释以下 loginRules 配置即可：
       *   username: [{ required: true, trigger: 'blur', validator: validateUsername }]
       *   password: [{ required: true, trigger: 'blur', validator: validatePassword }]
       */
      loginRules: {
        // username: [{ required: true, trigger: 'blur', validator: validateUsername }],
        // password: [{ required: true, trigger: 'blur', validator: validatePassword }]
      },

      /**
       * 登录按钮 loading 状态
       * true：显示旋转动画（防止重复点击）
       * false：正常状态（可点击）
       */
      loading: false,

      /**
       * 密码输入框的 type 属性
       * 'password'：隐藏输入（显示 ****）
       * ''（空字符串）：显示原文
       */
      passwordType: 'password',

      /**
       * 登录成功后要跳转的目标路径
       * 从 URL 的 ?redirect=xxx 参数中读取
       * 例如：访问 /dashboard 被重定向到登录页时，redirect = '/dashboard'
       */
      redirect: undefined
    }
  },

  watch: {
    /**
     * 监听路由变化，实时更新 redirect 参数
     * immediate: true 表示组件创建时立即执行一次（读取初始 redirect）
     *
     * 为什么用 watch 而不是在 mounted 中读一次？
     * 因为路由可能在组件已挂载后发生变化（如浏览器前进/后退）
     */
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect
      },
      immediate: true
    }
  },

  methods: {
    /**
     * 切换密码显示/隐藏
     * 点击眼睛图标时调用
     */
    showPwd() {
      if (this.passwordType === 'password') {
        this.passwordType = ''          // 切换为明文显示
      } else {
        this.passwordType = 'password'  // 切换为密码隐藏
      }
      // 等待 DOM 更新后，将焦点移回密码输入框（方便用户继续输入）
      this.$nextTick(() => {
        this.$refs.password.focus()
      })
    },

    /**
     * 处理登录逻辑
     * 点击登录按钮或按 Enter 键时调用
     */
    handleLogin() {
      /**
       * validate()：执行表单验证（检查 loginRules 中的规则）
       * 回调参数 valid = true 表示验证全部通过，false 表示有错误
       */
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          // ─── 验证通过，执行登录 ───────────────────────────────────

          this.loading = true  // 开启 loading 动画

          /**
           * dispatch('user/login', this.loginForm)：
           * 调用 Vuex store 中 user 模块的 login action
           * action 内部：调用 POST /auth/login → 成功则存 Token
           *
           * .then()：登录成功后的跳转逻辑
           * .catch()：登录失败（用户名/密码错误等），关闭 loading
           */
          this.$store.dispatch('user/login', this.loginForm).then(() => {
            // 登录完成后，先拉取用户信息并注入业务路由，再跳目标页。
            // 否则首次 push 到业务页时，Vue Router 会先对“未注册路由”报警告。
            return this.$store.dispatch('user/getInfo').then(() => {
              /**
               * 登录成功，执行跳转
               *
               * 安全过滤 redirect 参数：
               *   - redirect 存在且不是 /404 也不是 /login → 跳转到 redirect 指定的页面
               *   - 否则 → 跳转到首页 /
               *
               * 为什么过滤 /404 和 /login？
               *   - 跳到 /404 对用户没意义
               *   - 跳到 /login 会造成循环（刚登录又跳登录页）
               */
              const safePath = (this.redirect && this.redirect !== '/404' && this.redirect !== '/login')
                ? this.redirect
                : '/health-monitor/dashboard'

              this.$router.push({ path: safePath })  // 跳转到目标页
              this.loading = false                    // 关闭 loading
            })
          }).catch(() => {
            // 登录失败（request.js 的响应拦截器会显示错误提示）
            this.loading = false
          })

        } else {
          // ─── 验证未通过（输入格式有误）─────────────────────────────
          return false  // 表单校验未通过，阻止提交
        }
      })
    }
  }
}
</script>

<!-- 非 scoped 样式：影响当前组件内的 Element Plus 组件（需要穿透组件作用域） -->
<style lang="scss">
/* 登录页颜色变量 */
$bg: #283443;           /* 输入框背景色（深蓝色） */
$light_gray: #fff;      /* 文字颜色（白色） */
$cursor: #fff;          /* 光标颜色（白色） */

/*
  重置 Element Plus 输入框样式
  让输入框融入深色背景主题

  注意：这里不用 scoped，因为需要覆盖 Element Plus 内部组件的样式
  Element Plus 2.x 的输入框结构变了，直接选 .el-input__wrapper 和 .el-input__inner
*/
.login-container {
  /* 输入框外层包装（Element Plus 2.x 新增的 wrapper） */
  .el-input__wrapper {
    background: transparent !important;  /* 透明背景（显示出输入框所在的深色区域） */
    border: 0 !important;                /* 去掉边框 */
    border-radius: 0 !important;         /* 去掉圆角 */
    box-shadow: none !important;         /* 去掉阴影（Element Plus 默认有 focus 阴影） */
    padding: 0 !important;
    height: 47px;
    width: 85%;
  }

  /* 实际的 input 元素 */
  .el-input__inner {
    background: transparent !important;
    border: 0 !important;
    -webkit-appearance: none;            /* 取消 Safari 默认样式 */
    border-radius: 0;
    padding: 12px 5px 12px 15px;
    color: $light_gray !important;       /* 输入文字白色 */
    height: 47px;
    caret-color: $cursor;                /* 光标颜色 */

    /* 浏览器自动填充时的样式（防止自动填充变白色背景） */
    &:-webkit-autofill {
      box-shadow: 0 0 0px 1000px $bg inset !important;       /* 用内阴影覆盖自动填充背景 */
      -webkit-text-fill-color: $cursor !important;            /* 自动填充文字颜色 */
    }
  }

  /* 表单项（包含输入框的整行区域） */
  .el-form-item {
    border: 1px solid rgba(255, 255, 255, 0.1);   /* 半透明白色边框 */
    background: rgba(0, 0, 0, 0.1);               /* 半透明黑色背景 */
    border-radius: 5px;
    color: #454545;
  }
}
</style>

<!-- scoped 样式：只影响当前组件自己的元素 -->
<style lang="scss" scoped>
$bg: #2d3a4b;           /* 整个登录页背景色 */
$dark_gray: #889aa4;    /* 图标颜色（灰蓝色） */
$light_gray: #eee;      /* 标题颜色（浅灰） */

/* 登录页容器 */
.login-container {
  min-height: 100%;
  width: 100%;
  overflow: hidden;
  /* 背景图（项目根目录 src/assets/1.png） */
  background: url(@/assets/1.png);
  background-size: 100% 100%;    /* 图片拉伸填满整个屏幕 */

  /* 登录表单卡片 */
  .login-form {
    position: relative;
    width: 520px;
    max-width: 100%;               /* 移动端不超出屏幕 */
    padding: 160px 35px 0;        /* 顶部留白（让表单居于屏幕中偏上） */
    margin: 0 auto;               /* 水平居中 */
    overflow: hidden;
  }

  /* 底部账号密码提示文字 */
  .tips {
    font-size: 14px;
    color: #fff;
    margin-bottom: 10px;

    span:first-of-type {
      margin-right: 16px;
    }
  }

  /* 输入框左侧的 SVG 图标区域 */
  .svg-container {
    padding: 6px 5px 6px 15px;
    color: $dark_gray;
    vertical-align: middle;
    width: 30px;
    display: inline-block;
  }

  /* 标题区域 */
  .title-container {
    position: relative;

    .title {
      font-size: 26px;
      color: $light_gray;
      margin: 0px auto 40px auto;
      text-align: center;
      font-weight: bold;
    }
  }

  /* 密码输入框右侧的眼睛图标按钮 */
  .show-pwd {
    position: absolute;
    right: 10px;
    top: 7px;
    font-size: 16px;
    color: $dark_gray;
    cursor: pointer;              /* 鼠标悬停显示手型 */
    user-select: none;            /* 禁止文字选中（点击时防止意外选中图标） */
  }
}
</style>
