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
    const shell = document.querySelector('.tool-view-shell')
    const dragZoneElement = document.querySelector('[data-window-drag-zone]')
    const dragZone = dragZoneElement?.getBoundingClientRect()
    const dragRail = document.querySelector('[data-window-drag-rail]')
    const slot = document.querySelector('.tool-window-toggle-slot').getBoundingClientRect()
    return shell?.classList.contains('tool-view-shell--immersive')
      && !dragRail
      && Boolean(dragZone && dragZone.width >= 48 && dragZone.height >= 24)
      && getComputedStyle(dragZoneElement).getPropertyValue('-webkit-app-region') === 'drag'
      && Math.round(slot.width) === 7
  })()`)).value).toBe(true)

  await clickToolWindowToggle('calculator')
  await expect.poll(() => getToolSnapshot('calculator')).toMatchObject({ detached: true, ready: true })
  await expect(mainPage.getByRole('heading', { name: '计算器 已在独立窗口中打开' })).toBeVisible()
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

test('opens and returns an inactive tool from its navigation shortcut', async () => {
  await mainPage.getByRole('button', { name: '主页', exact: true }).click()

  const qrButton = mainPage.getByRole('button', { name: '二维码', exact: true })
  const qrRow = qrButton.locator('..')
  await qrRow.hover()

  const detachShortcut = qrRow.getByRole('button', { name: '在独立窗口中打开“二维码”', exact: true })
  await expect(detachShortcut).toHaveCSS('pointer-events', 'auto')
  await detachShortcut.click()

  await expect.poll(() => getToolSnapshot('qrCode')).toMatchObject({ detached: true, ready: true })
  await expect(mainPage.getByRole('button', { name: '主页', exact: true })).toHaveAttribute('aria-current', 'page')

  const dockShortcut = qrRow.getByRole('button', { name: '将“二维码”收回到功能区', exact: true })
  await expect(dockShortcut).toHaveClass(/tool-button__window-action--detached/)
  await expect(dockShortcut).toHaveCSS('opacity', '1')
  await dockShortcut.click()

  await expect.poll(() => getToolSnapshot('qrCode')).toMatchObject({ detached: false, ready: true })
  await expect(mainPage.getByRole('button', { name: '主页', exact: true })).toHaveAttribute('aria-current', 'page')
})

test('keeps multiple detached tools independent and returns each one to its dock', async () => {
  await mainPage.getByRole('button', { name: 'JSON', exact: true }).click()
  await waitForToolSelector('json', '.json-tool')
  await evaluateTool('json', `document.querySelector('.tool-window-toggle').click()`)
  await expect.poll(() => getToolSnapshot('json')).toMatchObject({ detached: true, ready: true })
  await expect.poll(async () => (await evaluateTool('json', `(() => {
    const shell = document.querySelector('.tool-view-shell')
    const page = document.querySelector('.json-tool')
    const workspaceDrag = document.querySelector('[data-window-drag-zone]')
    const slot = document.querySelector('.tool-window-toggle-slot')
    const trigger = slot?.querySelector('.tooltip-trigger')
    if (!shell || !page || !workspaceDrag || !slot || !trigger) return null
    const pageBounds = page.getBoundingClientRect()
    const workspaceDragBounds = workspaceDrag.getBoundingClientRect()
    const slotBounds = slot.getBoundingClientRect()
    const triggerStyle = getComputedStyle(trigger)
    return {
      immersive: shell.classList.contains('tool-view-shell--immersive'),
      accessibleName: page.getAttribute('aria-label'),
      visibleTitleCount: page.querySelectorAll('h1').length,
      brandCount: shell.querySelectorAll('.tool-window-brand-zone').length,
      pageTop: Math.round(pageBounds.top),
      dragRailCount: shell.querySelectorAll('[data-window-drag-rail]').length,
      workspaceDragUsable: workspaceDragBounds.width >= 32 && workspaceDragBounds.height >= 24,
      workspaceDragRegion: getComputedStyle(workspaceDrag).getPropertyValue('-webkit-app-region'),
      edgeSlotWidth: Math.round(slotBounds.width),
      toggleOpacity: triggerStyle.opacity,
      togglePointerEvents: triggerStyle.pointerEvents
    }
  })()`)).value).toEqual({
    immersive: true,
    accessibleName: 'JSON 工作台',
    visibleTitleCount: 0,
    brandCount: 0,
    pageTop: 0,
    dragRailCount: 0,
    workspaceDragUsable: true,
    workspaceDragRegion: 'drag',
    edgeSlotWidth: 7,
    toggleOpacity: '0',
    togglePointerEvents: 'none'
  })

  await evaluateTool('json', `document.querySelector('.tool-window-toggle').focus()`)
  await expect.poll(async () => (await evaluateTool('json', `(() => {
    const slot = document.querySelector('.tool-window-toggle-slot')
    const trigger = slot?.querySelector('.tooltip-trigger')
    if (!slot || !trigger) return null
    return {
      edgeSlotWidth: Math.round(slot.getBoundingClientRect().width),
      toggleOpacity: getComputedStyle(trigger).opacity,
      togglePointerEvents: getComputedStyle(trigger).pointerEvents
    }
  })()`)).value).toEqual({ edgeSlotWidth: 52, toggleOpacity: '1', togglePointerEvents: 'auto' })
  await evaluateTool('json', `document.querySelector('.tool-window-toggle').blur()`)

  await mainPage.getByRole('button', { name: 'HTTP 请求', exact: true }).click()
  await waitForToolSelector('http', '.http-tool-page')
  await evaluateTool('http', `document.querySelector('.tool-window-toggle').click()`)
  await expect.poll(() => getToolSnapshot('http')).toMatchObject({ detached: true, ready: true })
  await expect.poll(async () => (await evaluateTool('http', `(() => {
    const drag = document.querySelector('.http-window-drag-zone')
    const timeout = document.querySelector('.http-timeout')
    const send = document.querySelector('[data-testid="http-send"]')
    if (!drag || !timeout || !send) return null
    const dragBounds = drag.getBoundingClientRect()
    const timeoutBounds = timeout.getBoundingClientRect()
    const sendBounds = send.getBoundingClientRect()
    return {
      width: Math.round(dragBounds.width),
      betweenControls: dragBounds.left >= timeoutBounds.right && dragBounds.right <= sendBounds.left,
      dragRegion: getComputedStyle(drag).getPropertyValue('-webkit-app-region')
    }
  })()`)).value).toEqual({ width: 36, betweenControls: true, dragRegion: 'drag' })
  await expect(mainPage.getByRole('heading', { name: 'HTTP 请求 已在独立窗口中打开' })).toBeVisible()
  await expect.poll(() => getBaseWindowCount()).toBe(3)

  await mainPage.getByRole('button', { name: '主页', exact: true }).click()
  await expect(mainPage.locator('.tool-button__window-action--detached')).toHaveCount(2)

  await closeAllDetachedBaseWindows()
  await expect.poll(() => getToolSnapshot('json')).toMatchObject({ detached: false, ready: true })
  await expect.poll(() => getToolSnapshot('http')).toMatchObject({ detached: false, ready: true })
  await expect.poll(() => getBaseWindowCount()).toBe(1)
})

test('keeps a custom-header tool immersive while preserving its view controls', async () => {
  await mainPage.getByRole('button', { name: '随手记', exact: true }).click()
  await waitForToolSelector('quickNote', '.quick-note-tool')
  await evaluateTool('quickNote', `document.querySelector('.tool-window-toggle').click()`)
  await expect.poll(() => getToolSnapshot('quickNote')).toMatchObject({ detached: true, ready: true })

  await expect.poll(async () => (await evaluateTool('quickNote', `(() => {
    const shell = document.querySelector('.tool-view-shell')
    const page = document.querySelector('.quick-note-tool')
    const toolbar = document.querySelector('.quick-note-toolbar')?.getBoundingClientRect()
    const title = document.querySelector('.quick-note-tool > h1')?.getBoundingClientRect()
    const switcher = document.querySelector('.quick-note-view-switch')?.getBoundingClientRect()
    const workspaceDrag = document.querySelector('[data-window-drag-zone]')?.getBoundingClientRect()
    const sidebar = document.querySelector('.quick-note-sidebar')?.getBoundingClientRect()
    const editor = document.querySelector('.quick-note-editor-shell')?.getBoundingClientRect()
    const slot = document.querySelector('.tool-window-toggle-slot')?.getBoundingClientRect()
    if (!shell || !page || !toolbar || !title || !switcher || !workspaceDrag || !sidebar || !editor || !slot) return null
    return {
      immersive: shell.classList.contains('tool-view-shell--immersive'),
      brandCount: shell.querySelectorAll('.tool-window-brand-zone').length,
      standaloneHeaderCount: page.querySelectorAll(':scope > .tool-page__header').length,
      pageTop: Math.round(page.getBoundingClientRect().top),
      toolbarTop: Math.round(toolbar.top),
      toolbarCompact: toolbar.height >= 48 && toolbar.height <= 54,
      titleWidth: Math.round(title.width),
      titleHeight: Math.round(title.height),
      switchInsideToolbar: switcher.top >= toolbar.top && switcher.bottom <= toolbar.bottom,
      firstColumnsGap: Math.round(editor.left - sidebar.right),
      dragRailCount: shell.querySelectorAll('[data-window-drag-rail]').length,
      workspaceDragUsable: workspaceDrag.width >= 32 && workspaceDrag.height >= 24,
      workspaceDragRegion: getComputedStyle(document.querySelector('[data-window-drag-zone]')).getPropertyValue('-webkit-app-region'),
      edgeSlotWidth: Math.round(slot.width)
    }
  })()`)).value).toMatchObject({
    immersive: true,
    brandCount: 0,
    standaloneHeaderCount: 0,
    pageTop: 0,
    toolbarTop: 0,
    toolbarCompact: true,
    titleWidth: 1,
    titleHeight: 1,
    switchInsideToolbar: true,
    firstColumnsGap: 0,
    dragRailCount: 0,
    workspaceDragUsable: true,
    workspaceDragRegion: 'drag',
    edgeSlotWidth: 7
  })

  await sendToolWindowControlsVisibility('quickNote', true)
  await expect.poll(async () => (await evaluateTool('quickNote', `(() => {
    return {
      controlsClass: document.querySelector('.tool-view-shell').classList.contains('tool-view-shell--window-controls-visible'),
      brandCount: document.querySelectorAll('.tool-window-brand-zone').length,
      toolbarTop: Math.round(document.querySelector('.quick-note-toolbar').getBoundingClientRect().top),
      searchInset: Math.round(document.querySelector('.quick-note-search').getBoundingClientRect().left - document.querySelector('.quick-note-sidebar').getBoundingClientRect().left)
    }
  })()`)).value).toEqual({
    controlsClass: true,
    brandCount: 0,
    toolbarTop: 0,
    searchInset: process.platform === 'darwin' ? 82 : 8
  })

  await sendToolWindowControlsVisibility('quickNote', false)
  await expect.poll(async () => (await evaluateTool('quickNote', `document.querySelector('.tool-view-shell').classList.contains('tool-view-shell--window-controls-visible')`)).value).toBe(false)

  await closeDetachedBaseWindow()
  await expect.poll(() => getToolSnapshot('quickNote')).toMatchObject({ detached: false, ready: true })
})

test('keeps double-clicked Quick Note split-preview text selected after auto-save', async () => {
  await mainPage.locator('.tool-button').filter({ hasText: '随手记' }).click()
  await waitForToolSelector('quickNote', '.quick-note-tool')
  await evaluateTool('quickNote', `(() => {
    const split = [...document.querySelectorAll('[role="tab"]')].find((tab) => tab.getAttribute('aria-label') === '分栏')
    if (!split) throw new Error('Quick Note split tab not found')
    split.click()
  })()`)
  await waitForToolSelector('quickNote', '.quick-note-preview h1')

  const doubleClickResult = await doubleClickToolText('quickNote', '.quick-note-preview h1', 2)
  expect(doubleClickResult).toBe('MooTool')
  await typeInToolTextEditor('quickNote', '!')
  const dirtyDoubleClickResult = await doubleClickToolText('quickNote', '.quick-note-preview h1', 2)
  expect(dirtyDoubleClickResult).toBe('MooTool')
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

  const windowCount = await getBaseWindowCount()
  await mainPage.getByRole('button', { name: '设置', exact: true }).click()
  await expect(mainPage.locator('.settings-page')).toBeVisible()
  await expect.poll(() => getMainChildViewCount()).toBe(0)
  await expect.poll(() => getBaseWindowCount()).toBe(windowCount)

  await mainPage.evaluate(() => window.mootool.openSettings('runtime'))
  await expect(mainPage.locator('.settings-nav__item').filter({ hasText: '运行环境' })).toHaveAttribute('aria-current', 'page')
  await mainPage.getByRole('button', { name: '返回工作区', exact: true }).click()
  await expect(mainPage.locator('.settings-page')).toBeHidden()
  await expect.poll(() => getMainChildViewCount()).toBe(1)
  const restoredAfterSettings = await evaluateTool<string>('calculator', `document.querySelector('#calculator-expression').value`)
  expect(restoredAfterSettings.value).toBe(beforeSearch.value)
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
  await expect(mainPage.locator('.update-notes-workspace-placeholder')).toContainText('正在查看本次更新')
  await expect(mainPage.locator('.update-notes-workspace-placeholder')).toContainText('计算器')
  await expect(mainPage.locator('.workspace-loading')).toHaveCount(0)

  await mainPage.locator('.sidebar-actions').hover()
  await expect(mainPage.locator('.sidebar-update-notes')).toBeHidden()
  await expect.poll(() => getMainChildViewCount()).toBe(1)
})

test('keeps message board presentation controls from overlapping', async () => {
  await mainPage.getByRole('button', { name: '留言板', exact: true }).click()
  await waitForToolSelector('messageBoard', '.message-board-tool')
  await evaluateTool('messageBoard', `(() => {
    const button = [...document.querySelectorAll('button')].find((item) => item.textContent?.includes('沉浸展示'))
    if (!button) throw new Error('Message board presentation button not found')
    button.click()
  })()`)

  await expect.poll(async () => (await evaluateTool('messageBoard', `(() => {
    const exit = document.querySelector('.message-board-stage__exit')?.getBoundingClientRect()
    const dock = document.querySelector('.tool-window-toggle')?.getBoundingClientRect()
    if (!exit || !dock) return null
    return {
      gap: Math.round(dock.left - exit.right),
      overlaps: exit.left < dock.right && exit.right > dock.left && exit.top < dock.bottom && exit.bottom > dock.top
    }
  })()`)).value).toMatchObject({ overlaps: false })
  await expect.poll(async () => (await evaluateTool<number>('messageBoard', `(() => {
    const exit = document.querySelector('.message-board-stage__exit')?.getBoundingClientRect()
    const dock = document.querySelector('.tool-window-toggle')?.getBoundingClientRect()
    return exit && dock ? Math.round(dock.left - exit.right) : -1
  })()`)).value).toBeGreaterThanOrEqual(12)
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

    const edgePoint = await contents.executeJavaScript(`(() => {
      const slot = document.querySelector('.tool-window-toggle-slot')
      if (!slot) throw new Error('Tool window toggle slot not found')
      const rect = slot.getBoundingClientRect()
      return { x: Math.max(0, Math.round(rect.right - 2)), y: Math.round(rect.top + rect.height / 2) }
    })()`)

    contents.focus()
    contents.sendInputEvent({ type: 'mouseMove', x: edgePoint.x, y: edgePoint.y })
    await new Promise((resolve) => setTimeout(resolve, 180))

    const point = await contents.executeJavaScript(`(() => {
      const button = document.querySelector('.tool-window-toggle')
      if (!button) throw new Error('Tool window toggle not found')
      const rect = button.getBoundingClientRect()
      return { x: Math.round(rect.left + rect.width / 2), y: Math.round(rect.top + rect.height / 2) }
    })()`)

    contents.sendInputEvent({ type: 'mouseMove', x: point.x, y: point.y })
    contents.sendInputEvent({ type: 'mouseDown', x: point.x, y: point.y, button: 'left', clickCount: 1 })
    contents.sendInputEvent({ type: 'mouseUp', x: point.x, y: point.y, button: 'left', clickCount: 1 })
  }, toolId)
}

async function doubleClickToolText(
  toolId: string,
  selector: string,
  offset: number
): Promise<string> {
  return electronApp.evaluate(async ({ webContents }, input) => {
    const contents = webContents.getAllWebContents().find((item) => {
      const url = new URL(item.getURL())
      return url.searchParams.get('window') === 'tool' && url.searchParams.get('toolId') === input.toolId
    })
    if (!contents) throw new Error(`Tool webContents not found: ${input.toolId}`)
    const point = await contents.executeJavaScript(`(() => {
      window.getSelection()?.removeAllRanges()
      const element = document.querySelector(${JSON.stringify(input.selector)})
      const text = element?.firstChild
      if (!text || text.nodeType !== Node.TEXT_NODE || !text.textContent) throw new Error('Tool selection text is unavailable')
      const range = document.createRange()
      range.setStart(text, ${input.offset})
      range.setEnd(text, ${input.offset + 1})
      const rect = range.getBoundingClientRect()
      return { x: Math.round(rect.left + rect.width / 2), y: Math.round(rect.top + rect.height / 2) }
    })()`)

    contents.focus()
    contents.sendInputEvent({ type: 'mouseMove', x: point.x, y: point.y })
    contents.sendInputEvent({ type: 'mouseDown', x: point.x, y: point.y, button: 'left', clickCount: 1 })
    contents.sendInputEvent({ type: 'mouseUp', x: point.x, y: point.y, button: 'left', clickCount: 1 })
    contents.sendInputEvent({ type: 'mouseDown', x: point.x, y: point.y, button: 'left', clickCount: 2 })
    contents.sendInputEvent({ type: 'mouseUp', x: point.x, y: point.y, button: 'left', clickCount: 2 })
    await new Promise((resolve) => setTimeout(resolve, 800))
    return contents.executeJavaScript(`window.getSelection()?.toString() ?? ''`) as Promise<string>
  }, { toolId, selector, offset })
}

async function typeInToolTextEditor(toolId: string, text: string): Promise<void> {
  await electronApp.evaluate(async ({ webContents }, input) => {
    const contents = webContents.getAllWebContents().find((item) => {
      const url = new URL(item.getURL())
      return url.searchParams.get('window') === 'tool' && url.searchParams.get('toolId') === input.toolId
    })
    if (!contents) throw new Error(`Tool webContents not found: ${input.toolId}`)
    await contents.executeJavaScript(`document.querySelector('.quick-note-code-editor .cm-content')?.focus()`)
    contents.sendInputEvent({ type: 'keyDown', keyCode: 'END' })
    contents.sendInputEvent({ type: 'keyUp', keyCode: 'END' })
    for (const character of input.text) contents.sendInputEvent({ type: 'char', keyCode: character })
    await new Promise((resolve) => setTimeout(resolve, 50))
  }, { toolId, text })
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
