import { _electron as electron, expect, test, type ElectronApplication, type Locator, type Page } from '@playwright/test'
import { chmod, mkdtemp, mkdir, readFile, readdir, realpath, rm, writeFile } from 'node:fs/promises'
import { createServer, type Server } from 'node:http'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { DatabaseSync } from 'node:sqlite'

let electronApp: ElectronApplication
let mainPage: Page
let userDataDirectory: string
let updateServer: Server
let ollamaServer: Server
const ollamaRequests: string[] = []
const openAiUsageRequests: Array<{ path: string; authorization: string }> = []

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
              packageType: process.platform === 'darwin' ? 'dmg' : 'test-installer',
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
  ollamaServer = createServer((request, response) => {
    ollamaRequests.push(`${request.method} ${request.url}`)
    let source = ''
    request.setEncoding('utf8')
    request.on('data', (chunk) => { source += chunk })
    request.on('end', () => {
      response.setHeader('content-type', 'application/json')
      if (request.url?.startsWith('/v1/organization/usage/completions')) {
        openAiUsageRequests.push({ path: request.url, authorization: String(request.headers.authorization ?? '') })
        const startTime = Math.floor(Date.now() / 1000) - 3600
        response.end(JSON.stringify({ data: [{ start_time: startTime, end_time: startTime + 3600, results: [{ input_tokens: 120, output_tokens: 30, input_cached_tokens: 50, num_model_requests: 2, project_id: 'proj-provider-e2e', model: 'gpt-provider-e2e' }] }], has_more: false, next_page: null }))
      } else if (request.url?.startsWith('/v1/organization/costs')) {
        openAiUsageRequests.push({ path: request.url, authorization: String(request.headers.authorization ?? '') })
        const startTime = Math.floor(Date.now() / 1000) - 3600
        response.end(JSON.stringify({ data: [{ start_time: startTime, end_time: startTime + 3600, results: [{ amount: { value: 1.25, currency: 'usd' }, line_item: 'Text models', project_id: 'proj-provider-e2e' }] }], has_more: false, next_page: null }))
      } else if (request.url === '/api/version') response.end(JSON.stringify({ version: '0.13.3-e2e' }))
      else if (request.url === '/api/tags') response.end(JSON.stringify({ models: [{
        name: 'qwen3:8b-e2e', model: 'qwen3:8b-e2e', digest: 'sha256:e2e-model-digest', size: 4_294_967_296,
        modified_at: '2026-07-17T12:00:00.000Z', details: { format: 'gguf', family: 'qwen3', parameter_size: '8B', quantization_level: 'Q4_K_M' }
      }] }))
      else if (request.url === '/api/ps') response.end(JSON.stringify({ models: [{
        name: 'qwen3:8b-e2e', digest: 'sha256:e2e-model-digest', size: 5_368_709_120, size_vram: 0,
        context_length: 32768, expires_at: '2026-07-18T18:00:00.000Z'
      }] }))
      else if (request.url === '/api/show' && request.method === 'POST') {
        const body = JSON.parse(source) as { model?: string; verbose?: boolean }
        if (body.model !== 'qwen3:8b-e2e' || body.verbose !== false) { response.statusCode = 400; response.end('{}'); return }
        response.end(JSON.stringify({
          modified_at: '2026-07-17T12:00:00.000Z', capabilities: ['completion', 'tools'], parameters: 'temperature 0.7', license: 'E2E model license',
          template: 'THIS TEMPLATE MUST NOT BE RENDERED', details: { format: 'gguf', family: 'qwen3', parameter_size: '8B', quantization_level: 'Q4_K_M' },
          model_info: { 'qwen3.context_length': 131072 }
        }))
      } else if (request.url === '/api/embed' && request.method === 'POST') {
        const body = JSON.parse(source) as { model?: string; input?: string | string[] }
        if (body.model !== 'qwen3:8b-e2e') { response.statusCode = 400; response.end('{}'); return }
        const inputs = Array.isArray(body.input) ? body.input : [body.input ?? '']
        response.end(JSON.stringify({ model: body.model, embeddings: inputs.map((text) => text.includes('SQLite') || text.includes('database') ? [1, 0] : [0.8, 0.2]) }))
      } else { response.statusCode = 404; response.end('{}') }
    })
  })
  await new Promise<void>((resolve) => ollamaServer.listen(0, '127.0.0.1', resolve))
  const ollamaAddress = ollamaServer.address()
  if (!ollamaAddress || typeof ollamaAddress === 'string') throw new Error('Ollama E2E server did not expose a port')
  const ollamaModelDirectory = join(userDataDirectory, 'ollama-models')
  const agentBinaryDirectory = join(userDataDirectory, 'agent-bin')
  await Promise.all([mkdir(ollamaModelDirectory, { recursive: true }), mkdir(agentBinaryDirectory, { recursive: true })])
  const codexBinary = join(agentBinaryDirectory, 'codex')
  const claudeBinary = join(agentBinaryDirectory, 'claude')
  await Promise.all([writeFile(codexBinary, '#!/bin/sh\nexit 0\n'), writeFile(claudeBinary, '#!/bin/sh\nexit 0\n')])
  await Promise.all([chmod(codexBinary, 0o755), chmod(claudeBinary, 0o755)])
  electronApp = await electron.launch({
    args: ['.', `--user-data-dir=${userDataDirectory}`],
    cwd: process.cwd(),
    env: {
      ...process.env,
      NODE_ENV: 'test',
      MOOTOOL_UPDATE_FEED_URL: `http://127.0.0.1:${updateAddress.port}/update-manifest.json`,
      OLLAMA_HOST: `http://127.0.0.1:${ollamaAddress.port}`,
      OLLAMA_MODELS: ollamaModelDirectory,
      MOOTOOL_OPENAI_USAGE_BASE_URL: `http://127.0.0.1:${ollamaAddress.port}`,
      PATH: `${agentBinaryDirectory}:${process.env.PATH ?? ''}`,
      E2E_AGENT_SECRET: 'super-secret-value-that-must-not-cross-ipc'
    }
  })
  mainPage = await electronApp.firstWindow()
  await mainPage.waitForLoadState('domcontentloaded')
})

