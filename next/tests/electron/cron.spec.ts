import { _electron as electron, expect, test } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

test('Cron supports the Java builder, examples, language and favorite workflow', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-cron-e2e-'))
  const app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })
  try {
    const page = await app.firstWindow()
    const errors: string[] = []
    page.on('pageerror', error => errors.push(error.message))
    await page.locator('.tool-button').filter({ hasText: 'Cron' }).click()
    const expression = page.locator('#cron-expression')
    await expect(page.locator('.cron-runs li')).toHaveCount(10)
    await page.getByRole('tab', { name: '日', exact: true }).click()
    await page.getByRole('radio', { name: '最近工作日', exact: true }).check()
    await page.getByRole('spinbutton', { name: '最近工作日', exact: true }).fill('15')
    await expect(expression).toHaveValue('0 * * 15W * ?')
    await page.getByRole('tab', { name: '星期', exact: true }).click()
    await page.getByRole('radio', { name: '每月第 N 个星期', exact: true }).check()
    await page.getByRole('spinbutton', { name: '星期（1=周日，7=周六）' }).fill('6')
    await page.getByRole('spinbutton', { name: '第几次' }).fill('3')
    await expect(expression).toHaveValue('0 * * ? * 6#3')
    await expression.fill('0 15 10 ? * MON-FRI')
    await page.getByRole('button', { name: '反解析到界面' }).click()
    await expect(page.getByRole('radio', { name: '范围', exact: true })).toBeChecked()
    await page.getByRole('combobox', { name: '描述语言' }).selectOption('en-US')
    await expect(page.locator('.cron-expression-panel output')).toContainText('Monday through Friday')
    await page.getByRole('button', { name: '常用表达式', exact: true }).click()
    await expect(page.locator('.cron-example-list button')).toHaveCount(22)
    await page.locator('.cron-example-list button').filter({ hasText: '0 15 10 ? * 6L 2002-2006' }).click()
    await expect(page.locator('.cron-runs')).toContainText('没有未来执行时间')
    await expression.fill('0 15 10 ? * 6#3')
    await page.locator('.cron-builder').getByRole('button', { name: '收藏', exact: false }).click()
    const dialog = page.getByRole('dialog')
    await dialog.getByRole('textbox', { name: '名称', exact: true }).fill('Monthly Friday')
    await dialog.getByRole('button', { name: '保存', exact: true }).click()
    await expect(dialog.locator('.favorite-item')).toHaveCount(1)
    await dialog.getByRole('button', { name: '编辑', exact: true }).click()
    await dialog.getByRole('textbox', { name: '名称', exact: true }).fill('Third Friday')
    await dialog.getByRole('button', { name: '保存', exact: true }).click()
    await expect(dialog.locator('.favorite-item')).toContainText('Third Friday')
    await dialog.locator('.favorite-item > button').first().click()
    await expect(expression).toHaveValue('0 15 10 ? * 6#3')
    await expect(page.locator('.cron-runs li')).toHaveCount(10)
    await page.screenshot({ path: 'test-results/cron-parity.png', fullPage: true })
    await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].setSize(1080, 720))
    await page.getByRole('tab', { name: '秒', exact: true }).click()
    await page.getByRole('radio', { name: '指定值（可多选）', exact: true }).check()
    await expect(page.locator('.cron-value-grid input')).toHaveCount(60)
    await page.screenshot({ path: 'test-results/cron-parity-compact.png', fullPage: true })
    // The 1320px viewport breakpoint must not override the container's active two-column layout.
    for (const width of [1280, 1440, 1080]) {
      await app.evaluate(({ BrowserWindow }, width) => BrowserWindow.getAllWindows()[0].setSize(width, 800), width)
      await expect.poll(() => page.locator('.cron-workspace').evaluate(workspace => {
        const runs = workspace.querySelector<HTMLElement>('.cron-runs')!
        const last = runs.querySelector('li:last-child')!
        const bounds = workspace.getBoundingClientRect()
        const resultBounds = runs.getBoundingClientRect()
        return {
          spansWorkspace: Math.abs(resultBounds.width - workspace.clientWidth) < 3,
          aligned: Math.abs(resultBounds.left - bounds.left) < 3,
          containsLastRun: last.getBoundingClientRect().bottom <= resultBounds.bottom,
          nestedScroll: runs.scrollHeight > runs.clientHeight + 1
        }
      })).toEqual({ spansWorkspace: true, aligned: true, containsLastRun: true, nestedScroll: false })
    }
    await page.locator('.cron-runs li').last().scrollIntoViewIfNeeded()
    await expect(page.locator('.cron-runs li').last()).toBeInViewport()
    await page.screenshot({ path: 'test-results/cron-runs-layout.png', fullPage: true })
    expect(errors).toEqual([])
  } finally {
    await app.close()
    await rm(directory, { recursive: true, force: true })
  }
})
