import { onBeforeUnmount } from 'vue'

/**
 * 统一管理页面级一次性 timeout，避免页面手写重复的 setTimeout / clearTimeout。
 * @param {() => void | Promise<void>} task
 * @param {number} timeoutMs
 * @returns {{ start: (overrideMs?: number) => void, stop: () => void }}
 */
export function useTimeoutTask(task, timeoutMs) {
  let timer = null

  const stop = () => {
    if (!timer) return
    clearTimeout(timer)
    timer = null
  }

  const start = (overrideMs = timeoutMs) => {
    stop()
    if (!overrideMs || overrideMs <= 0) return
    timer = setTimeout(() => {
      timer = null
      task()
    }, overrideMs)
  }

  onBeforeUnmount(stop)

  return { start, stop }
}
