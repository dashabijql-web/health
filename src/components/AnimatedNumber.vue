<template>
  <span :class="['animated-number', className]" :style="numberStyle">
    {{ displayValue }}
  </span>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, computed } from 'vue'

const props = defineProps({
  // 目标数值
  value: {
    type: Number,
    default: 0
  },
  // 动画时长（毫秒）
  duration: {
    type: Number,
    default: 2000
  },
  // 小数位数
  decimals: {
    type: Number,
    default: 0
  },
  // 是否显示千分位
  separator: {
    type: Boolean,
    default: true
  },
  // 前缀
  prefix: {
    type: String,
    default: ''
  },
  // 后缀
  suffix: {
    type: String,
    default: ''
  },
  // 字体大小
  fontSize: {
    type: String,
    default: '32px'
  },
  // 字体颜色
  color: {
    type: String,
    default: '#00f6ff'
  },
  // 字体粗细
  fontWeight: {
    type: [String, Number],
    default: 'bold'
  },
  // 自定义类名
  className: {
    type: String,
    default: ''
  },
  // 缓动函数
  ease: {
    type: String,
    default: 'power2.out'
  }
})

// 当前显示的数值
const currentValue = ref(0)
const frameId = ref(null)

const EASING_MAP = {
  linear: (t) => t,
  'power1.out': (t) => 1 - Math.pow(1 - t, 1),
  'power2.out': (t) => 1 - Math.pow(1 - t, 2),
  'power3.out': (t) => 1 - Math.pow(1 - t, 3),
  'power4.out': (t) => 1 - Math.pow(1 - t, 4)
}

// 格式化数字
const formatNumber = (num) => {
  // 保留小数位
  let result = num.toFixed(props.decimals)

  // 添加千分位分隔符
  if (props.separator) {
    const parts = result.split('.')
    parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',')
    result = parts.join('.')
  }

  return props.prefix + result + props.suffix
}

// 计算显示值
const displayValue = computed(() => formatNumber(currentValue.value))

// 计算样式
const numberStyle = computed(() => ({
  fontSize: props.fontSize,
  color: props.color,
  fontWeight: props.fontWeight,
  fontFamily: "'Orbitron', 'Arial Black', sans-serif",
  letterSpacing: '2px',
  textShadow: `0 0 10px ${props.color}40`,
  display: 'inline-block',
  transition: 'all 0.3s ease'
}))

const roundValue = (value) => {
  const factor = Math.pow(10, props.decimals)
  return Math.round(value * factor) / factor
}

const stopAnimation = () => {
  if (frameId.value !== null) {
    cancelAnimationFrame(frameId.value)
    frameId.value = null
  }
}

const resolveEase = (easeName) => EASING_MAP[easeName] || EASING_MAP['power2.out']

// 执行动画
const animateTo = (target) => {
  stopAnimation()

  const from = Number(currentValue.value) || 0
  const to = Number(target) || 0

  if (props.duration <= 0 || from === to) {
    currentValue.value = roundValue(to)
    return
  }

  const easeFn = resolveEase(props.ease)
  const startedAt = performance.now()

  const step = (now) => {
    const progress = Math.min((now - startedAt) / props.duration, 1)
    const nextValue = from + (to - from) * easeFn(progress)
    currentValue.value = roundValue(progress >= 1 ? to : nextValue)

    if (progress < 1) {
      frameId.value = requestAnimationFrame(step)
      return
    }

    frameId.value = null
  }

  frameId.value = requestAnimationFrame(step)
}

// 监听value变化
watch(() => props.value, (newVal) => {
  animateTo(newVal)
}, { immediate: false })

// 组件挂载时执行初始动画
onMounted(() => {
  currentValue.value = 0
  frameId.value = requestAnimationFrame(() => animateTo(props.value))
})

onBeforeUnmount(() => {
  stopAnimation()
})
</script>

<style scoped>
.animated-number {
  display: inline-block;
  font-variant-numeric: tabular-nums;
  user-select: none;
}

/* 数字跳动动画 */
@keyframes pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}

.animated-number:hover {
  animation: pulse 0.5s ease-in-out;
}

</style>
