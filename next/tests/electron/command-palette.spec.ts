import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

let app: ElectronApplication
let page: Page
let directory: string
let previousClipboard: string

test.beforeAll(async () => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-command-e2e-'))
  app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })
  page = await app.firstWindow()
  await page.waitForLoadState('domcontentloaded')
  previousClipboard = await app.evaluate(({ clipboard }) => clipboard.readText())
})

test.afterAll(async () => {
  if (app) {
    await app.evaluate(({ clipboard }, text) => clipboard.writeText(text), previousClipboard ?? '')
    await app.close()
  }
  if (directory) await rm(directory, { recursive: true, force: true })
})

async function search(query: string): Promise<void> {
  if (!await page.locator('.command-palette').isVisible()) {
    await page.getByRole('button', { name: '搜索', exact: true }).click()
  }
  await page.locator('.command-palette__search input').fill(query)
}

test('generates a UUID with keyboard navigation, previews before copying and returns to search', async () => {
  await app.evaluate(({ clipboard }) => clipboard.writeText('unchanged until copy'))
  await search('uuid')
  const input = page.locator('.command-palette__search input')
  await input.press('Enter')
  const result = page.getByRole('textbox', { name: '结果预览' })
  await expect(result).toHaveValue(/^[\da-f]{8}-[\da-f]{4}-4[\da-f]{3}-[89ab][\da-f]{3}-[\da-f]{12}$/)
  const uuid = await result.inputValue()
  expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe('unchanged until copy')
  const copy = page.getByRole('button', { name: '复制结果', exact: true })
  await expect(copy).toBeFocused()
  await copy.press('Enter')
  await expect.poll(() => app.evaluate(({ clipboard }) => clipboard.readText())).toBe(uuid)
  await expect(page.getByText('已复制结果', { exact: true })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(input).toBeFocused()
  await expect(input).toHaveValue('uuid')
  await page.keyboard.press('Escape')
  await expect(page.locator('.command-palette')).toBeHidden()
})

test('formats clipboard JSON and executes explicit input without consuming the clipboard', async () => {
  const source = '{"b":2,"a":1}'
  await app.evaluate(({ clipboard }, value) => clipboard.writeText(value), source)
  await search('json format')
  await expect(page.locator('.command-palette').getByRole('option')).toHaveCount(1)
  await page.keyboard.press('Enter')
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue('{\n  "b": 2,\n  "a": 1\n}')
  expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe(source)
  await page.getByRole('button', { name: '复制结果', exact: true }).click()
  await expect.poll(() => app.evaluate(({ clipboard }) => clipboard.readText())).toBe('{\n  "b": 2,\n  "a": 1\n}')
  await page.keyboard.press('Escape')
  await search('base64 encode  hello ')
  await page.keyboard.press('Enter')
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue('IGhlbGxvIA==')
  await page.keyboard.press('Escape')
  await search('时间戳 1728000000000')
  await page.keyboard.press('Enter')
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue(/2024-10-0[34].*\(.+\)/)
  await page.keyboard.press('Escape')
  await page.keyboard.press('Escape')
})

test('keeps the clipboard intact on invalid input and preserves tool navigation', async () => {
  await app.evaluate(({ clipboard }) => clipboard.writeText('original text'))
  await search('base64 decode !!!')
  await page.keyboard.press('Enter')
  await expect(page.getByRole('alert')).toHaveText('请输入有效的 UTF-8 Base64 文本。')
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveCount(0)
  expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe('original text')
  await search('json {"a":1,"a":2}')
  await page.keyboard.press('Enter')
  await expect(page.getByRole('alert')).toContainText('a')
  await search('json')
  await page.keyboard.press('Enter')
  await expect(page.locator('.command-palette')).toBeHidden()
  await expect(page.locator('.json-tool')).toBeVisible()
})

test('ignores a clipboard read that finishes after closing and reopening the palette', async () => {
  await page.evaluate(() => {
    const state = window as typeof window & { __originalClipboardRead?: () => Promise<string>; __finishClipboard?: (text: string) => void }
    state.__originalClipboardRead = navigator.clipboard.readText.bind(navigator.clipboard)
    navigator.clipboard.readText = () => new Promise<string>((resolve) => { state.__finishClipboard = resolve })
  })
  try {
    await search('json format')
    await page.keyboard.press('Enter')
    await expect(page.getByRole('dialog', { name: '命令面板' })).toHaveAttribute('aria-busy', 'true')
    await page.keyboard.press('Escape')
    await search('uuid')
    await page.evaluate(() => {
      const state = window as typeof window & { __finishClipboard?: (text: string) => void }
      state.__finishClipboard?.('{"stale":true}')
    })
    await expect(page.locator('.command-palette__search input')).toHaveValue('uuid')
    await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveCount(0)
    await page.keyboard.press('Enter')
    await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveValue(/^[\da-f-]{36}$/)
    await page.keyboard.press('Escape')
    await page.keyboard.press('Escape')
  } finally {
    await page.evaluate(() => {
      const state = window as typeof window & { __originalClipboardRead?: () => Promise<string> }
      if (state.__originalClipboardRead) navigator.clipboard.readText = state.__originalClipboardRead
    })
  }
})

test('keeps focus and long result lists inside a compact palette in both themes', async ({}, testInfo) => {
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'light' } }))
  await app.evaluate(({ BrowserWindow }) => {
    const window = BrowserWindow.getAllWindows()[0]
    window.setMinimumSize(400, 300)
    window.setSize(680, 520)
  })
  await search('')
  const input = page.locator('.command-palette__search input')
  await input.press('Shift+Tab')
  await expect(page.getByRole('button', { name: '清空最近执行', exact: true })).toBeFocused()
  await page.keyboard.press('Tab')
  await expect(input).toBeFocused()
  for (let index = 0; index < 34; index += 1) await input.press('ArrowDown')
  await expect(page.locator('.command-palette').getByRole('option', { selected: true })).toContainText('系统信息')
  await expect.poll(() => page.locator('.command-palette').getByRole('option', { selected: true }).evaluate((element) => {
    const row = element.getBoundingClientRect()
    const list = element.parentElement!.getBoundingClientRect()
    return row.top >= list.top - 1 && row.bottom <= list.bottom + 1
  })).toBe(true)
  await input.fill('base64')
  await page.screenshot({ path: testInfo.outputPath('commands-light.png') })
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'dark' } }))
  await search('json {"hello":"world"}')
  await page.keyboard.press('Enter')
  const copy = page.getByRole('button', { name: '复制结果', exact: true })
  await expect(copy).toBeVisible()
  expect(await copy.evaluate((element) => element.getBoundingClientRect().bottom <= window.innerHeight)).toBe(true)
  await page.screenshot({ path: testInfo.outputPath('commands-dark-preview.png') })
  await page.keyboard.press('Escape')
  await page.keyboard.press('Escape')
})
