import { _electron as electron, expect, test, type Page } from '@playwright/test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'

for (const nativeViews of ['0', '1']) test('OS file opening imports copies and preserves edits (native views ' + nativeViews + ')', async () => {
  const root = await mkdtemp(join(tmpdir(), 'mootool-external-open-'))
  const source = join(root, '中文 sample.json')
  const markdown = join(root, '说明.md')
  const payload = '{"imported":"中文"}'
  await writeFile(source, payload)
  await writeFile(markdown, '# 外部笔记\n\n正文')
  const app = await electron.launch({
    args: ['.', '--user-data-dir=' + join(root, 'user')], cwd: process.cwd(),
    env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: nativeViews }
  })
  try {
    const page = await app.firstWindow()
    page.setDefaultTimeout(7000)
    await expect(page.locator('.sidebar')).toBeVisible()
    await page.evaluate(root => window.mootool.updateSettings({ general: { language: 'zh-CN' }, vault: { jsonPath: root + '/json', quickNotePath: root + '/notes' } }), root)
    await app.evaluate(({ dialog }) => {
      const state = globalThis as typeof globalThis & { fileOpenResponses: number[]; fileOpenDialogs: unknown[] }
      state.fileOpenResponses = []
      state.fileOpenDialogs = []
      dialog.showMessageBox = (async (_owner: unknown, options: unknown) => {
        state.fileOpenDialogs.push(options)
        return { response: state.fileOpenResponses.shift() ?? 2, checkboxChecked: false }
      }) as typeof dialog.showMessageBox
    })
    const tool = async (id: string): Promise<Page> => {
      if (nativeViews === '0') return page
      await expect.poll(() => app.context().pages().some(item => new URL(item.url() || 'about:blank').searchParams.get('toolId') === id)).toBe(true)
      return app.context().pages().find(item => new URL(item.url() || 'about:blank').searchParams.get('toolId') === id)!
    }
    const open = async (path: string, response: number, secondInstance = false) => {
      await app.evaluate(({ app }, input) => {
        const state = globalThis as typeof globalThis & { fileOpenResponses: number[] }
        state.fileOpenResponses.push(input.response)
        if (!input.secondInstance) app.emit('open-file', { preventDefault() {} }, input.path)
      }, { path, response, secondInstance })
      if (secondInstance) {
        const executable = await app.evaluate(({ app }) => app.getPath('exe'))
        await promisify(execFile)(executable, ['.', '--user-data-dir=' + join(root, 'user'), path], {
          cwd: process.cwd(), timeout: 10000, env: { ...process.env, NODE_ENV: 'test' }
        })
      }
    }
    // A mounted workspace must save its current buffer before opening the copy.
    await page.locator('.tool-button').filter({ hasText: 'JSON' }).click()
    const json = await tool('json')
    const editor = json.locator('.json-editor .cm-content')
    await expect(editor).toHaveText('{}')
    const previous = await json.locator('.vault-tree [aria-selected="true"]').getAttribute('data-path')
    await editor.fill('{"keep":"unsaved edit"}')
    await open(source, 1)
    await expect(editor).toHaveText(payload)
    await expect(json.locator('.vault-tree [aria-selected="true"]')).toHaveAttribute('data-path', '中文 sample.json')
    expect(await readFile(join(root, 'json', previous!), 'utf8')).toBe('{"keep":"unsaved edit"}')
    expect(await readFile(join(root, 'json', '中文 sample.json'), 'utf8')).toBe(payload)

    await open(source, 1, true)
    await expect(json.locator('.vault-tree [aria-selected="true"]')).toHaveAttribute('data-path', '中文 sample (2).json')
    expect(await readFile(source, 'utf8')).toBe(payload)
    await open(markdown, 0)
    const notes = await tool('quickNote')
    await expect(notes.getByLabel('笔记内容', { exact: true })).toHaveText('# 外部笔记\n\n正文', { useInnerText: true })
    const notePath = await notes.locator('.quick-note-tree [aria-selected="true"]').getAttribute('data-path')
    expect(notePath).toBe('说明.md')
    expect(await page.evaluate(path => window.mootool.readQuickNote(path), notePath!)).toMatchObject({
      content: '# 外部笔记\n\n正文', metadata: { syntax: 'text/markdown' }
    })
    expect(await readFile(markdown, 'utf8')).toBe('# 外部笔记\n\n正文')

    const before = await page.evaluate(() => window.mootool.listQuickNotes())
    await open(source, 2)
    await expect.poll(() => app.evaluate(() => (globalThis as typeof globalThis & { fileOpenResponses: number[] }).fileOpenResponses.length)).toBe(0)
    expect(await page.evaluate(() => window.mootool.listQuickNotes())).toEqual(before)

    // Failures do not block the next document; errors also use native dialogs.
    await open(join(root, 'missing.json'), 2)
    await expect.poll(() => app.evaluate(() => (globalThis as typeof globalThis & { fileOpenResponses: number[] }).fileOpenResponses.length)).toBe(0)
    await open(source, 0)
    await expect(notes.getByLabel('笔记内容', { exact: true })).toHaveText(payload)
    const importedPath = await notes.locator('.quick-note-tree [aria-selected="true"]').getAttribute('data-path')
    await page.reload()
    await expect(page.locator('.tool-button--active').filter({ hasText: '随手记' })).toBeVisible()
    await expect((await tool('quickNote')).locator('.quick-note-tree [aria-selected="true"]')).toHaveAttribute('data-path', importedPath!)

    if (nativeViews === '1') {
      await page.evaluate(() => window.mootool.detachToolWindow('json'))
      await expect.poll(() => page.evaluate(() => window.mootool.getToolWindowState('json'))).toMatchObject({ detached: true })
      await open(source, 1)
      await expect(json.locator('.vault-tree [aria-selected="true"]')).toHaveAttribute('data-path', '中文 sample (3).json')
      await expect.poll(() => json.evaluate(() => document.hasFocus())).toBe(true)
    }
  } finally {
    await app.close()
    await rm(root, { recursive: true, force: true })
  }
})

test('a cold-start command-line document waits for the workspace and opens its imported copy', async () => {
  const root = await mkdtemp(join(tmpdir(), 'mootool-cold-file-open-'))
  const source = join(root, 'cold start.json')
  const bootstrap = join(root, 'bootstrap.cjs')
  await writeFile(source, '{"coldStart":true}')
  // Install a deterministic native-dialog responder before loading the real
  // main entry. The application itself receives and queues the original argv.
  await writeFile(bootstrap, [
    "const { dialog } = require('electron');",
    "dialog.showMessageBox = async () => ({ response: 1, checkboxChecked: false });",
    'require(' + JSON.stringify(join(process.cwd(), 'out/main/index.js')) + ');'
  ].join('\n'))
  const app = await electron.launch({
    args: [bootstrap, '--user-data-dir=' + join(root, 'user'), source], cwd: process.cwd(),
    env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' }
  })
  try {
    const page = await app.firstWindow()
    await expect(page.locator('.json-editor .cm-content')).toHaveText('{"coldStart":true}')
    await expect(page.locator('.vault-tree [aria-selected="true"]')).toHaveAttribute('data-path', 'cold start.json')
    expect(await readFile(source, 'utf8')).toBe('{"coldStart":true}')
  } finally {
    await app.close()
    await rm(root, { recursive: true, force: true })
  }
})
