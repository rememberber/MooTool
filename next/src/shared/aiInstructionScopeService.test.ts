import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { AiDiscoveryService } from '../../electron/main/ai/discoveryService'
import { InstructionScopeService } from '../../electron/main/ai/instructionScopeService'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('InstructionScopeService', () => {
  it('orders user, ancestor, project, and path-matched instructions for a target', async () => {
    const root = await mkdtemp(join(tmpdir(), 'mootool-ai-instruction-scope-'))
    temporaryDirectories.push(root)
    const home = join(root, 'home')
    const project = join(root, 'project')
    const target = join(project, 'src', 'components')
    await Promise.all([
      mkdir(join(home, '.codex'), { recursive: true }),
      mkdir(join(home, '.claude'), { recursive: true }),
      mkdir(join(home, '.gemini'), { recursive: true }),
      mkdir(target, { recursive: true }),
      mkdir(join(project, '.claude', 'rules'), { recursive: true }),
      mkdir(join(project, '.cursor', 'rules'), { recursive: true }),
      mkdir(join(project, '.github', 'instructions'), { recursive: true })
    ])
    await Promise.all([
      writeFile(join(home, '.codex', 'AGENTS.md'), 'User Codex instruction.'),
      writeFile(join(home, '.claude', 'CLAUDE.md'), 'User Claude instruction.'),
      writeFile(join(home, '.gemini', 'GEMINI.md'), 'User Gemini instruction.'),
      writeFile(join(project, 'AGENTS.md'), 'Root Codex instruction.'),
      writeFile(join(project, 'src', 'AGENTS.md'), 'Nested Codex instruction.'),
      writeFile(join(project, 'CLAUDE.md'), 'Root Claude instruction.'),
      writeFile(join(project, 'GEMINI.md'), 'Root Gemini instruction.'),
      writeFile(join(project, 'src', 'GEMINI.md'), 'Nested Gemini instruction.'),
      writeFile(join(project, '.claude', 'rules', 'source.md'), '---\npaths:\n  - src/**\n---\nSource rule.'),
      writeFile(join(project, '.claude', 'rules', 'docs.md'), '---\npaths:\n  - docs/**\n---\nDocs rule.'),
      writeFile(join(project, '.cursor', 'rules', 'source.mdc'), '---\nglobs: src/**\n---\nCursor source rule.'),
      writeFile(join(project, '.github', 'copilot-instructions.md'), 'Copilot project instruction.'),
      writeFile(join(project, '.github', 'instructions', 'source.instructions.md'), '---\napplyTo: src/**\n---\nCopilot source instruction.')
    ])
    const discovery = new AiDiscoveryService({
      homeDirectory: home,
      pathValue: join(root, 'empty-bin'),
      includeDefaultExecutablePaths: false,
      requestTimeoutMs: 5,
      fetcher: async () => { throw new Error('offline') }
    })
    const service = new InstructionScopeService(discovery)

    const codex = await service.preview({ projectRoot: project, targetPath: target, clientId: 'codex' })
    expect(codex.instructions.map((instruction) => [instruction.name, instruction.reason])).toEqual([
      ['AGENTS.md', 'userScope'],
      ['AGENTS.md', 'directoryAncestor'],
      ['src/AGENTS.md', 'directoryAncestor']
    ])

    const claude = await service.preview({ projectRoot: project, targetPath: target, clientId: 'claudeCode' })
    expect(claude.instructions.map((instruction) => [instruction.name, instruction.reason])).toEqual([
      ['CLAUDE.md', 'userScope'],
      ['CLAUDE.md', 'projectScope'],
      ['.claude/rules/source.md', 'pathPattern']
    ])
    expect(claude.instructions.some((instruction) => instruction.name.endsWith('docs.md'))).toBe(false)

    const cursor = await service.preview({ projectRoot: project, targetPath: target, clientId: 'cursor' })
    expect(cursor.instructions.map((instruction) => [instruction.name, instruction.reason])).toEqual([
      ['AGENTS.md', 'projectScope'],
      ['CLAUDE.md', 'projectScope'],
      ['.cursor/rules/source.mdc', 'pathPattern']
    ])

    const gemini = await service.preview({ projectRoot: project, targetPath: target, clientId: 'geminiCli' })
    expect(gemini.instructions.map((instruction) => [instruction.name, instruction.reason])).toEqual([
      ['GEMINI.md', 'userScope'],
      ['GEMINI.md', 'directoryAncestor'],
      ['src/GEMINI.md', 'directoryAncestor']
    ])

    const copilot = await service.preview({ projectRoot: project, targetPath: target, clientId: 'githubCopilot' })
    expect(copilot.instructions.map((instruction) => [instruction.name, instruction.reason])).toEqual([
      ['AGENTS.md', 'directoryAncestor'],
      ['src/AGENTS.md', 'directoryAncestor'],
      ['.github/copilot-instructions.md', 'projectScope'],
      ['.github/instructions/source.instructions.md', 'pathPattern']
    ])
    expect(copilot.totalEstimatedTokens).toBeGreaterThan(0)
  })

  it('filters by client and rejects targets outside the selected project', async () => {
    const root = await mkdtemp(join(tmpdir(), 'mootool-ai-instruction-scope-'))
    temporaryDirectories.push(root)
    const home = join(root, 'home')
    const project = join(root, 'project')
    const outside = join(root, 'outside')
    await Promise.all([mkdir(home), mkdir(project), mkdir(outside)])
    await writeFile(join(project, 'AGENTS.md'), 'Project rule')
    const service = new InstructionScopeService(new AiDiscoveryService({
      homeDirectory: home,
      pathValue: '',
      includeDefaultExecutablePaths: false,
      fetcher: async () => { throw new Error('offline') }
    }))

    const preview = await service.preview({ projectRoot: project, targetPath: project, clientId: 'codex' })
    expect(preview.instructions.every((instruction) => instruction.clientId === 'codex')).toBe(true)
    await expect(service.preview({ projectRoot: project, targetPath: outside })).rejects.toThrow('escapes')
  })
})
