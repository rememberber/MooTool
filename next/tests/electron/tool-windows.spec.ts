import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

let electronApp: ElectronApplication
let mainPage: Page
let userDataDirectory: string

test.beforeAll(async () => {
  userDataDirectory = await mkdtemp(join(tmpdir(), 'mootool-tool-windows-e2e-'))
  electronApp = await electron.launch({
    args: ['.', `--user-data-dir=${userDataDirectory}`],
    cwd: process.cwd(),
    env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '1' }
  })
  mainPage = await electronApp.firstWindow()
  await mainPage.waitForLoadState('domcontentloaded')
})

test.afterAll(async () => {
  await electronApp.close()
  await rm(userDataDirectory, { recursive: true, force: true })
})

test('moves one live tool view into a separate window and restores it without losing state', async () => {
  await mainPage.locator('.tool-button').filter({ hasText: '计算器' }).click()

  const initial = await waitForTool('calculator')
  expect(initial.value).toBe('2 * (3 + 4)')

  const changed = await evaluateTool<string>('calculator', `(() => {
    const input = document.querySelector('#calculator-expression')
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set
    setter.call(input, '40 + 2')
    input.dispatchEvent(new Event('input', { bubbles: true }))
    return input.value
  })()`)
  expect(changed.value).toBe('40 + 2')

  await expect.poll(() => mainPage.evaluate(() => {
    const shell = document.querySelector('.app-shell')
    const dragRegion = document.querySelector('.app-shell > .window-drag-region')
    const workspace = document.querySelector('.workspace')
    if (!shell || !dragRegion || !workspace) return false
    return shell.classList.contains('app-shell--tool-view-docked')
      && dragRegion.getBoundingClientRect().right <= workspace.getBoundingClientRect().left
  })).toBe(true)
  await expect.poll(async () => (await evaluateTool<boolean>('calculator', `(() => {
    const dragRegion = document.querySelector('.window-drag-region').getBoundingClientRect()
    const button = document.querySelector('.tool-window-toggle').getBoundingClientRect()
    return dragRegion.right <= button.left
  })()`)).value).toBe(true)

  await clickToolWindowToggle('calculator')
  await expect(mainPage.getByRole('heading', { name: '计算器 已在独立窗口中打开' })).toBeVisible()
  await expect.poll(() => getToolSnapshot('calculator')).toMatchObject({ detached: true, ready: true })
  await expect.poll(() => getBaseWindowCount()).toBe(2)

  const detached = await evaluateTool<string>('calculator', `document.querySelector('#calculator-expression').value`)
  expect(detached.id).toBe(initial.id)
  expect(detached.value).toBe('40 + 2')

  await mainPage.getByRole('button', { name: '收回到功能区', exact: true }).click()
  await expect.poll(() => getToolSnapshot('calculator')).toMatchObject({ detached: false, ready: true })
  const docked = await evaluateTool<string>('calculator', `document.querySelector('#calculator-expression').value`)
  expect(docked.id).toBe(initial.id)
  expect(docked.value).toBe('40 + 2')

  await evaluateTool('calculator', `document.querySelector('.tool-window-toggle').click()`)
  await expect.poll(() => getToolSnapshot('calculator')).toMatchObject({ detached: true })
  await closeDetachedBaseWindow()
  await expect.poll(() => getToolSnapshot('calculator')).toMatchObject({ detached: false, ready: true })
  const closedAndRestored = await evaluateTool<string>('calculator', `document.querySelector('#calculator-expression').value`)
  expect(closedAndRestored.id).toBe(initial.id)
  expect(closedAndRestored.value).toBe('40 + 2')
})

test('keeps multiple detached tools independent and returns each one to its dock', async () => {
  await mainPage.getByRole('button', { name: 'JSON', exact: true }).click()
  await waitForToolSelector('json', '.json-tool')
  await evaluateTool('json', `document.querySelector('.tool-window-toggle').click()`)
  await expect.poll(() => getToolSnapshot('json')).toMatchObject({ detached: true, ready: true })

  await mainPage.getByRole('button', { name: 'HTTP 请求', exact: true }).click()
  await waitForToolSelector('http', '.http-tool-page')
  await evaluateTool('http', `document.querySelector('.tool-window-toggle').click()`)
  await expect.poll(() => getToolSnapshot('http')).toMatchObject({ detached: true, ready: true })
  await expect(mainPage.getByRole('heading', { name: 'HTTP 请求 已在独立窗口中打开' })).toBeVisible()
  await expect.poll(() => getBaseWindowCount()).toBe(3)

  await mainPage.getByRole('button', { name: '主页', exact: true }).click()
  await expect(mainPage.locator('.tool-button__detached')).toHaveCount(2)

  await closeAllDetachedBaseWindows()
  await expect.poll(() => getToolSnapshot('json')).toMatchObject({ detached: false, ready: true })
  await expect.poll(() => getToolSnapshot('http')).toMatchObject({ detached: false, ready: true })
  await expect.poll(() => getBaseWindowCount()).toBe(1)
})