test.afterAll(async () => {
  await electronApp.close()
  await new Promise<void>((resolve, reject) => updateServer.close((error) => error ? reject(error) : resolve()))
  await new Promise<void>((resolve, reject) => ollamaServer.close((error) => error ? reject(error) : resolve()))
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
    schemaVersion: 10,
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
  await expect(mainPage.locator('.tool-button')).toHaveCount(36)

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

test('runs the AI Doctor through the read-only Electron bridge', async () => {
  await openTool('AI 工作台', 'AI 工作台')
  await expect(mainPage.getByText('只读扫描', { exact: true })).toBeVisible()
  await expect(mainPage.locator('.ai-summary-card')).toHaveCount(4)
  await expect.poll(() => mainPage.locator('.ai-entity-row').count()).toBeGreaterThanOrEqual(3)

  const snapshot = await mainPage.evaluate((projectRoot) => window.mootool.scanAiEnvironment({ projectRoot }), process.cwd())
  expect(snapshot).toMatchObject({ readOnly: true, projectRoot: process.cwd() })
  expect(snapshot.clients).toEqual(expect.arrayContaining([expect.objectContaining({ id: 'codex' }), expect.objectContaining({ id: 'claudeCode' })]))
  expect(snapshot.runtimes).toEqual(expect.arrayContaining([expect.objectContaining({ id: 'ollama', endpoint: 'http://127.0.0.1:11434' })]))
  expect(JSON.stringify(snapshot)).not.toContain('api_key')

  await expect.poll(() => mainPage.locator('.ai-doctor-shell').evaluate((shell) => shell.scrollWidth <= shell.clientWidth)).toBe(true)
})

test('opens the Skill and Instruction managers and completes a verified Claude compatibility change', async () => {
  await openTool('Skill 管理', 'Skill 管理')
  await expect(mainPage.locator('.workspace-tool-session:not([hidden]) .ai-manager-metric')).toHaveCount(4)
  await expect(mainPage.locator('.workspace-tool-session:not([hidden]) .ai-readonly-badge')).toHaveText('只读扫描')

  await openTool('编码规约', '编码规约管理')
  await expect(mainPage.locator('.workspace-tool-session:not([hidden]) .ai-manager-metric')).toHaveCount(4)

  const projectRoot = await mkdtemp(join(tmpdir(), 'mootool-ai-change-e2e-'))
  try {
    const targetPath = join(projectRoot, 'src', 'components')
    await Promise.all([
      mkdir(targetPath, { recursive: true }),
      mkdir(join(projectRoot, '.claude', 'rules'), { recursive: true })
    ])
    await writeFile(join(projectRoot, 'AGENTS.md'), '# Shared project rules\n')
    await writeFile(join(projectRoot, 'src', 'AGENTS.md'), '# Source tree rules\n')
    await writeFile(join(projectRoot, '.claude', 'rules', 'source.md'), '---\npaths:\n  - src/**\n---\n# Claude source rule\n')
    await electronApp.evaluate(({ dialog }, root) => {
      dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [root] })
    }, projectRoot)
    const activeTool = mainPage.locator('.workspace-tool-session:not([hidden])')
    const chooseProject = activeTool.getByRole('button', { name: '选择项目', exact: true })
    await expect(chooseProject).toBeEnabled()
    await chooseProject.click()
    await expect(activeTool.locator('.ai-manager-scope strong')).toHaveText(await realpath(projectRoot))

    await electronApp.evaluate(({ dialog }, target) => {
      dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [target] })
    }, targetPath)
    await activeTool.getByRole('button', { name: '选择目录并预览', exact: true }).click()
    const effectiveDialog = mainPage.getByRole('dialog', { name: '最终生效规约' })
    await expect(effectiveDialog).toBeVisible()
    await expect(effectiveDialog).toContainText('src/AGENTS.md')
    await expect(effectiveDialog).toContainText('.claude/rules/source.md')
    await expect(effectiveDialog).toContainText('目录祖先')
    await expect(effectiveDialog).toContainText('路径匹配')
    await effectiveDialog.getByRole('button', { name: '关闭', exact: true }).click()

    const preview = activeTool.getByRole('button', { name: '预览变更', exact: true })
    await expect(preview).toBeEnabled()
    await preview.click()
    const dialog = mainPage.getByRole('dialog', { name: '审查 Claude 兼容入口' })
    await expect(dialog.locator('pre')).toContainText('@AGENTS.md')
    await dialog.getByRole('button', { name: '批准并应用', exact: true }).click()
    await expect(dialog.getByText('变更已验证通过。你可以关闭窗口，或立即回滚。')).toBeVisible()
    expect(await readFile(join(projectRoot, 'CLAUDE.md'), 'utf8')).toContain('@AGENTS.md')

    await dialog.getByRole('button', { name: '回滚变更', exact: true }).click()
    await expect(dialog).toHaveCount(0)
    await expect(readFile(join(projectRoot, 'CLAUDE.md'))).rejects.toMatchObject({ code: 'ENOENT' })
  } finally {
    await rm(projectRoot, { recursive: true, force: true })
  }
})

