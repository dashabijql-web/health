import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const healthShowRoot = path.resolve(__dirname, '..')

function readSource(relativePath) {
  return readFileSync(path.join(healthShowRoot, relativePath), 'utf8')
}

test('target beautification pages do not keep banned emoji UI shortcuts', () => {
  const targetFiles = [
    'src/views/health-monitor/workbench/index.vue',
    'src/views/health-monitor/employee-profile/index.vue',
    'src/views/health-monitor/employee-profile/employee-profile-view-model.js',
    'src/views/health-monitor/mine-entry/index.vue',
    'src/views/personnel-management/health-portrait/index.vue',
    'src/views/personnel-management/health-portrait/health-portrait-view-model.js',
    'src/views/safety-command/index.vue',
    'src/views/safety-command/components/SafetyCommandDialogs.vue',
    'src/views/safety-command/components/EventPanel.vue',
    'src/views/safety-command/components/KpiCardRow.vue',
    'src/views/safety-command/components/RiskPersonPanel.vue',
    'src/views/safety-command/components/PersonDetailDrawer.vue',
    'src/views/safety-command/safety-command-view-model.js',
    'src/views/safety-command/safety-command-interactions.js',
    'src/views/alert-management/sos/index.vue'
  ]
  const bannedUiTokens = [
    '❤',
    '♥',
    '❤️',
    '🩸',
    '💓',
    '💖',
    '💘',
    '📞',
    '📢',
    '🚨',
    '💨',
    '🫀',
    '👟',
    '⚡',
    '🌡',
    '🌡️',
    '📅',
    '✓',
    '⚠',
    '⚠️',
    '🆘',
    '🔇',
    '😴',
    '⛏️',
    '⌚',
    '🫂'
  ]

  const offenders = targetFiles.filter((relativePath) => {
    const source = readSource(relativePath)
    return bannedUiTokens.some((token) => source.includes(token))
  })
  assert.deepEqual(offenders, [], `Remove banned emoji UI shortcuts from: ${offenders.join(', ')}`)
})

test('admin CRUD pages use the shared hero shell', () => {
  const adminPages = [
    'src/views/device-management/index.vue',
    'src/views/user-list/index.vue',
    'src/views/role-management/index.vue',
    'src/views/org-management/department/index.vue',
    'src/views/org-management/job-type/index.vue'
  ]

  for (const relativePath of adminPages) {
    const source = readSource(relativePath)
    assert.match(source, /PageHeroHeader/, `${relativePath} should use PageHeroHeader`)
    assert.match(source, /hm-admin-page/, `${relativePath} should use the shared hm-admin-page shell class`)
  }
})

test('empty-state-sensitive pages use the shared PageEmptyState component', () => {
  const pages = [
    'src/views/health-monitor/mine-entry/index.vue',
    'src/views/ai-chat/index.vue',
    'src/views/health-monitor/employee-archive/index.vue'
  ]

  for (const relativePath of pages) {
    const source = readSource(relativePath)
    assert.match(source, /PageEmptyState/, `${relativePath} should use PageEmptyState`)
  }
})
