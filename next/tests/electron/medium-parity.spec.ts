import { _electron as electron, expect, test } from '@playwright/test'
import { mkdtemp, mkdir, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createServer } from 'node:http'

const launch = (directory: string) => electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })

test('calculator drafts survive a real restart and migrated regex drafts hydrate', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-drafts-'))
  let app = await launch(directory)
  try {
    let page = await app.firstWindow()
    await page.evaluate(() => window.mootool.saveToolDraft('regex', JSON.stringify({ pattern: '\\Qlegacy.text\\E', source: 'legacy.text', engine: 'java' })))
    await page.locator('.tool-button').filter({ hasText: '正则' }).click()
    await expect(page.locator('#regex-expression')).toHaveValue('\\Qlegacy.text\\E')
    await expect(page.getByRole('textbox', { name: '待匹配文本', exact: true })).toHaveText('legacy.text')
    await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
    await page.locator('#calculator-expression').fill('7*8')
    await page.locator('#calculator-expression').press('Enter')
    await page.locator('#calculator-hex').fill('abcd')
    await expect.poll(() => page.evaluate(() => window.mootool.getToolDraft('calculator'))).toContain('abcd')
    await app.close()
    app = await launch(directory)
    page = await app.firstWindow()
    await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
    await expect(page.locator('#calculator-expression')).toHaveValue('7*8')
    await expect(page.locator('#calculator-hex')).toHaveValue('abcd')
    await expect(page.locator('.calculator-result')).toHaveText('56')
    await expect(page.locator('.calculator-log')).toContainText('7*8 = 56')
  } finally { await app.close(); await rm(directory, { recursive: true, force: true }) }
})

test('color favorites can be edited, reordered and deleted in batches', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-colors-'))
  const app = await launch(directory)
  try {
    const page = await app.firstWindow()
    page.setDefaultTimeout(5000)
    await page.evaluate(async () => {
      const folder = (await window.mootool.listFavoriteFolders('color'))[0]
      await window.mootool.saveFavorite({ kind: 'color', folderId: folder.id, name: 'Red', value: '#FF0000' })
      await window.mootool.saveFavorite({ kind: 'color', folderId: folder.id, name: 'Blue', value: '#0000FF' })
    })
    await page.locator('.tool-button').filter({ hasText: '调色板' }).click()
    await page.getByRole('button', { name: '收藏夹', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: '收藏夹', exact: true })
    await expect(dialog.locator('input[type="checkbox"]')).toHaveCount(0)
    const red = dialog.locator('.color-collection-card').filter({ hasText: 'Red' })
    await expect(red.locator('.color-collection-swatch')).toHaveCSS('background-color', 'rgb(255, 0, 0)')
    await dialog.evaluate(element => Promise.all(element.getAnimations({ subtree: true }).map(animation => animation.finished)))
    const beforeMenu = await dialog.evaluate(element => ({ rect: element.getBoundingClientRect().toJSON(), scroll: element.querySelector('.dialog__body')!.scrollTop }))
    const swatchBounds = await red.locator('.color-collection-swatch').boundingBox()
    await red.locator('.color-collection-swatch').click({ button: 'right', position: { x: 12, y: 12 } })
    const menu = page.getByRole('menu')
    await expect(menu).toBeVisible()
    expect(await menu.evaluate(element => element.parentElement === document.body)).toBe(true)
    const menuBounds = await menu.boundingBox()
    expect(Math.abs(menuBounds!.x - swatchBounds!.x - 12)).toBeLessThan(2)
    expect(Math.abs(menuBounds!.y - swatchBounds!.y - 12)).toBeLessThan(2)
    expect(await dialog.evaluate(element => ({ rect: element.getBoundingClientRect().toJSON(), scroll: element.querySelector('.dialog__body')!.scrollTop }))).toEqual(beforeMenu)
    await page.screenshot({ path: 'test-results/color-favorites-context-menu.png' })
    await page.getByRole('menuitem', { name: '编辑颜色', exact: true }).click()
    const edit = page.getByRole('dialog', { name: '编辑颜色', exact: true })
    await edit.getByLabel('名称', { exact: true }).fill('Green')
    await edit.getByLabel('HEX / RGB', { exact: true }).fill('#00FF00')
    await edit.getByRole('button', { name: '保存', exact: true }).click()
    const green = dialog.locator('.color-collection-card').filter({ hasText: 'Green' })
    await expect(green.locator('.color-collection-swatch')).toHaveCSS('background-color', 'rgb(0, 255, 0)')
    await dialog.locator('.color-collection-card').last().click({ button: 'right' })
    const lastName = await dialog.locator('.color-collection-card').last().locator('strong').textContent()
    await page.getByRole('menuitem', { name: '上移', exact: true }).click()
    await expect(dialog.locator('.color-collection-card').first()).toContainText(lastName!)
    await page.screenshot({ path: 'test-results/color-favorites-clean.png' })
    await dialog.getByRole('button', { name: '选择', exact: true }).click()
    await dialog.getByRole('button', { name: '全选', exact: true }).click()
    await expect(dialog.locator('.color-collection-card--selected')).toHaveCount(2)
    await dialog.getByRole('button', { name: '删除', exact: true }).click()
    await page.getByRole('dialog').last().getByRole('button', { name: '是', exact: true }).click()
    await expect(dialog.locator('.color-collection-card')).toHaveCount(0)
  } finally { await app.close(); await rm(directory, { recursive: true, force: true }) }
})

