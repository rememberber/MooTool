import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, readFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import type { CommandActionId } from '../../src/shared/contracts/commandPalette'

for (const nativeViews of ['0', '1']) test.describe(`command result import (native views ${nativeViews})`, () => {
  let app: ElectronApplication
  let page: Page
  let directory: string
  let previous: { text: string; html: string; rtf: string; image: string }
  const preview = () => page.getByRole('textbox', { name: '结果预览', exact: true })
  const previewText = async () => (await preview().locator('.cm-line').allTextContents()).join('\n')
  const tool = async (id: string): Promise<Page> => {
    if (nativeViews === '0') return page
    await expect.poll(() => app.context().pages().some(item => new URL(item.url() || 'about:blank').searchParams.get('toolId') === id)).toBe(true)
    return app.context().pages().find(item => new URL(item.url() || 'about:blank').searchParams.get('toolId') === id)!
  }
  async function run(actionId: CommandActionId, text: string, html = '') {
    await app.evaluate(({ clipboard }, data) => clipboard.write(data), { text, html })
    await page.evaluate(id => window.mootool.openCommandAction(id), actionId)
    await expect(preview()).toBeVisible()
    await expect(page.locator('.command-palette')).toHaveAttribute('aria-busy', 'false')
  }
  test.beforeAll(async () => {
    directory = await mkdtemp(join(tmpdir(), 'mootool-command-import-'))
    app = await electron.launch({ args: ['.', `--user-data-dir=${join(directory, 'user')}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: nativeViews } })
    page = await app.firstWindow()
    page.setDefaultTimeout(5000)
    previous = await app.evaluate(({ clipboard }) => ({ text: clipboard.readText(), html: clipboard.readHTML(), rtf: clipboard.readRTF(), image: clipboard.readImage().isEmpty() ? '' : clipboard.readImage().toDataURL() }))
    await page.evaluate(root => window.mootool.updateSettings({ vault: { jsonPath: `${root}/json`, quickNotePath: `${root}/notes` } }), directory)
  })
  test.afterAll(async () => {
    if (app) {
      await app.evaluate(({ clipboard, nativeImage }, data) => clipboard.write({ text: data.text, html: data.html, rtf: data.rtf, ...(data.image ? { image: nativeImage.createFromDataURL(data.image) } : {}) }), previous)
      await app.close()
    }
    await rm(directory, { recursive: true, force: true })
  })

  test('highlights every JSON action and imports into a new bound file, preserving existing edits and the clipboard', async ({}, testInfo) => {
    let oldPath = ''
    for (const [actionId, input] of [['json-format', '{"name":"格式化","count":2,"ok":true}'], ['json-minify', '{ "name": "压缩", "ok": false }'], ['table-json', 'name\tcount\n表格\t001']] as const) {
      await run(actionId, input)
      const output = await previewText()
      expect(JSON.parse(output)).toBeTruthy()
      const property = preview().locator('span').filter({ hasText: /^"name"$/ }).first()
      await expect(property).toBeVisible()
      expect(await property.evaluate(el => getComputedStyle(el).color !== getComputedStyle(el.parentElement!).color)).toBe(true)
      await expect(preview()).toHaveAttribute('contenteditable', 'false')
      await page.getByRole('combobox', { name: '保存文件格式' }).selectOption('text')
      // Export format must not affect JSON highlighting or the import destination.
      await expect(page.getByRole('button', { name: '导入 JSON 工作台', exact: true })).toBeVisible()
      if (actionId === 'json-format') await page.screenshot({ path: testInfo.outputPath('json-highlight.png') })
      await page.getByRole('button', { name: '导入 JSON 工作台', exact: true }).click()
      await expect(page.locator('.command-palette')).toBeHidden()
      const json = await tool('json')
      const editor = json.locator('.json-editor .cm-content')
      await expect(editor).toHaveText(output, { useInnerText: true })
      const selected = json.locator('.vault-tree [aria-selected="true"]')
      await expect(selected).toHaveCount(1)
      const path = await selected.getAttribute('data-path')
      expect(path).not.toBe(oldPath)
      expect(await readFile(join(directory, 'json', path!), 'utf8')).toBe(output)
      expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe(input)
      if (oldPath) expect(await readFile(join(directory, 'json', oldPath), 'utf8')).toBe('{"keep":"existing edit"}')
      await editor.fill('{"keep":"existing edit"}')
      oldPath = path!
      // Reopen the panel immediately, before the editor's idle save timer.
    }
    await page.reload()
    // Native views survive this reload; wait for the main shell's navigation subscription too.
    await expect(page.locator('.tool-button--active').filter({ hasText: 'JSON' })).toBeVisible()
    await expect.poll(async () => {
      const json = await tool('json')
      return json.locator('.vault-tree [aria-selected="true"]').getAttribute('data-path')
    }).toBe(oldPath)
  })

  test('highlights Markdown and HTML, imports each with its syntax, and supports repeated imports without overwriting notes', async () => {
    const cases = [
      ['html-markdown', 'fallback', '<h1>标题</h1><p><strong>正文</strong></p>', 'text/markdown'],
      ['clipboard-html', 'fallback', '<h1>源码</h1><script>window.__importExecuted=true</script>', 'text/html'],
      ['clipboard-text', 'plain *text*\n保留原文', '', 'text/plain'],
      ['base64-decode', 'aGVsbG8=', '', 'text/plain']
    ] as const
    const paths = new Set<string>()
    for (const [action, input, html, syntax] of cases) {
      for (let repeat = 0; repeat < 2; repeat++) {
        await run(action, input, html)
        const output = await previewText()
        if (syntax !== 'text/plain') await expect(preview().locator('span[class]').first()).toBeVisible()
        await page.getByRole('button', { name: '导入随手记', exact: true }).click()
        const notes = await tool('quickNote')
        await expect(notes.getByLabel('笔记内容', { exact: true })).toHaveText(output, { useInnerText: true })
        const selected = notes.locator('.quick-note-tree [aria-selected="true"]')
        await expect(selected).toHaveCount(1)
        await expect.poll(async () => paths.has((await selected.getAttribute('data-path'))!)).toBe(false)
        const path = (await selected.getAttribute('data-path'))!
        expect(paths.has(path)).toBe(false)
        paths.add(path)
        const file = await page.evaluate(path => window.mootool.readQuickNote(path), path)
        expect(file.content).toBe(output)
        expect(file.metadata.syntax).toBe(syntax)
        expect(await notes.evaluate(() => Boolean((window as typeof window & { __importExecuted?: boolean }).__importExecuted))).toBe(false)
        expect(await app.evaluate(({ clipboard }) => clipboard.readText())).toBe(input)
      }
    }
  })

  if (nativeViews === '1') test('imports into an already detached JSON workspace and focuses its window', async () => {
    await page.evaluate(() => window.mootool.detachToolWindow('json'))
    await expect.poll(() => page.evaluate(() => window.mootool.getToolWindowState('json'))).toMatchObject({ detached: true })
    await run('json-format', '{"detached":true}')
    await page.getByRole('button', { name: '导入 JSON 工作台', exact: true }).click()
    const json = await tool('json')
    await expect(json.locator('.json-editor .cm-content')).toHaveText('{\n  "detached": true\n}', { useInnerText: true })
    await expect.poll(() => json.evaluate(() => document.hasFocus())).toBe(true)
    expect(await page.evaluate(() => window.mootool.getToolWindowState('json'))).toMatchObject({ detached: true })
  })

  test('rejects invalid IPC imports before creating files and keeps a failed import preview available', async () => {
    const before = await page.evaluate(() => window.mootool.listJsonVault())
    expect(await page.evaluate(async () => {
      try { await window.mootool.importCommandResult({ actionId: 'json-format', content: 'invalid JSON' }); return false }
      catch { return true }
    })).toBe(true)
    expect(await page.evaluate(() => window.mootool.listJsonVault())).toEqual(before)
    await run('json-format', '{"retry":true}')
    await app.evaluate(({ ipcMain }) => {
      ipcMain.removeHandler('command-palette:import')
      ipcMain.handle('command-palette:import', () => { throw new Error('simulated write failure') })
    })
    await page.getByRole('button', { name: '导入 JSON 工作台', exact: true }).click()
    await expect(page.getByRole('alert')).toHaveText('导入失败，请重试。')
    await expect(preview()).toHaveText('{\n  "retry": true\n}', { useInnerText: true })
    await page.getByRole('button', { name: '复制结果', exact: true }).click()
    await expect.poll(() => app.evaluate(({ clipboard }) => clipboard.readText())).toBe('{\n  "retry": true\n}')
    await page.getByRole('button', { name: '关闭搜索', exact: true }).click()
  })
})
