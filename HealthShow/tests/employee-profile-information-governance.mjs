import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = dirname(fileURLToPath(import.meta.url))
const source = (relativePath) => readFileSync(resolve(__dirname, '..', relativePath), 'utf8')

test('employee profile keeps only factual health and action surfaces', () => {
  const page = source('src/views/health-monitor/employee-profile/index.vue')

  assert.doesNotMatch(page, /HeartRateWave|实时心电图|ep-miner|miner-worker|健康风险评估|riskItems/)
  assert.match(page, /当前体征/)
  assert.doesNotMatch(page, /7日体征趋势/)
  assert.match(page, /近期预警轨迹/)
  assert.match(page, /联系与处置/)
  assert.match(page, /<EmployeeProfileCommandLayer/)
  assert.match(page, /每项取最近一次非空读数/)
  assert.match(page, /heartRateTime/)
})

test('employee profile uses backend freshness and authoritative warning totals', () => {
  const runtime = source('src/views/health-monitor/employee-profile/employee-profile-runtime.js')
  const composable = source('src/views/health-monitor/employee-profile/use-employee-profile-page.js')

  assert.match(runtime, /data\.vitals\?\.online/)
  assert.match(runtime, /data\.vitals\?\.recordTime/)
  assert.doesNotMatch(runtime, /isOnline\.value = true/)
  assert.doesNotMatch(runtime, /lastUpdate\.value = new Date/)
  assert.match(composable, /warningTotal\.value = Number\(warnRes\.value\.data\.total\)/)
  assert.match(composable, /pendingTotal\.value = Number\(pendingRes\.value\.data\.total\)/)
  assert.match(composable, /warning7Total\.value = Number\(warn7Res\.value\.data\.total\)/)
  assert.doesNotMatch(composable, /warnings\.value\.length/)
})

test('employee profile history supports employee-scoped ranges, curves and server pagination', () => {
  const page = source('src/views/health-monitor/employee-profile/index.vue')
  const history = source('src/views/health-monitor/employee-profile/components/EmployeeHealthHistory.vue')
  const historyModel = source('src/views/health-monitor/employee-profile/employee-profile-history.js')
  const api = source('src/api/health.js')

  assert.match(page, /<EmployeeHealthHistory/)
  assert.match(page, /:employee-code="empInfo\.empCode"/)
  assert.match(history, /type="daterange"/)
  assert.match(history, /历史曲线/)
  assert.match(history, /明细记录/)
  assert.match(history, /getEmployeeHealthHistory\(params\)/)
  assert.match(history, /getHealthRecords\(\{[\s\S]*userCode: props\.employeeCode[\s\S]*startTime:[\s\S]*endTime:/)
  assert.match(history, /v-model:current-page="recordPage"/)
  assert.match(history, /历史数据查询范围不能超过365天/)
  assert.match(historyModel, /HISTORY_METRICS/)
  assert.match(api, /\/api\/health\/record\/history\/trend/)
})

test('employee profile shows a live countdown aligned with its 30-second refresh', () => {
  const page = source('src/views/health-monitor/employee-profile/index.vue')
  const composable = source('src/views/health-monitor/employee-profile/use-employee-profile-page.js')

  assert.match(page, /距下次刷新 \{\{ nextRefreshSeconds \}\} 秒/)
  assert.match(composable, /const PROFILE_REFRESH_SECONDS = 30/)
  assert.match(composable, /nextRefreshSeconds\.value -= 1/)
  assert.match(composable, /nextRefreshSeconds\.value <= 0[\s\S]*refresh\(\)/)
  assert.match(composable, /useIntervalTask\([\s\S]*1000\)/)
})
