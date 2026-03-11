/**
 * 实时监控 API 模块
 *
 * 对应后端 Controller：RealtimeController（/health/realtime/...）
 *
 * 数据来源：
 *   后端通过 Netty TCP 接收手表设备上报的实时数据，
 *   存入 Redis（realtime:user:{imei} 键），此模块的接口从 Redis 读取。
 *
 * 主要用途：
 *   - health-monitor/real-time 实时监控页（在线人员列表）
 *   - safety-command 安全指挥中心（地下人数统计、SOS 报警等）
 *   - PersonDetailDrawer 人员详情抽屉（单人实时体征）
 */

import request from '@/utils/request'

/**
 * 获取实时监控概览统计
 *
 * 后端接口：GET /health/realtime/overview
 * 返回字段示例：
 *   { onlineCount: 234, totalEmployees: 1000, warningCount: 5, ... }
 *
 * 使用场景：安全指挥中心顶部 KPI 卡片（井下人数）
 */
export function getRealtimeOverview() {
  return request({
    url: '/realtime/overview',
    method: 'get'
  })
}

/**
 * 获取在线用户列表（实时监控页主表格）
 *
 * 后端接口：GET /health/realtime/online-users
 * 返回字段示例（每条记录）：
 *   { empCode, empName, deptName, heartRate, bloodOxygen, temperature,
 *     steps, sleepHours, lastUpdateTime, warningLevel }
 *
 * @param {Number} page 页码（从 1 开始）
 * @param {Number} size 每页条数，默认 20
 */
export function getOnlineUsers(page = 1, size = 20) {
  return request({
    url: '/realtime/online-users',
    method: 'get',
    params: { page, size }
  })
}

/**
 * 获取单个用户的实时体征数据
 *
 * 后端接口：GET /health/realtime/user/{userCode}
 * 返回字段示例：
 *   { heartRate: 72, bloodOxygen: 97, temperature: 366, steps: 4532,
 *     deptName: '综采一队', online: true }
 * 注意：temperature 后端以整数×10存储（366 = 36.6°C），前端需 /10
 *
 * @param {String} userCode 员工编号（emp_code，如 EMP0001）
 *
 * 使用场景：PersonDetailDrawer 人员详情抽屉顶部体征卡片
 */
export function getUserRealtimeData(userCode) {
  return request({
    url: `/realtime/user/${userCode}`,
    method: 'get'
  })
}

/**
 * 获取实时告警列表（未处理的告警）
 *
 * 后端接口：GET /health/realtime/alerts
 * 返回：近期未处理的告警记录列表
 *
 * 使用场景：实时监控页告警栏
 */
export function getRealtimeAlerts() {
  return request({
    url: '/realtime/alerts',
    method: 'get'
  })
}

/**
 * 获取设备实时统计数据（手表设备状态汇总）
 *
 * 后端接口：GET /health/realtime/statistics
 * 返回字段示例：
 *   { totalDevices: 1000, onlineDevices: 234, offlineDevices: 766,
 *     lowBattery: 12, charging: 5 }
 *
 * 使用场景：安全指挥中心手表设备状态 KPI 卡片
 */
export function getRealtimeStatistics() {
  return request({
    url: '/realtime/statistics',
    method: 'get'
  })
}
