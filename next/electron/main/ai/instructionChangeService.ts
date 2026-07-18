import { lstat, readFile, realpath } from 'node:fs/promises'
import { join, resolve } from 'node:path'
import type { AiChangeApplyResult, AiChangePlan, AiChangeRollbackResult } from '../../../src/shared/contracts/aiChanges'
import { ConfigChangeService } from './configChangeService'

export const claudeCompatibilityEntry = `# Claude Code project instructions

Read and follow the shared project instructions in @AGENTS.md.
`

export class InstructionChangeService {
  private readonly planRoots = new Map<string, string>()

  constructor(private readonly changes: ConfigChangeService) {}

  async previewClaudeCompatibilityEntry(projectRoot: string): Promise<AiChangePlan> {
    const root = await realpath(resolve(projectRoot))
    await requireRegularFile(join(root, 'AGENTS.md'), 'A project AGENTS.md is required before creating a Claude compatibility entry')
    const plan = await this.changes.createPlan(root, [{
      targetPath: 'CLAUDE.md',
      nextContent: claudeCompatibilityEntry,
      summary: 'Create a thin Claude Code entry that references AGENTS.md',
      expectedState: 'missing'
    }])
    this.planRoots.set(plan.id, root)
    return plan
  }

  async applyClaudeCompatibilityEntry(planId: string): Promise<AiChangeApplyResult> {
    const root = this.planRoots.get(planId)
    if (!root) throw new Error('Unknown or expired Claude compatibility plan')
    const result = await this.changes.apply(planId)
    try {
      await requireRegularFile(join(root, 'AGENTS.md'), 'AGENTS.md disappeared during verification')
      const target = join(root, 'CLAUDE.md')
      await requireRegularFile(target, 'CLAUDE.md was not created')
      if (await readFile(target, 'utf8') !== claudeCompatibilityEntry) throw new Error('CLAUDE.md verification failed')
      this.planRoots.delete(planId)
      return result
    } catch (error) {
      await this.changes.rollback(result.snapshotId)
      this.planRoots.delete(planId)
      throw error
    }
  }

  rollbackClaudeCompatibilityEntry(snapshotId: string): Promise<AiChangeRollbackResult> {
    return this.changes.rollback(snapshotId)
  }
}

async function requireRegularFile(path: string, message: string): Promise<void> {
  try {
    const info = await lstat(path)
    if (info.isSymbolicLink() || !info.isFile()) throw new Error(message)
  } catch (error) {
    if (error instanceof Error && error.message === message) throw error
    throw new Error(message)
  }
}
