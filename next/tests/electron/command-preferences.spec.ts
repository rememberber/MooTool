import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

let app: ElectronApplication
let page: Page
let directory: string
let originalClipboard: string

async function launch(): Promise<void> {
  app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '1' } })
  page = await app.firstWindow()
  await page.waitForLoadState('domcontentloaded')
}

test.beforeAll(async () => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-command-preferences-e2e-'))
  await launch()
  originalClipboard = await app.evaluate(({ clipboard }) => clipboard.readText())
})

test.afterAll(async () => {
  if (app) {
    await app.evaluate(({ clipboard }, text) => clipboard.writeText(text), originalClipboard ?? '')
    await app.close()
  }
  if (directory) await rm(directory, { recursive: true, force: true })
})

async function search(query: string): Promise<void> {
  if (!await page.locator('.command-palette').isVisible()) await page.getByRole('button', { name: '搜索', exact: true }).click()
  await page.locator('.command-palette__search input').fill(query)
}

test('records successful action names and pins the selected action without saving payloads', async ({}, testInfo) => {
  await app.evaluate(({ clipboard }) => clipboard.writeText('clipboard remains unchanged'))
  await search('base64 encode private-command-input')
  await page.keyboard.press('Enter')
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue('cHJpdmF0ZS1jb21tYW5kLWlucHV0')
  await expect.poll(() => page.evaluate(() => window.mootool.getCommandPaletteState())).toEqual({ pinnedActionIds: [], recentActionIds: ['base64-encode'] })
  await page.getByRole('button', { name: '设为常用', exact: true }).click()
  await expect(page.getByRole('button', { name: '取消常用', exact: true })).toHaveAttribute('aria-pressed', 'true')
  await page.keyboard.press('Escape')
  await page.keyboard.press('Escape')
  await search('')
  await expect(page.locator('.command-palette__section').first()).toHaveText('常用动作')
  await expect(page.locator('.command-result').first()).toHaveAttribute('data-command-id', 'base64-encode')
  await expect(page.getByRole('button', { name: '取消常用', exact: true })).toBeVisible()
  await page.screenshot({ path: testInfo.outputPath('pinned-action.png') })
  const userData = await page.evaluate(async () => (await window.mootool.getAppPaths()).userData)
  const stored = await readFile(join(userData, 'mootool-next.json'), 'utf8')
  expect(stored).not.toContain('private-command-input')
  expect(stored).not.toContain('cHJpdmF0ZS1jb21tYW5kLWlucHV0')
  expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe('clipboard remains unchanged')
  await page.keyboard.press('Escape')
})

test('does not record failed actions and keeps pins when clearing recents', async () => {
  await search('base64 decode !!!')
  await page.keyboard.press('Enter')
  await expect(page.getByRole('alert')).toContainText('有效')
  expect((await page.evaluate(() => window.mootool.getCommandPaletteState())).recentActionIds).toEqual(['base64-encode'])
  await search('uuid')
  await page.keyboard.press('Enter')
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue(/^[\da-f-]{36}$/)
  await expect.poll(() => page.evaluate(async () => (await window.mootool.getCommandPaletteState()).recentActionIds)).toEqual(['uuid', 'base64-encode'])
  await page.keyboard.press('Escape')
  await search('')
  await expect(page.locator('.command-palette__section')).toContainText(['常用动作', '最近执行', '全部动作', '工具'])
  await page.getByRole('button', { name: '清空最近执行', exact: true }).click()
  await expect(page.locator('.command-palette__search input')).toBeFocused()
  await expect.poll(() => page.evaluate(() => window.mootool.getCommandPaletteState())).toEqual({ pinnedActionIds: ['base64-encode'], recentActionIds: [] })
  await expect(page.locator('.command-palette__section')).toContainText(['常用动作', '全部动作', '工具'])
  await page.keyboard.press('Escape')
})

