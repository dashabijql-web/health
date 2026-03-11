import request from '@/utils/request'

/**
 * 获取睡眠统计概览
 */
export function getSleepOverview() {
  return request({
    url: '/sleep/overview',
    method: 'get'
  })
}

/**
 * 获取睡眠趋势数据
 * @param {Number} days 天数，默认7天
 */
export function getSleepTrend(days = 7) {
  return request({
    url: '/sleep/trend',
    method: 'get',
    params: { days }
  })
}

/**
 * 获取睡眠质量分布
 */
export function getSleepQualityDistribution() {
  return request({
    url: '/sleep/quality-distribution',
    method: 'get'
  })
}

/**
 * 获取睡眠不足记录
 * @param {Number} page 页码
 * @param {Number} size 每页条数
 */
export function getInsufficientRecords(page = 1, size = 10) {
  return request({
    url: '/sleep/insufficient',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 获取睡眠不足记录（别名，兼容旧版本）
 * @param {Number} page 页码
 * @param {Number} size 每页条数
 */
export function getInsufficientSleep(page = 1, size = 10) {
  return getInsufficientRecords(page, size)
}

/**
 * 获取睡眠详细记录
 * @param {String} startDate 开始日期
 * @param {String} endDate 结束日期
 */
export function getSleepRecords(startDate, endDate) {
  return request({
    url: '/sleep/records',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 获取睡眠页面完整数据
 */
export function getSleepPageData() {
  return request({
    url: '/sleep/page-data',
    method: 'get'
  })
}