import { ref, onBeforeUnmount } from 'vue'
import dayjs from 'dayjs'
import { useIntervalTask } from '@/composables/useIntervalTask'

/**
 * 时钟 composable — 每秒更新当前时间字符串。
 * @param {string} [fmt='YYYY年MM月DD日 HH:mm:ss'] dayjs 格式
 * @returns {{ currentTime: import('vue').Ref<string>, startClock: () => void, stopClock: () => void }}
 */
export function useClock(fmt = 'YYYY年MM月DD日 HH:mm:ss') {
  const currentTime = ref(dayjs().format(fmt))
  const { start, stop } = useIntervalTask(() => {
    currentTime.value = dayjs().format(fmt)
  }, 1000)

  start()
  onBeforeUnmount(stop)
  return { currentTime, startClock: start, stopClock: stop }
}
