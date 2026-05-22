export function buildEmpInfoFromRoute(route) {
  return {
    empCode: route.query.empCode || '',
    empName: route.query.empName || '',
    gender: Number(route.query.gender) || 1,
    birthDate: route.query.birthDate || '',
    deptName: route.query.deptName || '',
    jobTypeName: route.query.jobTypeName || '',
    phone: route.query.phone || ''
  }
}

export const fmtTemp = (t) => !t ? '--' : t > 100 ? (t / 10).toFixed(1) : Number(t).toFixed(1)
export const fmtTime = (t) => !t ? '' : String(t).length > 16 ? String(t).substring(5, 16) : String(t)
export const hrClass = (v) => !v ? '' : (v < 60 || v > 100) ? 'red' : (v < 65 || v > 90) ? 'yellow' : 'green'
export const spo2Class = (v) => !v ? '' : v < 90 ? 'red' : v < 95 ? 'yellow' : 'green'
export const tempClass = (v) => {
  if (!v) return ''
  const t = v > 100 ? v / 10 : v
  return (t > 37.3 || t < 36) ? 'red' : t > 37 ? 'yellow' : 'green'
}
export const pressClass = (v) => !v ? '' : v > 70 ? 'red' : v > 50 ? 'yellow' : 'green'
export const calcAge = (birthDate) => {
  if (!birthDate) return '--'
  const age = new Date().getFullYear() - new Date(birthDate).getFullYear()
  return age > 0 && age < 100 ? `${age}岁` : '--'
}

export function buildProfileRiskSummary({ pendCount, warn7Count, vitals }) {
  const isHrAbnormal = vitals.heartRate && (vitals.heartRate < 60 || vitals.heartRate > 100)
  const isSpo2Abnormal = vitals.bloodOxygen && vitals.bloodOxygen < 95
  const isTempAbnormal = vitals.temperature
    && ((vitals.temperature > 100 ? vitals.temperature / 10 : vitals.temperature) < 36
      || (vitals.temperature > 100 ? vitals.temperature / 10 : vitals.temperature) > 37.3)
  const isBpAbnormal = (vitals.systolic && vitals.systolic >= 140) || (vitals.diastolic && vitals.diastolic >= 90)
  const isPressureHigh = vitals.pressure && vitals.pressure >= 70

  if (pendCount > 0) {
    return {
      value: '重点复核',
      tone: 'danger',
      sub: `待处理 ${pendCount} 条，需先完成预警闭环`
    }
  }
  if (warn7Count >= 3) {
    return {
      value: '持续观察',
      tone: 'warn',
      sub: `近7日预警 ${warn7Count} 次，建议回看趋势与月历`
    }
  }
  if (isHrAbnormal || isSpo2Abnormal || isTempAbnormal || isBpAbnormal || isPressureHigh) {
    return {
      value: '指标波动',
      tone: 'warn',
      sub: '当前体征存在异常项，建议结合准入记录复核'
    }
  }
  return {
    value: '状态稳定',
    tone: 'safe',
    sub: '当前没有明显高风险信号，可持续趋势观察'
  }
}

export function buildNextActionSummary({ pendCount, isOnline, vitals }) {
  const isHrAbnormal = vitals.heartRate && (vitals.heartRate < 60 || vitals.heartRate > 100)
  const isSpo2Abnormal = vitals.bloodOxygen && vitals.bloodOxygen < 95
  const isTempAbnormal = vitals.temperature
    && ((vitals.temperature > 100 ? vitals.temperature / 10 : vitals.temperature) < 36
      || (vitals.temperature > 100 ? vitals.temperature / 10 : vitals.temperature) > 37.3)
  const isBpAbnormal = (vitals.systolic && vitals.systolic >= 140) || (vitals.diastolic && vitals.diastolic >= 90)

  if (pendCount > 0) {
    return { value: '预警处置', sub: '先处理未闭环预警，再回看画像趋势' }
  }
  if (!isOnline) {
    return { value: '设备核查', sub: '当前离线，先确认设备在线和数据回传状态' }
  }
  if (isBpAbnormal || isTempAbnormal || isSpo2Abnormal || isHrAbnormal) {
    return { value: '准入复核', sub: '建议进入准入页复核当前体征与班前状态' }
  }
  return { value: '趋势观察', sub: '保持观察近7日变化和预警频次' }
}

