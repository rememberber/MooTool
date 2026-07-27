export type PowerSaveBlockerAdapter = {
  start: (type: 'prevent-display-sleep') => number
  stop: (id: number) => void
  isStarted: (id: number) => boolean
}

export class DisplaySleepService {
  private readonly requesters = new Set<number>()
  private blockerId: number | null = null

  constructor(private readonly powerSaveBlocker: PowerSaveBlockerAdapter) {}

  set(requesterId: number, enabled: boolean): boolean {
    if (enabled) this.requesters.add(requesterId)
    else this.requesters.delete(requesterId)
    this.sync()
    return this.blockerId !== null && this.powerSaveBlocker.isStarted(this.blockerId)
  }

  dispose(): void {
    this.requesters.clear()
    this.sync()
  }

  private sync(): void {
    if (this.requesters.size > 0) {
      if (this.blockerId === null || !this.powerSaveBlocker.isStarted(this.blockerId)) {
        this.blockerId = this.powerSaveBlocker.start('prevent-display-sleep')
      }
      return
    }

    if (this.blockerId !== null && this.powerSaveBlocker.isStarted(this.blockerId)) {
      this.powerSaveBlocker.stop(this.blockerId)
    }
    this.blockerId = null
  }
}
