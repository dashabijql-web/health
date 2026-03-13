/**
 * 风险预警 API 模块
 *
 * 对应后端 Controller：RiskWarningController（/health/risk-warning/...）
 *
 * 【预警类型说明】
 *   - SOS：员工主动求救（手表 SOS 按钮）
 *   - fall：跌倒检测（加速度传感器）
 *   - heartRate：心率异常（过高/过低）
 *   - bloodOxygen：血氧偏低（< 95%）
 *   - temperature：体温异常（> 37.5°C 或 < 36.0°C）
 *   - staticAlert：长时间静止不动（可能晕倒）
 *
 * 主要用途：
 *   - 安全指挥中心（index.vue）：实时事件列表、紧急告警横幅
 *   - 告警管理/记录页（records/index.vue）：历史记录查询
 *   - health-monitor/risk-warning：预警分析统计
 */

import request from '@/utils/request'

/**
 * 获取风险预警统计概览（包含各类型统计）
 *
 * 后端接口：GET /health/risk-warning/overview
 * 返回字段示例：
 *   { sosCount: 2, fallCount: 1, staticCount: 5, abnormalCount: 12,
 *     totalOnline: 234, normalCount: 214 }
 *
 * 使用场景：安全指挥中心 KPI 卡片、告警管理顶部统计卡
 */
export function getRiskWarningOverview(startDate, endDate) {
  return request({
    url: '/risk-warning/overview',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 获取预警列表
 * @param {Object} params 查询参数
 * @param {String} params.level 预警级别
 * @param {Boolean} params.handled 是否已处理
 * @param {Number} params.page 页码
 * @param {Number} params.size 每页条数
 */
export function getRiskWarningList(params) {
  return request({
    url: '/risk-warning/list',
    method: 'get',
    params
  })
}

/**
 * 获取风险趋势（按类型分组）
 * @param {Number} days 天数，默认30天
 */
export function getRiskWarningTrend(days = 30) {
  return request({
    url: '/risk-warning/trend',
    method: 'get',
    params: { days }
  })
}

/**
 * 获取各部门预警统计
 */
export function getDeptWarningStats(startDate, endDate) {
  return request({
    url: '/risk-warning/dept-stats',
    method: 'get',
    params: { startDate, endDate }
  })
}

/**
 * 处理预警
 * @param {Number} id 预警ID
 * @param {Object} data 处理数据
 */
export function handleRiskWarning(id, data) {
  return request({
    url: `/risk-warning/handle/${id}`,
    method: 'post',
    data
  })
}

/**
 * 批量处理预警
 * @param {Array} ids 预警ID列表
 */
export function handleBatchRiskWarning(ids) {
  return request({
    url: '/risk-warning/handle-batch',
    method: 'post',
    data: ids
  })
}