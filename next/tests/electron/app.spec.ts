import { _electron as electron, expect, test, type ElectronApplication, type Locator, type Page } from '@playwright/test'
import { mkdtemp, mkdir, readdir, rm, writeFile } from 'node:fs/promises'
import { createServer, type Server } from 'node:http'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { DatabaseSync } from 'node:sqlite'

let electronApp: ElectronApplication
let mainPage: Page
let userDataDirectory: string
let updateServer: Server

test.beforeAll(async () => {
  userDataDirectory = await mkdtemp(join(tmpdir(), 'mootool-next-e2e-'))
  updateServer = createServer((_request, response) => {
    response.setHeader('content-type', 'application/json; charset=utf-8')
    response.end(JSON.stringify({
      schemaVersion: 1,
      products: {
        java: {
          displayName: 'MooTool Java',
          status: 'active',
          releases: [{ version: '99.0.0', title: 'Java only', notes: '', releaseUrl: 'https://example.test/java', assets: [] }]
        },
        'next-electron': {
          displayName: 'MooTool Next Electron',
          status: 'active',
          releases: [{
            version: 'v9.9.9',
            title: 'E2E update',
            notes: 'Update channel is working.',
            releaseUrl: 'https://example.test/next-electron-v9.9.9',
            assets: [{
              platform: process.platform,
              architecture: process.arch,
              packageType: 'test-installer',
              priority: 10,
              fileName: 'MooTool-Next-Electron-9.9.9-test.bin',
              url: 'https://example.test/MooTool-Next-Electron-9.9.9-test.bin',
              sha512: 'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==',
              size: 100
            }]
          }]
        }
      }
    }))
  })
  await new Promise<void>((resolve) => updateServer.listen(0, '127.0.0.1', resolve))
  const updateAddress = updateServer.address()
  if (!updateAddress || typeof updateAddress === 'string') throw new Error('Update test server did not expose a port')
  electronApp = await electron.launch({
    args: ['.', `--user-data-dir=${userDataDirectory}`],
    cwd: process.cwd(),
    env: { ...process.env, NODE_ENV: 'test', MOOTOOL_UPDATE_FEED_URL: `http://127.0.0.1:${updateAddress.port}/update-manifest.json` }
  })
  mainPage = await electronApp.firstWindow()
  await mainPage.waitForLoadState('domcontentloaded')
})

test.afterAll(async () => {
  await electronApp.close()
  await new Promise<void>((resolve, reject) => updateServer.close((error) => error ? reject(error) : resolve()))
  await rm(userDataDirectory, { recursive: true, force: true })
})

test('uses native translucency for the macOS sidebar only', async () => {
  const appearance = await mainPage.evaluate(() => {
    const alpha = (selector: string) => {
      const element = document.querySelector(selector)
      if (!element) throw new Error(`Missing ${selector}`)
      const canvas = document.createElement('canvas')
      canvas.width = 1
      canvas.height = 1
      const context = canvas.getContext('2d')
      if (!context) throw new Error('Canvas context is unavailable')
      context.fillStyle = getComputedStyle(element).backgroundColor
      context.fillRect(0, 0, 1, 1)
      return context.getImageData(0, 0, 1, 1).data[3]
    }

    return {
      platform: document.documentElement.dataset.platform,
      windowType: document.documentElement.dataset.window,
      bodyAlpha: alpha('body'),
      shellAlpha: alpha('.app-shell'),
      sidebarAlpha: alpha('.sidebar'),
      workspaceAlpha: alpha('.workspace')
    }
  })

  expect(appearance.platform).toBe(process.platform)
  expect(appearance.windowType).toBe('main')
  if (process.platform === 'darwin') {
    expect(appearance.bodyAlpha).toBe(0)
    expect(appearance.shellAlpha).toBe(0)
    expect(appearance.sidebarAlpha).toBeGreaterThan(0)
    expect(appearance.sidebarAlpha).toBeLessThan(255)
    expect(appearance.workspaceAlpha).toBe(255)
  } else {
    expect(appearance.bodyAlpha).toBe(255)
    expect(appearance.shellAlpha).toBe(255)
    expect(appearance.sidebarAlpha).toBe(255)
  }
})

test('matches the Java home content and persists sidebar collapse state', async () => {
  await expect.poll(() => mainPage.evaluate(() => window.mootool.getSettings())).toMatchObject({
    appearance: { accentColor: 'blue', fontSize: 13 },
    layout: { navigationStyle: 'classic' },
    editor: { quickNoteFontSize: 14 }
  })
  await expect(mainPage.locator('.app-shell')).toHaveClass(/app-shell--nav-classic/)
  await expect.poll(() => mainPage.evaluate(() => ({
    accent: getComputedStyle(document.documentElement).getPropertyValue('--accent').trim(),
    fontSize: getComputedStyle(document.documentElement).getPropertyValue('--app-font-size').trim()
  }))).toEqual({ accent: '#4f83cc', fontSize: '13px' })
  await expect(mainPage.getByRole('heading', { name: 'MooTool', exact: true })).toBeVisible()
  await expect(mainPage.getByRole('heading', { name: '关于', exact: true })).toBeVisible()
  await expect(mainPage.getByRole('heading', { name: '其他作品', exact: true })).toBeVisible()
  await expect(mainPage.getByRole('heading', { name: '贡献者', exact: true })).toBeVisible()
  await expect(mainPage.getByRole('button', { name: /CassianFlorin/ })).toBeVisible()

  await mainPage.getByRole('button', { name: '收起导航栏' }).click()
  await expect(mainPage.locator('.app-shell')).toHaveClass(/app-shell--hide-nav-titles/)
  await expect.poll(() => mainPage.locator('.sidebar').evaluate((element) => element.getBoundingClientRect().width)).toBe(84)
  await expect.poll(() => mainPage.evaluate(() => window.mootool.getSettings())).toMatchObject({ layout: { hideNavigationTitles: true } })

  await mainPage.getByRole('button', { name: '展开导航栏' }).click()
  await expect(mainPage.locator('.app-shell')).not.toHaveClass(/app-shell--hide-nav-titles/)
  await expect.poll(() => mainPage.evaluate(() => window.mootool.getSettings())).toMatchObject({ layout: { hideNavigationTitles: false } })
})

test('shows a first-run tip for migrating Java data without modifying the source', async () => {
  await mainPage.evaluate(() => window.mootool.updateSettings({ general: { legacyMigrationHintDismissed: false } }))
  const dialog = mainPage.getByRole('dialog', { name: '迁移 Java 版数据' })
  await expect(dialog).toBeVisible()
  await expect(dialog.getByText(/不会修改或删除 Java 版原始数据/)).toBeVisible()

  const settingsWindowPromise = electronApp.waitForEvent('window')
  await dialog.getByRole('button', { name: '打开设置' }).click()
  const settingsPage = await settingsWindowPromise
  await settingsPage.waitForLoadState('domcontentloaded')
  await expect(settingsPage.locator('.settings-nav__item').filter({ hasText: '数据与备份' })).toHaveAttribute('aria-current', 'page')
  await expect(settingsPage.getByRole('heading', { name: '旧版数据迁移', exact: true })).toBeVisible()
  await settingsPage.locator('.settings-titlebar .icon-ghost').click()

  await expect(dialog).toBeHidden()
  await expect.poll(() => mainPage.evaluate(() => window.mootool.getSettings())).toMatchObject({
    general: { legacyMigrationHintDismissed: true }
  })
})

test('creates and persists custom navigation groups', async () => {
  await mainPage.getByRole('button', { name: '管理分组', exact: true }).click()
  const dialog = mainPage.getByRole('dialog', { name: '管理功能分组' })
  await expect(dialog).toBeVisible()
  await dialog.getByRole('button', { name: '新建分组', exact: true }).click()
  await dialog.getByLabel('分组名称').fill('开发常用')
  await dialog.getByLabel('JSON', { exact: true }).check()
  await dialog.getByLabel('HTTP 请求', { exact: true }).check()
  await dialog.getByRole('button', { name: '保存', exact: true }).click()

  const customGroup = mainPage.locator('.tool-group--custom').filter({ hasText: '开发常用' })
  await expect(customGroup).toBeVisible()
  await expect(customGroup.locator('.tool-button')).toHaveCount(2)
  await expect.poll(() => mainPage.evaluate(() => window.mootool.getSettings())).toMatchObject({
    schemaVersion: 11,
    layout: { customGroups: [{ name: '开发常用', toolIds: ['json', 'http'] }] }
  })

  await mainPage.reload()
  await mainPage.waitForLoadState('domcontentloaded')
  await expect(mainPage.locator('.tool-group--custom').filter({ hasText: '开发常用' })).toBeVisible()
  await mainPage.getByRole('button', { name: '收起导航栏' }).click()
  await expect(mainPage.locator('.tool-group--custom')).toBeHidden()
  await mainPage.getByRole('button', { name: '展开导航栏' }).click()

  await mainPage.getByRole('button', { name: '管理分组', exact: true }).click()
  await dialog.locator('.custom-group-manager__group').filter({ hasText: '开发常用' }).click()
  mainPage.once('dialog', (confirmation) => confirmation.accept())
  await dialog.getByRole('button', { name: '删除分组', exact: true }).click()
  await dialog.getByRole('button', { name: '保存', exact: true }).click()
  await expect(mainPage.locator('.tool-group--custom')).toHaveCount(0)
  await expect.poll(() => mainPage.evaluate(() => window.mootool.getSettings())).toMatchObject({ layout: { customGroups: [] } })
})

