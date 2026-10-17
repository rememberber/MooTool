import { _electron as electron, expect, test, type ElectronApplication } from '@playwright/test'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join, resolve } from 'node:path'

test('download prompt requires a click, allows retry and offers restart after completion', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-component-prompt-'))
  const executablePath = process.env.MOOTOOL_OCR_EXECUTABLE
  const app = await electron.launch({ executablePath, args: [...(executablePath ? [] : ['.']), `--user-data-dir=${directory}`], env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })
  try {
    const page = await app.firstWindow()
    await page.evaluate(() => window.mootool.updateSettings({ general: { language: 'zh-CN' } }))
    await app.evaluate(({ ipcMain }) => {
      let status = 'missing', attempts = 0
      ipcMain.removeHandler('ocr:status'); ipcMain.removeHandler('ocr:download'); ipcMain.removeHandler('ocr:restart')
      ipcMain.handle('ocr:status', () => ({ available: false, engine: null, languages: [], reason: 'component', component: { status, percent: 50, total: 50 * 1024 * 1024 } }))
      ipcMain.handle('ocr:download', async () => {
        attempts++; status = 'downloading'
        await new Promise(resolve => setTimeout(resolve, 150))
        status = attempts === 1 ? 'error' : 'restart'
        return { status, percent: 100, total: 50 * 1024 * 1024 }
      })
      ipcMain.handle('ocr:restart', () => { status = 'installed'; return true })
    })
    await page.locator('.tool-button').filter({ hasText: '图片助手' }).click()
    await page.getByRole('button', { name: '截图识字', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: '下载文字识别组件' })
    await expect(dialog).toContainText('50.0 MiB')
    expect((await page.evaluate(() => window.mootool.getOcrStatus())).component?.status).toBe('missing')
    await dialog.getByRole('button', { name: '下载组件', exact: true }).click()
    await expect(dialog.getByRole('alert')).toContainText('失败')
    await dialog.getByRole('button', { name: '下载组件', exact: true }).click()
    await expect(dialog.getByRole('button', { name: '立即重启', exact: true })).toBeEnabled()
    await dialog.getByRole('button', { name: '稍后重启', exact: true }).click()
    await page.getByRole('button', { name: '截图识字', exact: true }).click()
    await expect(dialog.getByRole('button', { name: '立即重启', exact: true })).toBeEnabled()
    await dialog.getByRole('button', { name: '立即重启', exact: true }).click()
    expect((await page.evaluate(() => window.mootool.getOcrStatus())).component?.status).toBe('installed')
  } finally { await app.close(); await rm(directory, { recursive: true, force: true }) }
})

test('offers OCR download on first use and reuses the verified component after restarting offline', async () => {
  test.skip(process.platform === 'darwin' || !process.env.MOOTOOL_OCR_EXECUTABLE, 'Requires a packaged Windows/Linux app.')
  test.setTimeout(180000)
  const directory = await mkdtemp(join(tmpdir(), 'mootool-component-ui-'))
  let app: ElectronApplication | undefined
  const launch = () => electron.launch({ executablePath: process.env.MOOTOOL_OCR_EXECUTABLE, args: [`--user-data-dir=${directory}`], env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })
  try {
    app = await launch()
    const page = await app.firstWindow()
    await page.evaluate(() => window.mootool.updateSettings({ general: { language: 'zh-CN' } }))
    expect(await page.evaluate(() => window.mootool.getOcrStatus())).toMatchObject({ available: false, component: { status: 'missing' } })
    const target = `${process.platform}-${process.arch}`
    const metadata = JSON.parse(await readFile(resolve('out/optional-components', `${target}.json`), 'utf8'))
    // Use the exact release asset, with no dependency on a published release or network.
    await app.evaluate(async (_electron, path) => {
      // evaluate() runs through eval, which cannot dynamically import modules.
      const payload = process.getBuiltinModule('fs').readFileSync(path)
      const original = globalThis.fetch
      globalThis.fetch = (async (input, init) => String(input).endsWith('.bin.gz') ? new Response(payload) : original(input, init)) as typeof fetch
    }, resolve('dist/optional-components', metadata.download.fileName))
    await page.locator('.tool-button').filter({ hasText: '图片助手' }).click()
    await page.getByRole('button', { name: '截图识字', exact: true }).click()
    const dialog = page.getByRole('dialog', { name: '下载文字识别组件' })
    await expect(dialog).toContainText('首次使用需下载')
    await dialog.getByRole('button', { name: '下载组件', exact: true }).click()
    await expect(dialog.getByRole('button', { name: '立即重启', exact: true })).toBeEnabled({ timeout: 120000 })
    await expect(dialog).toContainText('校验完成')
    await dialog.getByRole('button', { name: '稍后重启', exact: true }).click()
    await app.close(); app = await launch()
    const restarted = await app.firstWindow()
    await app.evaluate(() => { globalThis.fetch = async () => { throw new Error('Offline test') } })
    expect(await restarted.evaluate(() => window.mootool.getOcrStatus())).toMatchObject({ available: true, component: { status: 'installed' } })
    const dataUrl = `data:image/png;base64,${(await readFile('doc/ocr-evaluation/light-20.png')).toString('base64')}`
    const result = await restarted.evaluate(dataUrl => window.mootool.recognizeImageText({ requestId: 'component-offline', language: 'auto', source: { name: 'fixture.png', dataUrl } }), dataUrl)
    expect(result.text).toContain('中文识别评估')
  } finally { await app?.close(); await rm(directory, { recursive: true, force: true }) }
})
