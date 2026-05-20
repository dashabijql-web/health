import request from '@/utils/request'

export function getEmployeeList(params) {
  return request({ url: '/employee/list', method: 'get', params })
}

export function getEmployeeListDetail() {
  return request({ url: '/employee/list-detail', method: 'get' })
}

export function getEmployeeStats() {
  return request({ url: '/employee/stats', method: 'get' })
}

export function createEmployee(data) {
  return request({ url: '/employee/create', method: 'post', data })
}

export function updateEmployee(data) {
  return request({ url: '/employee/update', method: 'put', data })
}

export function deleteEmployee(id) {
  return request({ url: `/employee/delete/${id}`, method: 'delete' })
}