test('dispatches a quick action into preview above a docked tool and repeats it with fresh clipboard input', async () => {
  await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
  await expect.poll(() => app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].contentView.children.length)).toBe(1)
  await app.evaluate(({ clipboard, BrowserWindow }) => {
    clipboard.writeText('{"first":1}')
    BrowserWindow.getAllWindows()[0].hide()
  })
  await page.evaluate(() => window.mootool.openCommandAction('json-format'))
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue('{\n  "first": 1\n}')
  expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe('{"first":1}')
  await expect.poll(() => app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].contentView.children.length)).toBe(0)
  await app.evaluate(({ clipboard }) => clipboard.writeText('{"second":2}'))
  await page.evaluate(() => window.mootool.openCommandAction('json-format'))
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue('{\n  "second": 2\n}')
  expect((await page.evaluate(() => window.mootool.getCommandPaletteState())).recentActionIds).toEqual(['json-format'])
  await expect(page.evaluate(() => window.mootool.openCommandAction('not-a-command' as any))).rejects.toThrow('Invalid command action')
  await page.keyboard.press('Escape')
  await page.keyboard.press('Escape')
})

test('a newer quick action replaces a pending read, and failure clears the previous preview', async () => {
  await page.evaluate(() => {
    const state = window as typeof window & { __savedRead?: () => Promise<string>; __resolveRead?: (value: string) => void }
    state.__savedRead = navigator.clipboard.readText.bind(navigator.clipboard)
    navigator.clipboard.readText = () => new Promise<string>((resolve) => { state.__resolveRead = resolve })
  })
  try {
    await page.evaluate(() => window.mootool.openCommandAction('json-format'))
    await expect(page.getByRole('dialog', { name: '命令面板' })).toHaveAttribute('aria-busy', 'true')
    await page.evaluate(() => window.mootool.openCommandAction('uuid'))
    await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue(/^[\da-f-]{36}$/)
    const uuid = await page.getByRole('textbox', { name: '结果预览' }).inputValue()
    await page.evaluate(() => {
      const state = window as typeof window & { __resolveRead?: (value: string) => void }
      state.__resolveRead?.('{"stale":true}')
    })
    await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue(uuid)
    await expect.poll(() => page.evaluate(async () => (await window.mootool.getCommandPaletteState()).recentActionIds)).toEqual(['uuid', 'json-format'])
  } finally {
    await page.evaluate(() => {
      const state = window as typeof window & { __savedRead?: () => Promise<string> }
      if (state.__savedRead) navigator.clipboard.readText = state.__savedRead
    })
  }
  await app.evaluate(({ clipboard }) => clipboard.writeText('!!!'))
  await page.evaluate(() => window.mootool.openCommandAction('base64-decode'))
  await expect(page.getByRole('alert')).toContainText('有效')
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveCount(0)
  expect((await page.evaluate(() => window.mootool.getCommandPaletteState())).recentActionIds).toEqual(['uuid', 'json-format'])
  await page.keyboard.press('Escape')
})

test('restores pins and recent actions after restart and allows unpinning', async ({}, testInfo) => {
  await app.close()
  await launch()
  await search('')
  await expect(page.locator('.command-result').first()).toHaveAttribute('data-command-id', 'base64-encode')
  await expect(page.locator('.command-palette__section')).toContainText(['常用动作', '最近执行', '全部动作', '工具'])
  await page.getByRole('button', { name: '取消常用', exact: true }).click()
  await expect.poll(() => page.evaluate(async () => (await window.mootool.getCommandPaletteState()).pinnedActionIds)).toEqual([])
  await expect(page.getByRole('button', { name: '设为常用', exact: true })).toHaveAttribute('aria-pressed', 'false')
  await expect(page.locator('.command-palette__section').first()).toHaveText('最近执行')
  await expect(page.locator('.command-palette').getByRole('option', { selected: true })).toHaveAttribute('data-command-id', 'base64-encode')
  await page.screenshot({ path: testInfo.outputPath('recent-actions.png') })
  await page.keyboard.press('Escape')
})
