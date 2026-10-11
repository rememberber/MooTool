import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

let app: ElectronApplication
let page: Page
let directory: string
test.beforeEach(async ({}, testInfo) => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-resize-'))
  const native = testInfo.title.includes('native tool')
  app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: native ? '1' : '0' } })
  page = await app.firstWindow()
  page.setDefaultTimeout(5000)
  await page.evaluate(async () => {
    const canvas = document.createElement('canvas'); canvas.width = 400; canvas.height = 200
    const context = canvas.getContext('2d')!
    context.fillStyle = 'red'; context.fillRect(0, 0, 100, 200)
    context.fillStyle = '#00FF00'; context.fillRect(100, 0, 200, 200)
    context.fillStyle = 'blue'; context.fillRect(300, 0, 100, 200)
    await window.mootool.saveImageAsset({ name: 'landscape.png', dataUrl: canvas.toDataURL() })
    canvas.width = 100; canvas.height = 200
    canvas.getContext('2d')!.fillRect(0, 0, 100, 200)
    await window.mootool.saveImageAsset({ name: 'portrait.jpg', dataUrl: canvas.toDataURL() })
  })
  await page.locator('.tool-button').filter({ hasText: '图片助手' }).click()
  if (native) return
  await expect(page.locator('.image-list-item')).toHaveCount(2)
  // Select exactly the landscape; clicking its row also makes it current.
  await page.locator('.image-list-item').filter({ hasText: 'landscape.png' }).getByRole('button').click()
  await expect(page.locator('.image-canvas img')).toHaveAttribute('alt', 'landscape.png')
  await expect(page.getByRole('checkbox', { name: '选择图片 portrait.jpg', exact: true })).not.toBeChecked()
  await expect(page.getByRole('checkbox', { name: '选择图片 landscape.png', exact: true })).toBeChecked()
})
test.afterEach(async () => { await app?.close(); await rm(directory, { recursive: true, force: true }) })

const resizeDialog = () => page.getByRole('dialog', { name: '批量调整图片尺寸', exact: true })
async function openResize(width = '100', height = '100') {
  await page.getByRole('button', { name: '调整尺寸', exact: true }).click()
  const dialog = resizeDialog()
  await dialog.getByLabel('宽度（px）', { exact: true }).fill(width)
  await dialog.getByLabel('高度（px）', { exact: true }).fill(height)
  return dialog
}
async function processResize() {
  await resizeDialog().getByRole('button', { name: '开始处理', exact: true }).click()
  const progress = page.getByRole('dialog', { name: '图片处理进度' })
  await expect(progress).toContainText('失败 0')
  await expect(progress.getByRole('button', { name: '关闭', exact: true })).toBeEnabled()
  await progress.getByRole('button', { name: '关闭', exact: true }).click()
}
async function pixels(name: string) {
  return page.evaluate(async name => {
    const asset = await window.mootool.readImageAsset(name)
    const image = new Image(); image.src = asset.dataUrl; await image.decode()
    const canvas = document.createElement('canvas'); canvas.width = asset.width; canvas.height = asset.height
    const context = canvas.getContext('2d')!; context.drawImage(image, 0, 0)
    return { width: asset.width, height: asset.height, left: [...context.getImageData(1, 1, 1, 1).data], right: [...context.getImageData(asset.width - 2, 1, 1, 1).data] }
  }, name)
}

test('previews mixed orientations and auto dimensions, preserving JPEG and source images', async ({}, testInfo) => {
  await page.getByRole('checkbox', { name: '选择图片 portrait.jpg', exact: true }).check()
  const before = await page.evaluate(() => window.mootool.readImageAsset('landscape.png'))
  const dialog = await openResize('100', '100')
  await expect(dialog.locator('tbody tr')).toHaveCount(2)
  await expect(dialog.locator('tbody tr').filter({ hasText: 'landscape.png' })).toContainText('100 × 50')
  await expect(dialog.locator('tbody tr').filter({ hasText: 'portrait.jpg' })).toContainText('50 × 100')
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'light' } }))
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')
  await dialog.evaluate(element => Promise.all(element.getAnimations({ subtree: true }).map(animation => animation.finished)))
  await page.screenshot({ path: testInfo.outputPath('resize-preview-light.png') })
  expect((await page.evaluate(() => window.mootool.listImageAssets())).length).toBe(2)
  await dialog.getByLabel('高度（px）', { exact: true }).fill('')
  await expect(dialog.locator('tbody tr').filter({ hasText: 'portrait.jpg' })).toContainText('100 × 200')
  await dialog.getByLabel('高度（px）', { exact: true }).fill('100')
  await processResize()
  const assets = await page.evaluate(() => window.mootool.listImageAssets())
  expect(assets.find(asset => asset.name === 'landscape_resized.png')).toMatchObject({ width: 100, height: 50 })
  expect(assets.find(asset => asset.name === 'portrait_resized.jpg')).toMatchObject({ width: 50, height: 100 })
  expect((await page.evaluate(() => window.mootool.readImageAsset('landscape.png'))).dataUrl).toBe(before.dataUrl)
})

