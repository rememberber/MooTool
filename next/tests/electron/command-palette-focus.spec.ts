import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

let app: ElectronApplication
let page: Page
let directory: string
test.beforeEach(async () => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-command-focus-'))
  app = await electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '1' } })
  page = await app.firstWindow()
  await page.locator('.tool-button').filter({ hasText: '计算器' }).click()
  await expect.poll(() => tool('Boolean(document.querySelector("#calculator-expression"))').catch(() => false)).toBe(true)
})
test.afterEach(async () => { if (app) await app.close(); await rm(directory, { recursive: true, force: true }) })

function tool(script: string) {
  return app.evaluate(async ({ webContents }, script) => {
    const contents = webContents.getAllWebContents().find(item => new URL(item.getURL() || 'about:blank').searchParams.get('toolId') === 'calculator')!
    return contents.executeJavaScript(script)
  }, script)
}
async function focusedKey(keyCode: string, modifiers: Array<'meta' | 'control'> = []) {
  await app.evaluate(({ webContents }, input) => {
    const contents = webContents.getFocusedWebContents()
    if (!contents) throw new Error('No WebContents owns keyboard focus')
    contents.sendInputEvent({ type: 'keyDown', keyCode: input.keyCode, modifiers: input.modifiers })
    contents.sendInputEvent({ type: 'keyUp', keyCode: input.keyCode, modifiers: input.modifiers })
  }, { keyCode, modifiers })
}
async function searchShortcut() { await focusedKey('K', [process.platform === 'darwin' ? 'meta' : 'control']) }
async function toolSearchAccelerator() {
  // sendInputEvent bypasses the OS menu accelerator in a native tool view.
  // Dispatch that menu handler, then route all navigation/typing to native focus.
  await app.evaluate(({ Menu, BrowserWindow }) => {
    const items = [...Menu.getApplicationMenu()!.items]
    while (items.length) {
      const item = items.shift()!
      if (item.accelerator === 'CommandOrControl+K') {
        item.click(item, BrowserWindow.getFocusedWindow() ?? undefined, { triggeredByAccelerator: true, shiftKey: false, ctrlKey: process.platform !== 'darwin', metaKey: process.platform === 'darwin', altKey: false })
        return
      }
      if (item.submenu) items.push(...item.submenu.items)
    }
    throw new Error('Search accelerator missing')
  })
}
async function assertPaletteKeyboard() {
  const input = page.locator('.command-palette__search input')
  await expect(input).toBeVisible()
  // DOM focus alone is insufficient: a removed WebContentsView can still own native focus.
  await expect.poll(() => page.evaluate(() => document.hasFocus())).toBe(true)
  await expect(input).toBeFocused()
  const options = page.locator('.command-palette [role=option]')
  const first = await options.nth(0).getAttribute('data-command-id')
  const second = await options.nth(1).getAttribute('data-command-id')
  await focusedKey('DOWN')
  await expect(page.locator('.command-palette [aria-selected=true]')).toHaveAttribute('data-command-id', second!)
  await focusedKey('UP')
  await expect(page.locator('.command-palette [aria-selected=true]')).toHaveAttribute('data-command-id', first!)
  await app.evaluate(({ webContents }) => webContents.getFocusedWebContents()!.insertText('uuid'))
  await expect(input).toHaveValue('uuid')
  await focusedKey('ENTER')
  await expect(page.getByRole('textbox', { name: '结果预览' })).toHaveText(/^[\da-f-]{36}$/, { useInnerText: true })
  await expect(page.getByRole('button', { name: '复制结果', exact: true })).toBeFocused()
  await focusedKey('ESCAPE')
  await expect(input).toBeFocused()
  await focusedKey('ESCAPE')
  await expect(page.locator('.command-palette')).toHaveCount(0)
}

test('focuses search and handles native keyboard navigation when the shortcut starts in a docked tool editor', async () => {
  const original = await tool('document.querySelector("#calculator-expression").value')
  for (let attempt = 0; attempt < 2; attempt++) {
    await app.evaluate(({ webContents }) => {
      const contents = webContents.getAllWebContents().find(item => new URL(item.getURL() || 'about:blank').searchParams.get('toolId') === 'calculator')!
      contents.focus()
    })
    await tool('document.querySelector("#calculator-expression").focus()')
    await toolSearchAccelerator()
    await assertPaletteKeyboard()
    await expect.poll(() => page.evaluate(() => window.mootool.getToolWindowSnapshot())).toMatchObject({ activeToolId: 'calculator' })
    expect(await tool('document.querySelector("#calculator-expression").value')).toBe(original)
  }
})

test('focuses search when its shortcut starts in the main sidebar above an attached tool view', async () => {
  await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows()[0].webContents.focus())
  await page.getByRole('button', { name: '搜索', exact: true }).focus()
  await searchShortcut()
  await assertPaletteKeyboard()
})

test('hands keyboard focus back to the main search panel from a detached tool window', async () => {
  await page.evaluate(() => window.mootool.detachToolWindow('calculator'))
  await expect.poll(() => page.evaluate(() => window.mootool.getToolWindowState('calculator'))).toMatchObject({ detached: true })
  await app.evaluate(({ BaseWindow, BrowserWindow, webContents }) => {
    const main = BrowserWindow.getAllWindows()[0]
    BaseWindow.getAllWindows().find(window => window.id !== main.id)!.focus()
    webContents.getAllWebContents().find(item => new URL(item.getURL() || 'about:blank').searchParams.get('toolId') === 'calculator')!.focus()
  })
  await toolSearchAccelerator()
  await assertPaletteKeyboard()
  expect(await tool('document.querySelector("#calculator-expression").value')).toBe('2 * (3 + 4)')
  expect(await page.evaluate(() => window.mootool.getToolWindowState('calculator'))).toMatchObject({ detached: true })
})
