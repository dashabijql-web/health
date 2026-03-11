import request from '@/utils/request'

export function getDeptHealthSummary() {
  return request({ url: '/statistics/dept-summary', method: 'get' })
}

export function getMonthlySummary(params) {
  return request({ url: '/statistics/monthly-summary', method: 'get', params })
}

export function getDailyCounts(params) {
  return request({ url: '/statistics/daily-counts', method: 'get', params })
}

export function getWarningTypes(params) {
  return request({ url: '/statistics/warning-types', method: 'get', params })
}