test('fills with a centered crop and stretches without cropping, keeping repeated outputs', async ({}, testInfo) => {
  let dialog = await openResize()
  await dialog.getByLabel('缩放模式', { exact: true }).selectOption('fill')
  await expect(dialog.locator('tbody')).toContainText('100 × 100')
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'dark' } }))
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
  await dialog.evaluate(element => Promise.all(element.getAnimations({ subtree: true }).map(animation => animation.finished)))
  await page.screenshot({ path: testInfo.outputPath('resize-fill-dark.png') })
  await processResize()
  const first = await pixels('landscape_resized.png')
  expect(first).toEqual({ width: 100, height: 100, left: [0, 255, 0, 255], right: [0, 255, 0, 255] })
  dialog = await openResize()
  await dialog.getByLabel('缩放模式', { exact: true }).selectOption('stretch')
  await processResize()
  expect(await pixels('landscape_resized-2.png')).toEqual({ width: 100, height: 100, left: [255, 0, 0, 255], right: [0, 0, 255, 255] })
  expect(await pixels('landscape_resized.png')).toEqual(first)
  expect(await pixels('portrait.jpg')).toMatchObject({ width: 100, height: 200 })
})

test('validates dimensions and pixel limits, and shrink-only never enlarges a fill', async () => {
  const dialog = await openResize('', '')
  const start = dialog.getByRole('button', { name: '开始处理', exact: true })
  await expect(start).toBeDisabled()
  await expect(dialog.getByRole('alert')).toContainText('有效尺寸')
  await dialog.getByLabel('宽度（px）', { exact: true }).fill('1.5')
  await expect(start).toBeDisabled()
  await dialog.getByLabel('宽度（px）', { exact: true }).fill('8000')
  await dialog.getByLabel('高度（px）', { exact: true }).fill('8000')
  await dialog.getByLabel('缩放模式', { exact: true }).selectOption('fill')
  await dialog.getByLabel('仅缩小，不放大', { exact: true }).uncheck()
  await expect(start).toBeDisabled()
  await expect(dialog.getByRole('alert')).toContainText('输出过大')
  await dialog.getByLabel('仅缩小，不放大', { exact: true }).check()
  await expect(dialog.locator('tbody')).toContainText('200 × 200')
  await expect(start).toBeEnabled()
  await processResize()
  expect(await pixels('landscape_resized.png')).toEqual({ width: 200, height: 200, left: [0, 255, 0, 255], right: [0, 255, 0, 255] })
})

test('overwrites only selected library copies and converts formats', async () => {
  const existing = await page.evaluate(async () => {
    const asset = await window.mootool.readImageAsset('portrait.jpg')
    await window.mootool.saveImageAsset({ name: 'landscape.jpg', dataUrl: asset.dataUrl })
    return window.mootool.readImageAsset('landscape.jpg')
  })
  const dialog = await openResize()
  await dialog.getByLabel('覆盖原图', { exact: true }).check()
  await dialog.getByLabel('输出格式', { exact: true }).selectOption('jpeg')
  await processResize()
  const assets = await page.evaluate(() => window.mootool.listImageAssets())
  expect(assets.map(asset => asset.name).sort()).toEqual(['landscape-2.jpg', 'landscape.jpg', 'portrait.jpg'])
  expect(assets.find(asset => asset.name === 'landscape-2.jpg')).toMatchObject({ width: 100, height: 50 })
  expect((await page.evaluate(() => window.mootool.readImageAsset('landscape.jpg'))).dataUrl).toBe(existing.dataUrl)
  expect(assets.find(asset => asset.name === 'portrait.jpg')).toMatchObject({ width: 100, height: 200 })
})

