import { onBeforeUnmount } from 'vue'

/**
 * 统一管理页面级 interval，避免页面手写重复的 setInterval / clearInterval。
 * @param {() => void | Promise<void>} task
 * @param {number} intervalMs
 * @returns {{ start: (overrideMs?: number) => void, stop: () => void }}
 */
export function useIntervalTask(task, intervalMs) {
  let timer = null

  const stop = () => {
    if (!timer) return
    clearInterval(timer)
    timer = null
  }

  const start = (overrideMs = intervalMs) => {
    stop()
    if (!overrideMs || overrideMs <= 0) return
    timer = setInterval(() => {
      task()
    }, overrideMs)
  }

  onBeforeUnmount(stop)

  return { start, stop }
}
