import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, readFile, readdir, realpath, rm, unlink, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

let app: ElectronApplication
let page: Page
let directory: string
let userData: string
let paths: string[]

async function launch(native = false): Promise<void> {
  app = await electron.launch({ args: ['.', `--user-data-dir=${userData}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: native ? '1' : '0' } })
  page = await app.firstWindow()
  await page.waitForLoadState('domcontentloaded')
  await app.evaluate(({ dialog }, files) => {
    dialog.showOpenDialog = async () => ({ canceled: false, filePaths: files })
  }, paths)
  await page.getByRole('button', { name: '搜索', exact: true }).click()
  await page.locator('.command-palette__search input').fill('rename')
  await page.locator('.command-palette__search input').press('Enter')
}

test.beforeAll(async () => {
  directory = await realpath(await mkdtemp(join(tmpdir(), 'mootool-batch-rename-ui-')))
  userData = join(directory, 'user-data')
  paths = [join(directory, 'IMG_sun.JPG'), join(directory, 'IMG_moon.JPG')]
  await writeFile(paths[0], 'sun content'); await writeFile(paths[1], 'moon content')
  await launch()
})
test.afterAll(async () => { await app?.close(); await rm(directory, { recursive: true, force: true }) })

test('previews names without modifying files, invalidates changed rules, and applies/undoes the batch', async ({}, testInfo) => {
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'light' } }))
  const tool = page.locator('.batch-rename-tool')
  await expect(tool).toBeVisible()
  await tool.getByRole('button', { name: '选择文件', exact: true }).click()
  await tool.getByLabel('查找文本', { exact: true }).fill('IMG')
  await tool.getByLabel('替换为', { exact: true }).fill('photo')
  await tool.getByLabel('前缀', { exact: true }).fill('archive_')
  await tool.getByLabel('连续编号', { exact: true }).check()
  await tool.getByRole('button', { name: '预览重命名', exact: true }).click()
  await expect(tool.locator('.batch-rename-new-name')).toHaveText(['archive_photo_sun_001.JPG', 'archive_photo_moon_002.JPG'])
  expect(await readFile(paths[0], 'utf8')).toBe('sun content')
  expect((await readdir(directory)).some((name) => name.startsWith('archive_'))).toBe(false)
  await tool.getByLabel('前缀', { exact: true }).fill('export_')
  await expect(tool.getByRole('button', { name: '执行重命名', exact: true })).toBeDisabled()
  await tool.getByLabel('前缀', { exact: true }).fill('archive_')
  await tool.getByRole('button', { name: '预览重命名', exact: true }).click()
  await expect(tool.getByRole('button', { name: '执行重命名', exact: true })).toBeEnabled()
  await page.screenshot({ path: testInfo.outputPath('rename-preview-light.png') })
  await tool.getByRole('button', { name: '执行重命名', exact: true }).click()
  await expect(tool.getByRole('status')).toContainText('已重命名 2')
  expect(await readFile(join(directory, 'archive_photo_sun_001.JPG'), 'utf8')).toBe('sun content')
  await tool.getByRole('button', { name: '撤销最近一批', exact: true }).click()
  await expect(tool.getByRole('status')).toContainText('已恢复 2')
  expect(await readFile(paths[1], 'utf8')).toBe('moon content')
})

test('blocks name conflicts and a source replaced after preview without overwriting anything', async ({}, testInfo) => {
  const tool = page.locator('.batch-rename-tool')
  const foreign = join(directory, 'archive_photo_sun_001.JPG')
  await writeFile(foreign, 'foreign content')
  await tool.getByRole('button', { name: '预览重命名', exact: true }).click()
  await expect(tool.locator('.batch-rename-row--conflict')).toContainText('已有同名文件')
  await expect(tool.getByRole('button', { name: '执行重命名', exact: true })).toBeDisabled()
  expect(await readFile(foreign, 'utf8')).toBe('foreign content')
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'dark' } }))
  await page.screenshot({ path: testInfo.outputPath('rename-conflict-dark.png') })
  await unlink(foreign)
  await tool.getByRole('button', { name: '预览重命名', exact: true }).click()
  await expect(tool.getByRole('button', { name: '执行重命名', exact: true })).toBeEnabled()
  await unlink(paths[0]); await writeFile(paths[0], 'replacement content')
  await tool.getByRole('button', { name: '执行重命名', exact: true }).click()
  await expect(tool.getByRole('alert')).toContainText('被替换')
  expect(await readFile(paths[0], 'utf8')).toBe('replacement content')
  expect(await readFile(paths[1], 'utf8')).toBe('moon content')
})

test('retains undo across an application restart', async () => {
  let tool = page.locator('.batch-rename-tool')
  await tool.getByRole('button', { name: '选择文件', exact: true }).click()
  await tool.getByLabel('查找文本', { exact: true }).fill('')
  await tool.getByLabel('替换为', { exact: true }).fill('')
  await tool.getByLabel('前缀', { exact: true }).fill('renamed_')
  await tool.getByLabel('连续编号', { exact: true }).uncheck()
  await tool.getByRole('button', { name: '预览重命名', exact: true }).click()
  await expect(tool.getByRole('button', { name: '执行重命名', exact: true })).toBeEnabled()
  await tool.getByRole('button', { name: '执行重命名', exact: true }).click()
  await expect(tool.getByRole('status')).toContainText('已重命名 2')
  await app.close()
  await launch()
  tool = page.locator('.batch-rename-tool')
  await expect(tool.getByRole('button', { name: '撤销最近一批', exact: true })).toBeEnabled()
  await tool.getByRole('button', { name: '撤销最近一批', exact: true }).click()
  await expect(tool.getByRole('status')).toContainText('已恢复 2')
  expect(await readFile(paths[0], 'utf8')).toBe('replacement content')
})

test('selects, previews and renames through a detached native tool view', async ({}, testInfo) => {
  await app.close()
  await launch(true)
  await expect.poll(() => page.evaluate(() => window.mootool.getToolWindowState('batchRename'))).toMatchObject({ ready: true })
  await page.evaluate(() => window.mootool.detachToolWindow('batchRename'))
  const evaluateTool = (script: string) => app.evaluate(async ({ webContents }, script) => {
    const contents = webContents.getAllWebContents().find((item) => new URL(item.getURL()).searchParams.get('toolId') === 'batchRename')
    if (!contents) throw new Error('Rename tool view missing')
    return contents.executeJavaScript(script)
  }, script)
  await expect.poll(() => evaluateTool('Boolean(document.querySelector(".batch-rename-tool"))')).toBe(true)
  await evaluateTool('document.querySelector(".batch-rename-toolbar button").click()')
  await expect.poll(() => evaluateTool('document.querySelectorAll("tr[data-rename-id]").length')).toBe(2)
  await evaluateTool(`(() => {
    const input = [...document.querySelectorAll('.batch-rename-rules > label')].find(label => label.textContent === '前缀').querySelector('input')
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, 'native_')
    input.dispatchEvent(new Event('input', { bubbles: true }))
  })()`)
  // Rule changes invalidate the previous preview; run after React commits input.
  await evaluateTool('document.querySelector(".batch-rename-preview-toolbar .dialog-button").click()')
  await expect.poll(() => evaluateTool('[...document.querySelectorAll(".batch-rename-new-name")].map(row => row.textContent)')).toEqual(['native_IMG_sun.JPG', 'native_IMG_moon.JPG'])
  if (process.platform === 'darwin') expect(await evaluateTool('document.querySelector(".batch-rename-toolbar > strong").getBoundingClientRect().left >= 90')).toBe(true)
  await app.evaluate(({ BaseWindow, BrowserWindow }) => {
    const main = BrowserWindow.getAllWindows()[0]
    const window = BaseWindow.getAllWindows().find((window) => window.id !== main.id)!
    window.setMinimumSize(600, 440)
    window.setSize(760, 600)
    window.focus()
  })
  await evaluateTool('new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(() => resolve(true))))')
  expect(await evaluateTool('document.querySelector(".batch-rename-preview-toolbar .primary-command").getBoundingClientRect().bottom <= innerHeight')).toBe(true)
  const capture = await app.evaluate(async ({ webContents }) => {
    const contents = webContents.getAllWebContents().find((item) => new URL(item.getURL()).searchParams.get('toolId') === 'batchRename')!
    return (await contents.capturePage()).toPNG().toString('base64')
  })
  await writeFile(testInfo.outputPath('rename-detached.png'), Buffer.from(capture, 'base64'))
  await evaluateTool('document.querySelector(".batch-rename-preview-toolbar .primary-command").click()')
  await expect.poll(() => evaluateTool('document.querySelector(".batch-rename-notice")?.textContent ?? ""')).toContain('已重命名 2')
  expect(await readFile(join(directory, 'native_IMG_sun.JPG'), 'utf8')).toBe('replacement content')
  await evaluateTool('document.querySelectorAll(".batch-rename-toolbar button")[1].click()')
  await expect.poll(() => evaluateTool('document.querySelector(".batch-rename-notice")?.textContent ?? ""')).toContain('已恢复 2')
})