test('sizes compact controls from their localized English content', async () => {
  const workspace = await mainPage.evaluate(() => window.mootool.getWorkspaceState())
  await mainPage.evaluate(() => window.mootool.updateSettings({ general: { language: 'en-US' } }))

  try {
    const languageSelect = mainPage.locator('.language-menu select')
    await expect(languageSelect).toHaveValue('en-US')
    await expectSelectedOptionToFit(languageSelect)

    await openTool('Quick Note', 'Quick Note')
    const sortSelect = mainPage.getByLabel('Sort')
    await expect(sortSelect).toHaveValue('modified')
    await expectSelectedOptionToFit(sortSelect)

    const viewTabs = mainPage.locator('.quick-note-view-switch .segmented__item')
    await expect(viewTabs).toHaveCount(3)
    await expect.poll(() => viewTabs.evaluateAll((tabs) => tabs.every((tab) => tab.scrollWidth <= tab.clientWidth))).toBe(true)

    await openTool('Translation', 'Translation')
    const sourceLanguage = mainPage.getByLabel('Source language')
    await sourceLanguage.selectOption('zh-CN')
    await expectSelectedOptionToFit(sourceLanguage)
  } finally {
    await mainPage.evaluate(async (previousWorkspace) => {
      await window.mootool.updateSettings({ general: { language: 'zh-CN' } })
      await window.mootool.setWorkspaceState(previousWorkspace)
    }, workspace)
    await mainPage.reload()
    await mainPage.waitForLoadState('domcontentloaded')
    await expect(mainPage.locator('.language-menu select')).toHaveValue('zh-CN')
  }
})

test('opens all registered tools through search and persists recent access', async () => {
  await expect(mainPage.locator('.tool-button')).toHaveCount(25)

  await mainPage.getByRole('button', { name: '搜索', exact: true }).click()
  const searchInput = mainPage.locator('.command-palette__search input')
  await searchInput.fill('python')
  await expect(mainPage.locator('.command-result')).toHaveCount(1)
  await expect(mainPage.locator('.command-result')).toContainText('代码运行')
  await mainPage.locator('.command-result').click()

  await expect(mainPage.locator('.runtime-tool h1')).toHaveText('代码运行')
  await expect(mainPage.locator('.recent-item').first()).toContainText('代码运行')
  const recentToggle = mainPage.getByRole('button', { name: '最近', exact: true })
  await expect(recentToggle).toHaveAttribute('aria-expanded', 'true')
  await recentToggle.click()
  await expect(recentToggle).toHaveAttribute('aria-expanded', 'false')
  await expect(mainPage.locator('.recent-list')).toHaveCount(0)
  await recentToggle.click()
  await expect(recentToggle).toHaveAttribute('aria-expanded', 'true')
  await expect(mainPage.locator('.recent-item').first()).toContainText('代码运行')
  const workspace = await mainPage.evaluate(() => window.mootool.getWorkspaceState())
  expect(workspace).toEqual({ activeToolId: 'java', recentToolIds: ['java'] })
})

test('formats JSON and completes history and Vault workflows', async () => {
  await mainPage.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  await expect.poll(() => mainPage.locator('.workspace-tool-session:not([hidden]) .tool-page').evaluate((element) => {
    const style = getComputedStyle(element)
    return { top: style.paddingTop, bottom: style.paddingBottom }
  })).toEqual({ top: '20px', bottom: '20px' })
  const jsonVault = mainPage.locator('.vault-panel')
  await expect(mainPage.locator('.vault-panel__header h2')).toHaveCSS('font-size', '14px')
  await expect(mainPage.locator('.vault-panel__options > select')).toHaveCSS('font-size', '12px')
  await expect.poll(() => mainPage.locator('.vault-panel__options > select').evaluate((element) => element.getBoundingClientRect().height)).toBe(34)
  await expect.poll(() => mainPage.locator('.vault-panel__actions button').first().evaluate((button) => {
    const icon = button.querySelector('svg')
    return {
      button: button.getBoundingClientRect().width,
      icon: icon?.getBoundingClientRect().width
    }
  })).toEqual({ button: 32, icon: 16 })
  await expect.poll(() => mainPage.locator('.vault-panel__actions').evaluate((actions) => {
    const actionsRect = actions.getBoundingClientRect()
    const panelRect = actions.closest('.vault-panel')!.getBoundingClientRect()
    return actionsRect.left >= panelRect.left && actionsRect.right <= panelRect.right
  })).toBe(true)
  const initialVaultWidth = await jsonVault.evaluate((element) => element.getBoundingClientRect().width)
  const firstDivider = mainPage.locator('.json-layout .pane-resizer').first()
  await expect.poll(() => firstDivider.evaluate((element) => getComputedStyle(element, '::after').opacity)).toBe('0')
  await firstDivider.hover()
  await expect.poll(() => firstDivider.evaluate((element) => getComputedStyle(element, '::after').opacity)).toBe('0.5')
  const dividerBounds = await firstDivider.boundingBox()
  expect(dividerBounds).not.toBeNull()
  await mainPage.mouse.move(dividerBounds!.x + dividerBounds!.width / 2, dividerBounds!.y + 80)
  await mainPage.mouse.down()
  await mainPage.mouse.move(dividerBounds!.x + dividerBounds!.width / 2 + 70, dividerBounds!.y + 80)
  await mainPage.mouse.up()
  await expect.poll(() => jsonVault.evaluate((element) => element.getBoundingClientRect().width)).toBeGreaterThan(initialVaultWidth + 50)
  const resizedVaultWidth = await jsonVault.evaluate((element) => element.getBoundingClientRect().width)
  await expect.poll(() => mainPage.evaluate(() => window.mootool.getSettings())).toMatchObject({
    layout: { paneSizes: { 'json-three-pane': expect.any(Array) } }
  })
  await mainPage.getByRole('button', { name: '主页', exact: true }).click()
  await mainPage.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  await expect.poll(() => jsonVault.evaluate((element) => element.getBoundingClientRect().width)).toBeCloseTo(resizedVaultWidth, 0)

  const editor = mainPage.locator('.json-editor .cm-content')
  await editor.fill('{"b":1,"a":2}')
  await mainPage.locator('.editor-toolbar').getByRole('button', { name: '格式化', exact: true }).click()
  await expect.poll(() => editor.evaluate((element) => (element as HTMLElement).innerText)).toBe('{\n  "b": 1,\n  "a": 2\n}')
  await expect(mainPage.locator('.json-editor .cm-activeLine')).toHaveCount(1)
  await expect(mainPage.locator('.json-editor .cm-lineNumbers .cm-activeLineGutter')).toHaveCount(1)
  await expect(mainPage.locator('.json-editor .cm-foldGutter')).toHaveCount(1)

  await mainPage.locator('.editor-toolbar').getByRole('button', { name: '查找', exact: true }).click()
  const findInput = mainPage.getByRole('textbox', { name: '查找', exact: true })
  await findInput.fill('"a"')
  await expect(mainPage.locator('.json-editor .cm-searchMatch')).toHaveCount(1)
  await editor.focus()
  await mainPage.keyboard.press('ControlOrMeta+End')
  await mainPage.keyboard.press('Shift+ArrowLeft')

  await mainPage.getByRole('button', { name: '主页', exact: true }).click()
  await mainPage.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  await expect(findInput).toHaveValue('"a"')
  await editor.focus()
  await mainPage.keyboard.type('}')
  await expect.poll(() => editor.evaluate((element) => (element as HTMLElement).innerText)).toBe(`{
  "b": 1,
  "a": 2
}`)
  await expect(mainPage.locator('.json-editor .cm-searchMatch')).toHaveCount(1)

  await mainPage.evaluate(() => window.mootool.updateSettings({ editor: { jsonFontSize: 22 } }))
  await expect(mainPage.locator('.json-editor')).toHaveCSS('font-size', '22px')
  const jsonLineOffsets = await editorLineTopOffsets(mainPage, '.json-editor')
  expect(jsonLineOffsets).toEqual(expect.arrayContaining([expect.any(Number)]))
  expect(Math.max(...jsonLineOffsets)).toBeLessThan(1.5)
  await mainPage.evaluate(() => window.mootool.updateSettings({ editor: { jsonFontSize: 14 } }))

  await mainPage.getByRole('button', { name: '历史', exact: true }).click()
  await expect(mainPage.getByRole('dialog', { name: '历史记录' })).toBeVisible()
  await expect(mainPage.locator('.history-item')).toHaveCount(1)
  await mainPage.locator('.dialog__footer').getByRole('button', { name: '关闭' }).click()

  await mainPage.getByRole('button', { name: '新建片段' }).click()
  await mainPage.getByLabel('文件名或相对路径').fill('e2e-sample')
  await mainPage.getByRole('button', { name: '创建', exact: true }).click()
  const vaultNode = mainPage.locator('.vault-node').filter({ hasText: 'e2e-sample.json' })
  await expect(vaultNode).toBeVisible()
  await expect(vaultNode).toHaveCSS('font-size', '13px')
  await expect.poll(() => vaultNode.locator('svg').evaluate((icon) => icon.getBoundingClientRect().width)).toBe(16)

  await editor.fill('{"saved":true}')
  await mainPage.getByRole('button', { name: '保存片段' }).click()
  const vaultFile = await mainPage.evaluate(() => window.mootool.readJsonVaultFile('e2e-sample.json'))
  expect(vaultFile.content).toBe('{"saved":true}')

  mainPage.once('dialog', (dialog) => dialog.accept())
  await mainPage.getByRole('button', { name: '删除片段' }).click()
  await expect(mainPage.locator('.vault-node').filter({ hasText: 'e2e-sample.json' })).toHaveCount(0)
})

test('manages JSON Vault folders, rename, duplicate, and move workflows', async () => {
  await mainPage.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  await mainPage.getByRole('button', { name: '新建文件夹' }).click()
  await mainPage.getByLabel('文件夹相对路径').fill('archive')
  await mainPage.getByRole('button', { name: '创建', exact: true }).click()
  await expect(mainPage.locator('.vault-node--directory').filter({ hasText: 'archive' })).toBeVisible()

  await mainPage.getByRole('button', { name: '新建片段' }).click()
  await mainPage.getByLabel('文件名或相对路径').fill('archive/advanced')
  await mainPage.getByRole('button', { name: '创建', exact: true }).click()
  await expect(mainPage.locator('.vault-node').filter({ hasText: 'advanced.json' })).toBeVisible()

  const more = mainPage.locator('.vault-more-menu')
  await more.locator('summary').click()
  await more.getByRole('button', { name: '重命名' }).click()
  await mainPage.getByLabel('新名称').fill('renamed')
  await mainPage.getByRole('button', { name: '保存', exact: true }).click()
  await expect(mainPage.locator('.vault-node').filter({ hasText: 'renamed.json' })).toBeVisible()

  await more.getByRole('button', { name: '复制片段' }).click()
  await expect(mainPage.locator('.vault-node').filter({ hasText: 'renamed Copy.json' })).toBeVisible()
  await more.getByRole('button', { name: '移动' }).click()
  await mainPage.getByLabel('目标文件夹').selectOption('')
  await mainPage.getByRole('button', { name: '保存', exact: true }).click()
  await expect(mainPage.locator('.vault-tree > div > .vault-node').filter({ hasText: 'renamed Copy.json' })).toBeVisible()

  mainPage.once('dialog', (dialog) => dialog.accept())
  await mainPage.getByRole('button', { name: '删除片段' }).click()
  await expect(mainPage.locator('.vault-node').filter({ hasText: 'renamed Copy.json' })).toHaveCount(0)
})