test('HTTP send opens an independent response snapshot with headers and cookies', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-response-'))
  const server = createServer((_request, response) => { response.setHeader('content-type', 'application/json'); response.setHeader('set-cookie', 'sample=1'); response.end('{"snapshot":true}') })
  await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve))
  const address = server.address() as { port: number }
  const app = await launch(directory)
  try {
    const page = await app.firstWindow()
    page.setDefaultTimeout(5000)
    await page.locator('.tool-button').filter({ hasText: 'HTTP 请求' }).click()
    await page.getByTestId('http-url').fill(`http://127.0.0.1:${address.port}`)
    await expect.poll(() => page.getByTestId('http-send').evaluate(element => element.getBoundingClientRect().bottom <= element.closest('.http-url-bar')!.getBoundingClientRect().bottom)).toBe(true)
    const opened = app.waitForEvent('window')
    await page.getByRole('button', { name: '发送并打开响应窗口', exact: true }).click()
    const response = await opened
    await expect(response.locator('.text-code-editor')).toContainText(/"snapshot":\s*true/)
    await response.getByRole('tab', { name: '响应 Headers', exact: true }).click()
    await expect(response.locator('.text-code-editor')).toContainText('application/json')
    await response.getByRole('tab', { name: '响应 Cookies', exact: true }).click()
    await expect(response.locator('.text-code-editor')).toContainText('sample')
    await page.getByTestId('http-url').fill('http://different.invalid')
    await expect(response.locator('header')).toContainText(`127.0.0.1:${address.port}`)
    await response.screenshot({ path: 'test-results/http-response-window.png' })
    await response.close()
    await expect(page.getByTestId('http-url')).toBeVisible()
  } finally { await app.close(); await new Promise<void>(resolve => server.close(() => resolve())); await rm(directory, { recursive: true, force: true }) }
})

test('SVG worker reports per-file failures, supports cancellation and compression reports progress', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-image-batch-'))
  const output = join(directory, 'output')
  await mkdir(output)
  const app = await launch(directory)
  try {
    const page = await app.firstWindow()
    page.setDefaultTimeout(5000)
    await page.evaluate(async () => {
      const canvas = document.createElement('canvas'); canvas.width = 128; canvas.height = 128
      const context = canvas.getContext('2d')!
      context.fillStyle = 'red'; context.fillRect(0, 0, 64, 128)
      context.fillStyle = 'blue'; context.fillRect(64, 0, 64, 128)
      for (const name of ['a.png', 'b.png']) await window.mootool.saveImageAsset({ name, dataUrl: canvas.toDataURL() })
    })
    await app.evaluate(({ dialog }, output) => { dialog.showOpenDialog = (async () => ({ canceled: false, filePaths: [output] })) as typeof dialog.showOpenDialog }, output)
    const result = await page.evaluate(() => window.mootool.vectorizeImageAssets(['a.png', 'missing.png', 'b.png'], { preset: 'poster', colorCount: 4, detail: 'medium', filterSpeckle: 0 }, 'test'))
    expect(result?.progress).toMatchObject({ succeeded: 2, completed: 3, failures: [{ name: 'missing.png', message: 'Unable to read image: missing.png' }] })
    expect(await readFile(result!.files[0], 'utf8')).toContain('<path')
    const cancelled = await page.evaluate(async () => {
      const stop = window.mootool.onImageBatchProgress(value => { if (value.jobId === 'cancel-test' && value.current) void window.mootool.cancelImageBatch() })
      try { return await window.mootool.vectorizeImageAssets(['a.png', 'b.png'], { preset: 'photo', colorCount: 32, detail: 'high', filterSpeckle: 0 }, 'cancel-test') }
      finally { stop() }
    })
    expect(cancelled?.progress.cancelled).toBe(true)
    expect(cancelled?.files.length).toBeLessThan(2)
    await page.locator('.tool-button').filter({ hasText: '图片助手' }).click()
    await page.locator('.image-list input[type="checkbox"]').nth(1).check()
    await page.getByRole('button', { name: '压缩', exact: true }).click()
    await page.getByRole('button', { name: '开始处理', exact: true }).click()
    const progress = page.getByRole('dialog', { name: '图片处理进度' })
    await expect(progress).toContainText('成功 2，失败 0')
    await expect(progress.getByRole('button', { name: '关闭', exact: true })).toBeEnabled()
  } finally { await app.close(); await rm(directory, { recursive: true, force: true }) }
})
