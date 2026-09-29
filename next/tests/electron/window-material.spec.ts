import { _electron as electron, expect, test } from '@playwright/test'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { defaultAppSettings } from '../../src/shared/contracts/settings'

for (const mode of ['solid', 'vibrancy', 'liquid-glass', 'auto'] as const) {
  test(`${mode}: initializes material, preserves opaque controls and responds to accessibility`, async ({}, testInfo) => {
    const directory = await mkdtemp(join(tmpdir(), 'mootool-material-'))
    await writeFile(join(directory, 'mootool-next.json'), JSON.stringify({ settings: {
      ...defaultAppSettings,
      general: { ...defaultAppSettings.general, autoCheckUpdates: false, legacyMigrationHintDismissed: true },
      appearance: { ...defaultAppSettings.appearance, windowMaterial: mode, theme: 'light' }
    } }))
    const app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test' } })
    try {
      const page = await app.firstWindow()
      await expect(page.locator('.sidebar')).toBeVisible()
      await expect.poll(() => app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].isVisible())).toBe(true)
      const material = await page.evaluate(() => window.mootool.getWindowMaterial())
      expect(mode === 'solid' || process.platform !== 'darwin' ? material === 'solid' : ['vibrancy', 'liquid-glass'].includes(material)).toBe(true)
      if (mode === 'vibrancy' && process.platform === 'darwin') expect(material).toBe('vibrancy')
      await expect(page.locator('html')).toHaveAttribute('data-window-material', material)
      const light = await page.locator('.sidebar').evaluate(element => ({
        opacity: getComputedStyle(element).opacity, image: getComputedStyle(element).backgroundImage,
        alpha: getComputedStyle(element).getPropertyValue('--window-glass-alpha').trim()
      }))
      expect(light.opacity).toBe('1')
      if (material !== 'solid') {
        expect(light.image).toContain('linear-gradient')
        expect(light.alpha).toBe('0.4')
      }
      await page.screenshot({ path: testInfo.outputPath('light.png') })
      await page.evaluate(() => window.mootool.updateSettings({ appearance: { theme: 'dark' } }))
      await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
      if (material !== 'solid') expect(await page.locator('.sidebar').evaluate(el => getComputedStyle(el).getPropertyValue('--window-glass-alpha').trim())).toBe('0.5')
      await page.screenshot({ path: testInfo.outputPath('dark.png') })
      await page.emulateMedia({ contrast: 'more' })
      await expect(page.locator('html')).toHaveAttribute('data-window-material', 'solid')
      expect(await page.locator('.sidebar').evaluate(el => getComputedStyle(el).backgroundImage)).toBe('none')
      await page.emulateMedia({ contrast: 'no-preference' })
      await expect(page.locator('html')).toHaveAttribute('data-window-material', material)
      // A saved mode change must not destroy the window or its active web state.
      const id = await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].id)
      await page.evaluate(() => window.mootool.updateSettings({ appearance: { windowMaterial: 'solid' } }))
      expect(await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].id)).toBe(id)
      if (mode === 'auto') {
        await page.evaluate(() => window.mootool.openSettings('appearance'))
        await expect(page.getByRole('combobox', { name: '窗口材质', exact: true })).toHaveValue('solid')
        await expect(page.getByText('下次启动生效。', { exact: false })).toBeVisible()
        await page.screenshot({ path: testInfo.outputPath('settings.png') })
      }
      console.log(`${mode} resolved to ${material}`)
    } finally {
      await app.close()
      await rm(directory, { recursive: true, force: true })
    }
  })
}