test('restores the JSON Vault expanded folder and selected file after switching tools', async () => {
  await mainPage.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  await mainPage.getByRole('button', { name: '新建文件夹' }).click()
  await mainPage.getByLabel('文件夹相对路径').fill('session-state')
  await mainPage.getByRole('button', { name: '创建', exact: true }).click()
  await mainPage.getByRole('button', { name: '新建片段' }).click()
  await mainPage.getByLabel('文件名或相对路径').fill('session-state/selected')
  await mainPage.getByRole('button', { name: '创建', exact: true }).click()
  await expect(mainPage.locator('.vault-node--selected').filter({ hasText: 'selected.json' })).toBeVisible()

  await mainPage.getByRole('button', { name: '主页', exact: true }).click()
  await mainPage.locator('.tool-button').filter({ hasText: 'JSON' }).click()

  await expect(mainPage.locator('.vault-node--directory').filter({ hasText: 'session-state' })).toBeVisible()
  await expect(mainPage.locator('.vault-node--selected').filter({ hasText: 'selected.json' })).toBeVisible()
})

test('initializes the JSON Vault Git repository and commits a snippet', async () => {
  await mainPage.evaluate(() => window.mootool.updateSettings({ vault: { autoCommit: false } }))
  await mainPage.getByRole('button', { name: '打开 Git 面板' }).click()
  const gitDialog = mainPage.getByRole('dialog', { name: 'JSON Vault Git' })
  await expect(gitDialog).toBeVisible()
  const initButton = gitDialog.getByRole('button', { name: '初始化 Git' })
  await expect(initButton).toBeVisible()
  await initButton.click()
  await expect(gitDialog.getByText(/^分支 /)).toBeVisible()
  await gitDialog.getByRole('button', { name: '关闭' }).click()

  await mainPage.getByRole('button', { name: '新建片段' }).click()
  await mainPage.getByLabel('文件名或相对路径').fill('git-sample')
  await mainPage.getByRole('button', { name: '创建', exact: true }).click()

  await mainPage.getByRole('button', { name: '打开 Git 面板' }).click()
  await expect(gitDialog).toBeVisible()
  await expect(gitDialog.locator('.git-list-item').filter({ hasText: 'git-sample.json' })).toBeVisible()

  await gitDialog.getByRole('button', { name: '提交全部变更' }).click()
  await gitDialog.getByRole('tab', { name: '提交历史' }).click()
  await expect(gitDialog.locator('.git-list-item').filter({ hasText: 'MooTool JSON checkpoint' }).first()).toBeVisible()
  await gitDialog.getByRole('button', { name: '关闭' }).click()
})

test('opens the settings window and synchronizes appearance changes', async () => {
  const settingsWindowPromise = electronApp.waitForEvent('window')
  await mainPage.locator('.sidebar-footer .icon-ghost').click()
  const settingsPage = await settingsWindowPromise
  await settingsPage.waitForLoadState('domcontentloaded')

  await expect(settingsPage.locator('.settings-nav__item')).toHaveCount(11)
  const trayToggle = settingsPage.getByRole('switch', { name: '启用系统托盘' })
  const autoDownloadToggle = settingsPage.getByRole('switch', { name: '自动静默下载新版' })
  await expect(autoDownloadToggle).toHaveAttribute('aria-checked', 'true')
  await autoDownloadToggle.click()
  await expect.poll(() => settingsPage.evaluate(() => window.mootool.getSettings())).toMatchObject({ general: { autoDownloadUpdates: false } })
  await autoDownloadToggle.click()
  await expect(trayToggle).toHaveAttribute('aria-checked', 'true')
  await trayToggle.click()
  await expect.poll(() => settingsPage.evaluate(() => window.mootool.getSettings())).toMatchObject({ general: { trayEnabled: false } })
  await trayToggle.click()
  await settingsPage.getByRole('button', { name: '隐藏', exact: true }).click()
  await expect.poll(() => settingsPage.evaluate(() => window.mootool.getSettings())).toMatchObject({ general: { trayEnabled: true, closeBehavior: 'hide' } })
  await settingsPage.getByRole('button', { name: '每次询问', exact: true }).click()
  await settingsPage.locator('.settings-nav__item').filter({ hasText: '外观' }).click()
  await expect(mainPage.locator('html')).toHaveAttribute('data-interface-style', 'modern')
  await settingsPage.getByRole('button', { name: '安静主题', exact: true }).click()
  await expect(settingsPage.locator('html')).toHaveAttribute('data-interface-style', 'quiet')
  await expect(mainPage.locator('html')).toHaveAttribute('data-interface-style', 'quiet')
  await expect.poll(() => mainPage.evaluate(() => window.mootool.getSettings())).toMatchObject({ appearance: { interfaceStyle: 'quiet' } })
  await settingsPage.getByRole('button', { name: '现代主题', exact: true }).click()
  await expect(mainPage.locator('html')).toHaveAttribute('data-interface-style', 'modern')
  await settingsPage.getByRole('button', { name: '浅色' }).click()
  await expect(mainPage.locator('html')).toHaveAttribute('data-theme', 'light')

  await settingsPage.locator('.settings-nav__item').filter({ hasText: '布局与习惯' }).click()
  const jsonNavigationToggle = settingsPage.getByRole('checkbox', { name: 'JSON', exact: true })
  await expect(jsonNavigationToggle).toBeChecked()
  await jsonNavigationToggle.uncheck()
  await expect.poll(() => settingsPage.evaluate(() => window.mootool.getSettings())).toMatchObject({ layout: { hiddenNavigationToolIds: ['json'] } })
  await expect(mainPage.getByRole('button', { name: 'JSON', exact: true })).toHaveCount(0)

  await settingsPage.locator('.settings-nav__item').filter({ hasText: '运行环境' }).click()
  const runtimeGroup = settingsPage.locator('.runtime-settings-group')
  await expect(runtimeGroup.locator('.runtime-row')).toHaveCount(4)
  await expect.poll(() => runtimeGroup.evaluate((group) => {
    const header = group.querySelector(':scope > header')!.getBoundingClientRect()
    const rows = group.querySelector('.settings-group__rows')!.getBoundingClientRect()
    const firstRow = group.querySelector('.runtime-row')!
    const firstRowStyle = getComputedStyle(firstRow)
    return {
      headerGap: rows.top - header.bottom,
      rowPaddingLeft: firstRowStyle.paddingLeft,
      rowPaddingRight: firstRowStyle.paddingRight
    }
  })).toEqual({ headerGap: 8, rowPaddingLeft: '14px', rowPaddingRight: '14px' })
  await settingsPage.getByRole('button', { name: '重新检测', exact: true }).click()
  await expect(settingsPage.getByRole('button', { name: '重新检测', exact: true })).toBeEnabled()

  const initialSecretStatus = await settingsPage.evaluate(() => window.mootool.getSecretStatus('proxyPassword'))
  if (initialSecretStatus.encryptionAvailable) {
    const savedStatus = await settingsPage.evaluate(() => window.mootool.setSecret('proxyPassword', 'e2e-secret'))
    expect(savedStatus.stored).toBe(true)
    const clearedStatus = await settingsPage.evaluate(() => window.mootool.clearSecret('proxyPassword'))
    expect(clearedStatus.stored).toBe(false)
  }

  await settingsPage.locator('.settings-nav__item').filter({ hasText: '关于与更新' }).click()
  await expect(settingsPage.locator('.settings-about')).toContainText('版本 1.0.0')
  await settingsPage.getByRole('button', { name: '检查更新', exact: true }).click()
  await expect(settingsPage.locator('.settings-update-result')).toContainText('发现新版本 9.9.9')
  await expect(settingsPage.locator('.settings-update-result')).toContainText('MooTool Next Electron')
  await expect(settingsPage.getByRole('button', { name: '下载更新', exact: true })).toBeVisible()
  const update = await settingsPage.evaluate(() => window.mootool.checkForUpdates())
  expect(update).toMatchObject({ productId: 'next-electron', latestVersion: '9.9.9' })
  expect(update.download?.fileName).toBe('MooTool-Next-Electron-9.9.9-test.bin')
  await expect.poll(() => settingsPage.evaluate(() => window.mootool.getUpdateState())).toMatchObject({ status: 'available', version: '9.9.9' })

  await settingsPage.locator('.settings-titlebar .icon-ghost').click()

  await mainPage.getByRole('button', { name: '搜索', exact: true }).click()
  await mainPage.locator('.command-palette__search input').fill('json')
  await expect(mainPage.locator('.command-result')).toContainText('JSON')
  await mainPage.getByRole('button', { name: '关闭搜索', exact: true }).click()
  await mainPage.evaluate(() => window.mootool.updateSettings({ layout: { hiddenNavigationToolIds: [] } }))
  await expect(mainPage.getByRole('button', { name: 'JSON', exact: true })).toBeVisible()

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
      releaseNotes: '## 9.9.9\n- hover notes'
    })
  })
  await expect(mainPage.getByRole('button', { name: /安装并重启/ })).toContainText('新版本 9.9.9 已就绪')
  await mainPage.locator('.sidebar-update-action').hover()
  await expect(mainPage.locator('.sidebar-update-notes')).toContainText('更新内容')
  await expect(mainPage.locator('.sidebar-update-notes')).toContainText('hover notes')
  await electronApp.evaluate(({ BrowserWindow }) => {
    BrowserWindow.getAllWindows().find((window) => !window.getParentWindow())?.webContents.send('update:state-changed', {
      status: 'ready',
      installMode: 'manual',
      version: '9.9.9',
      fileName: 'MooTool-Next-Electron-9.9.9-mac-arm64.dmg',
      percent: 100,
      transferred: 100,
      total: 100,
      message: null,
      releaseNotes: null
    })
  })
  await expect(mainPage.getByRole('button', { name: /更新已下载，打开 DMG 安装/ })).toContainText('新版本 9.9.9 已就绪')
  await electronApp.evaluate(({ BrowserWindow }) => {
    BrowserWindow.getAllWindows().find((window) => !window.getParentWindow())?.webContents.send('update:state-changed', {
      status: 'idle',
      installMode: 'automatic',
      version: null,
      fileName: null,
      percent: null,
      transferred: null,
      total: null,
      message: null,
      releaseNotes: null
    })
  })
  await expect(mainPage.locator('.sidebar-update-action')).toHaveCount(0)
})

