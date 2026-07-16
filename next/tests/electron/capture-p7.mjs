import { _electron as electron } from '@playwright/test'
import { DatabaseSync } from 'node:sqlite'
import { mkdtemp, mkdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const root = process.cwd()
const userData = await mkdtemp(join(tmpdir(), 'mootool-p7-visual-'))
const legacy = await mkdtemp(join(tmpdir(), 'mootool-p7-legacy-'))
await mkdir(join(legacy, 'config'))
await writeFile(join(legacy, 'config', 'config.setting'), '[setting.http]\nhttpProxyPassword = skipped\n')
const database = new DatabaseSync(join(legacy, 'MooTool.db'))
database.exec(`
  CREATE TABLE t_quick_note (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT);
  INSERT INTO t_quick_note VALUES (1, 'Visual Note', 'P7 migration preview', '2026-01-01', '2026-01-02');
  CREATE TABLE t_json_beauty (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT);
  INSERT INTO t_json_beauty VALUES (1, 'Visual JSON', '{"phase":7}', '2026-01-01', '2026-01-02');
`)
database.close()

const app = await electron.launch({ args: ['.', `--user-data-dir=${userData}`], cwd: root, env: { ...process.env, NODE_ENV: 'test' } })

try {
  const page = await app.firstWindow()
  await page.waitForLoadState('domcontentloaded')
  await resizeMain(app, page, 1440, 920)
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'light' } }))
  await page.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  const jsonEditor = page.locator('.json-editor .cm-content')
  await jsonEditor.fill(JSON.stringify({ phase: 7, editor: 'CodeMirror 6', stack: ['Electron', 'React', 'TypeScript'], parity: { theme: 'light', viewport: '1440x920' } }))
  await page.locator('.editor-toolbar').getByRole('button', { name: '格式化', exact: true }).click()
  await captureMain(page, 'p7-json-1440-light.png', 'json-1440-light')

  await resizeMain(app, page, 1080, 720)
  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'dark' } }))
  await page.waitForTimeout(180)
  await captureMain(page, 'p7-json-1080-dark.png', 'json-1080-dark')

  await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'light' } }))
  await page.locator('.sidebar-footer .icon-ghost').click()
  await page.waitForTimeout(350)
  const settingsPage = app.windows().find((candidate) => candidate !== page && !candidate.isClosed())
  if (!settingsPage) throw new Error('Settings window failed to open')
  await settingsPage.waitForLoadState('domcontentloaded')
  await app.evaluate(({ BrowserWindow }) => {
    BrowserWindow.getAllWindows().find((item) => item.getParentWindow())?.setContentSize(920, 700)
  })
  await settingsPage.locator('.settings-nav__item').filter({ hasText: '数据与备份' }).click()
  const source = settingsPage.getByLabel('Java 版数据目录')
  await source.fill(legacy)
  await source.press('Enter')
  await settingsPage.getByRole('button', { name: '扫描', exact: true }).click()
  const summary = settingsPage.locator('.legacy-migration-summary')
  await summary.waitFor()
  await summary.scrollIntoViewIfNeeded()
  await settingsPage.waitForTimeout(180)

  await capture(settingsPage, 'p7-migration-920-light.png', 'migration-920-light')
  await settingsPage.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'dark' } }))
  await settingsPage.waitForTimeout(220)
  await capture(settingsPage, 'p7-migration-920-dark.png', 'migration-920-dark')
} finally {
  await app.close()
  await Promise.all([rm(userData, { recursive: true, force: true }), rm(legacy, { recursive: true, force: true })])
}

async function capture(page, filename, label) {
  const result = await page.evaluate(() => {
    const summary = document.querySelector('.legacy-migration-summary')?.getBoundingClientRect()
    return {
      documentOverflowX: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      documentOverflowY: document.documentElement.scrollHeight - document.documentElement.clientHeight,
      summaryVisible: Boolean(summary && summary.width > 200 && summary.height > 40),
      summaryOutsideViewport: Boolean(summary && (summary.left < -1 || summary.top < -1 || summary.right > window.innerWidth + 1 || summary.bottom > window.innerHeight + 1))
    }
  })
  if (result.documentOverflowX > 1 || result.documentOverflowY > 1 || !result.summaryVisible || result.summaryOutsideViewport) {
    throw new Error(`${label} visual audit failed: ${JSON.stringify(result)}`)
  }
  console.log(`${label}: ${JSON.stringify(result)}`)
  await page.screenshot({ path: join(root, 'doc/screenshots', filename), scale: 'css' })
}

async function resizeMain(app, page, width, height) {
  await app.evaluate(({ BrowserWindow }, bounds) => {
    BrowserWindow.getAllWindows().find((item) => !item.getParentWindow())?.setContentSize(bounds.width, bounds.height)
  }, { width, height })
  await page.setViewportSize({ width, height })
  await page.waitForTimeout(220)
}

async function captureMain(page, filename, label) {
  const result = await page.evaluate(() => {
    const editor = document.querySelector('.json-editor')?.getBoundingClientRect()
    return {
      documentOverflowX: document.documentElement.scrollWidth - document.documentElement.clientWidth,
      documentOverflowY: document.documentElement.scrollHeight - document.documentElement.clientHeight,
      editorVisible: Boolean(editor && editor.width > 240 && editor.height > 240),
      editorOutsideViewport: Boolean(editor && (editor.left < -1 || editor.top < -1 || editor.right > window.innerWidth + 1 || editor.bottom > window.innerHeight + 1))
    }
  })
  if (result.documentOverflowX > 1 || result.documentOverflowY > 1 || !result.editorVisible || result.editorOutsideViewport) {
    throw new Error(`${label} visual audit failed: ${JSON.stringify(result)}`)
  }
  console.log(`${label}: ${JSON.stringify(result)}`)
  await page.screenshot({ path: join(root, 'doc/screenshots', filename), scale: 'css' })
}
