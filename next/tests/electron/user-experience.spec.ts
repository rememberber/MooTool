import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { PDFDocument } from 'pdf-lib'
let app: ElectronApplication, page: Page, directory: string, nativeViews: boolean
async function launch() {
  app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: nativeViews ? '1' : '0' } })
  page = await app.firstWindow()
  await page.locator('.tool-button').first().waitFor()
}
test.beforeEach(async ({}, info) => { directory = await mkdtemp(join(tmpdir(), 'mootool-ux-')); nativeViews = info.title.includes('native tool'); await launch() })
test.afterEach(async () => { await app?.close(); await rm(directory, { recursive: true, force: true }) })
async function openJson() { await page.locator('.tool-button').filter({ hasText: 'JSON' }).click(); await page.locator('.json-editor .cm-content').waitFor() }
async function visible() { return app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].isVisible()) }

test('dialogs trap focus, protect forms from backdrop clicks and restore the opener', async () => {
  await openJson()
  const opener = page.getByRole('button', { name: '新建片段', exact: true })
  await opener.click()
  const dialog = page.getByRole('dialog', { name: '新建片段', exact: true }), input = dialog.locator('input')
  await expect(input).toBeFocused()
  await input.fill('keep-my-name.json')
  await page.keyboard.press('Tab')
  await expect(dialog.getByRole('button', { name: '取消', exact: true })).toBeFocused()
  const last = dialog.getByRole('button', { name: '创建', exact: true })
  await last.focus(); await page.keyboard.press('Tab')
  await expect(dialog.getByRole('button', { name: '关闭 新建片段', exact: true })).toBeFocused()
  await page.keyboard.press('Shift+Tab'); await expect(last).toBeFocused()
  expect(await page.locator('.app-shell').evaluate(element => Boolean(element.closest('[inert]')))).toBe(true)
  await page.locator('.dialog-backdrop').click({ position: { x: 4, y: 4 } })
  await expect(dialog).toBeVisible(); await expect(input).toHaveValue('keep-my-name.json')
  await page.keyboard.press('Escape'); await expect(dialog).toHaveCount(0); await expect(opener).toBeFocused()
  expect(await visible()).toBe(true)
  expect(await page.locator('.app-shell').evaluate(element => Boolean(element.closest('[inert]')))).toBe(false)
  await page.getByRole('button', { name: '删除片段', exact: true }).click()
  await expect(page.getByRole('dialog').getByRole('button', { name: '取消', exact: true })).toBeFocused()
  await page.keyboard.press('Enter'); await expect(page.getByRole('dialog')).toHaveCount(0)
  await expect(page.locator('.json-editor')).toBeVisible()
})

test('errors persist through elapsed time and routine notifications and can be copied and dismissed', async () => {
  await openJson(); await page.clock.install()
  await page.locator('.json-editor .cm-content').fill('{broken')
  await page.locator('.editor-toolbar').getByRole('button', { name: '格式化', exact: true }).click()
  const error = page.locator('.toast--error')
  await expect(error).toHaveCount(1)
  const message = await error.locator('.toast__message').innerText()
  await page.clock.fastForward(5000); await expect(error).toBeVisible()
  for (let i = 0; i < 5; i++) await page.locator('.editor-toolbar').getByRole('button', { name: /^(复制|已复制)$/ }).click()
  await error.getByRole('button', { name: '复制错误信息', exact: true }).click()
  await expect.poll(() => app.evaluate(({ clipboard }) => clipboard.readText())).toBe(message)
  await page.clock.fastForward(5000)
  await expect(page.locator('.toast--success')).toHaveCount(0); await expect(error).toBeVisible()
  await error.getByRole('button', { name: '关闭通知', exact: true }).click(); await expect(error).toHaveCount(0)
})