test('temporarily reveals the main overlay when search is opened above a docked tool', async () => {
  await mainPage.locator('.tool-button').filter({ hasText: '计算器' }).click()
  await waitForToolSelector('calculator', '.calculator-workspace')
  await expect.poll(() => getMainChildViewCount()).toBe(1)

  await mainPage.getByRole('button', { name: '搜索', exact: true }).click()
  await expect(mainPage.locator('.command-palette')).toBeVisible()
  await expect.poll(() => getMainChildViewCount()).toBe(0)

  await mainPage.getByRole('button', { name: '关闭搜索', exact: true }).click()
  await expect.poll(() => getMainChildViewCount()).toBe(1)
  const restored = await evaluateTool<string>('calculator', `document.querySelector('#calculator-expression').value`)
  expect(restored.value).toBe('40 + 2')
})

async function waitForTool(toolId: string): Promise<{ id: number; value: string }> {
  await expect.poll(async () => {
    const result = await evaluateTool<string>(toolId, `document.querySelector('#calculator-expression')?.value ?? ''`).catch(() => null)
    return result?.value ?? ''
  }).toBe('2 * (3 + 4)')
  return evaluateTool<string>(toolId, `document.querySelector('#calculator-expression').value`)
}

async function waitForToolSelector(toolId: string, selector: string): Promise<void> {
  await expect.poll(async () => {
    const result = await evaluateTool<boolean>(toolId, `Boolean(document.querySelector(${JSON.stringify(selector)}))`).catch(() => null)
    return result?.value ?? false
  }).toBe(true)
}

async function evaluateTool<T = unknown>(toolId: string, script: string): Promise<{ id: number; value: T }> {
  return electronApp.evaluate(async ({ webContents }, input) => {
    const contents = webContents.getAllWebContents().find((item) => {
      const url = new URL(item.getURL())
      return url.searchParams.get('window') === 'tool' && url.searchParams.get('toolId') === input.toolId
    })
    if (!contents) throw new Error(`Tool webContents not found: ${input.toolId}`)
    return { id: contents.id, value: await contents.executeJavaScript(input.script) as T }
  }, { toolId, script })
}

async function clickToolWindowToggle(toolId: string): Promise<void> {
  await electronApp.evaluate(async ({ webContents }, id) => {
    const contents = webContents.getAllWebContents().find((item) => {
      const url = new URL(item.getURL())
      return url.searchParams.get('window') === 'tool' && url.searchParams.get('toolId') === id
    })
    if (!contents) throw new Error(`Tool webContents not found: ${id}`)

    const point = await contents.executeJavaScript(`(() => {
      const button = document.querySelector('.tool-window-toggle')
      if (!button) throw new Error('Tool window toggle not found')
      const rect = button.getBoundingClientRect()
      return { x: Math.round(rect.left + rect.width / 2), y: Math.round(rect.top + rect.height / 2) }
    })()`)

    contents.focus()
    contents.sendInputEvent({ type: 'mouseMove', x: point.x, y: point.y })
    contents.sendInputEvent({ type: 'mouseDown', x: point.x, y: point.y, button: 'left', clickCount: 1 })
    contents.sendInputEvent({ type: 'mouseUp', x: point.x, y: point.y, button: 'left', clickCount: 1 })
  }, toolId)
}

async function getToolSnapshot(toolId: string): Promise<unknown> {
  return mainPage.evaluate(async (id) => {
    const snapshot = await window.mootool.getToolWindowSnapshot()
    return snapshot.tools.find((item) => item.toolId === id)
  }, toolId)
}

async function getBaseWindowCount(): Promise<number> {
  return electronApp.evaluate(({ BaseWindow }) => BaseWindow.getAllWindows().length)
}

async function getMainChildViewCount(): Promise<number> {
  return electronApp.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows().find((window) => !window.getParentWindow())?.contentView.children.length ?? -1)
}

async function closeDetachedBaseWindow(): Promise<void> {
  await electronApp.evaluate(({ BaseWindow, BrowserWindow }) => {
    const browserWindowIds = new Set(BrowserWindow.getAllWindows().map((window) => window.id))
    BaseWindow.getAllWindows().find((window) => !browserWindowIds.has(window.id))?.close()
  })
}

async function closeAllDetachedBaseWindows(): Promise<void> {
  await electronApp.evaluate(({ BaseWindow, BrowserWindow }) => {
    const browserWindowIds = new Set(BrowserWindow.getAllWindows().map((window) => window.id))
    for (const window of BaseWindow.getAllWindows()) {
      if (!browserWindowIds.has(window.id)) window.close()
    }
  })
}
