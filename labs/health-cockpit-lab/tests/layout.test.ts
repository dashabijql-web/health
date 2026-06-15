import test from 'node:test'
import assert from 'node:assert/strict'

import { resolveViewportMode } from '../src/data/layout'

test('viewport mode prefers desktop for flagship widths', () => {
  assert.equal(resolveViewportMode(1920), 'desktop')
  assert.equal(resolveViewportMode(1520), 'compact')
  assert.equal(resolveViewportMode(720), 'mobile')
})