async function emptyJson() {
  // Simulate a workspace with no selected file; all file actions use the real repositories.
  await app.evaluate(({ ipcMain }) => {
    const original = (ipcMain as any)._invokeHandlers.get('json-vault:workspace')
    ipcMain.removeHandler('json-vault:workspace')
    ipcMain.handle('json-vault:workspace', async event => ({ ...await original(event), file: { relativePath: '', content: '', modifiedAt: '' } }))
  })
  await page.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  await page.locator('.json-empty-state').waitFor()
  await expect.poll(() => page.locator('.vault-panel [data-path]').count()).toBeGreaterThan(0)
}
async function createSnippet(name?: string) {
  const dialog = page.getByRole('dialog', { name: '新建片段', exact: true })
  if (name) await dialog.locator('input').fill(name)
  await dialog.getByRole('button', { name: '创建', exact: true }).click()
  await page.locator('.json-editor .cm-content').waitFor()
}

test('empty JSON creates a named snippet', async () => {
  await emptyJson()
  await page.locator('.json-empty-state').getByRole('button', { name: '新建片段', exact: true }).click()
  await createSnippet('from-empty.json')
  expect(await page.evaluate(() => window.mootool.readJsonVaultFile('from-empty.json'))).toMatchObject({ content: '{\n\n}' })
})

test('empty JSON imports into a new snippet without changing source bytes', async () => {
  await emptyJson()
  const content = '{"id":12345678901234567890,"x":1,"x":2}', path = join(directory, 'import.json')
  await writeFile(path, content)
  await app.evaluate(({ dialog }, path) => { dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [path] }) }, path)
  await page.locator('.json-empty-state').getByRole('button', { name: '导入文件', exact: true }).click()
  await expect(page.getByRole('dialog').locator('input')).toHaveValue('import.json')
  await createSnippet()
  expect((await page.evaluate(() => window.mootool.readJsonVaultFile('import.json'))).content).toBe(content)
  expect(await readFile(path, 'utf8')).toBe(content)
})

test('empty JSON creates a local example only after explicit confirmation', async ({}, info) => {
  await emptyJson()
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'light' } }))
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  await page.screenshot({ path: info.outputPath('json-empty-light.png'), animations: 'disabled' })
  await page.locator('.json-empty-state').getByRole('button', { name: '用示例新建', exact: true }).click()
  await expect(page.getByRole('dialog').locator('input')).toHaveValue('example.json')
  await createSnippet()
  expect(JSON.parse((await page.evaluate(() => window.mootool.readJsonVaultFile('example.json'))).content)).toMatchObject({ local: true })
})

async function pdf(path: string, pages: number) {
  const document = await PDFDocument.create()
  for (let i = 0; i < pages; i++) document.addPage([320, 480])
  await writeFile(path, await document.save())
}

test('PDF results open, reveal and copy; cancellation has its own state and preserves prior output', async ({}, info) => {
  test.setTimeout(60_000)
  const first = join(directory, 'first.pdf'), second = join(directory, 'second.pdf'), output = join(directory, 'result.pdf')
  await pdf(first, 4); await pdf(second, 3)
  await app.evaluate(({ dialog, shell }, paths) => {
    ;(globalThis as any).__pdfActions = []
    dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [paths.first, paths.second] })
    dialog.showSaveDialog = async () => ({ canceled: false, filePath: paths.output })
    shell.openPath = async path => { (globalThis as any).__pdfActions.push(['open', path]); return '' }
    shell.showItemInFolder = path => { (globalThis as any).__pdfActions.push(['reveal', path]) }
  }, { first, second, output })
  await page.locator('.tool-button').filter({ hasText: 'PDF' }).click()
  await page.getByRole('tab', { name: '合并 PDF', exact: true }).click()
  await page.getByRole('button', { name: '添加 PDF', exact: true }).click()
  await expect(page.locator('.pdf-table tbody tr')).toHaveCount(2)
  await page.getByRole('button', { name: '开始合并', exact: true }).click()
  await expect(page.locator('.pdf-output-item code')).toHaveText(output)
  for (const name of [/^打开文件 /, /^在文件夹中显示 /, /^复制路径 /]) await page.locator('.pdf-output-item').getByRole('button', { name }).click()
  await expect.poll(() => app.evaluate(({ clipboard }) => clipboard.readText())).toBe(output)
  expect(await app.evaluate(() => (globalThis as any).__pdfActions)).toEqual([['open', output], ['reveal', output]])
  expect(await page.evaluate(async path => { try { await window.mootool.actOnPdfOutput(path, 'open'); return false } catch { return true } }, first)).toBe(true)
  for (const close of await page.locator('.toast--success .toast__dismiss').all()) await close.click()
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'light' } }))
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  await page.screenshot({ path: info.outputPath('pdf-results-light.png'), animations: 'disabled' })
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'dark' } }))
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await page.screenshot({ path: info.outputPath('pdf-results-dark.png'), animations: 'disabled' })
  await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setContentSize(1080, 760))
  await page.screenshot({ path: info.outputPath('pdf-results-narrow-dark.png'), animations: 'disabled' })
  for (const button of await page.locator('.pdf-output-item__actions button').all()) {
    const box = await button.boundingBox()
    expect(box).not.toBeNull()
    expect(box!.x + box!.width).toBeLessThanOrEqual((await page.evaluate(() => innerWidth)) + 1)
  }
  const before = await readFile(output)
  await pdf(first, 5000); await pdf(second, 5000)
  for (const input of await page.locator('.pdf-table tbody input:not([type="checkbox"])').all()) await input.fill('1-5000')
  await page.getByRole('button', { name: '开始合并', exact: true }).click()
  await page.getByRole('button', { name: '取消', exact: true }).click()
  await expect(page.locator('.pdf-operation-summary')).toHaveText('已取消')
  await expect(page.locator('.pdf-table .pdf-status--cancelled')).toHaveCount(2)
  await expect(page.locator('.toast--error')).toHaveCount(0)
  expect(await readFile(output)).toEqual(before)
})

