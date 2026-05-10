export function createIntervalTask(task, defaultDelay) {
  let timer = null

  const stop = () => {
    if (timer === null) return
    clearInterval(timer)
    timer = null
  }

  const start = (delay = defaultDelay) => {
    stop()
    if (!delay || delay <= 0) return
    timer = setInterval(() => {
      task()
    }, delay)
  }

  return { start, stop }
}

export function createTimeoutTask(task, defaultDelay) {
  let timer = null

  const stop = () => {
    if (timer === null) return
    clearTimeout(timer)
    timer = null
  }

  const start = (delay = defaultDelay) => {
    stop()
    if (!delay || delay <= 0) return
    timer = setTimeout(() => {
      timer = null
      task()
    }, delay)
  }

  return { start, stop }
}

export function createEventBinding(getTarget, eventName, listener, options) {
  let target = null

  const stop = () => {
    if (!target) return
    target.removeEventListener(eventName, listener, options)
    target = null
  }

  const start = () => {
    const nextTarget = typeof getTarget === 'function' ? getTarget() : getTarget
    if (!nextTarget || typeof nextTarget.addEventListener !== 'function') return
    if (target === nextTarget) return
    stop()
    nextTarget.addEventListener(eventName, listener, options)
    target = nextTarget
  }

  return { start, stop }
}
