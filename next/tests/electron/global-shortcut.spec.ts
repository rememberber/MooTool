import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

let app: ElectronApplication
let page: Page
let directory: string

async function launch(): Promise<void> {
  app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '1' } })
  page = await app.firstWindow()
  await page.waitForLoadState('domcontentloaded')
  await page.evaluate(() => window.mootool.openSettings('shortcuts'))
  await expect(page.locator('.global-shortcut-settings')).toBeVisible()
}

test.beforeAll(async () => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-global-shortcut-e2e-'))
  await launch()
  await app.evaluate(({ globalShortcut }) => {
    const state = globalThis as any
    state.__shortcutCallbacks = new Map()
    const register = globalShortcut.register.bind(globalShortcut)
    globalShortcut.register = (accelerator, callback) => {
      if (state.__failedShortcut === accelerator) return false
      const success = register(accelerator, callback)
      if (success) state.__shortcutCallbacks.set(accelerator, callback)
      return success
    }
  })
})

test.afterAll(async () => { await app?.close(); if (directory) await rm(directory, { recursive: true, force: true }) })

test('defaults to disabled and rejects invalid or conflicting combinations before saving', async () => {
  const group = page.locator('.global-shortcut-settings')
  await expect(group.getByRole('switch')).toHaveAttribute('aria-checked', 'false')
  await expect(group.getByRole('status')).toContainText('未启用')
  const input = group.getByLabel('全局快捷键组合')
  await input.fill('Shift+Space')
  await expect(group.getByRole('alert')).toContainText('修饰键')
  await expect(group.getByRole('button', { name: '保存', exact: true })).toBeDisabled()
  await input.fill('CommandOrControl+K')
  await expect(group.getByRole('alert')).toContainText('冲突')
  await expect(group.getByRole('switch')).toBeDisabled()
  expect((await page.evaluate(() => window.mootool.getSettings())).shortcuts.globalSearchEnabled).toBe(false)
})

test('registers a custom combination and restores a hidden or minimized main window', async () => {
  const group = page.locator('.global-shortcut-settings')
  await group.getByLabel('全局快捷键组合').fill('Ctrl+Alt+Shift+F18')
  await group.getByRole('button', { name: '保存', exact: true }).click()
  await group.getByRole('switch').click()
  await expect(group.getByRole('status')).toContainText('已启用')
  expect(await app.evaluate(({ globalShortcut }) => globalShortcut.isRegistered('Control+Alt+Shift+F18'))).toBe(true)
  await page.getByRole('button', { name: '返回工作区', exact: true }).click()
  await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
  await expect.poll(() => app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].contentView.children.length)).toBe(1)
  for (const mode of ['hide', 'minimize'] as const) {
    await app.evaluate(({ BrowserWindow }, action) => {
      const window = BrowserWindow.getAllWindows()[0]
      if (action === 'hide') window.hide()
      else window.minimize()
    }, mode)
    await app.evaluate(() => (globalThis as any).__shortcutCallbacks.get('Control+Alt+Shift+F18')())
    await expect(page.getByRole('dialog', { name: '命令面板' })).toBeVisible()
    await expect.poll(() => app.evaluate(({ BrowserWindow }) => {
      const window = BrowserWindow.getAllWindows()[0]
      return { visible: window.isVisible(), minimized: window.isMinimized(), toolViews: window.contentView.children.length }
    })).toEqual({ visible: true, minimized: false, toolViews: 0 })
    await page.locator('.command-palette__search input').fill('uuid')
    await page.keyboard.press('Enter')
    await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue(/^[\da-f-]{36}$/)
    await page.keyboard.press('Escape')
    await page.keyboard.press('Escape')
  }
})

test('reports an unavailable combination, releases the old key and retries registration', async ({}, testInfo) => {
  await page.evaluate(() => window.mootool.openSettings('shortcuts'))
  const group = page.locator('.global-shortcut-settings')
  await expect(group).toBeVisible()
  await app.evaluate(() => { (globalThis as any).__failedShortcut = 'Control+Alt+Shift+F19' })
  await group.getByLabel('全局快捷键组合').fill('Ctrl+Alt+Shift+F19')
  await group.getByRole('button', { name: '保存', exact: true }).click()
  await expect(group.getByRole('status')).toContainText('注册失败')
  expect(await app.evaluate(({ globalShortcut }) => globalShortcut.isRegistered('Control+Alt+Shift+F18'))).toBe(false)
  await page.screenshot({ path: testInfo.outputPath('shortcut-registration-failed.png') })
  await app.evaluate(() => { (globalThis as any).__failedShortcut = undefined })
  await group.getByRole('button', { name: '重试注册', exact: true }).click()
  await expect(group.getByRole('status')).toContainText('已启用')
  expect(await app.evaluate(({ globalShortcut }) => globalShortcut.isRegistered('Control+Alt+Shift+F19'))).toBe(true)
})

test('restores configuration after restart and unregisters on disable', async ({}, testInfo) => {
  await app.close()
  await launch()
  const group = page.locator('.global-shortcut-settings')
  await expect(group.getByLabel('全局快捷键组合')).toHaveValue('Ctrl+Alt+Shift+F19')
  await expect(group.getByRole('switch')).toHaveAttribute('aria-checked', 'true')
  await expect(group.getByRole('status')).toContainText('已启用')
  expect(await app.evaluate(({ globalShortcut }) => globalShortcut.isRegistered('Control+Alt+Shift+F19'))).toBe(true)
  await group.getByRole('switch').click()
  await expect(group.getByRole('status')).toContainText('未启用')
  expect(await app.evaluate(({ globalShortcut }) => globalShortcut.isRegistered('Control+Alt+Shift+F19'))).toBe(false)
  await group.getByRole('button', { name: '恢复默认组合', exact: true }).click()
  await expect(group.getByLabel('全局快捷键组合')).toHaveValue(process.platform === 'darwin' ? 'Cmd+Shift+Space' : 'Ctrl+Shift+Space')
  await page.screenshot({ path: testInfo.outputPath('global-shortcut-settings.png') })
})