test('discovers and safely copies an MCP server across clients with rollback', async () => {
  const projectRoot = await mkdtemp(join(tmpdir(), 'mootool-ai-mcp-e2e-'))
  const mcpServer = createServer((request, response) => {
    let source = ''
    request.setEncoding('utf8')
    request.on('data', (chunk) => { source += chunk })
    request.on('end', () => {
      const payload = JSON.parse(source) as { id?: number; method: string }
      if (payload.id === undefined) { response.statusCode = 202; response.end(); return }
      const result = payload.method === 'initialize'
        ? { protocolVersion: '2025-11-25', capabilities: {} }
        : payload.method === 'tools/list'
          ? { tools: [{ name: 'e2e-tool' }] }
          : payload.method === 'resources/list'
            ? { resources: [{ uri: 'e2e://resource' }] }
            : { prompts: [] }
      response.setHeader('content-type', 'application/json')
      response.end(JSON.stringify({ jsonrpc: '2.0', id: payload.id, result }))
    })
  })
  try {
    await new Promise<void>((resolve) => mcpServer.listen(0, '127.0.0.1', resolve))
    const mcpAddress = mcpServer.address()
    if (!mcpAddress || typeof mcpAddress === 'string') throw new Error('MCP E2E server has no port')
    await writeFile(join(projectRoot, '.mcp.json'), JSON.stringify({
      futureField: { keep: true },
      mcpServers: {
        'mootool-e2e-remote': {
          type: 'http',
          url: `http://127.0.0.1:${mcpAddress.port}/mcp`,
          headers: { Authorization: 'Bearer ${E2E_MCP_TOKEN}' }
        }
      }
    }, null, 2))
    await electronApp.evaluate(({ dialog }, root) => {
      dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [root] })
    }, projectRoot)
    await electronApp.evaluate(() => { process.env.E2E_MCP_TOKEN = 'e2e-probe-token' })

    await openTool('MCP 管理', 'MCP 管理')
    const activeTool = mainPage.locator('.workspace-tool-session:not([hidden])')
    await activeTool.locator('.tool-header-actions').getByRole('button', { name: '选择项目', exact: true }).click()
    await expect(activeTool.locator('.ai-manager-scope strong')).toHaveText(await realpath(projectRoot))
    const serverCard = activeTool.locator('.ai-mcp-card').filter({ hasText: 'mootool-e2e-remote' }).filter({ hasText: 'Claude Code' })
    await expect(serverCard).toContainText('Streamable HTTP')
    await serverCard.getByRole('button', { name: '测试连接', exact: true }).click()
    const probeDialog = mainPage.getByRole('dialog', { name: 'MCP 连接与能力检查' })
    await probeDialog.getByRole('button', { name: '确认并开始检查', exact: true }).click()
    await expect(probeDialog).toContainText('连接正常')
    await expect(probeDialog.locator('.ai-mcp-capabilities')).toContainText('1')
    await probeDialog.getByRole('button', { name: '关闭', exact: true }).click()
    await serverCard.getByRole('button', { name: '复制', exact: true }).click()

    const dialog = mainPage.getByRole('dialog', { name: '跨客户端复制 MCP Server' })
    await expect(dialog).toBeVisible()
    await dialog.getByRole('button', { name: '生成安全预览', exact: true }).click()
    await expect(dialog).toContainText('E2E_MCP_TOKEN')
    await expect(dialog.locator('pre')).toContainText('bearer_token_env_var = "E2E_MCP_TOKEN"')
    await dialog.getByText('我了解目标客户端启动前需要设置以上环境变量。', { exact: true }).click()
    await dialog.getByRole('button', { name: '批准并复制', exact: true }).click()
    await expect(dialog).toContainText('MCP Server 已写入并重新解析验证通过。')
    expect(await readFile(join(projectRoot, '.codex', 'config.toml'), 'utf8')).toContain('bearer_token_env_var = "E2E_MCP_TOKEN"')

    await dialog.getByRole('button', { name: '回滚复制', exact: true }).click()
    await expect(dialog).toHaveCount(0)
    await expect(readFile(join(projectRoot, '.codex', 'config.toml'))).rejects.toMatchObject({ code: 'ENOENT' })
  } finally {
    await electronApp.evaluate(() => { delete process.env.E2E_MCP_TOKEN })
    await new Promise<void>((resolve, reject) => mcpServer.close((error) => error ? reject(error) : resolve()))
    await rm(projectRoot, { recursive: true, force: true })
  }
})

