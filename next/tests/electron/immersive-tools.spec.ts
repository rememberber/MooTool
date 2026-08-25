import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

let electronApp: ElectronApplication
let mainPage: Page
let userDataDirectory: string

test.beforeAll(async () => {
  userDataDirectory = await mkdtemp(join(tmpdir(), 'mootool-immersive-e2e-'))
  electronApp = await electron.launch({
    args: ['.', `--user-data-dir=${userDataDirectory}`],
    cwd: process.cwd(),
    env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' }
  })
  mainPage = await electronApp.firstWindow()
  await mainPage.waitForLoadState('domcontentloaded')
})

test.afterAll(async () => {
  await electronApp.close()
  await rm(userDataDirectory, { recursive: true, force: true })
})

test('renders every functional tool as an immersive workspace while leaving home unchanged', async () => {
  const toolLabels = [
    '随手记', '文本对比', '格式化', 'JSON', '代码运行', '配置文件转换', 'Protobuf', '环境变量',
    'HTTP 请求', 'Host', '网络/IP', 'UA 分析', '编码解码', '加解密/随机', '正则', 'Cron', '二维码',
    '时间转换', '留言板', '翻译', '计算器', '调色板', '图片助手', 'PDF', '系统信息'
  ]

  await mainPage.getByRole('button', { name: '主页', exact: true }).click()
  await expect(mainPage.locator('.home-page h1')).toBeVisible()
  await expect(mainPage.locator('.app-shell')).not.toHaveClass(/app-shell--immersive-tool/)

  for (const label of toolLabels) {
    const button = mainPage.locator('.tool-button').filter({ hasText: label }).first()
    await button.scrollIntoViewIfNeeded()
    await button.click()
    const page = mainPage.locator('.workspace-tool-session:not([hidden]) .tool-page')
    await expect(page).toBeVisible()
    await expect(mainPage.locator('.app-shell')).toHaveClass(/app-shell--immersive-tool/)
    await expect.poll(() => page.evaluate((element) => {
      const bounds = element.getBoundingClientRect()
      const workspace = element.closest('.workspace')?.getBoundingClientRect()
      const style = getComputedStyle(element)
      const title = element.querySelector(':scope > .tool-page__header h1')
      const titleBounds = title?.getBoundingClientRect()
      const actionHeader = element.querySelector(':scope > .tool-page__header--actions')
      const actionControls = actionHeader ? [...actionHeader.querySelectorAll<HTMLElement>('button, input, select')] : []
      const outerSurface = element.querySelector<HTMLElement>(':scope > .local-tool-shell, :scope > .runtime-shell')
      const outerStyle = outerSurface ? getComputedStyle(outerSurface) : null
      return {
        topAligned: workspace ? Math.abs(bounds.top - workspace.top) < 0.5 : false,
        padding: [style.paddingTop, style.paddingRight, style.paddingBottom, style.paddingLeft],
        visiblePageTitle: Boolean(titleBounds && titleBounds.width > 1 && titleBounds.height > 1),
        actionHeaderFlush: !actionHeader || getComputedStyle(actionHeader).marginBottom === '0px',
        actionControlsVisible: actionControls.every((control) => {
          const rect = control.getBoundingClientRect()
          return rect.width > 0 && rect.height > 0
        }),
        outerSurfaceFlat: !outerStyle || (outerStyle.borderRadius === '0px' && outerStyle.boxShadow === 'none')
      }
    })).toEqual({
      topAligned: true,
      padding: ['0px', '0px', '0px', '0px'],
      visiblePageTitle: false,
      actionHeaderFlush: true,
      actionControlsVisible: true,
      outerSurfaceFlat: true
    })
  }

  await mainPage.getByRole('button', { name: '主页', exact: true }).click()
  await expect(mainPage.locator('.home-page h1')).toBeVisible()
  await expect(mainPage.locator('.app-shell')).not.toHaveClass(/app-shell--immersive-tool/)
})
