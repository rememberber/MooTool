import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { PDFDocument } from 'pdf-lib'

let app: ElectronApplication
let page: Page
let directory: string
test.beforeEach(async ({}, info) => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-background-'))
  app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(),
    env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: info.title.includes('crash') ? '1' : '0' } })
  page = await app.firstWindow()
  await page.locator('.tool-button').first().waitFor()
})
test.afterEach(async () => { await app?.close(); await rm(directory, { recursive: true, force: true }) })

test('JSON background validation and formatting discard superseded input', async () => {
  await page.evaluate(async () => {
    await window.mootool.saveJsonVaultFile({ relativePath: 'worker.json', content: '{"hello":1}' })
  })
  await page.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  await page.locator('.vault-panel [data-path="worker.json"]').dblclick()
  const editor = page.locator('.json-editor .cm-content')
  await expect(editor).toContainText('hello')
  await page.evaluate(() => {
    const OriginalWorker = window.Worker
    ;(window as any).__tasks = []
    window.Worker = class extends OriginalWorker {
      postMessage(task: any) {
        ;(window as any).__tasks.push(task.kind === 'json' ? task.operation : task.kind)
        super.postMessage(task)
      }
    }
  })
  const large = JSON.stringify({ rows: Array.from({ length: 8000 }, (_, i) => ({ i, value: 'MooTool' })) })
  await editor.fill(large)
  await expect.poll(() => page.evaluate(() => (window as any).__tasks.includes('validateJson'))).toBe(true)
  await expect(page.locator('.json-statusbar--valid')).toBeVisible()
  await page.locator('.editor-toolbar').getByRole('button', { name: '格式化', exact: true }).click()
  await expect.poll(() => page.evaluate(async expected => (await window.mootool.listHistory({ funcType: 'json' })).some(record => record.outputText === expected), JSON.stringify(JSON.parse(large), null, 2))).toBe(true)
  // Delay worker replies to deterministically exercise the stale-result guard.
  await page.evaluate(() => {
    const OriginalWorker = window.Worker
    window.Worker = class extends OriginalWorker {
      postMessage(task: any) { setTimeout(() => { try { super.postMessage(task) } catch {} }, 350) }
    }
  })
  await editor.fill('{"old":1}')
  await page.locator('.editor-toolbar').getByRole('button', { name: '格式化', exact: true }).click()
  await editor.fill('{"new":2}')
  await expect(editor).toHaveText('{"new":2}')
  await expect(page.locator('.json-statusbar--valid')).toBeVisible()
  await page.waitForTimeout(500)
  await expect(editor).toHaveText('{"new":2}')
})

test('text diff runs in a cancellable worker and keeps the latest inputs', async () => {
  await page.locator('.tool-button').filter({ hasText: '文本对比' }).click()
  const editors = page.locator('.diff-editor .cm-content')
  await editors.nth(0).fill('one\ntwo\n')
  await editors.nth(1).fill('one\nthree\n')
  await expect(page.locator('.diff-status')).toContainText('共发现 1 处差异')
  await page.getByRole('combobox', { name: '显示模式' }).selectOption('unified')
  await expect(page.locator('.unified-diff')).toContainText('+three')
  await page.evaluate(() => {
    const OriginalWorker = window.Worker
    window.Worker = class extends OriginalWorker {
      postMessage(task: any) { setTimeout(() => { try { super.postMessage(task) } catch {} }, 350) }
    }
  })
  await editors.nth(1).fill('one\nstale\n')
  await page.getByRole('button', { name: '对比', exact: true }).click()
  await editors.nth(1).fill('one\ntwo\n')
  await expect(page.locator('.diff-status')).toContainText('共发现 0 处差异')
  await expect(page.locator('.unified-diff')).not.toContainText('stale')
  await page.getByRole('button', { name: '对比', exact: true }).click()
  await page.getByRole('button', { name: '取消', exact: true }).click()
  await expect(page.getByRole('button', { name: '取消', exact: true })).toHaveCount(0)
})

test('PDF background jobs inspect, merge, split and cancel without changing destinations', async () => {
  const source = await PDFDocument.create()
  for (let i = 0; i < 40; i++) source.addPage([320 + i, 480])
  const input = join(directory, 'source.pdf')
  const output = join(directory, 'merged.pdf')
  await writeFile(input, await source.save())
  await app.evaluate(({ dialog }, paths) => {
    dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [paths.input] })
    dialog.showSaveDialog = async () => ({ canceled: false, filePath: paths.output })
  }, { input, output })
  const files = await page.evaluate(() => window.mootool.choosePdfFiles('inspect'))
  expect(files[0].pageCount).toBe(40)
  const merged = await page.evaluate(path => window.mootool.mergePdfFiles([
    { path, pages: '2-3' }, { path, pages: '5,7' }
  ], 'merge'), input)
  expect(merged?.pageCount).toBe(4)
  expect((await PDFDocument.load(await readFile(output))).getPageCount()).toBe(4)
  const split = await page.evaluate(path => window.mootool.splitPdfFiles([
    { path, pageRange: '1-6', rule: 'odd', customRule: '' }
  ], 'split'), input)
  expect(split.pageCount).toBe(3)
  expect((await PDFDocument.load(await readFile(split.outputs[0]))).getPageCount()).toBe(3)
  const before = await readFile(output)
  const cancelled = await page.evaluate(async path => {
    const stop = window.mootool.onPdfProgress(progress => {
      if (progress.jobId === 'cancel' && progress.phase === 'processing') void window.mootool.cancelPdfJob('cancel')
    })
    try {
      await window.mootool.mergePdfFiles(Array.from({ length: 20 }, () => ({ path, pages: '1-40' })), 'cancel')
      return false
    } catch { return true }
    finally { stop() }
  }, input)
  expect(cancelled).toBe(true)
  expect(await readFile(output)).toEqual(before)
  await page.locator('.tool-button').filter({ hasText: 'PDF' }).click()
  await page.getByRole('button', { name: '添加拆分任务', exact: true }).click()
  await expect(page.locator('.pdf-table tbody tr')).toHaveCount(1)
  await expect(page.locator('.pdf-table')).toContainText('source.pdf')
  await expect.poll(() => page.locator('.pdf-table-wrap').evaluate(element => element.getBoundingClientRect().height)).toBeGreaterThan(100)
})

