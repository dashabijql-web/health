import test from 'node:test'
import assert from 'node:assert/strict'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import { existsSync } from 'node:fs'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const dashboardRuntime = path.resolve(__dirname, '../src/views/health-monitor/dashboard/dashboard-runtime-data.js')

function resolveRelativeImport(fromFile, importPath) {
  return path.resolve(path.dirname(fromFile), `${importPath}.js`)
}

test('dashboard runtime imports warning lifecycle helper from existing relative path', () => {
  const target = resolveRelativeImport(dashboardRuntime, '../../alert-management/common/warning-lifecycle')
  assert.equal(existsSync(target), true, `${target} should exist`)
})
