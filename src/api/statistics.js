import request from '@/utils/request'

export function getWarningTypes(params) {
  return request({ url: '/statistics/warning-types', method: 'get', params })
}