test('reviews, scopes, previews, archives, and deletes Agent memories', async () => {
  const projectRoot = await mkdtemp(join(tmpdir(), 'mootool-ai-memory-e2e-'))
  const targetPath = join(projectRoot, 'src', 'feature')
  const suffix = Date.now().toString(36)
  const candidateContent = `Candidate ${suffix}: keep architecture decisions in ADR files.`
  const manualContent = `OrbitMemory ${suffix}: MooTool stores local state in SQLite.`
  try {
    await mkdir(targetPath, { recursive: true })
    await mainPage.evaluate(({ root, content }) => window.mootool.createAiMemoryCandidate({
      kind: 'technicalDecision',
      proposedScope: 'project',
      proposedScopeValue: root,
      content,
      sourceKind: 'taskSummary',
      sourceRef: `e2e-task-${Date.now()}`,
      evidenceSummary: 'The decision was repeated in the completed task summary.',
      confidence: 0.91,
      sensitivity: 'internal'
    }), { root: projectRoot, content: candidateContent })

    await electronApp.evaluate(({ dialog }, root) => {
      dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [root] })
    }, projectRoot)
    await openTool('Agent 记忆', 'Agent 记忆管理')
    const activeTool = mainPage.locator('.workspace-tool-session:not([hidden])')
    await activeTool.getByRole('button', { name: '选择项目', exact: true }).click()
    await expect(activeTool.locator('.ai-manager-scope strong')).toHaveText(projectRoot)

    const candidate = activeTool.locator('.ai-memory-inbox > article').filter({ hasText: candidateContent })
    await expect(candidate).toContainText('技术决策')
    await candidate.getByRole('button', { name: '批准并保存', exact: true }).click()
    await expect(candidate).toHaveCount(0)
    await expect(activeTool.locator('.ai-memory-card').filter({ hasText: candidateContent })).toBeVisible()

    await activeTool.getByRole('button', { name: '新建记忆', exact: true }).click()
    const editor = mainPage.getByRole('dialog', { name: '新建记忆' })
    await expect(editor.getByLabel('作用域目标')).toHaveValue(projectRoot)
    await editor.getByLabel('记忆内容').fill(manualContent)
    await editor.getByLabel('来源说明').fill('Electron E2E')
    await editor.getByRole('button', { name: '保存', exact: true }).click()
    await expect(editor).toHaveCount(0)

    const manualCard = activeTool.locator('.ai-memory-card').filter({ hasText: manualContent })
    await expect(manualCard).toContainText('项目事实')
    await activeTool.getByLabel('搜索记忆内容').fill('OrbitMemory')
    await expect(activeTool.locator('.ai-memory-card')).toHaveCount(1)
    await expect(manualCard).toBeVisible()
    await activeTool.getByLabel('搜索记忆内容').fill('')

    await manualCard.getByRole('button', { name: '归档记忆', exact: true }).click()
    await expect(manualCard).toHaveCount(0)
    await activeTool.getByText('显示已归档', { exact: true }).click()
    const archivedCard = activeTool.locator('.ai-memory-card--archived').filter({ hasText: manualContent })
    await expect(archivedCard).toBeVisible()
    await archivedCard.getByRole('button', { name: '恢复记忆', exact: true }).click()
    await expect(activeTool.locator('.ai-memory-card:not(.ai-memory-card--archived)').filter({ hasText: manualContent })).toBeVisible()
    await activeTool.getByText('显示已归档', { exact: true }).click()

    await electronApp.evaluate(({ dialog }, target) => {
      dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [target] })
    }, targetPath)
    await activeTool.getByRole('button', { name: '生效预览', exact: true }).click()
    const preview = mainPage.getByRole('dialog', { name: 'Agent 记忆注入预览' })
    await expect(preview).toContainText(candidateContent)
    await expect(preview).toContainText(manualContent)
    await expect(preview).toContainText('项目级')
    await preview.getByLabel('Token 预算').fill('1')
    await preview.getByRole('button', { name: '刷新', exact: true }).click()
    await expect(preview).toContainText('预算省略 2 条')
    await expect(preview).toContainText('没有可注入的记忆')
    await preview.getByRole('button', { name: '关闭', exact: true }).click()

    await activeTool.getByRole('button', { name: '本地语义索引', exact: true }).click()
    const embedding = mainPage.getByRole('dialog', { name: 'Agent 记忆本地 Embedding' })
    await expect(embedding).toBeVisible()
    await embedding.getByText('我确认将公开/内部记忆发送给所选的本机模型进程处理。', { exact: true }).click()
    await embedding.getByRole('button', { name: '重建本地索引', exact: true }).click()
    await expect(embedding.locator('.ai-memory-embedding-metrics')).toContainText('100%')
    await embedding.getByPlaceholder('输入自然语言查询').fill('local state database')
    await embedding.getByRole('button', { name: '语义检索', exact: true }).click()
    await expect(embedding).toContainText(manualContent)
    await expect(embedding).toContainText(candidateContent)
    await embedding.getByRole('button', { name: '关闭', exact: true }).click()

    const restoredCard = activeTool.locator('.ai-memory-card').filter({ hasText: manualContent })
    mainPage.once('dialog', (confirmation) => confirmation.accept())
    await restoredCard.getByRole('button', { name: '删除', exact: true }).click()
    await expect(restoredCard).toHaveCount(0)

    const snapshot = await mainPage.evaluate(() => window.mootool.getAiMemorySnapshot({ includeArchived: true }))
    expect(snapshot.memories.some((memory) => memory.content === manualContent)).toBe(false)
    expect(snapshot.memories.some((memory) => memory.content === candidateContent && memory.createdBy === 'agentCandidate')).toBe(true)
  } finally {
    const snapshot = await mainPage.evaluate(() => window.mootool.getAiMemorySnapshot({ includeArchived: true }))
    const cleanupIds = snapshot.memories.filter((memory) => memory.content === candidateContent || memory.content === manualContent).map((memory) => memory.id)
    for (const id of cleanupIds) await mainPage.evaluate((memoryId) => window.mootool.deleteAiMemory(memoryId), id)
    await rm(projectRoot, { recursive: true, force: true })
  }
})

