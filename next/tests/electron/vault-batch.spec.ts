import { _electron as electron, expect, test } from '@playwright/test'
import { mkdtemp, mkdir, readFile, readdir, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

test('JSON and notes support multiselect, batch duplicate and collision-safe export', async () => {
  const root = await mkdtemp(join(tmpdir(), 'mootool-vault-batch-'))
  const destination = join(root, 'export')
  await mkdir(destination)
  const app = await electron.launch({ args: ['.', `--user-data-dir=${join(root, 'user')}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })
  try {
    const page = await app.firstWindow()
    page.setDefaultTimeout(5000)
    await page.evaluate(async root => {
      await window.mootool.updateSettings({ vault: { jsonPath: `${root}/json`, quickNotePath: `${root}/notes`, jsonTreeExpandMode: 'expandAll', quickNoteTreeExpandMode: 'expandAll' } })
      for (const name of ['a', 'b', 'c']) {
        await window.mootool.saveJsonVaultFile({ relativePath: `${name}.json`, content: `{"name":"${name}"}` })
        const note = await window.mootool.createQuickNote({ title: name })
        await window.mootool.saveQuickNote({ ...note, content: `${name} body` })
      }
    }, root)
    await app.evaluate(({ dialog }, path) => { dialog.showOpenDialog = (async () => ({ canceled: false, filePaths: [path] })) as typeof dialog.showOpenDialog }, destination)
    await page.locator('.tool-button').filter({ hasText: 'JSON' }).click()
    const jsonRows = page.locator('.vault-tree [role="treeitem"]')
    await expect(jsonRows).toHaveCount(3)
    await jsonRows.nth(0).click()
    await page.locator('.json-editor .cm-content').fill('{"edited":true}')
    await jsonRows.nth(1).click({ modifiers: ['ControlOrMeta'] })
    await expect(page.locator('.vault-tree [aria-selected="true"]')).toHaveCount(2)
    await jsonRows.nth(1).click({ button: 'right' })
    await page.getByRole('menuitem', { name: '导出选中文件' }).click()
    await expect.poll(() => readdir(destination)).toEqual(['a.json', 'b.json'])
    expect(await readFile(join(destination, 'a.json'), 'utf8')).toBe('{"edited":true}')
    await jsonRows.nth(0).click({ button: 'right' })
    await page.getByRole('menuitem', { name: '复制', exact: false }).click()
    await expect(jsonRows).toHaveCount(5)
    expect(await readFile(join(root, 'json', 'a Copy.json'), 'utf8')).toBe('{"edited":true}')
    await page.locator('.tool-button').filter({ hasText: '随手记' }).click()
    await page.locator('.quick-note-list-controls select').selectOption('name')
    const noteRows = page.locator('.quick-note-tree [role="treeitem"]')
    await expect(noteRows).toHaveCount(3)
    await noteRows.filter({ hasText: /^a$/ }).click()
    await expect(page.getByLabel('笔记内容', { exact: true })).toHaveText('a body')
    await page.getByLabel('笔记内容', { exact: true }).fill('edited body')
    await noteRows.filter({ hasText: /^c$/ }).click({ modifiers: ['Shift'] })
    await expect(page.locator('.quick-note-tree [aria-selected="true"]')).toHaveCount(3)
    await noteRows.nth(1).click({ button: 'right' })
    await page.getByRole('menuitem', { name: '导出选中笔记' }).click()
    await expect.poll(() => readdir(destination)).toHaveLength(5)
    expect(await readFile(join(destination, 'a.txt'), 'utf8')).toBe('edited body')
    await noteRows.nth(0).click({ button: 'right' })
    await page.getByRole('menuitem', { name: '创建副本', exact: true }).click()
    await expect(noteRows).toHaveCount(6)
    const duplicate = await page.evaluate(() => window.mootool.readQuickNote('a Copy.txt'))
    expect(duplicate.content).toBe('edited body')
    // Right-clicking an unselected item switches the batch target to that item.
    await noteRows.filter({ hasText: /^b$/ }).click({ button: 'right' })
    await expect(page.locator('.quick-note-tree [aria-selected="true"]')).toHaveCount(1)
    await page.getByRole('menuitem', { name: '导出选中笔记' }).click()
    await expect.poll(() => readdir(destination)).toHaveLength(6)
    expect(await readFile(join(destination, 'b-1.txt'), 'utf8')).toBe('b body')
  } finally { await app.close(); await rm(root, { recursive: true, force: true }) }
})