test('runs P3 time, encode, UA, calculator and config workflows', async () => {
  await openTool('时间转换', '时间转换')
  await mainPage.locator('#timestamp-input').fill('0')
  await mainPage.locator('#time-zone').selectOption('Asia/Shanghai')
  await mainPage.getByRole('button', { name: '转为本地时间' }).click()
  await expect(mainPage.locator('#local-time-input')).toHaveValue('1970-01-01 08:00:00')
  await mainPage.getByRole('button', { name: '大屏时钟' }).click()
  await expect(mainPage.locator('.clock-overlay')).toBeVisible()
  await mainPage.getByRole('button', { name: '退出大屏时钟' }).click()

  await openTool('编码解码', '编码解码')
  const encodePanes = mainPage.locator('.workspace-tool-session:not([hidden]) .io-workspace .cm-content')
  await encodePanes.nth(0).fill('Moo 工具')
  await expectTextEditorChrome(mainPage.locator('.workspace-tool-session:not([hidden]) .io-workspace .text-code-editor').nth(0))
  await mainPage.getByRole('button', { name: '转为 Unicode' }).click()
  await expect(encodePanes.nth(1)).toHaveText('Moo \\u5de5\\u5177')
  await mainPage.getByRole('tab', { name: 'URL 转码' }).click()
  await encodePanes.nth(0).fill('你好 a/b')
  await mainPage.getByRole('button', { name: 'URL 编码' }).click()
  await expect(encodePanes.nth(1)).toHaveText('%E4%BD%A0%E5%A5%BD%20a%2Fb')

  await openTool('UA 分析', 'UA 分析')
  await expectTextEditorChrome(mainPage.locator('.ua-input-panel .text-code-editor'))
  await mainPage.getByRole('button', { name: '解析', exact: true }).click()
  await expect(mainPage.locator('.ua-result-row').filter({ hasText: '浏览器' }).first()).toContainText('Chrome')

  await openTool('计算器', '计算器')
  await mainPage.locator('#calculator-expression').fill('(12 + 8) / 4')
  await mainPage.getByRole('button', { name: '计算', exact: true }).click()
  await expect(mainPage.locator('.calculator-expression output')).toHaveText('5')
  await mainPage.getByRole('button', { name: '最大公约数' }).click()
  await expect(mainPage.locator('.calculator-expression output')).toHaveText('6')

  await openTool('配置文件转换', '配置文件转换')
  const configPanes = mainPage.locator('.workspace-tool-session:not([hidden]) .io-workspace .cm-content')
  await configPanes.nth(0).fill('server.port=8080\napp.name=MooTool')
  await expectTextEditorChrome(mainPage.locator('.workspace-tool-session:not([hidden]) .io-workspace .text-code-editor').nth(0))
  await mainPage.getByRole('button', { name: '转为 YAML' }).click()
  await expect(configPanes.nth(1)).toContainText('server:')
  await mainPage.getByRole('tab', { name: 'YAML 校验' }).click()
  await mainPage.getByRole('button', { name: '校验', exact: true }).click()
  await expect(mainPage.locator('.validation-output')).toContainText('YAML 格式有效')
})

test('runs P3 regex, Cron and text Diff workflows with persistent favorites', async () => {
  await openTool('正则', '正则表达式')
  await mainPage.locator('#regex-expression').fill('(moo)(\\d+)')
  await mainPage.locator('.regex-source .cm-content').fill('moo1 moo22')
  await expectTextEditorChrome(mainPage.locator('.regex-source .text-code-editor'))
  await mainPage.getByRole('button', { name: '匹配测试', exact: true }).click()
  await expect(mainPage.locator('.regex-results article')).toHaveCount(2)
  await mainPage.getByRole('button', { name: '收藏夹' }).click()
  const regexFavorites = mainPage.getByRole('dialog', { name: '收藏夹' })
  await regexFavorites.getByLabel('名称').fill('E2E regex')
  await regexFavorites.getByRole('button', { name: '收藏当前内容' }).click()
  await expect(regexFavorites.locator('.favorite-item')).toContainText('E2E regex')
  await regexFavorites.getByRole('button', { name: '关闭' }).click()

  await openTool('Cron', 'Cron 表达式')
  await mainPage.getByRole('button', { name: '每小时' }).click()
  await mainPage.getByRole('button', { name: '解析运行时间' }).click()
  await expect(mainPage.locator('.cron-runs li')).toHaveCount(10)
  await mainPage.getByRole('button', { name: '收藏夹' }).click()
  const cronFavorites = mainPage.getByRole('dialog', { name: '收藏夹' })
  await cronFavorites.getByLabel('名称').fill('E2E hourly')
  await cronFavorites.getByRole('button', { name: '收藏当前内容' }).click()
  await expect(cronFavorites.locator('.favorite-item')).toContainText('E2E hourly')
  await cronFavorites.getByRole('button', { name: '关闭' }).click()

  await openTool('文本对比', '文本对比')
  const diffEditors = mainPage.locator('.diff-editor-grid .cm-content')
  await diffEditors.nth(0).fill('one\ntwo\n')
  await diffEditors.nth(1).fill('one\nthree\n')
  await expectTextEditorChrome(mainPage.locator('.diff-editor-grid .text-code-editor').nth(1))
  await mainPage.getByRole('button', { name: '对比', exact: true }).click()
  await expect(mainPage.locator('.diff-editor-grid .cm-diff-character-changed')).toHaveCount(2)
  await diffEditors.nth(1).focus()
  await mainPage.keyboard.press('ControlOrMeta+A')
  await diffEditors.nth(1).evaluate((element) => {
    const clipboard = new DataTransfer()
    clipboard.setData('text/plain', 'one\ntwo\n')
    element.dispatchEvent(new ClipboardEvent('paste', { bubbles: true, cancelable: true, clipboardData: clipboard }))
  })
  await mainPage.evaluate(() => new Promise<void>((resolve) => requestAnimationFrame(() => resolve())))
  expect(await mainPage.locator('.diff-status').textContent()).toContain('共发现 0 处差异')
  await expect(mainPage.locator('.diff-status')).toContainText('共发现 0 处差异')
  await expect(mainPage.locator('.diff-editor-grid [class*="cm-diff-"]')).toHaveCount(0)
  await diffEditors.nth(1).focus()
  await mainPage.keyboard.press('ControlOrMeta+A')
  await diffEditors.nth(1).evaluate((element) => {
    const clipboard = new DataTransfer()
    clipboard.setData('text/plain', 'one\nthree\n')
    element.dispatchEvent(new ClipboardEvent('paste', { bubbles: true, cancelable: true, clipboardData: clipboard }))
  })
  await mainPage.evaluate(() => new Promise<void>((resolve) => requestAnimationFrame(() => resolve())))
  expect(await mainPage.locator('.diff-status').textContent()).toContain('共发现 1 处差异')
  await expect(mainPage.locator('.diff-editor-grid .cm-diff-character-changed')).toHaveCount(2)
  await mainPage.getByLabel('高亮模式').selectOption('characters')
  await expect(mainPage.locator('.diff-editor-grid [class*="cm-diff-line-"]')).toHaveCount(0)
  await expect(mainPage.locator('.diff-status')).toContainText('字符差异 1 处')
  await mainPage.getByLabel('高亮模式').selectOption('lines')
  await expect(mainPage.locator('.diff-editor-grid [class*="cm-diff-character-"]')).toHaveCount(0)
  await mainPage.getByLabel('高亮模式').selectOption('both')
  await mainPage.getByRole('button', { name: '下一处', exact: true }).click()
  await expect(mainPage.locator('.diff-status')).toContainText('第 1/1 处差异')
  await mainPage.getByLabel('显示模式').selectOption('unified')
  await expect(mainPage.locator('.unified-diff')).toContainText('-two')
  await expect(mainPage.locator('.unified-diff')).toContainText('+three')
  await expect(mainPage.locator('.unified-diff .cm-diff-line-removed')).toBeVisible()

  await mainPage.getByRole('button', { name: '历史', exact: true }).click()
  await expect(mainPage.getByRole('dialog', { name: '历史记录' })).toBeVisible()
  await expect(mainPage.locator('.history-item')).not.toHaveCount(0)
  await mainPage.getByRole('dialog', { name: '历史记录' }).getByRole('button', { name: '关闭' }).click()
})