test('inspects Ollama models and machine resources without running inference', async () => {
  ollamaRequests.length = 0
  await openTool('模型与运行时', '模型与运行时管理')
  const activeTool = mainPage.locator('.workspace-tool-session:not([hidden])')
  await expect(activeTool.locator('.ai-runtime-overview')).toContainText('Ollama 0.13.3-e2e')
  await expect(activeTool.locator('.ai-runtime-overview')).toContainText('运行正常')
  await expect(activeTool.locator('.ai-manager-metric')).toHaveCount(4)
  await expect(activeTool.locator('.ai-manager-metric').first()).toContainText('1')

  const modelCard = activeTool.locator('.ai-runtime-model-card').filter({ hasText: 'qwen3:8b-e2e' })
  await expect(modelCard).toContainText('8B')
  await expect(modelCard).toContainText('Q4_K_M')
  await expect(modelCard).toContainText('已加载')
  await modelCard.getByRole('button', { name: '查看 qwen3:8b-e2e 元数据', exact: true }).click()

  const detail = mainPage.getByRole('dialog', { name: 'qwen3:8b-e2e 模型元数据' })
  await expect(detail).toContainText('131,072')
  await expect(detail).toContainText('completion')
  await expect(detail).toContainText('tools')
  await expect(detail).toContainText('E2E model license')
  await expect(detail).not.toContainText('THIS TEMPLATE MUST NOT BE RENDERED')
  await detail.getByRole('button', { name: '关闭', exact: true }).click()

  expect(ollamaRequests).toEqual(expect.arrayContaining(['GET /api/version', 'GET /api/tags', 'GET /api/ps', 'POST /api/show']))
  expect(ollamaRequests.some((request) => /generate|chat|completions|responses/.test(request))).toBe(false)
})

