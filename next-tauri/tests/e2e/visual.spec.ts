import { expect, test, type Page } from '@playwright/test'

const stableCss = `
  *, *::before, *::after { animation: none !important; transition: none !important; caret-color: transparent !important; }
  .utility-session, .json-workbench__session, .json-workbench__footer code,
  .tool-surface-report-error { visibility: hidden !important; }
`

test.beforeEach(async ({ page }) => {
  await page.addInitScript(() => window.localStorage.clear())
})

async function open(page: Page, path: string, width = 1366, height = 820) {
  await page.setViewportSize({ width, height })
  await page.goto(path)
  await page.addStyleTag({ content: stableCss })
  await page.waitForLoadState('networkidle')
}

test('main shell and compact navigation remain usable', async ({ page }) => {
  await open(page, '/')
  await expect(page.locator('.app-shell')).toHaveScreenshot('shell-wide.png')

  await page.keyboard.press('ControlOrMeta+K')
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.getByRole('textbox', { name: /搜索|Search|検索/ }).fill('JSON')
  await page.keyboard.press('Enter')
  await expect(page.locator('.nav-button[aria-label="JSON"]')).toHaveAttribute('aria-current', 'page')

  await page.setViewportSize({ width: 720, height: 760 })
  await expect(page.locator('.sidebar-footer')).toBeVisible()
  await expect(page.locator('.app-shell')).toHaveScreenshot('shell-compact.png')
})

for (const [name, path] of [
  ['json-workbench', '/?surface=json'],
  ['http-workbench', '/?surface=http'],
  ['quick-note', '/?surface=quick-note'],
  ['crypto', '/?surface=crypto'],
  ['network', '/?surface=network'],
  ['settings', '/?surface=settings']
] as const) {
  test(`${name} visual baseline`, async ({ page }) => {
    await open(page, path)
    await expect(page.locator('#root')).toHaveScreenshot(`${name}.png`, { maxDiffPixelRatio: 0.01 })
  })
}

test('JSON resizers and conversion menu are keyboard reachable', async ({ page }) => {
  await open(page, '/?surface=json')
  const separators = page.getByRole('separator')
  await expect(separators).toHaveCount(2)
  const initial = await separators.first().getAttribute('aria-valuenow')
  await separators.first().focus()
  await page.keyboard.press('ArrowRight')
  await expect(separators.first()).not.toHaveAttribute('aria-valuenow', initial ?? '')
  await page.getByText(/更多|More|その他/, { exact: true }).click()
  await expect(page.getByRole('button', { name: /JSON → XML/ })).toBeVisible()
})
