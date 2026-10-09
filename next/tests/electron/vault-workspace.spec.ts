import { _electron as electron, expect, test, type ElectronApplication, type Page } from '@playwright/test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const launch = (root: string) => electron.launch({
  args: ['.', `--user-data-dir=${join(root, 'user')}`], cwd: process.cwd(),
  env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' }
})
const show = (page: Page, name: string) => page.locator('.tool-button').filter({ hasText: name }).click()
const selectedJson = (page: Page) => page.locator('.vault-tree [aria-selected="true"]')
const selectedNote = (page: Page) => page.locator('.quick-note-tree [aria-selected="true"]')
const savedPaths = async (root: string) => Object.values(JSON.parse(await readFile(join(root, 'user', 'mootool-next.json'), 'utf8')).vaultActiveFiles)

test('empty vaults create bound files before editing, without duplicate files on reload or empty searches', async () => {
  const root = await mkdtemp(join(tmpdir(), 'mootool-workspace-empty-'))
  const app = await launch(root)
  try {
    const page = await app.firstWindow()
    page.setDefaultTimeout(5000)
    await page.evaluate((root) => window.mootool.updateSettings({ vault: { jsonPath: `${root}/json`, quickNotePath: `${root}/notes` } }), root)
    await show(page, 'JSON')
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'snippet.json')
    const jsonEditor = page.locator('.json-editor .cm-content')
    await expect(jsonEditor).toHaveText('{}')
    await jsonEditor.fill('{"saved":true}')
    await expect.poll(() => readFile(join(root, 'json', 'snippet.json'), 'utf8')).toBe('{"saved":true}')
    await page.locator('.vault-panel__search input').fill('no-matches')
    await expect(page.locator('.vault-tree [role="treeitem"]')).toHaveCount(0)
    expect(await page.evaluate(() => window.mootool.listJsonVault())).toHaveLength(1)
    await page.locator('.vault-panel__actions').getByRole('button', { name: '定位当前文件' }).click()
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'snippet.json')

    await show(page, '随手记')
    await expect(selectedNote(page)).toHaveAttribute('data-path', 'Untitled.txt')
    const noteEditor = page.getByLabel('笔记内容', { exact: true })
    await noteEditor.fill('saved note body')
    await expect.poll(() => page.evaluate(() => window.mootool.readQuickNote('Untitled.txt'))).toMatchObject({ content: 'saved note body' })
    await page.locator('.quick-note-search input').fill('no-matches')
    await expect(page.locator('.quick-note-tree [role="treeitem"]')).toHaveCount(0)
    expect(await page.evaluate(() => window.mootool.listQuickNotes())).toHaveLength(1)
    await page.locator('.quick-note-tree-actions').getByRole('button', { name: '定位当前文件' }).click()
    await expect(selectedNote(page)).toHaveAttribute('data-path', 'Untitled.txt')
    await expect.poll(() => savedPaths(root)).toEqual(expect.arrayContaining(['snippet.json', 'Untitled.txt']))

    await page.reload()
    await expect(selectedNote(page)).toHaveAttribute('data-path', 'Untitled.txt')
    await expect(page.getByLabel('笔记内容', { exact: true })).toHaveText('saved note body')
    await show(page, 'JSON')
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'snippet.json')
    await expect(page.locator('.json-editor .cm-content')).toHaveText('{"saved":true}')
    expect(await page.evaluate(() => window.mootool.listJsonVault())).toHaveLength(1)
    expect(await page.evaluate(() => window.mootool.listQuickNotes())).toHaveLength(1)
    // Deleting the last file also leaves the editor bound to a new file.
    await page.locator('.vault-panel__actions').getByRole('button', { name: /^删除/ }).click()
    await page.locator('.desktop-dialog__confirm').click()
    await expect(page.locator('.json-editor .cm-content')).toHaveText('{}')
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'snippet.json')
    await show(page, '随手记')
    await page.locator('.quick-note-tree-actions').getByRole('button', { name: '删除', exact: true }).click()
    await page.getByRole('dialog').locator('.dialog-button--danger').click()
    await expect(page.getByLabel('笔记内容', { exact: true })).toHaveText('')
    await expect(selectedNote(page)).toHaveAttribute('data-path', 'Untitled.txt')
  } finally { await app.close(); await rm(root, { recursive: true, force: true }) }
})

