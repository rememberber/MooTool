import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

let app: ElectronApplication
let page: Page
let directory: string

test.beforeAll(async () => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-translation-e2e-'))
  app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })
  page = await app.firstWindow()
  await page.waitForLoadState('domcontentloaded')
  await page.evaluate(() => window.mootool.updateSettings({ tools: { translationProvider: 'deepl', translationSourceLang: 'auto', translationTargetLang: 'en' } }))
  await page.locator('.tool-button').filter({ hasText: '翻译' }).first().click()
})

test.afterAll(async () => { await app?.close(); await rm(directory, { recursive: true, force: true }) })

test('offers secure DeepL configuration, explicit requests, identifier splitting and naming copies', async ({}, testInfo) => {
  const surface = page.locator('.translation-tool-page')
  const source = surface.getByTestId('translation-source')
  await expect(surface.locator('.translation-engine-hint')).toContainText('Free / Pro')
  await source.fill('getHTTPResponse')
  await surface.getByRole('button', { name: '拆分标识符', exact: true }).click()
  await expect(source).toHaveText('get HTTP Response')
  await surface.getByRole('button', { name: '翻译', exact: true }).click()
  await expect(page.getByText('请先在设置 → 工具默认值中保存 DeepL API Key。', { exact: false })).toBeVisible()

  await surface.getByRole('button', { name: 'DeepL API Key 设置', exact: true }).click()
  const settings = page
  await expect(settings.locator('.settings-page')).toBeVisible()
  // Password inputs have no implicit textbox role.
  const password = settings.locator('input[type="password"][aria-label="DeepL API Key 设置"]')
  await expect(password).toBeVisible()
  const status = await settings.evaluate(() => window.mootool.getSecretStatus('deeplApiKey'))
  if (status.encryptionAvailable) {
    await password.fill('e2e-not-a-real-key:fx')
    await password.locator('..').getByRole('button', { name: '保存', exact: true }).click()
    await expect(password).toHaveValue('')
    await expect.poll(() => settings.evaluate(() => window.mootool.getSecretStatus('deeplApiKey'))).toMatchObject({ stored: true })
    await settings.evaluate(() => window.mootool.clearSecret('deeplApiKey'))
  } else {
    await expect(password).toBeDisabled()
  }
  await settings.getByRole('button', { name: '返回工作区', exact: true }).click()

  await app.evaluate(({ ipcMain }) => {
    ;(globalThis as any).__translationCalls = []
    ipcMain.removeHandler('translation:send')
    ipcMain.handle('translation:send', (_event, input) => {
      ;(globalThis as any).__translationCalls.push(input)
      return { requestId: input.requestId, text: 'Get HTTP response', provider: input.preferredProvider, fallbackUsed: false }
    })
  })
  await source.fill('获取 HTTP 响应')
  await page.waitForTimeout(650)
  expect(await app.evaluate(() => (globalThis as any).__translationCalls.length)).toBe(0)
  await surface.getByRole('button', { name: '翻译', exact: true }).click()
  await expect(surface.getByTestId('translation-result')).toContainText('Get HTTP response')
  await expect(surface.locator('.translation-name-preview')).toHaveText('getHttpResponse')
  await surface.getByRole('button', { name: '复制命名', exact: true }).click()
  await expect.poll(() => app.evaluate(({ clipboard }) => clipboard.readText())).toBe('getHttpResponse')
  await surface.getByRole('combobox', { name: '命名格式', exact: true }).selectOption('snake_case')
  await surface.getByRole('button', { name: '复制命名', exact: true }).click()
  await expect.poll(() => app.evaluate(({ clipboard }) => clipboard.readText())).toBe('get_http_response')
  await source.fill('anotherValue')
  await expect(surface.getByRole('button', { name: '复制命名', exact: true })).toBeDisabled()
  await surface.getByRole('button', { name: '翻译', exact: true }).click()
  await expect(surface.locator('.translation-name-preview')).toHaveText('get_http_response')
  await page.screenshot({ path: testInfo.outputPath('translation.png') })
  expect(await app.evaluate(() => (globalThis as any).__translationCalls.length)).toBe(2)
})