test('crash recovery retains the view and detached window and restores saved drafts', async () => {
  await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
  await expect.poll(async () => (await page.evaluate(() => window.mootool.getToolWindowSnapshot())).tools.find(tool => tool.toolId === 'calculator')?.ready).toBe(true)
  const id = await app.evaluate(({ webContents }) => webContents.getAllWebContents().find(item => new URL(item.getURL()).searchParams.get('toolId') === 'calculator')!.id)
  await expect.poll(() => app.evaluate(async ({ webContents }, id) => {
    try { return await webContents.fromId(id)?.executeJavaScript(`Boolean(document.querySelector('#calculator-expression'))`) }
    catch { return false }
  }, id)).toBe(true)
  await app.evaluate(async ({ webContents }, id) => {
    const contents = webContents.fromId(id)!
    await contents.executeJavaScript(`(() => { const input = document.querySelector('#calculator-expression'); Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, '40 + 2'); input.dispatchEvent(new Event('input', { bubbles: true })); })()`)
  }, id)
  await expect.poll(() => page.evaluate(async () => JSON.parse(await window.mootool.getToolDraft('calculator') ?? '{}').expression)).toBe('40 + 2')
  await app.evaluate(async ({ webContents }, id) => {
    const contents = webContents.fromId(id)!
    const loaded = new Promise<void>(resolve => contents.once('did-finish-load', () => resolve()))
    contents.forcefullyCrashRenderer()
    await loaded
  }, id)
  await expect.poll(() => app.evaluate(async ({ webContents }, id) => {
    try { return await webContents.fromId(id)?.executeJavaScript(`document.querySelector('#calculator-expression')?.value`) }
    catch { return null }
  }, id)).toBe('40 + 2')
  await page.evaluate(() => window.mootool.detachToolWindow('calculator'))
  const before = await app.evaluate(({ BaseWindow, BrowserWindow }) => BaseWindow.getAllWindows().find(window => !(window instanceof BrowserWindow))!.id)
  // The second crash exercises the explicit retry path through the native error dialog.
  await app.evaluate(async ({ webContents, dialog }, id) => {
    dialog.showMessageBox = async () => ({ response: 0, checkboxChecked: false })
    const contents = webContents.fromId(id)!
    const loaded = new Promise<void>(resolve => contents.once('did-finish-load', () => resolve()))
    contents.forcefullyCrashRenderer()
    await loaded
  }, id)
  await expect.poll(() => app.evaluate(async ({ webContents }, id) => {
    try { return await webContents.fromId(id)?.executeJavaScript(`document.querySelector('#calculator-expression')?.value`) }
    catch { return null }
  }, id)).toBe('40 + 2')
  expect(await app.evaluate(({ BaseWindow, BrowserWindow }) => BaseWindow.getAllWindows().find(window => !(window instanceof BrowserWindow))!.id)).toBe(before)
  await page.evaluate(() => window.mootool.dockToolWindow('calculator'))
  await expect.poll(async () => (await page.evaluate(() => window.mootool.getToolWindowSnapshot())).tools.find(tool => tool.toolId === 'calculator')).toMatchObject({ ready: true, detached: false })
})

test('loads settings on demand and persists appearance changes', async () => {
  const network = await page.context().newCDPSession(page)
  const requested: string[] = []
  network.on('Network.requestWillBeSent', ({ request }) => requested.push(request.url))
  await network.send('Network.enable')
  await page.reload()
  await page.locator('.tool-button').first().waitFor()
  expect(requested.some(url => url.includes('/SettingsWindow-'))).toBe(false)
  await page.locator('.sidebar-footer .icon-ghost').click()
  await expect(page.locator('.settings-page')).toBeVisible()
  await expect.poll(() => requested.some(url => url.includes('/SettingsWindow-'))).toBe(true)
  const toggle = page.getByRole('switch', { name: '自动静默下载新版' })
  await toggle.click()
  await expect.poll(() => page.evaluate(() => window.mootool.getSettings())).toMatchObject({ general: { autoDownloadUpdates: false } })
})
