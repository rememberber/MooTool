import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createServer } from 'node:http'

let app: ElectronApplication
let page: Page
let directory: string
let previous: { text: string; html: string; rtf: string; image: string }

test.beforeAll(async () => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-clipboard-actions-'))
  app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '1' } })
  page = await app.firstWindow(); page.setDefaultTimeout(5000)
  previous = await app.evaluate(({ clipboard }) => ({ text: clipboard.readText(), html: clipboard.readHTML(), rtf: clipboard.readRTF(), image: clipboard.readImage().isEmpty() ? '' : clipboard.readImage().toDataURL() }))
})
test.afterAll(async () => {
  if (app) {
    await app.evaluate(({ clipboard, nativeImage }, previous) => clipboard.write({ text: previous.text, html: previous.html, rtf: previous.rtf, ...(previous.image ? { image: nativeImage.createFromDataURL(previous.image) } : {}) }), previous)
    await app.close()
  }
  await rm(directory, { recursive: true, force: true })
})
test.afterEach(async () => { if (await page.locator('.command-palette').isVisible()) await page.getByRole('button', { name: '关闭搜索', exact: true }).click() })

async function clipboard(text: string, html = '') {
  await app.evaluate(({ clipboard }, value) => value.html ? clipboard.write(value) : clipboard.writeText(value.text), { text, html })
}
async function run(actionId: 'clipboard-text' | 'clipboard-html' | 'html-markdown' | 'table-json') {
  await page.evaluate(actionId => window.mootool.openCommandAction(actionId), actionId)
}
const preview = () => page.getByRole('textbox', { name: '结果预览', exact: true })
const previewText = async () => (await preview().locator('.cm-line').allTextContents()).join('\n')
async function mockSave(path: string | null) {
  await app.evaluate(({ dialog }, path) => {
    const state = globalThis as typeof globalThis & { __clipboardSaveCount: number; __clipboardSaveDefault: string }
    state.__clipboardSaveCount = 0; state.__clipboardSaveDefault = ''
    dialog.showSaveDialog = (async (...args: any[]) => {
      state.__clipboardSaveCount++
      state.__clipboardSaveDefault = args[args.length - 1].defaultPath
      return { canceled: path === null, filePath: path ?? '' }
    }) as typeof dialog.showSaveDialog
  }, path)
}

test('converts rich HTML to Markdown without executing scripts, loading resources or changing the clipboard', async ({}, testInfo) => {
  let requests = 0
  const server = createServer((_request, response) => { requests++; response.end('image') })
  await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve))
  const address = server.address() as { port: number }
  try {
    await clipboard('plain clipboard fallback', `<h1>你好</h1><p><strong>粗体</strong>和<em>斜体</em> <a href="https://example.com/a(b)">链接</a></p><ul><li>第一项</li><li>第二项</li></ul><pre><code>a &lt; b\n\n\n&#96;x&#96;</code></pre><a href="javascript:alert(1)">危险链接</a><img src="http://127.0.0.1:${address.port}/image" alt="图片说明" onerror="window.__clipboardExecuted=true"><script>window.__clipboardExecuted=true</script>`)
    await run('html-markdown')
    await expect(preview()).toHaveText(/# 你好/, { useInnerText: true })
    const text = await previewText()
    expect(text).toContain('**粗体**和*斜体*')
    expect(text).toContain('[链接](<https://example.com/a(b)>)')
    expect(text).toContain('- 第一项\n- 第二项')
    expect(text).toContain('```\na < b\n\n\n`x`\n```')
    expect(text).toContain('危险链接图片说明')
    expect(text).not.toContain('javascript:')
    expect(await page.evaluate(() => Boolean((window as typeof window & { __clipboardExecuted?: boolean }).__clipboardExecuted))).toBe(false)
    expect(requests).toBe(0)
    expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe('plain clipboard fallback')
    await expect(page.getByRole('combobox', { name: '保存文件格式', exact: true })).toHaveValue('markdown')
    await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'light' } }))
    await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
    await page.screenshot({ path: testInfo.outputPath('clipboard-markdown-light.png'), animations: 'disabled' })
    await page.getByRole('button', { name: '复制结果', exact: true }).click()
    await expect.poll(() => app.evaluate(({ clipboard }) => clipboard.readText())).toBe(text)
  } finally { await new Promise<void>(resolve => server.close(() => resolve())) }
})

test('converts HTML tables to string-valued JSON and saves the preview snapshot above a docked native tool', async ({}, testInfo) => {
  await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
  await expect.poll(() => app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].contentView.children.length)).toBe(1)
  await clipboard('not a TSV table', '<table><tr><th>姓名</th><th>编号</th><th>备注</th></tr><tr><td>张三</td><td>001</td><td>一<br>二</td></tr><tr><td>李四</td><td>002</td><td></td></tr></table>')
  await run('table-json')
  await expect(preview()).toHaveText(/"编号": "001"/, { useInnerText: true })
  const text = await previewText()
  expect(JSON.parse(text)).toEqual([{ 姓名: '张三', 编号: '001', 备注: '一\n二' }, { 姓名: '李四', 编号: '002', 备注: '' }])
  await expect.poll(() => app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].contentView.children.length)).toBe(0)
  await clipboard('new clipboard content')
  const path = join(directory, 'table.json')
  await mockSave(path)
  await page.getByRole('button', { name: '保存为文件', exact: true }).click()
  await expect(page.getByText('已保存文件', { exact: true })).toBeVisible()
  expect(await readFile(path, 'utf8')).toBe(text)
  expect(await app.evaluate(() => (globalThis as typeof globalThis & { __clipboardSaveDefault: string }).__clipboardSaveDefault)).toBe('clipboard.json')
  expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe('new clipboard content')
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'dark' } }))
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.screenshot({ path: testInfo.outputPath('clipboard-table-dark.png'), animations: 'disabled' })
})

