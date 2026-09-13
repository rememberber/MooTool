import { _electron as electron, expect, test } from '@playwright/test'
import { mkdtemp, mkdir, readFile, realpath, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { Client } from '@modelcontextprotocol/sdk/client/index.js'
import { StdioClientTransport } from '@modelcontextprotocol/sdk/client/stdio.js'
import { parse } from 'smol-toml'
import { execFile, spawnSync } from 'node:child_process'

import { promisify } from 'node:util'

test('installs from Settings and runs the resulting MCP and standalone Skill', async ({}, testInfo) => {
  test.setTimeout(process.env.MOOTOOL_CODEX_EXECUTABLE ? 300_000 : 120_000)
  const home = await mkdtemp(join(tmpdir(), 'mootool-ai-ui-'))
  const notes = join(home, 'notes')
  const documents = join(home, 'json')
  await mkdir(notes)
  await mkdir(documents)
  await writeFile(join(notes, 'fixture.md'), '---\ntitle: MooTool验收\n---\nMooTool integration fixture 🐮')
  await writeFile(join(documents, 'fixture.json'), '{"acceptance":42}')
  const config = join(home, '.codex', 'config.toml')
  await mkdir(join(home, '.codex'))
  await writeFile(config, '# Preserve existing settings\nmodel = "test-model"\n')
  const executablePath = process.env.MOOTOOL_TEST_EXECUTABLE
  const app = await electron.launch({
    ...(executablePath ? { executablePath } : {}),
    args: [...(executablePath ? [] : ['.']), ...(process.platform === 'linux' ? ['--no-sandbox'] : []), `--user-data-dir=${join(home, 'app-data')}`], cwd: process.cwd(),
    env: { ...process.env, NODE_ENV: 'test', HOME: home, USERPROFILE: home, CODEX_HOME: join(home, '.codex'), CLAUDE_CONFIG_DIR: join(home, '.claude') }
  })
  try {
    // macOS resolves the native home independently of the HOME environment variable.
    // Set it before the integration service is lazily constructed.
    await app.evaluate(({ app }, isolatedHome) => app.setPath('home', isolatedHome), home)
    const page = await app.firstWindow()
    await page.waitForLoadState('domcontentloaded')
    await page.evaluate(() => window.mootool.updateSettings({ general: { language: 'en-US', autoCheckUpdates: false } }))
    await page.evaluate((roots) => window.mootool.updateSettings({ vault: { quickNotePath: roots.notes, jsonPath: roots.documents } }), { notes, documents })
    const preview = await page.evaluate(() => window.mootool.previewAiIntegration({ client: 'codex', mode: 'both' }))
    // Never click Install unless every destination is inside our isolated test home.
    expect(preview.files.every((file) => file.path.startsWith(`${home}/`) || file.path.startsWith(`${home}\\`))).toBe(true)
    await page.evaluate(() => window.mootool.openSettings('ai'))
    await expect(page.getByRole('heading', { name: 'Use MooTool with AI' })).toBeVisible()
    await expect(page.getByRole('button', { name: 'Install in one click' })).toBeEnabled()
    await page.getByRole('button', { name: 'Install in one click' }).click()
    await expect(page.getByRole('button', { name: 'Installed', exact: true })).toBeVisible({ timeout: 20_000 })
    await expect(page.getByText('Original configuration backups')).toBeVisible()
    await page.getByRole('button', { name: 'Test connection', exact: true }).click()
    await expect(page.getByRole('status').filter({ hasText: '11 tools available' }).first()).toBeVisible()
    await page.screenshot({ path: testInfo.outputPath('ai-integration.png') })

    const source = await readFile(config, 'utf8')
    expect(source).toContain('# Preserve existing settings')
    const installed = (parse(source).mcp_servers as Record<string, unknown>).mootool as { command: string; args: string[]; env: Record<string, string> }
    const client = new Client({ name: 'installed-config-test', version: '1.0.0' })
    const transport = new StdioClientTransport({ ...installed, stderr: 'ignore' })
    try {
      await client.connect(transport, { timeout: 10_000 })
      const result = await client.callTool({ name: 'mootool_hash', arguments: { text: 'abc' } })
      expect(result.isError).not.toBe(true)
      expect((await client.callTool({ name: 'mootool_notes_read', arguments: { path: 'fixture.md' } })).isError).toBe(true)
      await page.getByRole('checkbox', { name: 'Allow reading Quick Notes', exact: true }).click()
      await expect.poll(() => page.evaluate(() => window.mootool.getAiDataAccess())).toEqual({ notes: await realpath(notes), json: null })
      await page.getByRole('checkbox', { name: 'Allow reading JSON documents', exact: true }).click()
      await expect.poll(() => page.evaluate(() => window.mootool.getAiDataAccess())).toEqual({ notes: await realpath(notes), json: await realpath(documents) })
      const search = await client.callTool({ name: 'mootool_notes_search', arguments: { query: '验收' } })
      expect(JSON.stringify(search.content)).toContain('fixture.md')
      const read = await client.callTool({ name: 'mootool_notes_read', arguments: { path: 'fixture.md' } })
      expect(JSON.stringify(read.content)).toContain('MooTool integration fixture 🐮')
      expect(JSON.stringify(read.content)).not.toContain('title:')
      const document = await client.callTool({ name: 'mootool_json_documents_read', arguments: { path: 'fixture.json' } })
      expect(document.isError).not.toBe(true)
      expect(JSON.stringify(document.content)).toContain('42')

      // Opt-in live Codex acceptance. Auth stays in the existing CODEX_HOME; only
      // our generated MCP stanza is provided, and all tool data are test fixtures.
      if (process.env.MOOTOOL_CODEX_EXECUTABLE) {
        const args = ['exec', '--ignore-user-config', '--ephemeral', '--json', '--skip-git-repo-check', '--sandbox', 'read-only', '-C', home,
          '-c', `mcp_servers.mootool.command=${JSON.stringify(installed.command)}`,
          '-c', `mcp_servers.mootool.args=${JSON.stringify(installed.args)}`,
          '-c', 'mcp_servers.mootool.env={ELECTRON_RUN_AS_NODE="1"}',
          'This is an integration test. Use only the mootool MCP server. Call mootool_json_format on {"b":2,"a":1} with sortKeys true. Call mootool_notes_search for 验收, then mootool_notes_read for fixture.md. Call mootool_json_documents_read for fixture.json. Do not use shell or other tools. Report the returned values briefly.']
        const live = await promisify(execFile)(process.env.MOOTOOL_CODEX_EXECUTABLE, args, { encoding: 'utf8', timeout: 180_000, maxBuffer: 10_000_000 })
        const events = live.stdout.split('\n').filter((line) => line.startsWith('{')).map((line) => JSON.parse(line))
        const calls = events.filter((event) => event.type === 'item.completed' && event.item?.type === 'mcp_tool_call').map((event) => event.item)
        await writeFile(testInfo.outputPath('codex-mcp-calls.json'), JSON.stringify(calls, null, 2))
        for (const name of ['mootool_json_format', 'mootool_notes_search', 'mootool_notes_read', 'mootool_json_documents_read']) {
          expect(calls.some((call) => call.tool === name && call.status === 'completed' && !call.error && call.result?.isError !== true), `Codex must complete ${name}`).toBe(true)
        }
      }
      await page.getByRole('checkbox', { name: 'Allow reading Quick Notes', exact: true }).click()
      await expect.poll(() => page.evaluate(() => window.mootool.getAiDataAccess()).then((access) => access.notes)).toBeNull()
      expect((await client.callTool({ name: 'mootool_notes_read', arguments: { path: 'fixture.md' } })).isError).toBe(true)
      // Changing a vault location revokes all grants before the new setting is saved.
      await page.evaluate((path) => window.mootool.updateSettings({ vault: { jsonPath: path } }), notes)
      expect(await page.evaluate(() => window.mootool.getAiDataAccess())).toEqual({ notes: null, json: null })
      expect((await client.callTool({ name: 'mootool_json_documents_read', arguments: { path: 'fixture.json' } })).isError).toBe(true)
    } finally { await transport.close() }

    const runtimeFile = join(home, '.agents', 'skills', 'mootool', 'runtime.md')
    const runtime = await readFile(runtimeFile, 'utf8')
    expect(runtime).toContain(installed.args[0])
    // Execute the exact generated Skill commands, including paths and UTF-8.
    const windows = process.platform === 'win32'
    const snippets = [...runtime.matchAll(/```(?:sh|powershell)\n([\s\S]*?)\n```/g)].map((match) => match[1])
    const run = (command: string) => spawnSync(windows ? 'powershell.exe' : '/bin/sh', windows ? ['-NoProfile', '-NonInteractive', '-Command', command] : ['-c', command], { cwd: home, encoding: 'utf8', timeout: 20_000 })
    const listed = run(snippets[0])
    expect(listed.status, listed.stderr).toBe(0)
    expect(JSON.parse(listed.stdout)).toHaveLength(11)
    await writeFile(join(home, 'arguments.json'), JSON.stringify({ text: '{"牛":"🐮"}', spaces: 0 }))
    const called = run(snippets[1])
    expect(called.status, called.stderr).toBe(0)
    expect(JSON.parse(called.stdout).content[0].text).toBe('{"牛":"🐮"}')

    // Missing installer-owned files are repairable without altering user files.
    await rm(runtimeFile)
    await page.getByRole('button', { name: 'Refresh preview', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Repair / update', exact: true })).toBeEnabled()
    await page.getByRole('button', { name: 'Repair / update', exact: true }).click()
    await expect(page.getByRole('button', { name: 'Installed', exact: true })).toBeVisible()
    expect(await readFile(runtimeFile, 'utf8')).toBe(runtime)
    await writeFile(join(home, '.agents', 'skills', 'mootool', 'personal.txt'), 'keep')
    await page.getByRole('button', { name: 'Uninstall selected integration', exact: true }).click()
    await expect(page.getByRole('status').filter({ hasText: 'Selected integration removed.' }).first()).toBeVisible()
    expect((parse(await readFile(config, 'utf8')).mcp_servers as Record<string, unknown> | undefined)?.mootool).toBeUndefined()
    expect(await readFile(config, 'utf8')).toContain('# Preserve existing settings')
    expect(await readFile(join(home, '.agents', 'skills', 'mootool', 'personal.txt'), 'utf8')).toBe('keep')
    await expect(readFile(runtimeFile)).rejects.toMatchObject({ code: 'ENOENT' })
    await page.getByRole('combobox', { name: 'AI client', exact: true }).selectOption('cursor')
    await expect(page.getByRole('combobox', { name: 'Integration', exact: true })).toHaveValue('mcp')
    await expect(page.getByRole('combobox', { name: 'Integration', exact: true }).getByRole('option')).toHaveCount(1)
  } finally {
    await app.close()
    await rm(home, { recursive: true, force: true })
  }
})