test('previews, imports, budgets, and clears Usage metadata without retaining prompts', async () => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-ai-usage-e2e-'))
  const usagePath = join(directory, 'usage.json')
  const adminKey = 'sk-admin-e2e-must-stay-in-main-process'
  try {
    await writeFile(usagePath, JSON.stringify({ events: [{
      source: 'providerApi',
      provider: 'openai',
      clientId: 'codex',
      projectId: '/e2e/project',
      sessionId: 'e2e-usage-session',
      model: 'gpt-e2e',
      startedAt: new Date().toISOString(),
      inputTokens: 3000,
      outputTokens: 750,
      cachedInputTokens: 1200,
      reasoningTokens: 125,
      requestCount: 3,
      billedCost: { currency: 'USD', micros: 500000 },
      sourceFingerprint: 'e2e-usage-event',
      prompt: 'SECRET E2E PROMPT MUST NOT APPEAR'
    }] }))
    await electronApp.evaluate(({ dialog }, path) => {
      dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [path] })
    }, usagePath)

    await openTool('Token 与成本', 'Token 与成本')
    const activeTool = mainPage.locator('.workspace-tool-session:not([hidden])')
    await expect(activeTool).toContainText('还没有 Usage 数据')
    await activeTool.getByRole('button', { name: '导入本地日志', exact: true }).click()

    const preview = mainPage.getByRole('dialog', { name: '审查 Usage 导入' })
    await expect(preview).toContainText('gpt-e2e')
    await expect(preview).toContainText('inputTokens')
    await expect(preview).toContainText('billedCost')
    await expect(preview).not.toContainText('SECRET E2E PROMPT')
    await preview.getByRole('button', { name: '批准并导入', exact: true }).click()
    await expect(preview).toHaveCount(0)

    await expect(activeTool.locator('.ai-manager-metric').first()).toContainText('3,750')
    await expect(activeTool.locator('.ai-manager-metric').nth(3)).toContainText('$0.50')
    await expect(activeTool.locator('.ai-usage-breakdowns')).toContainText('gpt-e2e')
    await expect(activeTool.locator('.ai-usage-token-split')).toContainText('1,200')

    await activeTool.getByRole('button', { name: '预算', exact: true }).click()
    const budget = mainPage.getByRole('dialog', { name: '本地软预算' })
    await budget.getByLabel('Token 上限').fill('4000')
    await budget.getByLabel('成本上限（USD）').fill('1')
    await budget.getByRole('button', { name: '保存', exact: true }).click()
    await expect(budget).toHaveCount(0)
    await expect(activeTool.locator('.ai-usage-budgets')).toContainText('94%')
    await expect(activeTool.locator('.ai-usage-budgets')).toContainText('50%')

    const exportPath = join(directory, 'usage-export.json')
    await electronApp.evaluate(({ dialog }, path) => {
      dialog.showSaveDialog = async () => ({ canceled: false, filePath: path })
    }, exportPath)
    await activeTool.getByRole('button', { name: '导出', exact: true }).click()
    await expect(activeTool.locator('.ai-usage-export-message')).toContainText('已导出 1 条 Usage 元数据')
    const exported = await readFile(exportPath, 'utf8')
    expect(exported).toContain('"model": "gpt-e2e"')
    expect(exported).toContain('"billedCost"')
    expect(exported).not.toContain('SECRET E2E PROMPT')

    openAiUsageRequests.length = 0
    await activeTool.getByRole('button', { name: 'Provider API', exact: true }).click()
    const provider = mainPage.getByRole('dialog', { name: 'Provider 用量数据源' })
    await provider.getByLabel('OpenAI Admin Key').fill(adminKey)
    await provider.getByRole('button', { name: '安全保存', exact: true }).click()
    await expect(provider).toContainText('Admin Key 已存入系统安全存储')
    await provider.getByRole('button', { name: '立即同步', exact: true }).click()
    await expect(provider).toContainText('同步完成：1 条 Token 用量、1 条账单成本')
    await provider.getByRole('button', { name: '关闭', exact: true }).click()
    await expect(activeTool.locator('.ai-usage-breakdowns')).toContainText('gpt-provider-e2e')
    await expect(activeTool.locator('.ai-manager-metric').nth(3)).toContainText('$1.75')
    expect(openAiUsageRequests).toHaveLength(2)
    expect(openAiUsageRequests.every((request) => request.authorization === `Bearer ${adminKey}`)).toBe(true)
    const providerDashboard = await mainPage.evaluate(() => window.mootool.getAiUsageDashboard({ rangeDays: 30, timezoneOffsetMinutes: new Date().getTimezoneOffset() }))
    expect(JSON.stringify(providerDashboard)).not.toContain(adminKey)

    mainPage.once('dialog', (confirmation) => confirmation.accept())
    await activeTool.getByRole('button', { name: '清空 Usage 统计', exact: true }).click()
    await expect(activeTool).toContainText('还没有 Usage 数据')
    await expect.poll(() => mainPage.evaluate(() => window.mootool.getAiUsageDashboard({ rangeDays: 30, timezoneOffsetMinutes: new Date().getTimezoneOffset() }))).toMatchObject({ totals: { events: 0 } })
  } finally {
    await mainPage.evaluate(() => window.mootool.clearAiUsage())
    await mainPage.evaluate(() => window.mootool.clearSecret('openAiAdminApiKey'))
    await rm(directory, { recursive: true, force: true })
  }
})

test('creates an Agent profile and generates a shell-safe credential-free launch plan without executing it', async () => {
  const projectRoot = await mkdtemp(join(tmpdir(), 'mootool-agent-manager-e2e-'))
  const profileName = `E2E Agent ${Date.now()}`
  try {
    await electronApp.evaluate(({ dialog }, root) => {
      dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [root] })
    }, projectRoot)
    await openTool('Agent 管理', 'Agent 管理')
    const activeTool = mainPage.locator('.workspace-tool-session:not([hidden])')
    await expect.poll(() => activeTool.locator('.ai-agent-client--healthy').count()).toBeGreaterThanOrEqual(2)
    await expect(activeTool.locator('.ai-agent-client--healthy').filter({ hasText: 'Codex' })).toHaveCount(1)
    await expect(activeTool.locator('.ai-agent-client--healthy').filter({ hasText: 'Claude Code' })).toHaveCount(1)
    await activeTool.getByRole('button', { name: '选择项目', exact: true }).click()
    await expect(activeTool.locator('.ai-agent-safety code')).toHaveText(await realpath(projectRoot))

    await activeTool.locator('.tool-header-actions').getByRole('button', { name: '新建 Profile', exact: true }).click()
    const editor = mainPage.getByRole('dialog', { name: '新建 Agent Profile' })
    await editor.getByLabel('Profile 名称').fill(profileName)
    await editor.getByLabel('模型运行时').selectOption('ollama')
    await expect(editor.getByRole('combobox', { name: '模型', exact: true })).toHaveValue('qwen3:8b-e2e')
    await editor.getByLabel('权限模式').selectOption('workspaceWrite')
    await editor.getByLabel('Codex 配置 Profile').fill('work')
    await editor.getByLabel('Skill 依赖').fill('testing')
    await editor.getByLabel('MCP Server 依赖').fill('docs')
    await editor.getByLabel('环境变量引用').fill('E2E_AGENT_SECRET')
    await editor.getByLabel('--search').check()
    await editor.getByRole('button', { name: '保存', exact: true }).click()
    await expect(editor).toHaveCount(0)

    const profile = activeTool.locator('.ai-agent-profile').filter({ hasText: profileName })
    await expect(profile).toContainText('qwen3:8b-e2e')
    await expect(profile).toContainText('Ollama')
    await writeFile(join(projectRoot, 'AGENTS.md'), 'Run the focused E2E checks.\n')
    await activeTool.getByRole('button', { name: '刷新', exact: true }).click()
    await expect(activeTool.locator('.ai-agent-client').filter({ hasText: 'Codex' })).toContainText('配置文件元数据自上次扫描后已变化')
    await profile.getByRole('button', { name: '生成启动计划', exact: true }).click()
    const plan = mainPage.getByRole('dialog', { name: '安全启动计划' })
    await expect(plan).toContainText('agent-bin/codex')
    await expect(plan.locator('pre')).toContainText("--model' 'qwen3:8b-e2e")
    await expect(plan.locator('pre')).toContainText("--sandbox' 'workspace-write")
    await expect(plan).toContainText('E2E_AGENT_SECRET')
    await expect(plan).toContainText('model runtime binding is declarative')
    await expect(plan).not.toContainText('super-secret-value-that-must-not-cross-ipc')
    await plan.getByRole('button', { name: '关闭', exact: true }).click()

    mainPage.once('dialog', (confirmation) => confirmation.accept())
    await profile.getByRole('button', { name: '删除 Profile', exact: true }).click()
    await expect(profile).toHaveCount(0)
  } finally {
    const snapshot = await mainPage.evaluate(() => window.mootool.getAiAgentManagerSnapshot())
    const cleanupIds = snapshot.profiles.filter((profile) => profile.name === profileName).map((profile) => profile.id)
    for (const id of cleanupIds) await mainPage.evaluate((profileId) => window.mootool.deleteAiAgentProfile(profileId), id)
    await rm(projectRoot, { recursive: true, force: true })
  }
})

