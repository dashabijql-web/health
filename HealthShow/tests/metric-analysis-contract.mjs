import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'

const ROOT = process.cwd()
const read = (...parts) => fs.readFileSync(path.join(ROOT, ...parts), 'utf8')
const readBackend = (...parts) => fs.readFileSync(path.join(ROOT, '..', 'HealthData', ...parts), 'utf8')

test('metric realtime queries return each person latest row', () => {
  for (const mapper of ['PressureMapper.java', 'BloodPressureMapper.java', 'BloodOxygenMapper.java']) {
    const source = readBackend('src', 'main', 'java', 'com', 'xzkj', 'health', 'mapper', mapper)
    assert.match(source, /ROW_NUMBER\(\) OVER \(PARTITION BY hr\.user_code ORDER BY hr\.record_time DESC\)/)
    assert.match(source, /latest WHERE rn = 1/)
  }
})

test('blood oxygen metrics share the same abnormal threshold and person semantics', () => {
  const mapper = readBackend('src', 'main', 'java', 'com', 'xzkj', 'health', 'mapper', 'BloodOxygenMapper.java')
  const page = read('src', 'views', 'health-monitor', 'blood-oxygen', 'index.vue')
  assert.match(mapper, /COUNT\(\*\) \* 100 \/ NULLIF\(\(SELECT COUNT\(\*\) FROM employee\), 0\)/)
  assert.match(mapper, /WHERE hr\.blood_oxygen < 95/)
  assert.match(mapper, /SUM\(CASE WHEN hr\.blood_oxygen < 95 THEN 1 ELSE 0 END\) AS low_count/)
  assert.match(page, /异常人数/)
  assert.match(page, /检测人数/)
  assert.doesNotMatch(page, /ref="hourlyRef"/)
})

test('metric pages use top ten rankings and current-person scope cards', () => {
  for (const pageName of ['heart-rate', 'pressure', 'blood-pressure', 'blood-oxygen']) {
    const source = read('src', 'views', 'health-monitor', pageName, 'index.vue')
    assert.match(source, /slice\(0, 10\)/)
    assert.match(source, /当前覆盖人员/)
    assert.match(source, /近2小时每人最新一条/)
    assert.doesNotMatch(source, /ref="gaugeRef"/)
  }
})

test('pressure distribution uses comparison bars without a duplicate donut chart', () => {
  const page = read('src', 'views', 'health-monitor', 'pressure', 'index.vue')
  assert.doesNotMatch(page, /ref="distRef"/)
  assert.match(page, /ps-dist-bar-wrap/)
})

test('shared metric thresholds and mobile layout preserve action-first semantics', () => {
  const thresholds = read('src', 'constants', 'health-thresholds.js')
  const layout = read('src', 'styles', '_hm-layout.scss')
  const loader = read('src', 'views', 'health-monitor', 'metric-page', 'metric-data-loader.js')
  assert.match(thresholds, /RELAXED: 50, ELEVATED: 70, HIGH: 85/)
  assert.match(loader, /limit = 10/)
  assert.match(loader, /seen\.has\(key\)/)
  assert.match(layout, /-panel-anomaly \{ order: 1; min-height: 300px; \}/)
  assert.match(layout, /-panel-trend,[\s\S]*height: 220px;/)
})
