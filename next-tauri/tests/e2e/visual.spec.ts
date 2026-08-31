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

async function useAppearance(page: Page, theme: 'light' | 'dark', uiScale: 90 | 100 | 110) {
  await page.addInitScript(({ theme: nextTheme, uiScale: nextScale }) => {
    window.localStorage.setItem('mootool-next-tauri:settings', JSON.stringify({
      appearance: { theme: nextTheme, uiScale: nextScale }
    }))
  }, { theme, uiScale })
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

test('tool surfaces load on first visit instead of at shell startup', async ({ page }) => {
  const requested = new Set<string>()
  page.on('request', (request) => requested.add(request.url()))
  await open(page, '/')
  expect([...requested].some((url) => url.includes('/features/json/JsonToolSurface.tsx'))).toBe(false)
  expect([...requested].some((url) => url.includes('/features/qrcode/QrcodeSurface.tsx'))).toBe(false)

  await page.getByRole('button', { name: 'JSON', exact: true }).click()
  await expect(page.locator('.json-workbench')).toBeVisible()
  expect([...requested].some((url) => url.includes('/features/json/JsonToolSurface.tsx'))).toBe(true)
  expect([...requested].some((url) => url.includes('/features/qrcode/QrcodeSurface.tsx'))).toBe(false)
})

test('dark theme visual baseline', async ({ page }) => {
  await useAppearance(page, 'dark', 100)
  await open(page, '/?surface=json')
  await expect(page.locator('#root')).toHaveScreenshot('json-workbench-dark.png', { maxDiffPixelRatio: 0.01 })
})

for (const scale of [90, 110] as const) {
  test(`shell remains usable at ${scale}% interface scale`, async ({ page }) => {
    await useAppearance(page, 'light', scale)
    await open(page, '/', 1280, 760)
    await expect(page.locator('.app-shell')).toHaveScreenshot(`shell-scale-${scale}.png`, { maxDiffPixelRatio: 0.01 })
  })
}