test('app restarts restore the last opened files, fall back after deletion, and isolate changed vault roots', async () => {
  test.setTimeout(60_000)
  const root = await mkdtemp(join(tmpdir(), 'mootool-workspace-restore-'))
  let app: ElectronApplication | undefined
  try {
    app = await launch(root)
    let page = await app.firstWindow()
    await page.evaluate(async (root) => {
      await window.mootool.updateSettings({ vault: { jsonPath: `${root}/json`, quickNotePath: `${root}/notes`, jsonTreeExpandMode: 'collapseAll', quickNoteTreeExpandMode: 'collapseAll' } })
      await window.mootool.createQuickNoteFolder('folder')
      for (const name of ['a', 'z']) {
        await window.mootool.saveJsonVaultFile({ relativePath: `folder/${name}.json`, content: `{"name":"${name}"}` })
        const note = await window.mootool.createQuickNote({ title: name, parentPath: 'folder' })
        await window.mootool.saveQuickNote({ ...note, content: `${name} note` })
      }
    }, root)
    await show(page, 'JSON')
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'folder/a.json')
    await page.locator('.vault-tree [data-path="folder/z.json"]').click()
    await expect(page.locator('.json-editor .cm-content')).toHaveText('{"name":"z"}')
    await show(page, '随手记')
    await page.locator('.quick-note-tree [data-path="folder/a.txt"]').click()
    await expect(page.getByLabel('笔记内容', { exact: true })).toHaveText('a note')
    await expect.poll(() => savedPaths(root)).toEqual(expect.arrayContaining(['folder/z.json', 'folder/a.txt']))
    // Selecting a directory does not replace the remembered file.
    await page.locator('.quick-note-tree [data-path="folder"]').click()
    await app.close()

    app = await launch(root)
    page = await app.firstWindow()
    await expect(selectedNote(page)).toHaveAttribute('data-path', 'folder/a.txt')
    await expect(selectedNote(page)).toBeInViewport()
    await expect(page.getByLabel('笔记内容', { exact: true })).toHaveText('a note')
    await show(page, 'JSON')
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'folder/z.json')
    await expect(selectedJson(page)).toBeInViewport()
    await expect(page.locator('.json-editor .cm-content')).toHaveText('{"name":"z"}')
    await app.close()
    app = undefined
    await rm(join(root, 'json', 'folder', 'z.json'))
    await rm(join(root, 'notes', 'folder', 'a.txt'))

    app = await launch(root)
    page = await app.firstWindow()
    await show(page, 'JSON')
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'folder/a.json')
    await expect(page.locator('.json-editor .cm-content')).toHaveText('{"name":"a"}')
    await show(page, '随手记')
    await expect(selectedNote(page)).toHaveAttribute('data-path', 'folder/z.txt')
    await expect(page.getByLabel('笔记内容', { exact: true })).toHaveText('z note')

    // A custom root equal to the data directory differs from its default subfolder.
    await page.evaluate((root) => window.mootool.updateSettings({ data: { directory: `${root}/data` }, vault: { jsonPath: '', quickNotePath: '' } }), root)
    await show(page, 'JSON')
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'snippet.json')
    await writeFile(join(root, 'data', 'root.json'), '{"customRoot":true}')
    await page.evaluate((root) => window.mootool.updateSettings({ vault: { jsonPath: `${root}/data` } }), root)
    await expect(selectedJson(page)).not.toHaveAttribute('data-path', 'snippet.json')
    expect(await readFile(join(root, 'data', 'json-vault', 'snippet.json'), 'utf8')).toBe('{}')
    // Changing roots must not write the old editor contents into a new file.
    await page.evaluate((root) => window.mootool.updateSettings({ vault: { jsonPath: `${root}/other-json`, quickNotePath: `${root}/other-notes` } }), root)
    await expect(selectedNote(page)).toHaveAttribute('data-path', 'Untitled.txt')
    await expect(page.getByLabel('笔记内容', { exact: true })).toHaveText('')
    await show(page, 'JSON')
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'snippet.json')
    await expect(page.locator('.json-editor .cm-content')).toHaveText('{}')
    expect(await readFile(join(root, 'other-json', 'snippet.json'), 'utf8')).toBe('{}')
    expect(await page.evaluate(() => window.mootool.readQuickNote('Untitled.txt'))).toMatchObject({ content: '' })
    await page.evaluate((root) => window.mootool.updateSettings({ vault: { jsonPath: `${root}/json`, quickNotePath: `${root}/notes` } }), root)
    await expect(selectedJson(page)).toHaveAttribute('data-path', 'folder/a.json')
    await show(page, '随手记')
    await expect(selectedNote(page)).toHaveAttribute('data-path', 'folder/z.txt')
  } finally { await app?.close(); await rm(root, { recursive: true, force: true }) }
})
