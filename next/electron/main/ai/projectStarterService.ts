import { lstat, readFile, realpath } from 'node:fs/promises'
import { basename, join, relative, resolve } from 'node:path'
import type { AiChangeApplyResult, AiChangeOperationInput, AiChangeRollbackResult } from '../../../src/shared/contracts/aiChanges'
import type { AiProjectStarterItem, AiProjectStarterPreview, AiProjectStarterPreviewInput, AiProjectStarterSkipped } from '../../../src/shared/contracts/aiProjectStarter'
import { ConfigChangeService } from './configChangeService'

const gitignoreEntries = ['# MooTool local AI state', '.mootool/']

export class ProjectStarterService {
  constructor(private readonly changes: ConfigChangeService) {}

  async preview(input: AiProjectStarterPreviewInput): Promise<AiProjectStarterPreview> {
    const projectRoot = await realpath(resolve(input.projectRoot))
    const projectName = basename(projectRoot).replace(/[^a-zA-Z0-9._-]+/g, '-') || 'project'
    const operations: AiChangeOperationInput[] = []
    const skipped: AiProjectStarterSkipped[] = []

    for (const item of input.items) {
      if (item === 'gitignore') {
        await this.addGitignore(projectRoot, operations, skipped)
        continue
      }
      const candidate = starterFile(item, projectName)
      const target = join(projectRoot, candidate.path)
      const state = await pathState(target)
      if (state === 'symlink') throw new Error(`Project Starter target is a symbolic link: ${target}`)
      if (state === 'file') {
        skipped.push({ item, path: target, reason: 'alreadyExists' })
        continue
      }
      if (state === 'other') throw new Error(`Project Starter target is not a regular file: ${target}`)
      operations.push({ targetPath: candidate.path, nextContent: candidate.content, summary: candidate.summary, expectedState: 'missing' })
    }

    if (operations.length === 0) throw new Error('All selected Project Starter artifacts are already configured')
    return { plan: await this.changes.createPlan(projectRoot, operations), skipped }
  }

  apply(planId: string): Promise<AiChangeApplyResult> {
    return this.changes.apply(planId)
  }

  rollback(snapshotId: string): Promise<AiChangeRollbackResult> {
    return this.changes.rollback(snapshotId)
  }

  private async addGitignore(projectRoot: string, operations: AiChangeOperationInput[], skipped: AiProjectStarterSkipped[]): Promise<void> {
    const target = join(projectRoot, '.gitignore')
    const state = await pathState(target)
    if (state === 'symlink') throw new Error(`Project Starter target is a symbolic link: ${target}`)
    if (state === 'other') throw new Error(`Project Starter target is not a regular file: ${target}`)
    const source = state === 'file' ? await readFile(target, 'utf8') : ''
    const existing = new Set(source.split(/\r?\n/).map((line) => line.trim()))
    const additions = gitignoreEntries.filter((line) => !existing.has(line))
    if (additions.length === 0) {
      skipped.push({ item: 'gitignore', path: target, reason: 'alreadyConfigured' })
      return
    }
    const nextContent = `${source.trimEnd()}${source.trim() ? '\n\n' : ''}${additions.join('\n')}\n`
    operations.push({
      targetPath: relative(projectRoot, target),
      nextContent,
      summary: 'Add MooTool local AI state to .gitignore',
      expectedState: state === 'file' ? 'existing' : 'missing'
    })
  }
}

function starterFile(item: Exclude<AiProjectStarterItem, 'gitignore'>, projectName: string): { path: string; content: string; summary: string } {
  if (item === 'instructions') return {
    path: 'AGENTS.md',
    summary: 'Create the project instruction entry point',
    content: `# ${projectName} Developer Instructions\n\n## Working agreement\n\n- Keep changes scoped to the requested task.\n- Follow the existing project architecture and formatting.\n- Run the relevant tests and report any checks that could not be run.\n- Never commit credentials, local configuration, or generated secrets.\n`
  }
  if (item === 'projectSkill') return {
    path: join('.agents', 'skills', 'project-workflow', 'SKILL.md'),
    summary: 'Create a project workflow Skill',
    content: `---\nname: project-workflow\ndescription: Apply the ${projectName} project workflow, validation, and handoff conventions.\n---\n\n# ${projectName} workflow\n\n1. Read AGENTS.md and the files relevant to the requested change.\n2. Keep edits minimal and preserve unrelated work.\n3. Run focused checks first, then the broader project verification when appropriate.\n4. Summarize changed files, verification, and remaining risks.\n`
  }
  return {
    path: '.mcp.json',
    summary: 'Create an empty project MCP manifest',
    content: '{\n  "mcpServers": {}\n}\n'
  }
}

async function pathState(path: string): Promise<'missing' | 'file' | 'symlink' | 'other'> {
  try {
    const info = await lstat(path)
    if (info.isSymbolicLink()) return 'symlink'
    if (info.isFile()) return 'file'
    return 'other'
  } catch (error) {
    if (typeof error === 'object' && error !== null && (error as { code?: unknown }).code === 'ENOENT') return 'missing'
    throw error
  }
}