test('runs P4 reformat, crypto, Protobuf and QR workflows', async () => {
  await openTool('格式化', '格式化')
  const reformatEditor = mainPage.locator('.reformat-workspace .code-editor .cm-content')
  await expect(reformatEditor).toHaveCSS('font-size', '13px')
  await reformatEditor.fill('server { listen 80; location / { return 200; } }')
  await expectTextEditorChrome(mainPage.locator('.reformat-workspace .code-editor'))
  await mainPage.locator('.reformat-workspace').getByRole('button', { name: '格式化', exact: true }).click()
  await expect(reformatEditor).toContainText('listen 80;')
  await expect(reformatEditor).toContainText('\n')

  await openTool('加解密/随机', '加解密 / 随机')
  const cryptoEditors = mainPage.locator('.crypto-io-grid .cm-content')
  await cryptoEditors.nth(0).fill('MooTool E2E')
  await expectTextEditorChrome(mainPage.locator('.crypto-io-grid .text-code-editor').nth(0))
  await mainPage.getByRole('button', { name: '加密', exact: true }).click()
  const cipherText = await cryptoEditors.nth(1).innerText()
  expect(cipherText).not.toBe('')
  await mainPage.getByRole('button', { name: '解密', exact: true }).click()
  await expect(cryptoEditors.nth(0)).toHaveText('MooTool E2E')

  await openTool('Protobuf', 'Protobuf')
  await expect(mainPage.locator('.proto-definition header')).toHaveCSS('font-size', '12px')
  const protobufBounds = await mainPage.locator('.protobuf-workspace').evaluate((element) => ({
    bottom: element.getBoundingClientRect().bottom,
    viewportHeight: window.innerHeight,
    scrollHeight: element.scrollHeight,
    clientHeight: element.clientHeight
  }))
  expect(protobufBounds.bottom).toBeLessThanOrEqual(protobufBounds.viewportHeight)
  expect(protobufBounds.scrollHeight).toBe(protobufBounds.clientHeight)
  const protobufPanes = mainPage.locator('.protobuf-convert-grid .cm-content')
  await expectTextEditorChrome(mainPage.locator('.protobuf-convert-grid .text-code-editor').nth(0))
  await mainPage.getByRole('button', { name: '转为 Binary' }).click()
  await expect(protobufPanes.nth(1)).not.toHaveText('')
  await protobufPanes.nth(0).fill('{}')
  await mainPage.getByRole('button', { name: '转为 JSON' }).click()
  await expect(protobufPanes.nth(0)).toContainText('MooTool')

  await openTool('二维码', '二维码')
  await expectTextEditorChrome(mainPage.locator('.qrcode-content-editor'))
  await mainPage.getByRole('button', { name: '生成二维码' }).click()
  const qrImage = mainPage.locator('.qrcode-preview-panel > img')
  await expect(qrImage).toBeVisible()
  const qrDataUrl = await qrImage.getAttribute('src')
  expect(qrDataUrl).toMatch(/^data:image\/png;base64,/)
  await mainPage.evaluate((dataUrl) => window.mootool.writeClipboardImage(dataUrl), qrDataUrl!)
  await mainPage.getByRole('tab', { name: '识别' }).click()
  await mainPage.getByRole('button', { name: '从剪贴板读取' }).click()
  const qrOutcome = await mainPage.waitForFunction(() => {
    const value = document.querySelector('[data-testid="qrcode-result-text"]')?.textContent
    const error = document.querySelector('.toast--error .toast__message')?.textContent
    return value ? { value } : error ? { error } : null
  }).then((handle) => handle.jsonValue())
  expect(qrOutcome).toEqual({ value: 'https://github.com/rememberber/MooTool' })
})

test('runs P4 color favorites, image persistence and PDF workspace', async () => {
  await openTool('调色板', '调色板')
  const colorCode = mainPage.locator('.color-code-input input')
  await colorCode.fill('#123456')
  await colorCode.press('Enter')
  await expect(mainPage.locator('.color-preview strong')).toHaveText('#123456')
  await mainPage.getByRole('button', { name: '收藏夹' }).click()
  const colorFavorites = mainPage.getByRole('dialog', { name: '收藏夹' })
  await colorFavorites.getByLabel('名称').fill('E2E color')
  await colorFavorites.getByRole('button', { name: '收藏当前内容' }).click()
  await expect(colorFavorites.locator('.favorite-item')).toContainText('E2E color')
  await colorFavorites.getByRole('button', { name: '关闭' }).click()

  const imageName = await mainPage.evaluate(async () => {
    const canvas = document.createElement('canvas')
    canvas.width = 8
    canvas.height = 8
    const context = canvas.getContext('2d')!
    context.fillStyle = '#de8f7d'
    context.fillRect(0, 0, 8, 8)
    const result = await window.mootool.saveImageAsset({
      name: 'e2e-image.png',
      dataUrl: canvas.toDataURL('image/png')
    })
    return result.name
  })
  expect(imageName).toBe('e2e-image.png')
  await openTool('图片助手', '图片助手')
  await expect(mainPage.locator('.image-list-item').filter({ hasText: 'e2e-image.png' })).toBeVisible()
  await expect(mainPage.locator('.image-canvas img')).toBeVisible()
  await mainPage.getByRole('button', { name: '复制图片' }).click()
  expect(await mainPage.evaluate(() => window.mootool.readClipboardImage())).toMatch(/^data:image\/png;base64,/)

  await openTool('PDF', 'PDF')
  await expect(mainPage.getByRole('tab', { name: '拆分 PDF' })).toBeVisible()
  await mainPage.getByRole('tab', { name: '合并 PDF' }).click()
  await expect(mainPage.getByRole('button', { name: '开始合并' })).toBeVisible()
})

test('runs P5 HTTP requests and persists the request collection', async () => {
  const server = createServer((request, response) => {
    response.setHeader('content-type', 'application/json; charset=utf-8')
    response.end(JSON.stringify({ ok: true, method: request.method, url: request.url }))
  })
  await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve))
  const address = server.address()
  if (!address || typeof address === 'string') throw new Error('HTTP test server did not expose a port')

  try {
    await openTool('HTTP 请求', 'HTTP 请求')
    await expect(mainPage.getByTestId('http-url')).toHaveCSS('font-size', '12px')
    await expect.poll(() => mainPage.getByTestId('http-url').evaluate((element) => element.getBoundingClientRect().height)).toBe(34)
    await mainPage.getByRole('tab', { name: 'Body', exact: true }).first().click()
    await expectTextEditorChrome(mainPage.locator('.http-body-code-editor'))
    await mainPage.getByTestId('http-url').fill(`http://127.0.0.1:${address.port}/echo?source=mootool`)
    await mainPage.getByTestId('http-send').click()
    await expect(mainPage.getByTestId('http-response')).toContainText('"ok": true')
    await expect(mainPage.getByTestId('http-response')).toContainText('/echo?source=mootool')

    await mainPage.getByRole('button', { name: '保存', exact: true }).click()
    const saveDialog = mainPage.getByRole('dialog', { name: '请求名称' })
    await saveDialog.getByRole('textbox', { name: '请求名称' }).fill('E2E local request')
    await saveDialog.getByRole('button', { name: '保存', exact: true }).click()
    await expect.poll(() => mainPage.evaluate(() => window.mootool.listHttpRequests())).toEqual(
      expect.arrayContaining([expect.objectContaining({ name: 'E2E local request' })])
    )
    await expect(mainPage.locator('.http-saved-item').filter({ hasText: 'E2E local request' })).toBeVisible()

    await mainPage.getByRole('button', { name: '历史', exact: true }).click()
    const historyDialog = mainPage.getByRole('dialog', { name: '请求历史' })
    await expect(historyDialog).toBeVisible()
    await expect(historyDialog.locator('.http-history-list article')).not.toHaveCount(0)
    await historyDialog.getByRole('button', { name: '关闭' }).click()
  } finally {
    await new Promise<void>((resolve, reject) => server.close((error) => error ? reject(error) : resolve()))
  }
})

test('runs P5 Hosts, translation records, network and system workflows', async () => {
  await openTool('Host', 'Host')
  await mainPage.getByRole('button', { name: '新建', exact: true }).click()
  await mainPage.getByLabel('方案名称').fill('E2E development')
  await mainPage.getByTestId('host-content').fill('# E2E profile\n127.0.0.1 e2e.mootool.local\n')
  await expectTextEditorChrome(mainPage.locator('.host-content-editor'))
  await mainPage.getByRole('button', { name: '保存', exact: true }).click()
  await expect(mainPage.locator('.host-profile').filter({ hasText: 'E2E development' })).toBeVisible()

  await mainPage.evaluate(() => window.mootool.saveTranslationWord({
    sourceText: 'MooTool parity',
    targetText: 'MooTool 对齐',
    sourceLang: 'en',
    targetLang: 'zh-CN',
    remark: 'E2E'
  }))
  await openTool('翻译', '翻译')
  await expectTextEditorChrome(mainPage.locator('.translation-editor-grid .text-code-editor').nth(0))
  await mainPage.getByRole('tab', { name: '单词本' }).click()
  await expect(mainPage.locator('.translation-record').filter({ hasText: 'MooTool parity' })).toBeVisible()
  await expect(mainPage.locator('.translation-record-layout main')).toContainText('英语 → 中文（简体）')
  await expectTextEditorChrome(mainPage.locator('.translation-record-editor').nth(0))

  await openTool('网络/IP', '网络/IP')
  await expectTextEditorChrome(mainPage.locator('.local-address-editor').nth(0))
  await mainPage.getByTestId('net-resolve').click()
  await expect(mainPage.getByTestId('net-output')).toContainText(/127\.0\.0\.1|::1/)

  await openTool('环境变量', '环境变量')
  await expect(mainPage.locator('.environment-table tbody tr')).not.toHaveCount(0)
  await mainPage.getByRole('tab', { name: 'Electron 运行属性' }).click()
  await expect(mainPage.locator('.environment-table')).toContainText('Electron')

  await openTool('系统信息', '系统信息')
  await expect(mainPage.locator('.hardware-group')).not.toHaveCount(0)
  await mainPage.getByRole('tab', { name: '处理器' }).click()
  await expect(mainPage.locator('.hardware-group')).not.toHaveCount(0)
})

