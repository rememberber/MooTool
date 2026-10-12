import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { PDFDocument, PDFName, PDFString, rgb } from 'pdf-lib'

let app: ElectronApplication
let page: Page
let directory: string
let pdf: Uint8Array
test.beforeEach(async ({}, testInfo) => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-file-preview-'))
  const document = await PDFDocument.create()
  const descriptor = document.context.register(document.context.obj({ Type: 'FontDescriptor', FontName: 'STSong-Light', Flags: 6, FontBBox: [-250, -250, 1000, 1000], ItalicAngle: 0, Ascent: 880, Descent: -120, CapHeight: 880, StemV: 80 }))
  const descendant = document.context.register(document.context.obj({ Type: 'Font', Subtype: 'CIDFontType0', BaseFont: 'STSong-Light', CIDSystemInfo: { Registry: PDFString.of('Adobe'), Ordering: PDFString.of('GB1'), Supplement: 5 }, FontDescriptor: descriptor, DW: 1000 }))
  const chineseFont = document.context.register(document.context.obj({ Type: 'Font', Subtype: 'Type0', BaseFont: 'STSong-Light', Encoding: 'UniGB-UCS2-H', DescendantFonts: [descendant] }))
  for (let index = 0; index < 2; index++) {
    const sheet = document.addPage([600, 800])
    sheet.drawRectangle({ x: 50, y: 50, width: 500, height: 650, color: index === 0 ? rgb(.8, .2, .2) : rgb(.2, .2, .8) })
    sheet.drawText(`Preview page ${index + 1}`, { x: 60, y: 730, size: 24 })
    if (index === 0) {
      const text = [...'中文 PDF 预览'].map(character => character.charCodeAt(0).toString(16).padStart(4, '0')).join('')
      sheet.node.setFontDictionary(PDFName.of('CJK'), chineseFont)
      sheet.node.addContentStream(document.context.register(document.context.flateStream(`BT /CJK 24 Tf 60 680 Td <${text}> Tj ET`)))
    }
  }
  document.catalog.set(PDFName.of('OpenAction'), document.context.obj({ S: PDFName.of('JavaScript'), JS: PDFString.of('globalThis.__pdfExecuted = true;') }))
  pdf = await document.save()
  await writeFile(join(directory, 'two-pages.pdf'), pdf)
  await writeFile(join(directory, 'hello.txt'), '中文文本预览\n第二行')
  await writeFile(join(directory, 'data.json'), '{"id":12345678901234567890,"name":"中文","x":1,"x":2}')
  await writeFile(join(directory, 'bad.json'), '{"broken":}')
  await writeFile(join(directory, 'old.txt'), Buffer.from([214, 208, 206, 196]))
  await writeFile(join(directory, 'long.txt'), Array.from({ length: 500 }, (_, index) => `${index} 中文 alpha beta a.b A.B`).join('\n'))
  await writeFile(join(directory, 'unknown.zip'), 'not a zip')
  await writeFile(join(directory, 'photo.png'), await readFile('doc/ocr-evaluation/light-20.png'))
  const executablePath = process.env.MOOTOOL_PREVIEW_EXECUTABLE
  app = await electron.launch({ executablePath, args: [...(executablePath ? [] : ['.']), `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: testInfo.title.includes('native tool') ? '1' : '0' } })
  page = await app.firstWindow()
  await page.locator('.tool-button').filter({ hasText: '文件快速预览' }).click()
})
test.afterEach(async () => { if (app) await app.close(); await rm(directory, { recursive: true, force: true }) })
async function choose(names: string[]) { await page.getByTestId('preview-file-input').setInputFiles(names.map(name => join(directory, name))) }

test('previews a mixed batch, preserves JSON tokens, navigates with keys and leaves source files unchanged', async ({}, testInfo) => {
  await choose(['hello.txt', 'data.json', 'photo.png', 'unknown.zip', 'bad.json', 'old.txt'])
  await expect(page.locator('.file-preview-list button')).toHaveCount(6)
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('中文文本预览')
  await expect(page.locator('.file-preview-content .cm-content')).toHaveAttribute('contenteditable', 'false')
  await page.locator('.file-preview-tool').focus()
  await page.keyboard.press('ArrowRight')
  await expect(page.locator('.file-preview-metadata')).toContainText('data.json')
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('12345678901234567890')
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('"x": 1')
  await page.getByRole('checkbox', { name: '格式化 JSON' }).uncheck()
  await expect(page.locator('.file-preview-content .cm-line')).toHaveCount(1)
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(page.locator('.file-preview-image img')).toBeVisible()
  await expect.poll(() => page.locator('.file-preview-image img').evaluate((image: HTMLImageElement) => image.naturalWidth)).toBe(1000)
  await page.getByRole('button', { name: '放大', exact: true }).click()
  await expect(page.locator('.file-preview-options')).toContainText('125%')
  await page.screenshot({ path: testInfo.outputPath('preview-image-light.png'), animations: 'disabled' })
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(page.locator('.file-preview-detail')).toContainText('暂不支持此文件类型')
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(page.locator('.file-preview-text-status')).toContainText('JSON 格式无效')
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('{"broken":}')
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(page.locator('.file-preview-detail')).toContainText('无法按当前编码解码')
  await page.getByRole('combobox', { name: '编码', exact: true }).selectOption('gb18030')
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('中文')
  expect(await readFile(join(directory, 'data.json'), 'utf8')).toBe('{"id":12345678901234567890,"name":"中文","x":1,"x":2}')
  await page.getByRole('button', { name: '清空', exact: true }).click()
  await expect(page.locator('.file-preview-drop')).toBeVisible()
})

test('renders PDF pages locally, cancels stale rendering on switch, and handles corrupt files', async ({}, testInfo) => {
  await page.evaluate(() => {
    let live = 0
    const OriginalWorker = window.Worker
    window.Worker = class extends OriginalWorker {
      private terminated = false
      constructor(url: string | URL, options?: WorkerOptions) { super(url, options); live++ }
      override terminate() { if (!this.terminated) { this.terminated = true; live-- } super.terminate() }
    }
    ;(globalThis as typeof globalThis & { __previewWorkers?: () => number }).__previewWorkers = () => live
  })
  const workers = () => page.evaluate(() => (globalThis as typeof globalThis & { __previewWorkers?: () => number }).__previewWorkers?.())
  const requests: string[] = []
  page.on('request', request => { if (/^https?:/.test(request.url())) requests.push(request.url()) })
  await choose(['two-pages.pdf', 'hello.txt'])
  const canvas = page.locator('.file-preview-pdf canvas[data-rendered=true]')
  await expect(canvas).toHaveAttribute('data-page', '1')
  expect(await workers()).toBe(1)
  const sample = () => canvas.evaluate((canvas: HTMLCanvasElement) => {
    const color = canvas.getContext('2d')!.getImageData(Math.floor(canvas.width / 2), Math.floor(canvas.height / 2), 1, 1).data
    return [...color]
  })
  expect((await sample())[0]).toBeGreaterThan((await sample())[2])
  await page.screenshot({ path: testInfo.outputPath('preview-pdf-chinese.png'), animations: 'disabled' })
  await page.getByRole('button', { name: '下一页', exact: true }).click()
  await expect(canvas).toHaveAttribute('data-page', '2')
  expect((await sample())[2]).toBeGreaterThan((await sample())[0])
  await page.getByRole('button', { name: '放大', exact: true }).click()
  await expect(canvas).toHaveCount(1)
  await page.screenshot({ path: testInfo.outputPath('preview-pdf-light.png'), animations: 'disabled' })
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('中文文本预览')
  await expect.poll(workers).toBe(0)
  await page.getByRole('button', { name: '上一个文件', exact: true }).click()
  await expect(canvas).toHaveAttribute('data-page', '2')
  await expect(page.locator('.file-preview-options')).toContainText('125%')
  expect(await page.evaluate(() => (globalThis as typeof globalThis & { __pdfExecuted?: boolean }).__pdfExecuted)).toBeUndefined()
  expect(requests).toEqual([])
  await page.getByTestId('preview-file-input').setInputFiles({ name: 'damaged.pdf', mimeType: 'application/pdf', buffer: Buffer.from('broken pdf') })
  await expect(page.locator('.file-preview-detail')).toContainText('无法显示 PDF')
  await expect.poll(workers).toBe(0)
  await page.evaluate(() => {
    const original = File.prototype.arrayBuffer
    const state = globalThis as typeof globalThis & { __releasePdf?: () => void }
    File.prototype.arrayBuffer = function () {
      const bytes = original.call(this)
      return this.name.endsWith('.pdf') ? bytes.then(bytes => new Promise<ArrayBuffer>(resolve => {
        state.__releasePdf = () => { File.prototype.arrayBuffer = original; resolve(bytes) }
      })) : bytes
    }
  })
  await choose(['two-pages.pdf', 'hello.txt'])
  await expect.poll(() => page.evaluate(() => Boolean((globalThis as typeof globalThis & { __releasePdf?: () => void }).__releasePdf))).toBe(true)
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect.poll(workers).toBe(0)
  await page.evaluate(() => (globalThis as typeof globalThis & { __releasePdf?: () => void }).__releasePdf?.())
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('中文文本预览')
  await expect(page.locator('.file-preview-pdf canvas')).toHaveCount(0)
  expect(new Uint8Array(await readFile(join(directory, 'two-pages.pdf')))).toEqual(pdf)
})

test('handles file drops over the editor, rejects oversized batches, and keeps HTML as source', async () => {
  await choose(['hello.txt'])
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('中文文本预览')
  await page.evaluate(() => {
    const data = new DataTransfer()
    data.items.add(new File(['<script>globalThis.__htmlExecuted=true</script>'], 'index.html', { type: 'text/html' }))
    data.items.add(new File([new Uint8Array([97, 0, 98])], 'binary.txt'))
    document.querySelector('.cm-content')!.dispatchEvent(new DragEvent('drop', { dataTransfer: data, bubbles: true, cancelable: true }))
  })
  await expect(page.locator('.file-preview-list button')).toHaveCount(2)
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('<script>')
  expect(await page.evaluate(() => (globalThis as typeof globalThis & { __htmlExecuted?: boolean }).__htmlExecuted)).toBeUndefined()
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(page.locator('.file-preview-detail')).toContainText('二进制内容')
  await page.evaluate(() => {
    const data = new DataTransfer()
    for (let i = 0; i < 101; i++) data.items.add(new File(['test'], `${i}.txt`))
    document.querySelector('.file-preview-tool')!.dispatchEvent(new DragEvent('drop', { dataTransfer: data, bubbles: true, cancelable: true }))
  })
  await expect(page.getByRole('alert')).toContainText('最多 100 个文件')
  await expect(page.locator('.file-preview-list button')).toHaveCount(2)
  await page.getByTestId('preview-file-input').setInputFiles({ name: 'large.txt', mimeType: 'text/plain', buffer: Buffer.alloc(2 * 1024 ** 2 + 1, 97) })
  await expect(page.locator('.file-preview-detail')).toContainText('超过此类型的预览上限')
  await choose(['hello.txt'])
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('中文文本预览')
})

test('remembers each file view, finds text without editing, and resets state for a new batch', async ({}, testInfo) => {
  await choose(['old.txt', 'data.json', 'photo.png', 'two-pages.pdf', 'long.txt'])
  await page.getByRole('combobox', { name: '编码', exact: true }).selectOption('gb18030')
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('中文')
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await page.getByRole('checkbox', { name: '格式化 JSON' }).uncheck()
  await page.getByRole('checkbox', { name: '自动换行' }).uncheck()
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(page.locator('.file-preview-image img')).toBeVisible()
  await page.getByRole('button', { name: '放大', exact: true }).click()
  await expect(page.locator('.file-preview-options')).toContainText('125%')
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(page.locator('canvas[data-rendered=true]')).toHaveAttribute('data-page', '1')
  await page.getByRole('button', { name: '下一页', exact: true }).click()
  await expect(page.locator('canvas[data-rendered=true]')).toHaveAttribute('data-page', '2')
  await page.getByRole('button', { name: '放大', exact: true }).click()
  await page.getByRole('button', { name: '上一个文件', exact: true }).click()
  await expect(page.locator('.file-preview-options')).toContainText('125%')
  await page.getByRole('button', { name: '上一个文件', exact: true }).click()
  await expect(page.getByRole('checkbox', { name: '格式化 JSON' })).not.toBeChecked()
  await expect(page.getByRole('checkbox', { name: '自动换行' })).not.toBeChecked()
  await page.getByRole('button', { name: '上一个文件', exact: true }).click()
  await expect(page.getByRole('combobox', { name: '编码', exact: true })).toHaveValue('gb18030')
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('中文')
  await page.locator('.file-preview-list button').filter({ hasText: 'two-pages.pdf' }).click()
  await expect(page.locator('canvas[data-rendered=true]')).toHaveAttribute('data-page', '2')
  await expect(page.locator('.file-preview-options')).toContainText('125%')
  await page.locator('.file-preview-list button').filter({ hasText: 'long.txt' }).click()
  await expect(page.getByRole('textbox', { name: '文件内容预览' })).toContainText('中文')
  await page.locator('.file-preview-tool').focus()
  await page.keyboard.press(process.platform === 'darwin' ? 'Meta+f' : 'Control+f')
  const bar = page.locator('.file-preview-find')
  await expect(bar).toBeVisible()
  await bar.getByRole('textbox', { name: '查找', exact: true }).fill('a.b')
  await expect(bar.locator('.find-replace-bar__count')).toContainText('1000')
  await expect(bar.getByRole('checkbox', { name: '使用正则' })).toHaveCount(0)
  await bar.getByRole('checkbox', { name: '区分大小写' }).check()
  await expect(bar.locator('.find-replace-bar__count')).toContainText('500')
  await bar.getByRole('textbox', { name: '查找', exact: true }).press('Enter')
  await expect.poll(() => page.locator('.cm-searchMatch').count()).toBeGreaterThan(0)
  await page.locator('.file-preview-content .cm-scroller').evaluate(element => { element.scrollTop = 3500; element.dispatchEvent(new Event('scroll')) })
  await expect.poll(() => page.locator('.file-preview-content .cm-scroller').evaluate(element => element.scrollTop)).toBeGreaterThan(3000)
  await page.locator('.file-preview-list button').filter({ hasText: 'old.txt' }).click()
  await page.locator('.file-preview-list button').filter({ hasText: 'long.txt' }).click()
  await expect(bar.getByRole('textbox', { name: '查找', exact: true })).toHaveValue('a.b')
  await expect(bar.getByRole('checkbox', { name: '区分大小写' })).toBeChecked()
  await expect.poll(() => page.locator('.file-preview-content .cm-scroller').evaluate(element => element.scrollTop)).toBeGreaterThan(3000)
  await page.screenshot({ path: testInfo.outputPath('preview-find-light.png'), animations: 'disabled' })
  await bar.getByRole('textbox', { name: '查找', exact: true }).press('Escape')
  await expect(bar).toHaveCount(0)
  expect(await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].isVisible())).toBe(true)
  await choose(['old.txt', 'long.txt', 'data.json'])
  await expect(page.getByRole('combobox', { name: '编码', exact: true })).toHaveValue('auto')
  await expect(page.locator('.file-preview-detail')).toContainText('无法按当前编码解码')
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(bar).toHaveCount(0)
  await page.getByRole('button', { name: '查找', exact: true }).click()
  await expect(bar.getByRole('textbox', { name: '查找', exact: true })).toHaveValue('')
  await page.getByRole('button', { name: '下一个文件', exact: true }).click()
  await expect(page.getByRole('checkbox', { name: '格式化 JSON' })).toBeChecked()
  await expect(page.getByRole('checkbox', { name: '自动换行' })).toBeChecked()
  expect(await readFile(join(directory, 'long.txt'), 'utf8')).toContain('0 中文 alpha beta a.b A.B')
})

test('keeps regex and replacement available in the existing JSON find bar', async () => {
  await page.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  const editor = page.locator('.json-editor .cm-content')
  await editor.fill('{"text":"alpha ALPHA alphabet"}')
  await page.locator('.editor-toolbar').getByRole('button', { name: '查找', exact: true }).click()
  const bar = page.locator('.json-findbar')
  await expect(bar.getByRole('checkbox', { name: '使用正则' })).toBeVisible()
  await bar.getByRole('checkbox', { name: '使用正则' }).check()
  await bar.getByRole('textbox', { name: '查找', exact: true }).fill('alpha|ALPHA')
  await expect(page.locator('.json-editor .cm-searchMatch')).toHaveCount(3)
  await expect(bar.getByRole('button', { name: '替换全部', exact: true })).toBeVisible()
})

test('previews PDF in a detached native tool view and fits a narrow dark window', async ({}, testInfo) => {
  const tool = (script: string) => app.evaluate(async ({ webContents }, script) => {
    const contents = webContents.getAllWebContents().find(item => new URL(item.getURL()).searchParams.get('toolId') === 'filePreview')
    if (!contents) throw new Error('Preview view missing')
    return contents.executeJavaScript(script)
  }, script)
  await expect.poll(() => page.evaluate(() => window.mootool.getToolWindowState('filePreview'))).toMatchObject({ ready: true })
  await page.evaluate(() => window.mootool.detachToolWindow('filePreview'))
  await expect.poll(() => tool('Boolean(document.querySelector(".file-preview-tool"))')).toBe(true)
  const base64 = Buffer.from(pdf).toString('base64')
  await tool(`(() => { const data = new DataTransfer(); data.items.add(new File([Uint8Array.from(atob('${base64}'), c => c.charCodeAt(0))], 'native.pdf', {type:'application/pdf'})); data.items.add(new File(['中文预览'], 'native.txt')); document.querySelector('.file-preview-tool').dispatchEvent(new DragEvent('drop', {dataTransfer:data,bubbles:true,cancelable:true})); })()`)
  await expect.poll(() => tool('document.querySelectorAll("canvas[data-rendered=true]").length')).toBe(1)
  await app.evaluate(({ BaseWindow, BrowserWindow }) => {
    const main = BrowserWindow.getAllWindows()[0]
    const detached = BaseWindow.getAllWindows().find(window => window.id !== main.id)!
    detached.setMinimumSize(600, 440); detached.setSize(620, 660); detached.focus()
  })
  await tool('window.mootool.updateSettings({ appearance: { theme: "dark" } })')
  await expect.poll(() => tool('document.documentElement.dataset.theme')).toBe('dark')
  await expect.poll(() => tool('document.querySelectorAll("canvas[data-rendered=true]").length')).toBe(1)
  expect(await tool('document.querySelector(".file-preview-toolbar").scrollWidth <= document.querySelector(".file-preview-toolbar").clientWidth')).toBe(true)
  expect(await tool('document.querySelector(".file-preview-options").scrollWidth <= document.querySelector(".file-preview-options").clientWidth')).toBe(true)
  if (process.platform === 'darwin') expect(await tool('document.querySelector(".file-preview-toolbar button").getBoundingClientRect().left >= 90')).toBe(true)
  await tool('Promise.all(document.getAnimations().map(animation => animation.finished))')
  const capture = await app.evaluate(async ({ webContents }) => {
    const contents = webContents.getAllWebContents().find(item => new URL(item.getURL()).searchParams.get('toolId') === 'filePreview')!
    return (await contents.capturePage()).toPNG().toString('base64')
  })
  await writeFile(testInfo.outputPath('preview-detached-dark.png'), Buffer.from(capture, 'base64'))
  await tool('document.querySelectorAll(".file-preview-list button")[1].click()')
  await expect.poll(() => tool('document.querySelector(".file-preview-content .cm-content")?.textContent')).toContain('中文预览')
  await tool('document.querySelector(".file-preview-tool").dispatchEvent(new KeyboardEvent("keydown", { key: "f", ctrlKey: true, bubbles: true, cancelable: true }))')
  await expect.poll(() => tool('document.querySelectorAll(".file-preview-find").length')).toBe(1)
  expect(await tool('document.querySelector(".file-preview-find .find-replace-bar__row").scrollWidth <= document.querySelector(".file-preview-find .find-replace-bar__row").clientWidth')).toBe(true)
  await tool('new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))')
  const findCapture = await app.evaluate(async ({ webContents }) => {
    const contents = webContents.getAllWebContents().find(item => new URL(item.getURL()).searchParams.get('toolId') === 'filePreview')!
    return (await contents.capturePage()).toPNG().toString('base64')
  })
  await writeFile(testInfo.outputPath('preview-find-detached-dark.png'), Buffer.from(findCapture, 'base64'))
  await tool('window.mootool.dockToolWindow("filePreview")')
  expect(await tool('document.querySelector(".file-preview-metadata").textContent')).toContain('native.txt')
})