test('connects instructions, Skills, scoped memory, and explicitly probed MCP Schema in Context Inspector', async () => {
  const projectRoot = await mkdtemp(join(tmpdir(), 'mootool-context-inspector-e2e-'))
  const skillDirectory = join(projectRoot, '.agents', 'skills', 'context-e2e')
  const memoryContent = `Context E2E ${Date.now()}: use SQLite for local state.`
  let memoryId = ''
  let profileId = ''
  const mcpServer = createServer((request, response) => {
    let source = ''
    request.setEncoding('utf8')
    request.on('data', (chunk) => { source += chunk })
    request.on('end', () => {
      const payload = JSON.parse(source) as { id?: number; method: string }
      if (payload.id === undefined) { response.statusCode = 202; response.end(); return }
      const result = payload.method === 'initialize'
        ? { protocolVersion: '2025-11-25', capabilities: {} }
        : payload.method === 'tools/list'
          ? { tools: [{ name: 'context_read', description: 'Read context fixture data', inputSchema: { type: 'object', properties: { path: { type: 'string' } } } }] }
          : payload.method === 'resources/list' ? { resources: [] } : { prompts: [] }
      response.setHeader('content-type', 'application/json')
      response.end(JSON.stringify({ jsonrpc: '2.0', id: payload.id, result }))
    })
  })
  try {
    await new Promise<void>((resolve) => mcpServer.listen(0, '127.0.0.1', resolve))
    const address = mcpServer.address()
    if (!address || typeof address === 'string') throw new Error('Context MCP E2E server has no port')
    await Promise.all([mkdir(skillDirectory, { recursive: true }), mkdir(join(projectRoot, '.codex'), { recursive: true })])
    await Promise.all([
      writeFile(join(projectRoot, 'AGENTS.md'), 'Always run the focused tests before finishing.\n'),
      writeFile(join(skillDirectory, 'SKILL.md'), '---\nname: context-e2e\ndescription: Inspect context composition.\n---\n\nLoad this body only when selected.\n'),
      writeFile(join(projectRoot, '.codex', 'config.toml'), `[mcp_servers.context-e2e]\nurl = "http://127.0.0.1:${address.port}/mcp"\n`)
    ])
    const created = await mainPage.evaluate(({ root, content }) => window.mootool.saveAiMemory({
      kind: 'projectFact', scope: 'project', scopeValue: root, content, sourceKind: 'user', confidence: 1, sensitivity: 'internal'
    }), { root: projectRoot, content: memoryContent })
    memoryId = created.id
    const profile = await mainPage.evaluate((root) => window.mootool.saveAiAgentProfile({
      name: 'Context E2E profile', clientId: 'codex', workingDirectory: root, permissionMode: 'readOnly',
      mcpServerNames: ['context-e2e'], skillNames: ['context-e2e'], environmentVariableRefs: [], optionalFlags: []
    }), projectRoot)
    profileId = profile.id
    const inventory = await mainPage.evaluate((root) => window.mootool.getMcpInventory({ projectRoot: root }), projectRoot)
    const server = inventory.servers.find((candidate) => candidate.name === 'context-e2e')
    expect(server).toBeTruthy()
    const probe = await mainPage.evaluate(({ root, serverId }) => window.mootool.probeMcpServer({
      requestId: window.crypto.randomUUID(), sourceServerId: serverId, projectRoot: root, confirmCommand: false
    }), { root: projectRoot, serverId: server!.id })
    expect(probe).toMatchObject({ status: 'healthy', toolSchemas: [{ name: 'context_read', estimatedTokens: expect.any(Number) }] })

    await electronApp.evaluate(({ dialog }, root) => {
      dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [root] })
    }, projectRoot)
    await openTool('上下文检查', '上下文检查')
    const activeTool = mainPage.locator('.workspace-tool-session:not([hidden])')
    await activeTool.locator('.tool-header-actions').getByRole('button', { name: '选择项目', exact: true }).click()
    await expect(activeTool.locator('.ai-context-estimate')).toBeVisible()
    await expect(activeTool.locator('.ai-manager-metric')).toHaveCount(4)
    await activeTool.getByLabel('Agent Profile').selectOption(profileId)
    await expect(activeTool.getByLabel('context-e2e')).toBeChecked()
    await expect(activeTool.locator('.ai-context-breakdown')).toContainText('规约')
    await expect(activeTool.locator('.ai-context-breakdown')).toContainText('Skill 正文')
    await expect(activeTool.locator('.ai-context-breakdown')).toContainText('Agent 记忆')
    await expect(activeTool.locator('.ai-context-breakdown')).toContainText('MCP Schema')
    await expect(activeTool.locator('.ai-context-top')).toContainText('context_read')
    await expect(activeTool.locator('.ai-context-top')).toContainText(memoryContent)
    await expect(activeTool.locator('.ai-context-top')).toContainText('AGENTS.md')
  } finally {
    if (memoryId) await mainPage.evaluate((id) => window.mootool.deleteAiMemory(id), memoryId).catch(() => undefined)
    if (profileId) await mainPage.evaluate((id) => window.mootool.deleteAiAgentProfile(id), profileId).catch(() => undefined)
    await new Promise<void>((resolve, reject) => mcpServer.close((error) => error ? reject(error) : resolve()))
    await rm(projectRoot, { recursive: true, force: true })
  }
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
  await expect(mainPage.locator('.json-editor .cm-activeLineGutter')).toHaveCount(1)

  await mainPage.locator('.editor-toolbar').getByRole('button', { name: '查找', exact: true }).click()
  await mainPage.getByLabel('在 JSON 中查找…').fill('"a"')
  await expect(mainPage.locator('.json-editor .cm-searchMatch')).toHaveCount(1)
  await editor.focus()
  await mainPage.keyboard.press('ControlOrMeta+End')
  await mainPage.keyboard.press('Shift+ArrowLeft')

  await mainPage.getByRole('button', { name: '主页', exact: true }).click()
  await mainPage.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  await expect(mainPage.getByLabel('在 JSON 中查找…')).toHaveValue('"a"')
  await editor.focus()
  await mainPage.keyboard.type('}')
  await expect.poll(() => editor.evaluate((element) => (element as HTMLElement).innerText)).toBe(`{
  "b": 1,
  "a": 2
}`)
  await expect(mainPage.locator('.json-editor .cm-searchMatch')).toHaveCount(1)

  await mainPage.evaluate(() => window.mootool.updateSettings({ editor: { jsonFontSize: 22 } }))
  await expect(mainPage.locator('.json-editor')).toHaveCSS('font-size', '22px')
  await expect.poll(async () => {
    const offsets = await editorLineTopOffsets(mainPage, '.json-editor')
    return offsets.length > 0 ? Math.max(...offsets) : Number.POSITIVE_INFINITY
  }).toBeLessThan(1.5)
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
  const jsonBuiltInNavigation = mainPage.locator('.tool-nav > .tool-group:not(.tool-group--custom)').getByRole('button', { name: 'JSON', exact: true })
  await expect(jsonNavigationToggle).toBeChecked()
  await jsonNavigationToggle.uncheck()
  await expect.poll(() => settingsPage.evaluate(() => window.mootool.getSettings())).toMatchObject({ layout: { hiddenNavigationToolIds: ['json'] } })
  await expect(jsonBuiltInNavigation).toHaveCount(0)

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
  await expect(settingsPage.locator('.settings-about')).toContainText('版本 1.0.1')
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
  await expect(jsonBuiltInNavigation).toBeVisible()

  await electronApp.evaluate(({ BrowserWindow }) => {
    BrowserWindow.getAllWindows().find((window) => !window.getParentWindow())?.webContents.send('update:state-changed', {
      status: 'ready',
      installMode: 'automatic',
      version: '9.9.9',
      fileName: 'MooTool-Next-Electron-9.9.9-test.bin',
      percent: 100,
      transferred: 100,
      total: 100,
      message: null
    })
  })
  await expect(mainPage.getByRole('button', { name: /安装并重启/ })).toContainText('新版本 9.9.9 已就绪')
  await electronApp.evaluate(({ BrowserWindow }) => {
    BrowserWindow.getAllWindows().find((window) => !window.getParentWindow())?.webContents.send('update:state-changed', {
      status: 'ready',
      installMode: 'manual',
      version: '9.9.9',
      fileName: 'MooTool-Next-Electron-9.9.9-mac-arm64.dmg',
      percent: 100,
      transferred: 100,
      total: 100,
      message: null
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
      message: null
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
  await expect.poll(async () => {
    const offsets = await editorLineTopOffsets(mainPage, '.quick-note-code-editor')
    return offsets.length > 0 ? Math.max(...offsets) : Number.POSITIVE_INFINITY
  }).toBeLessThan(1.5)
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
  const syntaxSelect = mainPage.getByLabel('语法')
  await syntaxSelect.selectOption('text/markdown')
  await expect(syntaxSelect).toHaveValue('text/markdown')
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