test('runs P6 Quick Note Vault, Markdown preview and Git workflows', async () => {
  await openTool('随手记', '随手记')
  await mainPage.evaluate(() => window.mootool.updateSettings({ vault: { autoCommit: false } }))
  await mainPage.getByRole('button', { name: 'Git 同步' }).click()
  const setupGitDialog = mainPage.getByRole('dialog', { name: '随手记 Vault Git' })
  const setupInitButton = setupGitDialog.getByRole('button', { name: '初始化 Git' })
  await expect(setupInitButton).toBeVisible()
  await setupInitButton.click()
  await expect(setupGitDialog.getByText(/^分支 /)).toBeVisible()
  await setupGitDialog.getByRole('button', { name: '关闭' }).click()
  await mainPage.getByRole('tab', { name: '分栏' }).click()
  await expect(mainPage.getByRole('tab', { name: '分栏' })).toHaveAttribute('aria-selected', 'true')
  await mainPage.getByRole('tab', { name: '编辑' }).click()
  await expect(mainPage.getByRole('tab', { name: '编辑' })).toHaveAttribute('aria-selected', 'true')
  await expect(mainPage.locator('.quick-note-tree__row')).not.toHaveCount(0)
  await expect(mainPage.locator('.quick-note-search input')).toHaveCSS('font-size', '13px')
  await expect.poll(() => mainPage.locator('.quick-note-search').evaluate((element) => element.getBoundingClientRect().height)).toBe(34)
  await expect(mainPage.locator('.quick-note-tree__row').first()).toHaveCSS('font-size', '13px')
  await expect.poll(() => mainPage.getByLabel('语法').evaluate((element) => element.getBoundingClientRect().height)).toBe(32)
  await expect.poll(() => mainPage.locator('.quick-note-tree-actions button').first().evaluate((element) => ({
    button: element.getBoundingClientRect().height,
    icon: element.querySelector('svg')?.getBoundingClientRect().width
  }))).toEqual({ button: 32, icon: 16 })

  await mainPage.getByRole('button', { name: '新建笔记' }).click()
  const createDialog = mainPage.getByRole('dialog', { name: '新建笔记' })
  await createDialog.getByLabel('名称').fill('E2E Quick Note')
  await createDialog.getByRole('button', { name: '创建' }).click()
  await expect(mainPage.getByLabel('字体')).toHaveValue('ui-monospace')
  const colorTrigger = mainPage.getByRole('button', { name: '笔记颜色', exact: true })
  await expect(colorTrigger).toHaveAttribute('data-color', 'default')
  await expect.poll(() => colorTrigger.evaluate((element) => ({
    width: element.getBoundingClientRect().width,
    height: element.getBoundingClientRect().height
  }))).toEqual({ width: 36, height: 32 })
  await expect(mainPage.locator('.quick-note-color-menu')).toHaveCount(0)
  await colorTrigger.click()
  const colorMenu = mainPage.getByRole('menu', { name: '笔记颜色' })
  await expect(colorMenu).toBeVisible()
  await expect(colorMenu).toHaveCSS('position', 'fixed')
  await expect(colorMenu.getByRole('menuitemradio')).toHaveCount(7)
  await colorMenu.getByRole('menuitemradio', { name: '笔记颜色 yellow' }).click()
  await expect(colorTrigger).toHaveAttribute('data-color', 'yellow')
  await expect(colorMenu).toHaveCount(0)
  const editor = mainPage.getByLabel('笔记内容')
  await editor.fill('## E2E Markdown\n\n- Vault file\n- Git checkpoint')
  await expect(mainPage.locator('.quick-note-code-editor .cm-activeLine')).toHaveCount(1)
  await expect(mainPage.locator('.quick-note-code-editor .cm-activeLineGutter')).toHaveCount(1)
  await mainPage.getByLabel('字号').fill('24')
  await expect(mainPage.locator('.quick-note-code-editor')).toHaveCSS('font-size', '24px')
  const quickNoteLineOffsets = await editorLineTopOffsets(mainPage, '.quick-note-code-editor')
  expect(quickNoteLineOffsets).toEqual(expect.arrayContaining([expect.any(Number)]))
  expect(Math.max(...quickNoteLineOffsets)).toBeLessThan(1.5)
  await mainPage.getByRole('button', { name: '查找与替换' }).click()
  await mainPage.getByRole('textbox', { name: '查找', exact: true }).fill('Vault')
  await expect(mainPage.locator('.quick-note-code-editor .cm-searchMatch')).toHaveCount(1)
  await editor.focus()
  await mainPage.keyboard.press('ControlOrMeta+End')
  await mainPage.keyboard.press('Shift+ArrowLeft')
  await mainPage.locator('.quick-note-search input').fill('E2E Quick')
  await openTool('JSON', 'JSON 工作台')
  await openTool('随手记', '随手记')
  await expect(mainPage.locator('.quick-note-search input')).toHaveValue('E2E Quick')
  await expect(mainPage.getByRole('textbox', { name: '查找', exact: true })).toHaveValue('Vault')
  await expect(mainPage.locator('.quick-note-tree__row--active')).toContainText('E2E Quick Note')
  await editor.focus()
  await mainPage.keyboard.type('t')
  await expect(editor).toContainText('E2E Markdown')
  await expect(editor).toContainText('Vault file')
  await expect(mainPage.locator('.quick-note-code-editor .cm-searchMatch')).toHaveCount(1)
  await mainPage.getByLabel('语法').selectOption('text/markdown')
  await mainPage.getByRole('button', { name: '保存', exact: true }).click()
  await expect.poll(() => mainPage.evaluate(() => window.mootool.readQuickNote('E2E Quick Note.txt'))).toEqual(
    expect.objectContaining({
      content: '## E2E Markdown\n\n- Vault file\n- Git checkpoint',
      metadata: expect.objectContaining({ color: 'yellow', fontName: 'ui-monospace' })
    })
  )

  await mainPage.getByRole('tab', { name: '预览' }).click()
  await expect(mainPage.locator('.quick-note-preview h2')).toHaveText('E2E Markdown')
  await mainPage.getByRole('button', { name: '快速替换' }).click()
  await expect(mainPage.locator('.quick-replace-panel')).toBeVisible()
  await mainPage.locator('.quick-replace-panel').getByRole('button', { name: '转大写' }).click()
  await mainPage.getByRole('button', { name: '保存', exact: true }).click()

  await mainPage.getByRole('button', { name: 'Git 同步' }).click()
  const gitDialog = mainPage.getByRole('dialog', { name: '随手记 Vault Git' })
  await expect(gitDialog).toBeVisible()
  const initButton = gitDialog.getByRole('button', { name: '初始化 Git' })
  if (await initButton.isVisible()) await initButton.click()
  await expect(gitDialog.getByText(/^分支 /)).toBeVisible()
  await gitDialog.getByRole('button', { name: '提交全部变更' }).click()
  await gitDialog.getByRole('tab', { name: '提交历史' }).click()
  await expect(gitDialog.locator('.git-list-item').filter({ hasText: 'MooTool 随手记检查点' }).first()).toBeVisible()
  await gitDialog.getByRole('button', { name: '关闭' }).click()
})

test('pastes clipboard images into Quick Note without intercepting text pastes', async () => {
  await openTool('随手记', '随手记')
  await mainPage.getByRole('button', { name: '新建笔记' }).click()
  const createDialog = mainPage.getByRole('dialog', { name: '新建笔记' })
  await createDialog.getByLabel('名称').fill('E2E Quick Clipboard')
  await createDialog.getByRole('button', { name: '创建' }).click()
  await mainPage.getByRole('tab', { name: '编辑' }).click()

  const editor = mainPage.getByLabel('笔记内容')
  await expect(mainPage.getByLabel('语法')).toHaveValue('text/plain')
  await editor.fill('BeforeAfter')
  await expect.poll(() => mainPage.evaluate(() => window.mootool.readQuickNote('E2E Quick Clipboard.txt'))).toMatchObject({ content: 'BeforeAfter' })
  await editor.press('ControlOrMeta+Home')
  for (let index = 0; index < 6; index += 1) await editor.press('ArrowRight')

  const pastePrevented = await editor.evaluate((element) => {
    const base64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
    const bytes = Uint8Array.from(atob(base64), (character) => character.charCodeAt(0))
    const data = new DataTransfer()
    data.items.add(new File([bytes], 'pixel.png', { type: 'image/png' }))
    const event = new ClipboardEvent('paste', { bubbles: true, cancelable: true, clipboardData: data })
    element.dispatchEvent(event)
    return event.defaultPrevented
  })
  expect(pastePrevented).toBe(true)
  await expect(mainPage.getByLabel('语法')).toHaveValue('text/markdown')
  await expect(editor).toContainText('![image](attachments/')

  await expect.poll(() => mainPage.evaluate(async () => {
    const note = await window.mootool.readQuickNote('E2E Quick Clipboard.txt')
    return /!\[image]\((attachments\/[^)]+\.png)\)/.exec(note.content)?.[1] ?? ''
  })).toMatch(/^attachments\/\d{14}_[a-f0-9]{8}\.png$/)
  const note = await mainPage.evaluate(() => window.mootool.readQuickNote('E2E Quick Clipboard.txt'))
  expect(note.content).toMatch(/^Before\n!\[image]\(attachments\/[^)]+\.png\)\nAfter$/)
  const attachmentPath = /!\[image]\((attachments\/[^)]+\.png)\)/.exec(note.content)?.[1]
  expect(attachmentPath).toBeTruthy()

  await mainPage.getByRole('tab', { name: '预览' }).click()
  const previewImage = mainPage.locator('.quick-note-preview img')
  await expect(previewImage).toBeVisible()
  await expect(previewImage).toHaveAttribute('src', /^data:image\/png;base64,/)

  await mainPage.getByRole('tab', { name: '编辑' }).click()
  await editor.press('ControlOrMeta+End')
  await editor.evaluate(async (element) => {
    const canvas = document.createElement('canvas')
    canvas.width = 2
    canvas.height = 2
    const context = canvas.getContext('2d')
    if (!context) throw new Error('Canvas is unavailable')
    context.fillStyle = '#d97868'
    context.fillRect(0, 0, 2, 2)
    const jpeg = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.9))
    if (!jpeg) throw new Error('Unable to create JPEG fixture')
    const data = new DataTransfer()
    data.items.add(new File([jpeg], 'photo.jpg', { type: 'image/jpeg' }))
    element.dispatchEvent(new ClipboardEvent('paste', { bubbles: true, cancelable: true, clipboardData: data }))
  })
  await expect.poll(() => mainPage.evaluate(async () => {
    const current = await window.mootool.readQuickNote('E2E Quick Clipboard.txt')
    return /!\[image]\((attachments\/[^)]+\.jpg)\)/.exec(current.content)?.[1] ?? ''
  })).toMatch(/^attachments\/\d{14}_[a-f0-9]{8}\.jpg$/)
  const noteWithJpeg = await mainPage.evaluate(() => window.mootool.readQuickNote('E2E Quick Clipboard.txt'))
  const jpegAttachmentPath = /!\[image]\((attachments\/[^)]+\.jpg)\)/.exec(noteWithJpeg.content)?.[1]
  expect(jpegAttachmentPath).toBeTruthy()
  expect(await mainPage.evaluate((path) => window.mootool.readQuickNoteAttachment(path), jpegAttachmentPath!)).toMatch(/^data:image\/jpeg;base64,/)

  await mainPage.getByRole('tab', { name: '预览' }).click()
  await expect(previewImage).toHaveCount(2)
  await expect(previewImage.nth(1)).toHaveAttribute('src', /^data:image\/jpeg;base64,/)
  await mainPage.getByRole('tab', { name: '编辑' }).click()
  await editor.press('ControlOrMeta+End')
  await editor.evaluate((element) => {
    const data = new DataTransfer()
    data.setData('text/plain', '\nplain text paste')
    const event = new ClipboardEvent('paste', { bubbles: true, cancelable: true, clipboardData: data })
    element.dispatchEvent(event)
  })
  await expect(editor).toContainText('plain text paste')
  expect(await mainPage.evaluate((path) => window.mootool.readQuickNoteAttachment(path), attachmentPath!)).toMatch(/^data:image\/png;base64,/)
})

