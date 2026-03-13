import { ref, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'

/**
 * 时钟 composable — 每秒更新当前时间字符串。
 * @param {string} [fmt='YYYY年MM月DD日 HH:mm:ss'] dayjs 格式
 * @returns {{ currentTime: import('vue').Ref<string>, stopClock: () => void }}
 */
export function useClock(fmt = 'YYYY年MM月DD日 HH:mm:ss') {
  const currentTime = ref(dayjs().format(fmt))
  const timer = setInterval(() => { currentTime.value = dayjs().format(fmt) }, 1000)
  const stopClock = () => clearInterval(timer)
  onBeforeUnmount(stopClock)
  return { currentTime, stopClock }
}
