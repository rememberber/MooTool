import { expect, test, type Page } from '@playwright/test'

type Locale = 'zh-CN' | 'en-US' | 'ja-JP'
type Theme = 'light' | 'dark'

const productSurfaces = [
  'quick-note', 'text-diff', 'reformat', 'json', 'config', 'runtime', 'protobuf',
  'variables', 'http', 'host', 'network', 'ua', 'encode', 'crypto', 'regex',
  'cron', 'qrcode', 'timestamp', 'message-board', 'translation', 'calculator',
  'color', 'image', 'pdf', 'system'
] as const

const visualMatrix: Array<{ key: string; locale: Locale; theme: Theme; width: 1080 | 1440 }> = [
  { key: 'zh-light-wide', locale: 'zh-CN', theme: 'light', width: 1440 },
  { key: 'en-dark-compact', locale: 'en-US', theme: 'dark', width: 1080 },
  { key: 'ja-light-compact', locale: 'ja-JP', theme: 'light', width: 1080 }
]

const stableCss = `
  *, *::before, *::after { animation: none !important; transition: none !important; caret-color: transparent !important; }
  .utility-session, .json-workbench__session, .json-workbench__footer code,
  .tool-surface-report-error { visibility: hidden !important; }
`

test.beforeEach(async ({ page }) => {
  await page.clock.install({ time: new Date('2025-06-15T08:30:00.000Z') })
})

async function open(
  page: Page,
  path: string,
  width = 1366,
  height = 820,
  appearance: { locale: Locale; theme: Theme; uiScale?: 90 | 100 | 110 } = { locale: 'zh-CN', theme: 'light' }
) {
  await page.addInitScript((value) => {
    window.localStorage.clear()
    window.localStorage.setItem('mootool-next-tauri:settings', JSON.stringify({
      general: { language: value.locale },
      appearance: { theme: value.theme, uiScale: value.uiScale ?? 100 }
    }))
  }, appearance)
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

for (const surface of productSurfaces) {
  for (const visual of visualMatrix) {
    test(`${surface} visual baseline · ${visual.key}`, async ({ page }) => {
      await open(page, `/?surface=${surface}`, visual.width, 900, visual)
      await expect(page.locator('#root')).toHaveScreenshot(`${surface}-${visual.key}.png`, { maxDiffPixelRatio: 0.01 })
    })
  }

  test(`${surface} immersive, responsive, and accessible DOM contract`, async ({ page }) => {
    await open(page, `/?surface=${surface}`, 720, 760)
    await expect(page.locator('h1')).toHaveCount(1)
    await expect(page.locator('h1.visually-hidden')).toHaveCount(1)
    expect(await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth)).toBeLessThanOrEqual(1)
    expect(await page.locator('.eyebrow').allTextContents()).not.toContainEqual(expect.stringMatching(/tauri/i))
    expect(await page.locator('button').evaluateAll((buttons) => buttons
      .filter((button) => (button as HTMLElement).getClientRects().length > 0 && !(button.getAttribute('aria-label')
        || button.getAttribute('aria-labelledby')
        || button.getAttribute('title')
        || button.textContent?.trim()))
      .map((button) => ({
        button: button.outerHTML.slice(0, 180),
        icon: button.querySelector('svg')?.getAttribute('class') || '',
        text: button.textContent || '',
        ancestors: [button.parentElement, button.parentElement?.parentElement, button.parentElement?.parentElement?.parentElement]
          .map((element) => element?.className || element?.tagName || '').join(' > ')
      })))).toEqual([])
  })
}

test('settings visual baseline', async ({ page }) => {
  await open(page, '/?surface=settings')
  await expect(page.locator('#root')).toHaveScreenshot('settings.png', { maxDiffPixelRatio: 0.01 })
})

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
  await open(page, '/?surface=json', 1366, 820, { locale: 'zh-CN', theme: 'dark' })
  await expect(page.locator('#root')).toHaveScreenshot('json-workbench-dark.png', { maxDiffPixelRatio: 0.01 })
})

for (const scale of [90, 110] as const) {
  test(`shell remains usable at ${scale}% interface scale`, async ({ page }) => {
    await open(page, '/', 1280, 760, { locale: 'zh-CN', theme: 'light', uiScale: scale })
    await expect(page.locator('.app-shell')).toHaveScreenshot(`shell-scale-${scale}.png`, { maxDiffPixelRatio: 0.01 })
  })
}
