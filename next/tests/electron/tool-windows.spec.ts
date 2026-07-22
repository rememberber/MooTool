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
  await expect.poll(() => getDetachedWindowChrome()).toMatchObject({
    contentInsetTop: 0,
    contentInsetHeight: 0,
    ...(process.platform === 'darwin' ? { windowButtonPosition: { x: 18, y: 18 } } : {})
  })

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
  await expect.poll(async () => (await evaluateTool('json', `(() => {
    const logo = document.querySelector('.tool-window-brand')
    const title = document.querySelector('.tool-page__header h1')
    const zone = document.querySelector('.tool-window-brand-zone')
    if (!logo || !title || !zone) return null
    const logoBounds = logo.getBoundingClientRect()
    const titleBounds = title.getBoundingClientRect()
    const zoneBounds = zone.getBoundingClientRect()
    const zoneStyle = getComputedStyle(zone)
    return {
      title: title.textContent,
      logoBeforeTitle: logoBounds.right < titleBounds.left,
      logoVisible: logoBounds.width > 0 && logoBounds.height > 0,
      logoLoaded: logo instanceof HTMLImageElement && logo.complete && logo.naturalWidth > 0,
      logoLeft: Math.round(logoBounds.left),
      titleLeft: Math.round(titleBounds.left),
      sameRow: Math.abs((logoBounds.top + logoBounds.height / 2) - (titleBounds.top + titleBounds.height / 2)) < 2,
      headerTop: Math.round(zoneBounds.top),
      controlsHidden: zone.getAttribute('data-window-controls-visible') === 'false',
      hasVisualOverlay: zoneStyle.backgroundImage !== 'none'
        || zoneStyle.boxShadow !== 'none'
        || String(zoneStyle.backdropFilter || zoneStyle.getPropertyValue('-webkit-backdrop-filter')) !== 'none'
    }
  })()`)).value).toEqual({
    title: 'JSON 工作台',
    logoBeforeTitle: true,
    logoVisible: true,
    logoLoaded: true,
    logoLeft: 20,
    titleLeft: 54,
    sameRow: true,
    headerTop: 18,
    controlsHidden: true,
    hasVisualOverlay: false
  })

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

test('aligns detached tool branding, header controls, and dock action in one top row', async () => {
  await mainPage.getByRole('button', { name: '随手记', exact: true }).click()
  await waitForToolSelector('quickNote', '.quick-note-tool')
  await evaluateTool('quickNote', `document.querySelector('.tool-window-toggle').click()`)
  await expect.poll(() => getToolSnapshot('quickNote')).toMatchObject({ detached: true, ready: true })

  await expect.poll(async () => (await evaluateTool('quickNote', `(() => {
    const bounds = (selector) => document.querySelector(selector)?.getBoundingClientRect()
    const header = bounds('.quick-note-page-header')
    const zone = bounds('.tool-window-brand-zone')
    const logo = bounds('.tool-window-brand')
    const title = bounds('.quick-note-page-header h1')
    const switcher = bounds('.quick-note-view-switch')
    const dock = bounds('.tool-window-toggle')
    if (!header || !zone || !logo || !title || !switcher || !dock) return null
    const centerY = (rect) => rect.top + rect.height / 2
    return {
      headerTop: Math.round(header.top),
      brandTop: Math.round(zone.top),
      dockTop: Math.round(dock.top),
      logoAligned: Math.abs(centerY(logo) - centerY(dock)) <= 2,
      titleAligned: Math.abs(centerY(title) - centerY(dock)) <= 2,
      switcherAligned: Math.abs(centerY(switcher) - centerY(dock)) <= 2,
      controlsHidden: document.querySelector('.tool-window-brand-zone').getAttribute('data-window-controls-visible') === 'false',
      logoLeft: Math.round(logo.left),
      brandDraggable: getComputedStyle(document.querySelector('.tool-window-brand-zone')).getPropertyValue('-webkit-app-region') === 'drag'
    }
  })()`)).value).toMatchObject({
    headerTop: 18,
    brandTop: 18,
    dockTop: 18,
    logoAligned: true,
    titleAligned: true,
    switcherAligned: true,
    controlsHidden: true,
    logoLeft: 20,
    brandDraggable: true
  })

  await sendToolWindowControlsVisibility('quickNote', true)
  await expect.poll(async () => (await evaluateTool('quickNote', `(() => {
    const zone = document.querySelector('.tool-window-brand-zone')
    const logo = document.querySelector('.tool-window-brand').getBoundingClientRect()
    const title = document.querySelector('.quick-note-page-header h1').getBoundingClientRect()
    return {
      controlsVisible: zone.getAttribute('data-window-controls-visible') === 'true',
      logoLeft: Math.round(logo.left),
      titleLeft: Math.round(title.left)
    }
  })()`)).value).toEqual({
    controlsVisible: process.platform === 'darwin',
    logoLeft: process.platform === 'darwin' ? 88 : 20,
    titleLeft: process.platform === 'darwin' ? 122 : 54
  })

  await sendToolWindowControlsVisibility('quickNote', false)
  await expect.poll(async () => (await evaluateTool('quickNote', `(() => {
    const zone = document.querySelector('.tool-window-brand-zone')
    const logo = document.querySelector('.tool-window-brand').getBoundingClientRect()
    return {
      controlsHidden: zone.getAttribute('data-window-controls-visible') === 'false',
      logoLeft: Math.round(logo.left)
    }
  })()`)).value).toEqual({ controlsHidden: true, logoLeft: 20 })

  await closeDetachedBaseWindow()
  await expect.poll(() => getToolSnapshot('quickNote')).toMatchObject({ detached: false, ready: true })
})

