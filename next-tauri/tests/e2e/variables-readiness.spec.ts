import { expect, test } from '@playwright/test'
import type { ToolSessionReport } from '../../src/platform/contracts/toolWebview'

declare global {
  interface Window {
    variablesReadinessTest: {
      reports: ToolSessionReport[]
      resolve?: () => void
      reject?: () => void
    }
  }
}

test.beforeEach(async ({ page }) => {
  await page.clock.install()
  await page.addInitScript(() => {
    window.variablesReadinessTest = { reports: [] }
  })
  // Delay only the diagnostics transport; run the real component and report hook.
  await page.route('**/src/platform/api/diagnosticsApi.ts', async route => {
    const response = await route.fetch()
    await route.fulfill({ response, body: `${await response.text()}
diagnosticsApi.environment = () => new Promise((resolve, reject) => {
  window.variablesReadinessTest.resolve = () => resolve([
    { name: 'TEST_VISIBLE', value: 'fixture', sensitive: false },
    { name: 'TEST_SECRET', value: '••••••••', sensitive: true }
  ]);
  window.variablesReadinessTest.reject = () => reject(new Error('fixture diagnostics unavailable'));
});
` })
  })
  await page.route('**/src/platform/api/toolWebviewApi.ts', async route => {
    const response = await route.fetch()
    await route.fulfill({ response, body: `${await response.text()}
const originalVariablesReport = toolWebviewApis.variables.report;
toolWebviewApis.variables.report = report => {
  window.variablesReadinessTest.reports.push(structuredClone(report));
  return originalVariablesReport(report);
};
` })
  })
  await page.goto('/?surface=variables')
  await expect.poll(() => page.evaluate(() => Boolean(window.variablesReadinessTest.resolve))).toBe(true)
})

for (const outcome of ['success', 'failure'] as const) {
  test(`variables reports a settled initial ${outcome}, not a pending empty list`, async ({ page }) => {
    // Longer than the native harness's 1-second stability window: a slow request
    // must not let the empty initial render masquerade as a ready session.
    await page.clock.runFor(2_500)
    expect(await page.evaluate(() => window.variablesReadinessTest.reports)).toEqual([])
    await page.evaluate(value => {
      if (value === 'success') window.variablesReadinessTest.resolve?.()
      else window.variablesReadinessTest.reject?.()
    }, outcome)
    await expect.poll(() => page.evaluate(() => window.variablesReadinessTest.reports.length)).toBeGreaterThan(0)
    const reports = await page.evaluate(() => window.variablesReadinessTest.reports)
    for (const report of reports) {
      expect(JSON.parse(report.stateDigest)).toMatchObject({
        count: outcome === 'success' ? 2 : 0,
        sensitiveCount: outcome === 'success' ? 1 : 0,
        revealed: false,
        scope: 'process'
      })
    }
    expect(new Set(reports.map(report => report.sessionId)).size).toBe(1)
    if (outcome === 'success') await expect(page.getByText('TEST_VISIBLE', { exact: true })).toBeVisible()
    else await expect(page.getByText('fixture diagnostics unavailable', { exact: true })).toBeVisible()
  })
}
