import request from '@/utils/request'

/**
 * 获取工作台日历数据（指定年月，每天的健康均值 + 预警数）
 */
export function getCalendarData(year, month) {
  return request({ url: '/dashboard/calendar', method: 'get', params: { year, month } })
}
