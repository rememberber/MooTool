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
    const app = await electron.launch({ executablePath: process.env.MOOTOOL_E2E_EXECUTABLE, args: [...(process.env.MOOTOOL_E2E_EXECUTABLE ? [] : ['.']), `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '1' } })
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
      // Native child-view backgrounds must not conceal standalone window chrome.
      await page.evaluate(() => window.mootool.detachToolWindow('calculator'))
      await expect.poll(() => page.evaluate(() => window.mootool.getToolWindowState('calculator'))).toMatchObject({ ready: true, detached: true })
      const toolValue = <T,>(expression: string) => app.evaluate(async ({ webContents }, code) => {
        const tool = webContents.getAllWebContents().find(contents => contents.getURL().includes('toolId=calculator'))
        if (!tool) throw new Error('Calculator view unavailable')
        return await tool.executeJavaScript(code)
      }, expression) as Promise<T>
      await expect.poll(() => toolValue<string>(`document.documentElement.dataset.windowMaterial`)).toBe(material)
      await expect.poll(() => toolValue<number>(`document.querySelectorAll('.window-chrome').length`)).toBe(0)
      await expect.poll(() => toolValue<boolean>(`getComputedStyle(document.querySelector('.tool-view-shell')).backgroundColor !== 'rgba(0, 0, 0, 0)'`)).toBe(true)
      await toolValue(`window.mootool.setMaterialAccessibility(true)`)
      await expect.poll(() => toolValue<string>(`document.documentElement.dataset.windowMaterial`)).toBe('solid')
      await toolValue(`window.mootool.setMaterialAccessibility(false)`)
      await expect.poll(() => toolValue<string>(`document.documentElement.dataset.windowMaterial`)).toBe(material)
      if (mode === 'auto') {
        // Renderer readiness precedes the native window's asynchronous material/show work.
        await expect.poll(() => app.evaluate(({ BaseWindow, BrowserWindow }) =>
          BaseWindow.getAllWindows().find(win => !BrowserWindow.getAllWindows().includes(win as never))?.isVisible()
        )).toBe(true)
        await app.evaluate(({ BaseWindow, BrowserWindow }) => {
          const detached = BaseWindow.getAllWindows().find(win => !BrowserWindow.getAllWindows().includes(win as never))
          if (!detached) throw new Error('Detached native window unavailable')
          detached.focus()
          detached.setFullScreen(true)
        })
        await expect.poll(() => app.evaluate(({ BaseWindow, BrowserWindow }) =>
          BaseWindow.getAllWindows().find(win => !BrowserWindow.getAllWindows().includes(win as never))?.isFullScreen()
        ), { timeout: 10_000 }).toBe(true)
        await expect.poll(() => toolValue<string>(`document.documentElement.dataset.fullscreen`)).toBe('true')
        await app.evaluate(({ BaseWindow, BrowserWindow }) => {
          BaseWindow.getAllWindows().find(win => !BrowserWindow.getAllWindows().includes(win as never))?.setFullScreen(false)
        })
        await expect.poll(() => toolValue<string>(`document.documentElement.dataset.fullscreen`)).toBe('false')
      }
      await page.evaluate(() => window.mootool.dockToolWindow('calculator'))
      await expect.poll(() => toolValue<string>(`document.documentElement.dataset.windowMaterial`)).toBe('solid')
      const status = await page.evaluate(() => window.mootool.getWindowMaterialStatus())
      expect(status?.effective).toBe(material)
      expect(status?.pendingRestart).toBe(false)
      // A saved mode change must not destroy the window or its active web state.
      const id = await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].id)
      await page.evaluate(() => window.mootool.updateSettings({ appearance: { windowMaterial: 'solid' } }))
      expect(await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].id)).toBe(id)
      if (mode === 'auto') {
        await page.evaluate(() => window.mootool.openSettings('appearance'))
        await expect(page.getByRole('combobox', { name: '窗口材质', exact: true })).toHaveValue('solid')
        await expect(page.getByText('下次启动生效。', { exact: false })).toBeVisible()
        await expect(page.getByText('更改已保存，重启应用后生效', { exact: true })).toBeVisible()
        await expect(page.locator('.window-material-status')).toContainText('当前实际材质')
        await page.screenshot({ path: testInfo.outputPath('settings.png') })
      }
      console.log(`${mode} resolved to ${material}`)
    } finally {
      await app.close()
      await rm(directory, { recursive: true, force: true })
    }
  })
}
