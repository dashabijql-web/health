/**
 * 设备管理 API
 */
import request from '@/utils/request'

/**
 * 获取在线设备列表
 */
export function getOnlineDevices() {
  return request({
    url: '/api/device/online',
    method: 'get'
  })
}

/**
 * 检查设备是否在线
 */
export function checkDeviceOnline(imei) {
  return request({
    url: `/api/device/check/${imei}`,
    method: 'get'
  })
}

/**
 * 获取设备缓冲数据统计
 */
export function getBufferDataCount(deviceId) {
  return request({
    url: `/api/device/${deviceId}/buffer-count`,
    method: 'get'
  })
}

/**
 * 转移缓冲数据到用户
 */
export function transferBufferData(deviceId, userId) {
  return request({
    url: `/api/device/${deviceId}/transfer-buffer`,
    method: 'post',
    params: { userId }
  })
}

/**
 * 删除设备缓冲数据
 */
export function deleteBufferData(deviceId) {
  return request({
    url: `/api/device/${deviceId}/buffer`,
    method: 'delete'
  })
}

/**
 * 发送指令到设备
 */
export function sendCommand(data) {
  return request({
    url: '/api/device/command',
    method: 'post',
    data
  })
}

/**
 * 发送立即定位指令
 */
export function sendLocateCommand(imei) {
  return request({
    url: `/api/device/locate/${imei}`,
    method: 'post'
  })
}

/**
 * 发送重启指令
 */
export function sendRestartCommand(imei) {
  return request({
    url: `/api/device/restart/${imei}`,
    method: 'post'
  })
}

/**
 * 绑定设备到用户
 */
export function bindDeviceToUser(deviceId, userId) {
  return request({
    url: `/api/device/${deviceId}/bind`,
    method: 'post',
    params: { userId }
  })
}

/**
 * 解绑设备
 */
export function unbindDevice(deviceId) {
  return request({
    url: `/api/device/${deviceId}/unbind`,
    method: 'post'
  })
}
