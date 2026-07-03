import request from '@/utils/request'

export function getWatchRawPackets(params = {}) {
  return request({
    url: '/api/watch/raw-packets',
    method: 'get',
    params,
    timeout: 10000
  })
}