test('keeps Quick Note split-preview text selected when the docked view regains focus', async () => {
  await mainPage.getByRole('button', { name: '随手记', exact: true }).click()
  await waitForToolSelector('quickNote', '.quick-note-tool')
  await evaluateTool('quickNote', `(() => {
    const split = [...document.querySelectorAll('[role="tab"]')].find((tab) => tab.textContent === '分栏')
    if (!split) throw new Error('Quick Note split tab not found')
    split.click()
  })()`)
  await waitForToolSelector('quickNote', '.quick-note-preview h1')

  const result = await evaluateTool<{ selection: string }>('quickNote', `new Promise((resolve) => {
    const heading = document.querySelector('.quick-note-preview h1')
    if (!heading) throw new Error('Quick Note preview heading not found')
    const range = document.createRange()
    range.selectNodeContents(heading)
    const selection = window.getSelection()
    selection.removeAllRanges()
    selection.addRange(range)
    window.dispatchEvent(new Event('focus'))
    requestAnimationFrame(() => requestAnimationFrame(() => resolve({
      selection: window.getSelection()?.toString() ?? ''
    })))
  })`)
  expect(result.value.selection).toContain('MooTool Quick Note')
})

test('temporarily reveals main overlays above a docked tool', async () => {
  await mainPage.locator('.tool-button').filter({ hasText: '计算器' }).click()
  await waitForToolSelector('calculator', '.calculator-workspace')
  await expect.poll(() => getMainChildViewCount()).toBe(1)
  const beforeSearch = await evaluateTool<string>('calculator', `document.querySelector('#calculator-expression').value`)

  await mainPage.getByRole('button', { name: '搜索', exact: true }).click()
  await expect(mainPage.locator('.command-palette')).toBeVisible()
  await expect.poll(() => getMainChildViewCount()).toBe(0)

  await mainPage.getByRole('button', { name: '关闭搜索', exact: true }).click()
  await expect.poll(() => getMainChildViewCount()).toBe(1)
  const restored = await evaluateTool<string>('calculator', `document.querySelector('#calculator-expression').value`)
  expect(restored.value).toBe(beforeSearch.value)

  await mainPage.getByRole('button', { name: '管理分组', exact: true }).click()
  const groupDialog = mainPage.getByRole('dialog', { name: '管理功能分组' })
  await expect(groupDialog).toBeVisible()
  await expect.poll(() => getMainChildViewCount()).toBe(0)

  await groupDialog.getByRole('button', { name: '取消', exact: true }).click()
  await expect.poll(() => getMainChildViewCount()).toBe(1)
  const restoredAfterGroups = await evaluateTool<string>('calculator', `document.querySelector('#calculator-expression').value`)
  expect(restoredAfterGroups.value).toBe(beforeSearch.value)
})

test('reveals update notes above a docked tool view', async () => {
  await mainPage.locator('.tool-button').filter({ hasText: '计算器' }).click()
  await waitForToolSelector('calculator', '.calculator-workspace')
  await expect.poll(() => getMainChildViewCount()).toBe(1)

  await electronApp.evaluate(({ BrowserWindow }) => {
    BrowserWindow.getAllWindows().find((window) => !window.getParentWindow())?.webContents.send('update:state-changed', {
      status: 'ready',
      installMode: 'automatic',
      version: '9.9.9',
      fileName: 'MooTool-Next-Electron-9.9.9-test.bin',
      percent: 100,
      transferred: 100,
      total: 100,
      message: null,
      releaseNotes: '## 9.9.9\n- visible above the tool view'
    })
  })

  const updateAction = mainPage.locator('.sidebar-update-action')
  await expect(updateAction).toBeVisible()
  await updateAction.hover()
  await expect(mainPage.locator('.sidebar-update-notes')).toContainText('visible above the tool view')
  await expect.poll(() => getMainChildViewCount()).toBe(0)

  await mainPage.locator('.sidebar-actions').hover()
  await expect(mainPage.locator('.sidebar-update-notes')).toBeHidden()
  await expect.poll(() => getMainChildViewCount()).toBe(1)
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

async function sendToolWindowControlsVisibility(toolId: string, visible: boolean): Promise<void> {
  await electronApp.evaluate(({ webContents }, input) => {
    const contents = webContents.getAllWebContents().find((item) => {
      const url = new URL(item.getURL())
      return url.searchParams.get('window') === 'tool' && url.searchParams.get('toolId') === input.toolId
    })
    if (!contents) throw new Error(`Tool webContents not found: ${input.toolId}`)
    contents.send('tool-window:controls-visibility-changed', input.visible)
  }, { toolId, visible })
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

async function getDetachedWindowChrome(): Promise<{
  contentInsetTop: number
  contentInsetHeight: number
  windowButtonPosition: { x: number; y: number } | null
}> {
  return electronApp.evaluate(({ BaseWindow, BrowserWindow }) => {
    const browserWindowIds = new Set(BrowserWindow.getAllWindows().map((window) => window.id))
    const window = BaseWindow.getAllWindows().find((item) => !browserWindowIds.has(item.id))
    if (!window) throw new Error('Detached tool window not found')
    const bounds = window.getBounds()
    const contentBounds = window.getContentBounds()
    return {
      contentInsetTop: contentBounds.y - bounds.y,
      contentInsetHeight: bounds.height - contentBounds.height,
      windowButtonPosition: window.getWindowButtonPosition()
    }
  })
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