test('keeps consecutive Quick Note image pastes in order', async () => {
  await openTool('随手记', '随手记')
  await mainPage.getByRole('button', { name: '新建笔记' }).click()
  const createDialog = mainPage.getByRole('dialog', { name: '新建笔记' })
  await createDialog.getByLabel('名称').fill('E2E Quick Consecutive Images')
  await createDialog.getByRole('button', { name: '创建' }).click()
  await mainPage.getByRole('tab', { name: '编辑' }).click()

  const editor = mainPage.getByLabel('笔记内容')
  const prevented = await editor.evaluate(async (element) => {
    const pngBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
    const pngBytes = Uint8Array.from(atob(pngBase64), (character) => character.charCodeAt(0))
    const canvas = document.createElement('canvas')
    canvas.width = 2
    canvas.height = 2
    const context = canvas.getContext('2d')
    if (!context) throw new Error('Canvas is unavailable')
    context.fillStyle = '#4f83cc'
    context.fillRect(0, 0, 2, 2)
    const jpeg = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.9))
    if (!jpeg) throw new Error('Unable to create JPEG fixture')

    const dispatchImagePaste = (file: File) => {
      const data = new DataTransfer()
      data.items.add(file)
      const event = new ClipboardEvent('paste', { bubbles: true, cancelable: true, clipboardData: data })
      element.dispatchEvent(event)
      return event.defaultPrevented
    }

    return [
      dispatchImagePaste(new File([pngBytes], 'first.png', { type: 'image/png' })),
      dispatchImagePaste(new File([jpeg], 'second.jpg', { type: 'image/jpeg' }))
    ]
  })
  expect(prevented).toEqual([true, true])

  await expect.poll(() => mainPage.evaluate(async () => {
    const note = await window.mootool.readQuickNote('E2E Quick Consecutive Images.txt')
    return note.content.match(/!\[image]\(attachments\/[^)]+\.(?:png|jpg)\)/g) ?? []
  })).toHaveLength(2)
  const note = await mainPage.evaluate(() => window.mootool.readQuickNote('E2E Quick Consecutive Images.txt'))
  expect(note.content).toMatch(/^!\[image]\(attachments\/[^)]+\.png\)\n!\[image]\(attachments\/[^)]+\.jpg\)$/)

  await mainPage.getByRole('tab', { name: '预览' }).click()
  await expect(mainPage.locator('.quick-note-preview img')).toHaveCount(2)
  await expect(mainPage.locator('.quick-note-preview img').nth(0)).toHaveAttribute('src', /^data:image\/png;base64,/)
  await expect(mainPage.locator('.quick-note-preview img').nth(1)).toHaveAttribute('src', /^data:image\/jpeg;base64,/)
})

test('appends a second Quick Note image without requiring a manual line break', async () => {
  await openTool('随手记', '随手记')
  await mainPage.getByRole('button', { name: '新建笔记' }).click()
  const createDialog = mainPage.getByRole('dialog', { name: '新建笔记' })
  await createDialog.getByLabel('名称').fill('E2E Quick Images Without Break')
  await createDialog.getByRole('button', { name: '创建' }).click()
  await mainPage.getByRole('tab', { name: '编辑' }).click()

  const editor = mainPage.getByLabel('笔记内容')
  await editor.evaluate((element) => {
    const base64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
    const bytes = Uint8Array.from(atob(base64), (character) => character.charCodeAt(0))
    const data = new DataTransfer()
    data.items.add(new File([bytes], 'first.png', { type: 'image/png' }))
    element.dispatchEvent(new ClipboardEvent('paste', { bubbles: true, cancelable: true, clipboardData: data }))
  })
  await expect.poll(() => mainPage.evaluate(async () => {
    const note = await window.mootool.readQuickNote('E2E Quick Images Without Break.txt')
    return /\.png\)$/.test(note.content)
  })).toBe(true)

  await editor.evaluate(async (element) => {
    const canvas = document.createElement('canvas')
    canvas.width = 2
    canvas.height = 2
    const context = canvas.getContext('2d')
    if (!context) throw new Error('Canvas is unavailable')
    context.fillStyle = '#c96761'
    context.fillRect(0, 0, 2, 2)
    const jpeg = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.9))
    if (!jpeg) throw new Error('Unable to create JPEG fixture')
    const data = new DataTransfer()
    data.items.add(new File([jpeg], 'second.jpg', { type: 'image/jpeg' }))
    element.dispatchEvent(new ClipboardEvent('paste', { bubbles: true, cancelable: true, clipboardData: data }))
  })

  await expect.poll(() => mainPage.evaluate(async () => {
    const note = await window.mootool.readQuickNote('E2E Quick Images Without Break.txt')
    return note.content.match(/!\[image]\(attachments\/[^)]+\.(?:png|jpg)\)/g) ?? []
  })).toHaveLength(2)
  const note = await mainPage.evaluate(() => window.mootool.readQuickNote('E2E Quick Images Without Break.txt'))
  expect(note.content).toMatch(/^!\[image]\(attachments\/[^)]+\.png\)\n!\[image]\(attachments\/[^)]+\.jpg\)$/)
})

test('drops external image files into the Quick Note editor', async () => {
  await openTool('随手记', '随手记')
  await mainPage.getByRole('button', { name: '新建笔记' }).click()
  const createDialog = mainPage.getByRole('dialog', { name: '新建笔记' })
  await createDialog.getByLabel('名称').fill('E2E Quick External Drop')
  await createDialog.getByRole('button', { name: '创建' }).click()
  await mainPage.getByRole('tab', { name: '编辑' }).click()

  const editor = mainPage.getByLabel('笔记内容')
  await editor.fill('Drop target')
  await expect(editor).toContainText('Drop target')
  await mainPage.getByRole('button', { name: '保存', exact: true }).click()
  await expect.poll(() => mainPage.evaluate(() => window.mootool.readQuickNote('E2E Quick External Drop.txt'))).toMatchObject({ content: 'Drop target' })
  const content = mainPage.locator('.quick-note-code-editor .cm-content')
  const box = await content.boundingBox()
  if (!box) throw new Error('Quick Note editor content has no bounding box')
  const prevented = await content.evaluate(async (element, point) => {
    const pngBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII='
    const pngBytes = Uint8Array.from(atob(pngBase64), (character) => character.charCodeAt(0))
    const canvas = document.createElement('canvas')
    canvas.width = 2
    canvas.height = 2
    const context = canvas.getContext('2d')
    if (!context) throw new Error('Canvas is unavailable')
    context.fillStyle = '#4e9275'
    context.fillRect(0, 0, 2, 2)
    const jpeg = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.9))
    if (!jpeg) throw new Error('Unable to create JPEG fixture')

    const data = new DataTransfer()
    data.items.add(new File([pngBytes], 'dropped-first.png', { type: 'image/png' }))
    data.items.add(new File([jpeg], 'dropped-second.jpg', { type: 'image/jpeg' }))
    const dragOver = new DragEvent('dragover', {
      bubbles: true,
      cancelable: true,
      clientX: point.x,
      clientY: point.y,
      dataTransfer: data
    })
    element.dispatchEvent(dragOver)
    const drop = new DragEvent('drop', {
      bubbles: true,
      cancelable: true,
      clientX: point.x,
      clientY: point.y,
      dataTransfer: data
    })
    element.dispatchEvent(drop)
    return [dragOver.defaultPrevented, drop.defaultPrevented]
  }, { x: box.x + box.width - 8, y: box.y + 8 })
  expect(prevented).toEqual([true, true])

  await expect.poll(() => mainPage.evaluate(async () => {
    const note = await window.mootool.readQuickNote('E2E Quick External Drop.txt')
    return note.content.match(/!\[image]\(attachments\/[^)]+\.(?:png|jpg)\)/g) ?? []
  })).toHaveLength(2)
  const note = await mainPage.evaluate(() => window.mootool.readQuickNote('E2E Quick External Drop.txt'))
  const imageMarkdown = note.content.match(/!\[image]\(attachments\/[^)]+\.(?:png|jpg)\)/g) ?? []
  expect(imageMarkdown[0]).toMatch(/\.png\)$/)
  expect(imageMarkdown[1]).toMatch(/\.jpg\)$/)
  expect(note.content).toContain('Drop target')

  await mainPage.getByRole('tab', { name: '预览' }).click()
  await expect(mainPage.locator('.quick-note-preview img')).toHaveCount(2)
})

test('restores the Quick Note expanded folder and selected note after switching tools', async () => {
  await openTool('随手记', '随手记')
  await mainPage.getByRole('button', { name: '新建文件夹' }).click()
  await mainPage.getByRole('dialog', { name: '新建文件夹' }).getByLabel('名称').fill('E2E Quick Session')
  await mainPage.getByRole('dialog', { name: '新建文件夹' }).getByRole('button', { name: '创建' }).click()
  await mainPage.getByRole('button', { name: '新建笔记' }).click()
  await mainPage.getByRole('dialog', { name: '新建笔记' }).getByLabel('名称').fill('E2E Quick Nested')
  await mainPage.getByRole('dialog', { name: '新建笔记' }).getByRole('button', { name: '创建' }).click()
  await expect(mainPage.locator('.quick-note-tree__row--active')).toContainText('E2E Quick Nested')

  await openTool('JSON', 'JSON 工作台')
  await openTool('随手记', '随手记')

  await expect(mainPage.locator('.quick-note-tree__row').filter({ hasText: 'E2E Quick Session' })).toBeVisible()
  await expect(mainPage.locator('.quick-note-tree__row--active')).toContainText('E2E Quick Nested')
})

