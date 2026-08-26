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
      const actionHeaders = element.querySelectorAll(':scope > .tool-page__header--actions')
      const outerSurface = element.querySelector<HTMLElement>(':scope > .local-tool-shell, :scope > .runtime-shell')
      const outerStyle = outerSurface ? getComputedStyle(outerSurface) : null
      const workspaceDragUsable = [...element.querySelectorAll<HTMLElement>('[data-window-drag-zone]')].some((zone) => {
        const rect = zone.getBoundingClientRect()
        return rect.width >= 32
          && rect.height >= 24
          && rect.right > bounds.left
          && rect.left < bounds.right
          && getComputedStyle(zone).getPropertyValue('-webkit-app-region') === 'drag'
      })
      return {
        topAligned: workspace ? Math.abs(bounds.top - workspace.top) < 0.5 : false,
        padding: [style.paddingTop, style.paddingRight, style.paddingBottom, style.paddingLeft],
        visiblePageTitle: Boolean(titleBounds && titleBounds.width > 1 && titleBounds.height > 1),
        standaloneActionHeaderCount: actionHeaders.length,
        outerSurfaceFlat: !outerStyle || (outerStyle.borderRadius === '0px' && outerStyle.boxShadow === 'none'),
        workspaceDragUsable
      }
    })).toEqual({
      topAligned: true,
      padding: ['0px', '0px', '0px', '0px'],
      visiblePageTitle: false,
      standaloneActionHeaderCount: 0,
      outerSurfaceFlat: true,
      workspaceDragUsable: true
    })
  }

  await mainPage.getByRole('button', { name: '主页', exact: true }).click()
  await expect(mainPage.locator('.home-page h1')).toBeVisible()
  await expect(mainPage.locator('.app-shell')).not.toHaveClass(/app-shell--immersive-tool/)
})
