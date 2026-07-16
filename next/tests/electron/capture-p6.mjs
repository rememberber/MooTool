import { _electron as electron } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const root = process.cwd()
const userData = await mkdtemp(join(tmpdir(), 'mootool-p6-visual-'))
const app = await electron.launch({ args: ['.', `--user-data-dir=${userData}`], cwd: root, env: { ...process.env, NODE_ENV: 'test' } })

try {
  const page = await app.firstWindow()
  await page.waitForLoadState('domcontentloaded')

  async function resize(width, height) {
    await app.evaluate(({ BrowserWindow }, bounds) => {
      const window = BrowserWindow.getAllWindows().find((item) => !item.isDestroyed() && !item.getParentWindow())
      window?.setContentSize(bounds.width, bounds.height)
    }, { width, height })
    await page.setViewportSize({ width, height })
    await page.waitForTimeout(250)
  }

  async function theme(value) {
    await page.evaluate((nextTheme) => window.mootool.updateSettings({ appearance: { theme: nextTheme } }), value)
    await page.waitForTimeout(180)
  }

  async function openTool(label) {
    const button = page.locator('.tool-button').filter({ hasText: label }).first()
    await button.scrollIntoViewIfNeeded()
    await button.click()
    await page.locator('.tool-page > .tool-page__header h1').waitFor()
  }

  async function audit(targetPage, label, selector) {
    const result = await targetPage.evaluate((targetSelector) => {
      const target = document.querySelector(targetSelector)
      const rect = target?.getBoundingClientRect()
      return {
        documentOverflowX: document.documentElement.scrollWidth - document.documentElement.clientWidth,
        documentOverflowY: document.documentElement.scrollHeight - document.documentElement.clientHeight,
        targetVisible: Boolean(rect && rect.width > 40 && rect.height > 40),
        targetOutsideViewport: Boolean(rect && (rect.left < -1 || rect.top < -1 || rect.right > window.innerWidth + 1 || rect.bottom > window.innerHeight + 1))
      }
    }, selector)
    if (result.documentOverflowX > 1 || result.documentOverflowY > 1 || !result.targetVisible || result.targetOutsideViewport) {
      throw new Error(`${label} visual audit failed: ${JSON.stringify(result)}`)
    }
    console.log(`${label}: ${JSON.stringify(result)}`)
  }

  await resize(1440, 920)
  await theme('light')
  await openTool('随手记')
  await page.getByRole('tab', { name: '分栏' }).click()
  await page.screenshot({ path: join(root, 'doc/screenshots/p6-quick-note-1440-light.png'), scale: 'css' })
  await audit(page, 'quick-note-1440-light', '.quick-note-layout')

  await resize(1080, 720)
  await theme('dark')
  await page.screenshot({ path: join(root, 'doc/screenshots/p6-quick-note-1080-dark.png'), scale: 'css' })
  await audit(page, 'quick-note-1080-dark', '.quick-note-layout')

  await resize(1440, 920)
  await theme('light')
  await openTool('代码运行')
  await page.getByRole('tab', { name: 'Node.js' }).click()
  await page.getByLabel('代码编辑器').fill('console.log("MooTool runtime ready", { value: 42 })')
  const run = page.getByRole('button', { name: '运行', exact: true })
  if (await run.isEnabled()) {
    await run.click()
    await page.locator('.runtime-output-state').filter({ hasText: '运行完成' }).waitFor({ timeout: 15_000 })
  }
  await page.screenshot({ path: join(root, 'doc/screenshots/p6-runtime-1440-light.png'), scale: 'css' })
  await audit(page, 'runtime-1440-light', '.runtime-shell')

  await resize(1080, 720)
  await theme('dark')
  await page.screenshot({ path: join(root, 'doc/screenshots/p6-runtime-1080-dark.png'), scale: 'css' })
  await audit(page, 'runtime-1080-dark', '.runtime-shell')

  await theme('light')
  const windowsBefore = app.windows().length
  await page.locator('.sidebar-footer .icon-ghost').click()
  await page.waitForTimeout(400)
  const settingsPage = app.windows().find((candidate) => candidate !== page && !candidate.isClosed())
  if (!settingsPage || app.windows().length < windowsBefore) throw new Error('Settings window failed to open')
  await settingsPage.waitForLoadState('domcontentloaded')
  await settingsPage.locator('.settings-nav__item').filter({ hasText: '数据与备份' }).click()
  await settingsPage.locator('.backup-row').first().waitFor()
  await app.evaluate(({ BrowserWindow }) => {
    BrowserWindow.getAllWindows().find((item) => item.getParentWindow())?.setContentSize(920, 700)
  })
  await settingsPage.screenshot({ path: join(root, 'doc/screenshots/p6-backup-920-light.png'), scale: 'css' })
  await audit(settingsPage, 'backup-920-light', '.settings-layout')
  await settingsPage.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'dark' } }))
  await settingsPage.waitForTimeout(200)
  await settingsPage.screenshot({ path: join(root, 'doc/screenshots/p6-backup-920-dark.png'), scale: 'css' })
  await audit(settingsPage, 'backup-920-dark', '.settings-layout')
} finally {
  await app.close()
  await rm(userData, { recursive: true, force: true })
}
