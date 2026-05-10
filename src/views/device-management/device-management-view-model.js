import dayjs from 'dayjs'

export function deviceOnlineLevel(row) {
  if (row.status === 1) return 2
  if (row.lastOnlineTime && dayjs().diff(dayjs(row.lastOnlineTime), 'minute') <= 30) return 1
  return 0
}

export function deviceOnlineLabel(row) {
  const level = deviceOnlineLevel(row)
  if (level === 2) return '在线'
  if (level === 1) return '最近活跃'
  return '离线'
}

export function buildFilteredDeviceList(deviceList, filters) {
  let list = deviceList
  if (filters.searchImei) {
    const query = filters.searchImei.toLowerCase()
    list = list.filter((device) => {
      return (device.imei && device.imei.toLowerCase().includes(query))
        || (device.userName && device.userName.toLowerCase().includes(query))
    })
  }

  if (filters.filterOnline !== null && filters.filterOnline !== '') {
    list = list.filter((device) => device.status === filters.filterOnline)
  }

  if (filters.filterBind !== null && filters.filterBind !== '') {
    list = list.filter((device) => !!device.bindStatus === filters.filterBind)
  }

  if (filters.filterWarning) {
    list = list.filter((device) => device.hasWarning)
  }

  if (filters.filterBatteryVal === 'low') {
    list = list.filter((device) => device.batteryLevel != null && device.batteryLevel < 20)
  } else if (filters.filterBatteryVal === 'ok') {
    list = list.filter((device) => device.batteryLevel == null || device.batteryLevel >= 20)
  } else if (filters.filterLowBattery) {
    list = list.filter((device) => device.batteryLevel != null && device.batteryLevel < 20)
  }

  return list
}

export function buildDeviceStats(deviceList) {
  const total = deviceList.length
  const online = deviceList.filter((device) => deviceOnlineLevel(device) >= 1).length
  const bound = deviceList.filter((device) => device.bindStatus).length
  const bufferTotal = deviceList.reduce((sum, device) => sum + (device.bufferCount || 0), 0)
  return { total, online, bound, bufferTotal }
}

export function paginateDeviceList(deviceList, pagination) {
  const start = (pagination.page - 1) * pagination.size
  const end = start + pagination.size
  return deviceList.slice(start, end)
}

export function createPaginationState() {
  return {
    page: 1,
    size: 20,
    total: 0
  }
}

export function createBindFormState() {
  return {
    userId: '',
    userName: '',
    userDept: '',
    searchKey: ''
  }
}

export function createTransferFormState() {
  return {
    userId: '',
    userName: '',
    userDept: '',
    searchKey: ''
  }
}

export function createMessageFormState() {
  return {
    text: ''
  }
}

export function resetBindForm(bindForm) {
  bindForm.userId = ''
  bindForm.userName = ''
  bindForm.userDept = ''
  bindForm.searchKey = ''
}

export function resetTransferForm(transferForm) {
  transferForm.userId = ''
  transferForm.userName = ''
  transferForm.userDept = ''
  transferForm.searchKey = ''
}

export function resetMessageForm(messageForm) {
  messageForm.text = ''
}

export function resetDeviceFilters(filters, pagination) {
  filters.searchImei = ''
  filters.filterOnline = null
  filters.filterBind = null
  filters.filterWarning = false
  filters.filterLowBattery = false
  filters.filterWarningVal = ''
  filters.filterBatteryVal = ''
  pagination.page = 1
}

export function applyDeviceRouteFilters(query, filters) {
  filters.filterOnline = query.online !== undefined ? Number(query.online) : null
  filters.filterWarning = query.filter === 'warning'
  filters.filterWarningVal = filters.filterWarning ? 'warning' : ''
  filters.filterLowBattery = query.filter === 'lowBattery'
  filters.filterBatteryVal = filters.filterLowBattery ? 'low' : ''
}
