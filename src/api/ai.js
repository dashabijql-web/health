import request from '@/utils/request'

/**
 * 查询已有缓存的 AI 健康报告（不触发生成）
 */
export function getCachedAiReport(empCode) {
  return request({ url: '/ai/health-report', method: 'get', params: { empCode } })
}

/**
 * 生成 AI 健康报告
 * @param {string} empCode
 * @param {boolean} force - true: 强制刷新忽略缓存
 */
export function generateAiReport(empCode, force = false) {
  return request({ url: '/ai/health-report/generate', method: 'post', params: { empCode, force }, timeout: 90000 })
}