test('Escape defaults to hiding and opt-out persists after restart without disabling dialog Escape', async () => {
  expect((await page.evaluate(() => window.mootool.getSettings())).general.escapeHidesWindow).toBe(true)
  await page.keyboard.press('Escape'); await expect.poll(visible).toBe(false)
  await app.evaluate(({ BrowserWindow }) => { const window = BrowserWindow.getAllWindows()[0]; window.show(); window.focus() })
  await page.locator('.sidebar-footer .icon-ghost').click()
  await page.getByRole('switch', { name: '按 Esc 隐藏窗口', exact: true }).click()
  await expect.poll(() => page.evaluate(async () => (await window.mootool.getSettings()).general.escapeHidesWindow)).toBe(false)
  await openJson(); await page.getByRole('button', { name: '新建片段', exact: true }).click()
  await page.keyboard.press('Escape'); await expect(page.getByRole('dialog')).toHaveCount(0)
  expect(await visible()).toBe(true)
  await app.close(); await launch()
  expect((await page.evaluate(() => window.mootool.getSettings())).general.escapeHidesWindow).toBe(false)
  await page.keyboard.press('Escape'); await page.waitForTimeout(100); expect(await visible()).toBe(true)
})

test('Escape preference updates an existing detached native tool view', async () => {
  await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
  await expect.poll(() => app.evaluate(async ({ webContents }) => {
    const contents = webContents.getAllWebContents().find(item => item.getURL().includes('toolId=calculator'))
    try { return await contents?.executeJavaScript(`Boolean(document.querySelector('#calculator-expression'))`) } catch { return false }
  })).toBe(true)
  await page.evaluate(() => window.mootool.detachToolWindow('calculator'))
  async function escape() {
    await app.evaluate(async ({ webContents }) => {
      const contents = webContents.getAllWebContents().find(item => item.getURL().includes('toolId=calculator'))!
      await contents.executeJavaScript(`new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))`)
      contents.sendInputEvent({ type: 'keyDown', keyCode: 'ESC' }); contents.sendInputEvent({ type: 'keyUp', keyCode: 'ESC' })
    })
  }
  await page.evaluate(() => window.mootool.updateSettings({ general: { escapeHidesWindow: false } })); await escape()
  expect((await page.evaluate(() => window.mootool.getToolWindowSnapshot())).tools.find(tool => tool.toolId === 'calculator')?.detached).toBe(true)
  await page.evaluate(() => window.mootool.updateSettings({ general: { escapeHidesWindow: true } })); await escape()
  await expect.poll(async () => (await page.evaluate(() => window.mootool.getToolWindowSnapshot())).tools.find(tool => tool.toolId === 'calculator')?.detached).toBe(false)
})