test('rejects duplicate headers, malformed rows, merged cells and oversized clipboard HTML', async () => {
  for (const [text, html, error] of [
    ['a\ta\n1\t2', '', '列名不能为空或重复'],
    ['a\tb\n1', '', '每行列数必须一致'],
    ['', '<table><tr><th colspan="2">a</th></tr><tr><td>1</td><td>2</td></tr></table>', '合并单元格'],
    ['fallback', 'x'.repeat(100001), '100,000']
  ]) {
    await clipboard(text, html)
    await run('table-json')
    await expect(page.getByRole('alert')).toContainText(error)
    await expect(preview()).toHaveCount(0)
    expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe(text)
  }
})

test('preserves plain Markdown fallbacks and previews raw HTML as text', async () => {
  await clipboard('literal *text* [link]\n你好')
  await run('html-markdown')
  await expect(preview()).toHaveText('literal *text* [link]\n你好', { useInnerText: true })
  await run('clipboard-html')
  await expect(page.getByRole('alert')).toContainText('没有 HTML')
  await clipboard('raw HTML', '<h1>源码</h1><script>window.__rawClipboardExecuted=true</script>')
  const html = await app.evaluate(({ clipboard }) => clipboard.readHTML())
  await run('clipboard-html')
  await expect(preview()).toHaveText(html, { useInnerText: true })
  await expect(page.getByRole('combobox', { name: '保存文件格式', exact: true })).toHaveValue('html')
  const path = join(directory, 'source.html')
  await mockSave(path)
  await page.getByRole('button', { name: '保存为文件', exact: true }).click()
  await expect(page.getByText('已保存文件', { exact: true })).toBeVisible()
  expect(await readFile(path, 'utf8')).toBe(html)
  expect(await page.evaluate(() => Boolean((window as typeof window & { __rawClipboardExecuted?: boolean }).__rawClipboardExecuted))).toBe(false)
})

test('validates JSON saves, handles cancellation and errors, and keeps controls inside a compact palette', async ({}, testInfo) => {
  await clipboard('plain 文本')
  await run('clipboard-text')
  await expect(preview()).toHaveText('plain 文本', { useInnerText: true })
  await mockSave(null)
  await page.getByRole('combobox', { name: '保存文件格式', exact: true }).selectOption('json')
  await page.getByRole('button', { name: '保存为文件', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('不是有效 JSON')
  expect(await app.evaluate(() => (globalThis as typeof globalThis & { __clipboardSaveCount: number }).__clipboardSaveCount)).toBe(0)
  await page.getByRole('combobox', { name: '保存文件格式', exact: true }).selectOption('text')
  await page.getByRole('button', { name: '保存为文件', exact: true }).click()
  await expect(page.locator('.command-palette')).toHaveAttribute('aria-busy', 'false')
  await expect(page.getByText('已保存文件', { exact: true })).toHaveCount(0)
  const path = join(directory, 'plain.txt')
  await mockSave(path)
  await page.getByRole('button', { name: '保存为文件', exact: true }).click()
  await expect(page.getByText('已保存文件', { exact: true })).toBeVisible()
  expect(await readFile(path, 'utf8')).toBe('plain 文本')
  await mockSave(join(directory, 'missing', 'output.txt'))
  await page.getByRole('button', { name: '保存为文件', exact: true }).click()
  await expect(page.getByRole('alert')).toContainText('保存失败')
  await expect(preview()).toHaveText('plain 文本', { useInnerText: true })
  await app.evaluate(({ BrowserWindow }) => { const window = BrowserWindow.getAllWindows()[0]; window.setMinimumSize(400, 300); window.setSize(680, 520) })
  expect(await page.getByRole('button', { name: '保存为文件', exact: true }).evaluate(button => button.getBoundingClientRect().bottom <= innerHeight)).toBe(true)
  expect(await page.locator('.command-palette').evaluate(element => element.scrollWidth <= element.clientWidth)).toBe(true)
  await page.screenshot({ path: testInfo.outputPath('clipboard-save-compact.png'), animations: 'disabled' })
  expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe('plain 文本')
})

test('ignores a rich clipboard snapshot that resolves after a newer quick action', async () => {
  await app.evaluate(({ ipcMain }) => {
    ipcMain.removeHandler('command-palette:read-clipboard')
    ipcMain.handle('command-palette:read-clipboard', () => new Promise(resolve => {
      (globalThis as typeof globalThis & { __finishRichClipboard?: (value: { text: string; html: string }) => void }).__finishRichClipboard = resolve
    }))
  })
  await run('html-markdown')
  await expect(page.locator('.command-palette')).toHaveAttribute('aria-busy', 'true')
  await page.evaluate(() => window.mootool.openCommandAction('uuid'))
  await expect(preview()).toHaveText(/^[\da-f-]{36}$/, { useInnerText: true })
  const uuid = await previewText()
  await app.evaluate(() => (globalThis as typeof globalThis & { __finishRichClipboard?: (value: { text: string; html: string }) => void }).__finishRichClipboard?.({ text: 'stale', html: '<h1>stale</h1>' }))
  await expect(preview()).toHaveText(uuid, { useInnerText: true })
  const preferences = await page.evaluate(() => window.mootool.getCommandPaletteState())
  expect(preferences.recentActionIds[0]).toBe('uuid')
  const stored = await readFile(join(directory, 'mootool-next.json'), 'utf8')
  expect(stored).not.toContain('literal *text*')
  expect(stored).not.toContain('plain 文本')
})