export function buildProfileInsightLines({ vitals, pendCount, warnCount, warn7Count }) {
  const lines = []
  const hr = vitals.heartRate
  const spo2 = vitals.bloodOxygen
  const pressure = vitals.pressure
  const systolic = vitals.systolic
  const diastolic = vitals.diastolic

  if (pendCount > 0) {
    lines.push(`近期仍有 ${pendCount} 条未处理预警，建议先核查最新异常记录与现场处置状态。`)
  } else if (warnCount > 0) {
    lines.push(`近30日共记录 ${warnCount} 次预警，但当前均已处理，可重点回看高频异常类型。`)
  } else {
    lines.push('当前未发现近30日预警记录，人员近期健康状态整体稳定。')
  }

  if (hr && (hr < 60 || hr > 100)) {
    lines.push(`当前心率为 ${hr} bpm，已偏离正常范围，建议优先结合活动状态和既往预警复核。`)
  } else if (spo2 && spo2 < 95) {
    lines.push(`当前血氧为 ${spo2}%，存在偏低趋势，建议复测并重点关注作业环境与呼吸状态。`)
  } else if ((systolic && systolic >= 140) || (diastolic && diastolic >= 90)) {
    lines.push(`当前血压为 ${systolic || '--'}/${diastolic || '--'} mmHg，建议纳入班前准入重点复核对象。`)
  } else if (pressure && pressure >= 70) {
    lines.push(`当前压力指数为 ${pressure}，已接近高风险区间，建议结合睡眠、步数和岗位强度持续观察。`)
  } else {
    lines.push('实时体征未见明显高风险项，可继续关注近7日趋势和阶段性波动。')
  }

  if (warn7Count >= 3) {
    lines.push(`近7日预警达到 ${warn7Count} 次，建议从趋势页和月度日历页复盘连续异常时段。`)
  } else {
    lines.push('建议结合月度日历和准入记录，持续观察是否存在连续性异常。')
  }

  return lines
}

export function buildProfileSummaryCards({ profileRiskSummary, pendCount, warnCount, isOnline, lastUpdate, nextActionSummary }) {
  return [
    {
      label: '当前风险',
      value: profileRiskSummary.value,
      sub: profileRiskSummary.sub,
      tone: profileRiskSummary.tone
    },
    {
      label: '预警闭环',
      value: `${pendCount} / ${warnCount}`,
      sub: warnCount ? '待处理 / 近30日总预警' : '近30日暂无预警记录',
      tone: pendCount > 0 ? 'danger' : warnCount > 0 ? 'info' : 'safe'
    },
    {
      label: '实时在线',
      value: isOnline ? '在线监测' : '离线待核查',
      sub: lastUpdate && lastUpdate !== '--' ? `最近更新 ${lastUpdate}` : '等待最新体征数据',
      tone: isOnline ? 'accent' : 'muted'
    },
    {
      label: '建议动作',
      value: nextActionSummary.value,
      sub: nextActionSummary.sub,
      tone: 'accent'
    }
  ]
}

export function buildHrItems({ vitals, trend7, warnings, warn7Count }) {
  const hr = vitals.heartRate
  const status = !hr ? '--' : hr < 60 ? '偏低' : hr > 100 ? '偏高' : '正常'
  return [
    { key: 'h1', label: '当前心率', v: hr || '--', unit: 'bpm', cls: hrClass(hr) },
    { key: 'h2', label: '7日均值', v: trend7.avgHr || '--', unit: 'bpm', cls: 'cyan' },
    { key: 'h3', label: '正常范围', v: '60~100', unit: 'bpm', cls: 'dim' },
    { key: 'h4', label: '心率状态', v: status, unit: '', cls: hrClass(hr) || 'green' },
    { key: 'h5', label: '心率预警', v: warnings.filter(w => (w.warningType || '').includes('心率')).length, unit: '次', cls: warnings.filter(w => (w.warningType || '').includes('心率')).length > 0 ? 'red' : 'green' },
    { key: 'h6', label: '7日预警', v: warn7Count, unit: '次', cls: warn7Count > 3 ? 'red' : 'green' }
  ]
}

export function buildSpo2Items({ vitals, trend7, warnings, warnCount, pendCount }) {
  const s = vitals.bloodOxygen
  const status = !s ? '--' : s < 90 ? '严重偏低' : s < 95 ? '偏低' : '正常'
  return [
    { key: 's1', label: '当前血氧', v: s || '--', unit: '%', cls: spo2Class(s) },
    { key: 's2', label: '7日均值', v: trend7.avgSpo2 || '--', unit: '%', cls: 'cyan' },
    { key: 's3', label: '正常值', v: '≥ 95', unit: '%', cls: 'dim' },
    { key: 's4', label: '血氧状态', v: status, unit: '', cls: spo2Class(s) || 'green' },
    { key: 's5', label: '近期预警', v: warnCount, unit: '次', cls: warnCount > 5 ? 'red' : 'green' },
    { key: 's6', label: '未处理', v: pendCount, unit: '条', cls: pendCount > 0 ? 'red' : 'green' }
  ]
}

