import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { seedOcrComponent } from './ocrComponentFixture'

let app: ElectronApplication
let page: Page
let directory: string
let fixture: string
let previous: { text: string; html: string; rtf: string; image: string }
test.skip(process.platform === 'darwin' && process.arch !== 'arm64' && process.env.MOOTOOL_OCR_BACKEND === 'pp-ocrv6-small', 'ONNX Runtime 1.30.0 has no Intel macOS binary.')
test.setTimeout(150000)
test.beforeEach(async ({}, testInfo) => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-ocr-ui-'))
  await seedOcrComponent(directory)
  fixture = `data:image/png;base64,${(await readFile('doc/ocr-evaluation/light-20.png')).toString('base64')}`
  const executablePath = process.env.MOOTOOL_OCR_EXECUTABLE
  app = await electron.launch({ executablePath, args: [...(executablePath ? [] : ['.']), `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: testInfo.title.includes('native tool') ? '1' : '0' } })
  page = await app.firstWindow(); page.setDefaultTimeout(5000)
  previous = await app.evaluate(({ clipboard }) => ({ text: clipboard.readText(), html: clipboard.readHTML(), rtf: clipboard.readRTF(), image: clipboard.readImage().isEmpty() ? '' : clipboard.readImage().toDataURL() }))
  await page.evaluate(() => window.mootool.updateSettings({ general: { language: 'zh-CN' } }))
  await page.evaluate(dataUrl => window.mootool.saveImageAsset({ name: 'Chinese.png', dataUrl }), fixture)
  await page.locator('.tool-button').filter({ hasText: '图片助手' }).click()
  if (testInfo.title.includes('native tool')) return
  await expect(page.locator('.image-list-item')).toHaveCount(1)
  await page.locator('.image-list-item').getByRole('button').click()
  await expect(page.locator('.image-main-toolbar').getByRole('button', { name: '识别文字', exact: true })).toBeEnabled({ timeout: 35000 })
})
test.afterEach(async () => {
  if (app) {
    if (previous) await app.evaluate(({ clipboard, nativeImage }, previous) => clipboard.write({ text: previous.text, html: previous.html, rtf: previous.rtf, ...(previous.image ? { image: nativeImage.createFromDataURL(previous.image) } : {}) }), previous)
    await app.close()
  }
  await rm(directory, { recursive: true, force: true })
})
const dialog = () => page.getByRole('dialog', { name: '识别文字', exact: true })
const editor = () => dialog().getByRole('textbox', { name: '识别结果', exact: true })
async function open() { await page.locator('.image-main-toolbar').getByRole('button', { name: '识别文字', exact: true }).click() }
async function start() { await dialog().getByRole('button', { name: '开始识别', exact: true }).click() }

test('recognizes Chinese locally, previews before copying, and saves edited text without modifying the image', async ({}, testInfo) => {
  const before = await page.evaluate(() => window.mootool.readImageAsset('Chinese.png'))
  await app.evaluate(({ clipboard }) => clipboard.writeText('OCR clipboard sentinel'))
  await open()
  await expect(editor()).toHaveValue('')
  const status = await page.evaluate(() => window.mootool.getOcrStatus())
  if (status.engine === 'apple-vision') await dialog().getByRole('combobox', { name: '识别语言' }).selectOption('zh-Hans')
  else { await expect(dialog().getByRole('combobox', { name: '识别语言' })).toHaveValue('auto'); await expect(dialog()).toContainText('简体/繁体/英文/日文') }
  await start()
  await expect(editor()).toHaveValue(/中文识别评估/, { timeout: 125000 })
  await expect(editor()).toHaveValue(/批量重命名/)
  await expect(dialog().locator('.image-ocr-metadata')).toContainText(status.engine === 'apple-vision' ? 'Vision' : 'PP-OCRv6 small')
  expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe('OCR clipboard sentinel')
  await page.screenshot({ path: testInfo.outputPath('ocr-light.png'), animations: 'disabled' })
  const corrected = '校对后的中文识别结果\n订单编号 001234'
  await editor().fill(corrected)
  await dialog().getByRole('button', { name: '复制文字', exact: true }).click()
  await expect.poll(() => app.evaluate(({ clipboard }) => clipboard.readText().replaceAll('\r\n', '\n'))).toBe(corrected)
  const output = join(directory, 'recognized.txt')
  await app.evaluate(({ dialog }, output) => { dialog.showSaveDialog = (async () => ({ canceled: false, filePath: output })) as typeof dialog.showSaveDialog }, output)
  await dialog().getByRole('button', { name: '保存文字', exact: true }).click()
  await expect.poll(async () => { try { return await readFile(output, 'utf8') } catch { return '' } }).toBe(corrected)
  expect((await page.evaluate(() => window.mootool.readImageAsset('Chinese.png'))).dataUrl).toBe(before.dataUrl)
  expect((await page.evaluate(() => window.mootool.listImageAssets())).length).toBe(1)
})

test('reports a blank image without enabling copy or export', async () => {
  await page.evaluate(async () => {
    const canvas = document.createElement('canvas'); canvas.width = 400; canvas.height = 200
    const context = canvas.getContext('2d')!; context.fillStyle = 'white'; context.fillRect(0, 0, 400, 200)
    await window.mootool.saveImageAsset({ name: 'blank.png', dataUrl: canvas.toDataURL() })
  })
  await page.reload()
  await page.locator('.tool-button').filter({ hasText: '图片助手' }).click()
  await page.locator('.image-list-item').filter({ hasText: 'blank.png' }).getByRole('button').click()
  await open(); await start()
  await expect(dialog().getByRole('status')).toContainText('未识别到文字', { timeout: 125000 })
  await expect(editor()).toHaveValue('')
  await expect(dialog().getByRole('button', { name: '复制文字', exact: true })).toBeDisabled()
  await expect(dialog().getByRole('button', { name: '保存文字', exact: true })).toBeDisabled()
})

test('cancels pending work and ignores late results after cancellation and closing', async () => {
  await app.evaluate(({ ipcMain }) => {
    const state = globalThis as typeof globalThis & { __lateOcr?: () => void; __ocrCount?: number; __ocrCancelled?: number }
    state.__ocrCount = 0; state.__ocrCancelled = 0
    ipcMain.removeHandler('ocr:recognize'); ipcMain.removeHandler('ocr:cancel')
    ipcMain.handle('ocr:recognize', (_event, request) => {
      const result = { requestId: request.requestId, sourceName: request.source.name, engine: 'apple-vision', language: request.language, width: 1000, height: 240, text: '当前识别结果', lines: [{ text: '当前识别结果', confidence: 1, box: { x: 0, y: 0, width: 100, height: 20 } }], durationMs: 1 }
      state.__ocrCount!++
      if (state.__ocrCount === 2) return result
      return new Promise(resolve => { state.__lateOcr = () => resolve({ ...result, text: '过期识别结果' }) })
    })
    ipcMain.handle('ocr:cancel', () => { state.__ocrCancelled!++; return true })
  })
  await open(); await start()
  await expect(editor()).toBeDisabled()
  await dialog().getByRole('button', { name: '取消', exact: true }).click()
  await expect(dialog().getByRole('status')).toContainText('取消')
  await start(); await expect(editor()).toHaveValue('当前识别结果')
  await app.evaluate(() => (globalThis as typeof globalThis & { __lateOcr?: () => void }).__lateOcr?.())
  await expect(editor()).toHaveValue('当前识别结果')
  await dialog().getByRole('button', { name: '重新识别', exact: true }).click()
  await dialog().getByRole('button', { name: '关闭', exact: true }).click()
  await expect(dialog()).toHaveCount(0)
  await expect.poll(() => app.evaluate(() => (globalThis as typeof globalThis & { __ocrCancelled?: number }).__ocrCancelled)).toBe(2)
  await app.evaluate(() => (globalThis as typeof globalThis & { __lateOcr?: () => void }).__lateOcr?.())
  await open(); await expect(editor()).toHaveValue('')
})

test('recognizes a selected screenshot without saving it to the image library', async () => {
  await app.evaluate(({ desktopCapturer, nativeImage, screen }, fixture) => {
    desktopCapturer.getSources = async () => [{ id: 'screen:test:0', name: 'OCR fixture', display_id: String(screen.getPrimaryDisplay().id), thumbnail: nativeImage.createFromDataURL(fixture), appIcon: null }]
  }, fixture)
  const overlayPromise = app.waitForEvent('window')
  await page.getByRole('button', { name: '截图识字', exact: true }).click()
  const overlay = await overlayPromise
  await overlay.waitForLoadState('domcontentloaded')
  const confirmation = overlay.evaluate(async () => {
    const image = await window.mootool.getScreenCaptureOverlay()
    if (!image) throw new Error('Capture overlay missing')
    await window.mootool.confirmScreenCapture({ x: 0, y: 0, width: image.width, height: image.height })
  }).catch(error => { if (!String(error).includes('closed')) throw error })
  await expect(dialog()).toContainText('截图选区')
  await confirmation
  await start(); await expect(editor()).toHaveValue(/中文识别评估/, { timeout: 125000 })
  expect((await page.evaluate(() => window.mootool.listImageAssets())).map(asset => asset.name)).toEqual(['Chinese.png'])
})

test('recognizes in a detached native tool view and fits a narrow window', async ({}, testInfo) => {
  await expect.poll(() => page.evaluate(() => window.mootool.getToolWindowState('image'))).toMatchObject({ ready: true })
  await page.evaluate(() => window.mootool.detachToolWindow('image'))
  const tool = (script: string) => app.evaluate(async ({ webContents }, script) => {
    const contents = webContents.getAllWebContents().find(item => new URL(item.getURL()).searchParams.get('toolId') === 'image')
    if (!contents) throw new Error('Image view missing')
    return contents.executeJavaScript(script)
  }, script)
  await expect.poll(() => tool('document.querySelectorAll(".image-list-item").length')).toBe(1)
  await tool('document.querySelector(".image-list-item button").click()')
  await expect.poll(() => tool('[...document.querySelectorAll(".image-main-toolbar button")].find(button => button.textContent === "识别文字")?.disabled'), { timeout: 35000 }).toBe(false)
  await tool('[...document.querySelectorAll(".image-main-toolbar button")].find(button => button.textContent === "识别文字").click()')
  await expect.poll(() => tool('Boolean(document.querySelector(".image-ocr-options"))')).toBe(true)
  await tool('document.querySelector(".dialog__footer .dialog-button--primary").click()')
  await expect.poll(() => tool('document.querySelector(".image-ocr-editor textarea")?.value'), { timeout: 125000 }).toContain('中文识别评估')
  await app.evaluate(({ BaseWindow, BrowserWindow }) => {
    const main = BrowserWindow.getAllWindows()[0]
    const detached = BaseWindow.getAllWindows().find(window => window.id !== main.id)!
    detached.setMinimumSize(600, 440); detached.setSize(620, 660); detached.focus()
  })
  await expect.poll(() => tool('innerWidth')).toBeLessThanOrEqual(620)
  await tool('Promise.all(document.querySelector(".dialog").getAnimations({ subtree: true }).map(animation => animation.finished))')
  await tool('new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))')
  if (process.platform === 'darwin') expect(await tool('document.querySelector(".image-main-toolbar button").getBoundingClientRect().left >= 90')).toBe(true)
  expect(await tool('document.querySelector(".dialog__body").scrollWidth <= document.querySelector(".dialog__body").clientWidth')).toBe(true)
  expect(await tool('[...document.querySelectorAll(".dialog__footer button")].every(button => { const box = button.getBoundingClientRect(); return box.left >= 0 && box.right <= innerWidth && box.bottom <= innerHeight })')).toBe(true)
  await tool('window.mootool.updateSettings({ appearance: { theme: "dark" } })')
  await expect.poll(() => tool('document.documentElement.dataset.theme')).toBe('dark')
  await tool('new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))')
  await tool('Promise.all(document.querySelector(".dialog").getAnimations({ subtree: true }).map(animation => animation.finished))')
  const capture = await app.evaluate(async ({ webContents }) => {
    const contents = webContents.getAllWebContents().find(item => new URL(item.getURL()).searchParams.get('toolId') === 'image')!
    return (await contents.capturePage()).toPNG().toString('base64')
  })
  await writeFile(testInfo.outputPath('ocr-detached-dark.png'), Buffer.from(capture, 'base64'))
})