test('runs and stops P6 Node.js code with persistent history', async () => {
  await openTool('代码运行', '代码运行')
  await mainPage.getByRole('tab', { name: 'Node.js' }).click()
  await expect(mainPage.locator('.runtime-toolbar')).toContainText(/node|Node/i)
  const editor = mainPage.getByLabel('代码编辑器')
  await expect(editor).toHaveCSS('font-size', '13px')
  await editor.fill('console.log("E2E runtime", 21 * 2)')
  await expectTextEditorChrome(mainPage.locator('.runtime-code-editor'))
  await mainPage.getByRole('button', { name: '运行', exact: true }).click()
  await expect(mainPage.locator('.runtime-stdout')).toContainText('E2E runtime 42')
  await expect(mainPage.locator('.runtime-output-state')).toHaveText('运行完成')

  await mainPage.getByRole('button', { name: '运行历史' }).click()
  const historyDialog = mainPage.getByRole('dialog', { name: '历史记录' })
  await expect(historyDialog.locator('.history-item')).not.toHaveCount(0)
  await historyDialog.getByRole('button', { name: '关闭' }).click()

  await editor.fill('setInterval(() => console.log("tick"), 50)')
  await mainPage.getByRole('button', { name: '运行', exact: true }).click()
  await expect(mainPage.locator('.runtime-stdout')).toContainText('tick')
  await mainPage.getByRole('button', { name: '停止', exact: true }).click()
  await expect(mainPage.locator('.runtime-output-state')).toHaveText('运行已停止')
})

test('shows P6 backup paths and export controls in Settings', async () => {
  await mainPage.locator('.sidebar-footer .icon-ghost').click()
  await expect.poll(() => electronApp.windows().filter((page) => page !== mainPage && !page.isClosed()).length).toBeGreaterThan(0)
  const settingsPage = electronApp.windows().find((page) => page !== mainPage && !page.isClosed())
  if (!settingsPage) throw new Error('Settings window did not open')
  await settingsPage.waitForLoadState('domcontentloaded')
  await settingsPage.locator('.settings-nav__item').filter({ hasText: '数据与备份' }).click()
  await expect(settingsPage.locator('.backup-row')).toHaveCount(3)
  await expect(settingsPage.getByRole('button', { name: '完整备份' })).toBeVisible()
  const backupInfo = await settingsPage.evaluate(() => window.mootool.getBackupInfo())
  expect(backupInfo.databasePath).toContain('MooToolNext.db')
  expect(backupInfo.quickNotePath).toContain('quick-notes')
  await settingsPage.locator('.settings-titlebar .icon-ghost').click()
})

test('scans and migrates Java data from the Data & Backup settings page', async () => {
  const legacyDirectory = await mkdtemp(join(tmpdir(), 'mootool-java-e2e-'))
  try {
    await mkdir(join(legacyDirectory, 'config'))
    await writeFile(join(legacyDirectory, 'config', 'config.setting'), '# E2E legacy data')
    const legacyDatabase = new DatabaseSync(join(legacyDirectory, 'MooTool.db'))
    legacyDatabase.exec(`
      CREATE TABLE t_quick_note (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT);
      INSERT INTO t_quick_note VALUES (1, 'Migrated E2E Note', 'Imported from Java', '2026-01-01', '2026-01-02');
      CREATE TABLE t_json_beauty (id INTEGER PRIMARY KEY, name TEXT, content TEXT, create_time TEXT, modified_time TEXT);
      INSERT INTO t_json_beauty VALUES (1, 'Migrated E2E JSON', '{"from":"java"}', '2026-01-01', '2026-01-02');
    `)
    legacyDatabase.close()

    await mainPage.locator('.sidebar-footer .icon-ghost').click()
    await expect.poll(() => electronApp.windows().filter((page) => page !== mainPage && !page.isClosed()).length).toBeGreaterThan(0)
    const settingsPage = electronApp.windows().find((page) => page !== mainPage && !page.isClosed())
    if (!settingsPage) throw new Error('Settings window did not open')
    await settingsPage.waitForLoadState('domcontentloaded')
    await settingsPage.locator('.settings-nav__item').filter({ hasText: '数据与备份' }).click()
    const sourceInput = settingsPage.getByLabel('Java 版数据目录')
    await sourceInput.fill(legacyDirectory)
    await sourceInput.press('Enter')
    await settingsPage.getByRole('button', { name: '扫描', exact: true }).click()
    await expect(settingsPage.getByText('发现 2 项可迁移数据')).toBeVisible()
    await settingsPage.getByRole('button', { name: '开始迁移', exact: true }).click()
    const confirmDialog = settingsPage.getByRole('dialog', { name: '迁移 Java 版数据' })
    await confirmDialog.getByRole('button', { name: '开始迁移', exact: true }).click()
    await expect(settingsPage.getByText('该来源已经迁移完成')).toBeVisible()

    expect(await settingsPage.evaluate(() => window.mootool.readQuickNote('Migrated E2E Note.txt'))).toMatchObject({ content: 'Imported from Java' })
    expect(await settingsPage.evaluate(() => window.mootool.readJsonVaultFile('Migrated E2E JSON.json'))).toMatchObject({ content: '{"from":"java"}' })
    const backupInfo = await settingsPage.evaluate(() => window.mootool.getBackupInfo())
    const migrationBackups = await readdir(join(backupInfo.dataDirectory, 'migration-backups'))
    expect(migrationBackups.some((name) => name.startsWith('MooTool-backup-'))).toBe(true)
    await settingsPage.locator('.settings-titlebar .icon-ghost').click()
  } finally {
    await rm(legacyDirectory, { recursive: true, force: true })
  }
})

test('preserves operation state across regular tools', async () => {
  await openTool('文本对比', '文本对比')
  await mainPage.getByLabel('显示模式').selectOption('side')
  await mainPage.getByLabel('原始文本').fill('left session value')
  await mainPage.getByLabel('新文本').fill('right session value')

  await openTool('计算器', '计算器')
  await mainPage.locator('#calculator-expression').fill('123 * 456')

  await openTool('编码解码', '编码解码')
  await mainPage.getByRole('tab', { name: 'URL 转码' }).click()
  await mainPage.getByLabel('URL 原文').fill('https://example.test/session?q=状态')

  await openTool('文本对比', '文本对比')
  await expect(mainPage.getByLabel('原始文本')).toContainText('left session value')
  await expect(mainPage.getByLabel('新文本')).toContainText('right session value')

  await openTool('计算器', '计算器')
  await expect(mainPage.locator('#calculator-expression')).toHaveValue('123 * 456')

  await openTool('编码解码', '编码解码')
  await expect(mainPage.getByRole('tab', { name: 'URL 转码' })).toHaveAttribute('aria-selected', 'true')
  await expect(mainPage.getByLabel('URL 原文')).toContainText('https://example.test/session?q=状态')
})

test('keeps the application alive when the close behavior is set to hide', async () => {
  await mainPage.evaluate(() => window.mootool.updateSettings({ general: { closeBehavior: 'hide', trayEnabled: true } }))
  await electronApp.evaluate(({ BrowserWindow }) => {
    BrowserWindow.getAllWindows().find((window) => !window.getParentWindow())?.close()
  })
  await expect.poll(() => electronApp.evaluate(({ BrowserWindow }) => {
    return BrowserWindow.getAllWindows().find((window) => !window.getParentWindow())?.isVisible()
  })).toBe(false)
  await electronApp.evaluate(({ BrowserWindow }) => {
    const window = BrowserWindow.getAllWindows().find((item) => !item.getParentWindow())
    window?.show()
    window?.focus()
  })
  await expect.poll(() => mainPage.evaluate(() => document.visibilityState)).toBe('visible')
  await mainPage.evaluate(() => window.mootool.updateSettings({ general: { closeBehavior: 'ask' } }))
})

async function editorLineTopOffsets(page: Page, rootSelector: string): Promise<number[]> {
  return page.locator(rootSelector).evaluate((root) => {
    const lines = [...root.querySelectorAll<HTMLElement>('.cm-line')]
    const numbers = [...root.querySelectorAll<HTMLElement>('.cm-lineNumbers .cm-gutterElement')]
    return numbers.flatMap((number) => {
      const lineNumber = Number(number.textContent)
      const line = Number.isInteger(lineNumber) ? lines[lineNumber - 1] : undefined
      return line ? [Math.abs(line.getBoundingClientRect().top - number.getBoundingClientRect().top)] : []
    })
  })
}

async function expectTextEditorChrome(editor: Locator): Promise<void> {
  await expect(editor.locator('.cm-lineNumbers')).toHaveCount(1)
  await expect(editor.locator('.cm-activeLine')).toHaveCount(1)
  await expect(editor.locator('.cm-activeLineGutter')).toHaveCount(1)
}

async function expectSelectedOptionToFit(select: Locator): Promise<void> {
  await expect.poll(() => select.evaluate((element) => {
    const control = element as HTMLSelectElement
    const style = getComputedStyle(control)
    const probe = document.createElement('span')
    probe.textContent = control.selectedOptions[0]?.textContent ?? ''
    Object.assign(probe.style, {
      position: 'fixed',
      visibility: 'hidden',
      whiteSpace: 'pre',
      fontFamily: style.fontFamily,
      fontSize: style.fontSize,
      fontStyle: style.fontStyle,
      fontWeight: style.fontWeight,
      letterSpacing: style.letterSpacing
    })
    document.body.append(probe)
    const textWidth = probe.getBoundingClientRect().width
    probe.remove()

    const horizontalChrome = Number.parseFloat(style.paddingLeft)
      + Number.parseFloat(style.paddingRight)
      + Number.parseFloat(style.borderLeftWidth)
      + Number.parseFloat(style.borderRightWidth)
      + 16
    return style.getPropertyValue('field-sizing') === 'content'
      && control.getBoundingClientRect().width + 0.5 >= textWidth + horizontalChrome
  })).toBe(true)
}

async function openTool(label: string, title: string): Promise<void> {
  const button = mainPage.locator('.tool-button').filter({ hasText: label }).first()
  await button.scrollIntoViewIfNeeded()
  await button.click()
  await expect(mainPage.locator('.workspace-tool-session:not([hidden]) .tool-page h1')).toHaveText(title)
}
