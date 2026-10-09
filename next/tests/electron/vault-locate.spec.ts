import { _electron as electron, expect, test } from '@playwright/test'
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

test('vault toolbars locate the open file after scrolling, multiselect and filtering', async () => {
  const root = await mkdtemp(join(tmpdir(), 'mootool-vault-locate-'))
  const app = await electron.launch({
    args: ['.', `--user-data-dir=${join(root, 'user')}`],
    cwd: process.cwd(),
    env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' }
  })
  try {
    const page = await app.firstWindow()
    await page.evaluate(async (root) => {
      await window.mootool.updateSettings({ vault: {
        jsonPath: `${root}/json`, quickNotePath: `${root}/notes`,
        jsonTreeExpandMode: 'expandAll', quickNoteTreeExpandMode: 'expandAll'
      } })
      await window.mootool.createQuickNoteFolder('folder')
      await window.mootool.createQuickNoteFolder('folder/deep')
      await window.mootool.createJsonVaultFolder('folder')
      await window.mootool.createJsonVaultFolder('folder/deep')
      for (const title of [...Array.from({ length: 35 }, (_, index) => `entry-${String(index).padStart(2, '0')}`), 'zz-current']) {
        await window.mootool.saveJsonVaultFile({ relativePath: `folder/deep/${title}.json`, content: `{"name":"${title}"}` })
        const note = await window.mootool.createQuickNote({ title, parentPath: 'folder/deep' })
        await window.mootool.saveQuickNote({ ...note, content: `${title} body` })
      }
    }, root)

    for (const tool of [
      { name: 'JSON', actions: '.vault-panel__actions', tree: '.vault-tree', search: '.vault-panel__search input', extension: 'json', editor: '.json-editor .cm-content' },
      { name: '随手记', actions: '.quick-note-tree-actions', tree: '.quick-note-tree-scroll', search: '.quick-note-search input', extension: 'txt', editor: '.quick-note-editor-shell .cm-content' }
    ]) {
      await page.locator('.tool-button').filter({ hasText: tool.name }).click()
      if (tool.name === '随手记') await page.locator('.quick-note-list-controls select').selectOption('name')
      const actions = page.locator(tool.actions)
      await expect(actions.locator('button')).toHaveCount(7)
      expect(await actions.locator('button svg').evaluateAll((icons) => icons.map((icon) => [...icon.classList].filter((name) => name.startsWith('lucide-')).at(-1)))).toEqual([
        'lucide-file-plus-corner', 'lucide-folder-plus', 'lucide-save', 'lucide-trash-2', 'lucide-git-branch', 'lucide-fold-vertical', 'lucide-locate-fixed'
      ])
      await expect(actions).toHaveCSS('display', 'flex')
      await expect(actions).toHaveCSS('gap', '3px')
      expect(await actions.locator('button').evaluateAll(([first, second]) => {
        const a = first.getBoundingClientRect()
        const b = second.getBoundingClientRect()
        return { gap: b.left - a.right, sameRow: a.top === b.top }
      })).toEqual({ gap: 3, sameRow: true })
      const path = `folder/deep/zz-current.${tool.extension}`
      const row = page.locator(`${tool.tree} [data-path="${path}"]`)
      const locate = actions.getByRole('button', { name: '定位当前文件', exact: true })
      await row.click()
      await expect(page.locator(tool.editor)).toContainText('zz-current')

      // Scroll away from the open file without changing the editor.
      await page.locator(tool.tree).evaluate((tree) => { tree.scrollTop = 0 })
      await expect(row).not.toBeInViewport()
      await locate.click()
      await expect(row).toBeInViewport()
      await expect(row).toBeFocused()

      // Locating resets the batch selection to the open file.
      await page.locator(`${tool.tree} [data-path="folder/deep/entry-00.${tool.extension}"]`).click({ modifiers: ['ControlOrMeta'] })
      await expect(page.locator(`${tool.tree} [aria-selected="true"]`)).toHaveCount(2)
      await locate.click()
      await expect(page.locator(`${tool.tree} [aria-selected="true"]`)).toHaveCount(1)
      await expect(row).toHaveAttribute('aria-selected', 'true')

      // A search can hide the current file; locate restores the full tree.
      const editedContent = tool.name === 'JSON' ? '{"name":"zz-current","edited":true}' : 'zz-current edited body'
      await page.locator(tool.editor).fill(editedContent)
      await page.locator(tool.search).fill('no-matching-file')
      await expect(row).toHaveCount(0)
      await locate.click()
      await expect(page.locator(tool.search)).toHaveValue('')
      await expect(row).toBeInViewport()
      await expect(row).toHaveAttribute('aria-selected', 'true')
      await expect(page.locator(tool.editor)).toHaveText(editedContent)

      if (tool.name === 'JSON') {
        // A directory selection must still locate the file open in the editor.
        await page.locator(`${tool.tree} [data-path="folder"]`).click()
        await expect(row).toHaveCount(0)
        await locate.click()
        await expect(row).toBeInViewport()
        await expect(row).toHaveAttribute('aria-selected', 'true')
      }
    }
  } finally {
    await app.close()
    await rm(root, { recursive: true, force: true })
  }
})
