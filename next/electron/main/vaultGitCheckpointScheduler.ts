import type { VaultGitActionResult } from '../../src/shared/contracts/vaultGit'

export type VaultGitCheckpointSchedulerOptions = {
  enabled: () => boolean
  hasUnsavedEditorChanges: () => boolean
  idleMilliseconds: () => number
  inactiveMilliseconds: () => number
  checkpoint: (message: string) => Promise<VaultGitActionResult>
  now?: () => number
  tickMilliseconds?: number
}

export class VaultGitCheckpointScheduler {
  private readonly now: () => number
  private readonly tickMilliseconds: number
  private timer: NodeJS.Timeout | undefined
  private lastActivityAt = 0
  private windowDeactivatedAt = 0
  private lastIdleCheckpointForActivity = -1
  private lastInactiveCheckpointAt = -1
  private message = 'Automatic Vault checkpoint'
  private running = false

  constructor(private readonly options: VaultGitCheckpointSchedulerOptions) {
    this.now = options.now ?? Date.now
    this.tickMilliseconds = options.tickMilliseconds ?? 5_000
  }

  start(): void {
    if (this.timer) return
    this.timer = setInterval(() => { void this.evaluate() }, this.tickMilliseconds)
    this.timer.unref?.()
  }

  stop(): void {
    clearInterval(this.timer)
    this.timer = undefined
  }

  recordActivity(message: string): void {
    this.lastActivityAt = this.now()
    this.message = message
  }

  setWindowActive(active: boolean): void {
    this.windowDeactivatedAt = active ? 0 : this.now()
  }

  async evaluate(): Promise<boolean> {
    if (this.running || !this.options.enabled() || this.options.hasUnsavedEditorChanges()) return false
    if (this.lastActivityAt === 0 && this.windowDeactivatedAt === 0) return false

    const now = this.now()
    const idleReady = this.lastActivityAt > 0
      && now - this.lastActivityAt >= this.options.idleMilliseconds()
      && this.lastIdleCheckpointForActivity < this.lastActivityAt
    const inactiveReady = this.windowDeactivatedAt > 0
      && now - this.windowDeactivatedAt >= this.options.inactiveMilliseconds()
      && this.lastInactiveCheckpointAt < this.windowDeactivatedAt
    if (!idleReady && !inactiveReady) return false

    const activitySnapshot = this.lastActivityAt
    const inactiveSnapshot = this.windowDeactivatedAt
    this.running = true
    try {
      const result = await this.options.checkpoint(this.message)
      if (!result.success) return false
      if (idleReady) this.lastIdleCheckpointForActivity = activitySnapshot
      if (inactiveReady) this.lastInactiveCheckpointAt = inactiveSnapshot
      return true
    } catch {
      return false
    } finally {
      this.running = false
    }
  }
}