export function buildVitalItems({ vitals, exercise }) {
  const t = vitals.temperature
  const tStatus = !t ? '--' : (tempClass(t) === 'red' ? '异常' : tempClass(t) === 'yellow' ? '偏高' : '正常')
  return [
    { key: 'v1', label: '体温', v: fmtTemp(t), unit: '°C', cls: tempClass(t) },
    { key: 'v2', label: '今日步数', v: exercise.todaySteps > 0 ? exercise.todaySteps : '--', unit: '步', cls: 'green' },
    { key: 'v3', label: '今日卡路里', v: exercise.todayCalories > 0 ? exercise.todayCalories : '--', unit: 'kcal', cls: 'cyan' },
    { key: 'v4', label: '压力指数', v: vitals.pressure || '--', unit: '', cls: pressClass(vitals.pressure) },
    { key: 'v5', label: '体温状态', v: tStatus, unit: '', cls: tempClass(t) || 'green' },
    { key: 'v6', label: '血压', v: (vitals.systolic && vitals.diastolic) ? `${vitals.systolic}/${vitals.diastolic}` : '--', unit: 'mmHg', cls: 'cyan' }
  ]
}

export function buildWarnItems({ warnings, warnCount, pendCount, warn7Count }) {
  const hrW = warnings.filter(w => (w.warningType || '').includes('心率')).length
  const s2W = warnings.filter(w => (w.warningType || '').includes('血氧')).length
  const prW = warnings.filter(w => (w.warningType || '').includes('压力')).length
  return [
    { key: 'w1', label: '近30日预警', v: warnCount, unit: '次', cls: warnCount > 10 ? 'red' : warnCount > 5 ? 'yellow' : 'green' },
    { key: 'w2', label: '未处理', v: pendCount, unit: '条', cls: pendCount > 0 ? 'red' : 'green' },
    { key: 'w3', label: '7日预警', v: warn7Count, unit: '次', cls: 'cyan' },
    { key: 'w4', label: '心率预警', v: hrW, unit: '次', cls: hrW > 0 ? 'red' : 'green' },
    { key: 'w5', label: '血氧预警', v: s2W, unit: '次', cls: s2W > 0 ? 'red' : 'green' },
    { key: 'w6', label: '压力预警', v: prW, unit: '次', cls: prW > 0 ? 'yellow' : 'green' }
  ]
}

export function buildRiskItems({ vitals, warnCount }) {
  const hr = vitals.heartRate || 0
  const spo2 = vitals.bloodOxygen || 0
  const t = vitals.temperature ? (vitals.temperature > 100 ? vitals.temperature / 10 : vitals.temperature) : 0
  const prs = vitals.pressure || 0
  const wc = warnCount

  const mk = (name, icon, val, thHigh, thMid, maxV) => {
    const pct = Math.min(100, Math.round(val / maxV * 100))
    const level = val >= thHigh ? 'high' : val >= thMid ? 'mid' : 'low'
    const levelText = level === 'high' ? '风险高' : level === 'mid' ? '风险中' : '风险低'
    const color = level === 'high' ? '#ff4444' : level === 'mid' ? '#ffaa00' : '#00c853'
    return { name, icon, pct, level, levelText, color }
  }

  const hrRisk = hr === 0 ? 0 : Math.abs(hr - 75) / 25 * 60
  const spo2Risk = spo2 === 0 ? 0 : Math.max(0, (100 - spo2) * 5)
  const tRisk = t === 0 ? 0 : Math.abs(t - 36.5) / 1.5 * 60
  const pRisk = prs > 0 ? prs * 0.7 : 20
  const wRisk = Math.min(80, wc * 5)

  return [
    mk('心率健康', 'HR', hrRisk, 60, 30, 100),
    mk('血氧健康', 'SpO2', spo2Risk, 60, 30, 100),
    mk('体温健康', 'TEMP', tRisk, 60, 30, 100),
    mk('压力水平', 'PRS', pRisk, 60, 30, 100),
    mk('预警风险', 'WARN', wRisk, 50, 20, 100),
    mk('综合健康', 'ALL', Math.min(100, (hrRisk + spo2Risk + tRisk) / 3), 60, 30, 100)
  ]
}