test('resizes through a detached native tool view with a narrow preview dialog', async ({}, testInfo) => {
  await expect.poll(() => page.evaluate(() => window.mootool.getToolWindowState('image'))).toMatchObject({ ready: true })
  await page.evaluate(() => window.mootool.detachToolWindow('image'))
  const tool = (script: string) => app.evaluate(async ({ webContents }, script) => {
    const contents = webContents.getAllWebContents().find(item => new URL(item.getURL()).searchParams.get('toolId') === 'image')
    if (!contents) throw new Error('Image view missing')
    return contents.executeJavaScript(script)
  }, script)
  await expect.poll(() => tool('document.querySelectorAll(".image-list-item").length')).toBe(2)
  await tool('[...document.querySelectorAll(".image-list-item button")].find(button => button.textContent.includes("landscape.png")).click()')
  await expect.poll(() => tool('document.querySelector(".image-canvas img")?.alt')).toBe('landscape.png')
  await tool('[...document.querySelectorAll(".image-main-toolbar button")].find(button => button.textContent === "调整尺寸").click()')
  await expect.poll(() => tool('Boolean(document.querySelector(".image-resize-options"))')).toBe(true)
  await tool(`(() => {
    for (const input of document.querySelectorAll('.image-resize-options input[type="number"]')) {
      Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, '100')
      input.dispatchEvent(new Event('input', { bubbles: true }))
    }
  })()`)
  await expect.poll(() => tool('document.querySelector(".image-resize-preview tbody")?.textContent')).toContain('100 × 50')
  await app.evaluate(({ BaseWindow, BrowserWindow }) => {
    const main = BrowserWindow.getAllWindows()[0]
    const detached = BaseWindow.getAllWindows().find(window => window.id !== main.id)!
    detached.setMinimumSize(600, 440); detached.setSize(620, 660); detached.focus()
  })
  await expect.poll(() => tool('innerWidth')).toBeLessThanOrEqual(620)
  await tool('Promise.all(document.querySelector(".dialog").getAnimations({ subtree: true }).map(animation => animation.finished))')
  await tool('new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))')
  expect(await tool('document.querySelector(".dialog").getBoundingClientRect().right <= innerWidth')).toBe(true)
  expect(await tool('document.querySelector(".dialog__footer").getBoundingClientRect().bottom <= innerHeight')).toBe(true)
  expect(await tool('document.querySelector(".dialog__body").scrollWidth <= document.querySelector(".dialog__body").clientWidth')).toBe(true)
  await tool('document.querySelector(".image-resize-preview").scrollIntoView({ block: "nearest" })')
  await expect.poll(() => tool(`(() => {
    const preview = document.querySelector('.image-resize-preview').getBoundingClientRect()
    const body = document.querySelector('.dialog__body').getBoundingClientRect()
    return preview.top >= body.top - 1 && preview.bottom <= body.bottom + 1
  })()`)).toBe(true)
  await tool('new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))')
  const capture = await app.evaluate(async ({ webContents }) => {
    const contents = webContents.getAllWebContents().find(item => new URL(item.getURL()).searchParams.get('toolId') === 'image')!
    return (await contents.capturePage()).toPNG().toString('base64')
  })
  await writeFile(testInfo.outputPath('resize-detached.png'), Buffer.from(capture, 'base64'))
  await tool('document.querySelector(".dialog__footer .dialog-button--primary").click()')
  await expect.poll(() => tool('document.querySelector(".image-batch-progress")?.textContent')).toContain('成功 1，失败 0')
  expect(await page.evaluate(async () => (await window.mootool.listImageAssets()).find(asset => asset.name === 'landscape_resized.png'))).toMatchObject({ width: 100, height: 50 })
})

test('exclusive output creation protects existing images even across concurrent requests', async () => {
  const before = await page.evaluate(() => window.mootool.readImageAsset('landscape.png'))
  const results = await page.evaluate(async () => {
    const asset = await window.mootool.readImageAsset('portrait.jpg')
    return Promise.all(Array.from({ length: 3 }, () => window.mootool.saveImageAsset({ name: 'landscape.png', dataUrl: asset.dataUrl, createOnly: true })))
  })
  expect(results.map(result => result.name).sort()).toEqual(['landscape-2.png', 'landscape-3.png', 'landscape-4.png'])
  expect((await page.evaluate(() => window.mootool.readImageAsset('landscape.png'))).dataUrl).toBe(before.dataUrl)
})
