import assert from 'node:assert/strict'
import { chromium } from 'playwright'

const baseUrl = process.env.BASE_URL || 'http://127.0.0.1:9528'
const browser = await chromium.launch({ headless: true })

try {
  const loginContext = await browser.newContext()
  const loginResponse = await loginContext.request.post(`${baseUrl}/dev-api/auth/login`, {
    data: { username: 'admin', password: 'admin123' }
  })
  const login = await loginResponse.json()
  assert.equal(login.code, 200, 'test login should succeed')
  assert.ok(login.data?.token, 'test login should return a token')
  await loginContext.close()

  const results = []
  for (const viewport of [{ name: 'desktop', width: 1440, height: 900 }, { name: 'mobile', width: 390, height: 844 }]) {
    const context = await browser.newContext({ viewport })
    await context.addCookies([
      { name: 'User-Token', value: login.data.token, url: baseUrl },
      { name: 'satoken', value: login.data.token, url: baseUrl },
      { name: 'Health-Data-Source', value: 'old', url: baseUrl }
    ])
    const page = await context.newPage()
    await page.addInitScript(() => window.localStorage.setItem('Health-Data-Source', 'old'))
    await page.goto(`${baseUrl}/#/health-monitor/mine-entry?status=review&from=dashboard`, {
      waitUntil: 'domcontentloaded'
    })

    const pendingKpi = page.locator('.me-kpi').filter({ hasText: '待复检' }).locator('.me-kpi-n')
    await pendingKpi.waitFor({ state: 'visible', timeout: 20000 })
    const reviewGroup = page.locator('.me-group-hd').filter({ hasText: '待复检' })
    await reviewGroup.waitFor({ state: 'visible', timeout: 20000 })
    await page.locator('.el-loading-mask').waitFor({ state: 'hidden', timeout: 20000 }).catch(() => {})
    await page.waitForTimeout(200)
    const visibleState = await page.evaluate(() => {
      const kpi = Array.from(document.querySelectorAll('.me-kpi'))
        .find((item) => item.textContent?.includes('待复检'))
      const group = Array.from(document.querySelectorAll('.me-group-hd'))
        .find((item) => item.textContent?.includes('待复检'))
      return {
        pending: Number.parseInt(kpi?.querySelector('.me-kpi-n')?.textContent || '0', 10),
        groupCount: Number.parseInt(group?.textContent?.match(/（(\d+) 人）/)?.[1] || '0', 10),
        cards: document.querySelectorAll('.me-card-fail').length
      }
    })
    const { pending, groupCount, cards } = visibleState
    assert.ok(pending > 0, `${viewport.name}: expected authoritative pending review count`)
    assert.ok(cards > 0, `${viewport.name}: pending reviews must render concrete review cards`)
    assert.equal(cards, groupCount, `${viewport.name}: review group count and rendered cards should agree`)
    assert.ok(cards >= pending, `${viewport.name}: review queue must not be truncated below the authoritative count`)
    results.push({ viewport: viewport.name, pending, groupCount, cards })
    await context.close()
  }

  console.log(JSON.stringify({ status: 'passed', results }))
} finally {
  await browser.close()
}
