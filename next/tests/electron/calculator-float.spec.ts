import { _electron as electron, expect, test } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

test('converts hexadecimal fractions and IEEE 754 floats and restores the draft', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-calculator-float-'))
  const launch = () => electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })
  let app = await launch()
  try {
    let page = await app.firstWindow()
    page.setDefaultTimeout(5000)
    await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
    await page.locator('#calculator-hex').fill('A.F')
    await page.getByRole('button', { name: 'HEX → DEC', exact: true }).click()
    await expect(page.locator('#calculator-decimal')).toHaveValue('10.9375')
    await expect(page.locator('.calculator-result')).toHaveText('10.9375')
    await page.getByRole('button', { name: 'DEC → HEX', exact: true }).click()
    await expect(page.locator('#calculator-hex')).toHaveValue('a.f')

    const convert = page.getByRole('button', { name: 'IEEE 754 HEX → DEC', exact: true })
    await page.locator('#calculator-float-hex').fill('3F800000')
    await convert.click()
    await expect(page.locator('#calculator-float-decimal')).toHaveValue('1.0')
    await expect(page.locator('.calculator-result')).toHaveText('1.0')
    await expect(page.locator('.calculator-log')).toContainText('IEEE 754 32 (big) HEX → DEC: 3F800000 = 1.0')

    await page.getByLabel('浮点格式', { exact: true }).selectOption('64')
    await page.getByLabel('字节序', { exact: true }).selectOption('little')
    await page.locator('#calculator-float-hex').fill('00 00 00 00 00 00 04 C0')
    await convert.click()
    await expect(page.locator('#calculator-float-decimal')).toHaveValue('-2.5')
    await expect(page.locator('.calculator-result')).toHaveText('-2.5')
    await page.locator('#calculator-float-hex').fill('123')
    await convert.click()
    await expect(page.getByRole('alert')).toContainText('exactly 16 hexadecimal digits')
    await expect(page.locator('#calculator-float-decimal')).toHaveValue('-2.5')
    await page.locator('#calculator-float-hex').fill('00 00 00 00 00 00 04 C0')
    await expect.poll(() => page.evaluate(() => window.mootool.getToolDraft('calculator'))).toContain('00 00 00 00 00 00 04 C0')
    await app.close()

    app = await launch()
    page = await app.firstWindow()
    page.setDefaultTimeout(5000)
    await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
    await expect(page.getByLabel('浮点格式', { exact: true })).toHaveValue('64')
    await expect(page.getByLabel('字节序', { exact: true })).toHaveValue('little')
    await expect(page.locator('#calculator-float-hex')).toHaveValue('00 00 00 00 00 00 04 C0')
    await expect(page.locator('#calculator-float-decimal')).toHaveValue('-2.5')
    await expect(page.locator('#calculator-hex')).toHaveValue('a.f')
    await page.screenshot({ path: 'test-results/calculator-float.png' })
  } finally {
    await app.close()
    await rm(directory, { recursive: true, force: true })
  }
})
