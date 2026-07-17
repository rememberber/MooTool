// @vitest-environment node
import { describe, expect, it, vi } from 'vitest'
import { VaultGitCheckpointScheduler } from '../../electron/main/vaultGitCheckpointScheduler'

describe('VaultGitCheckpointScheduler', () => {
  it('waits for the Java-compatible idle threshold and checkpoints once per activity', async () => {
    let now = 1_000
    const checkpoint = vi.fn(async () => ({ success: true, message: 'Done' }))
    const scheduler = new VaultGitCheckpointScheduler({
      enabled: () => true,
      hasUnsavedEditorChanges: () => false,
      idleMilliseconds: () => 30_000,
      inactiveMilliseconds: () => 120_000,
      checkpoint,
      now: () => now
    })

    scheduler.recordActivity('Update Quick Note')
    now += 29_999
    expect(await scheduler.evaluate()).toBe(false)
    now += 1
    expect(await scheduler.evaluate()).toBe(true)
    expect(checkpoint).toHaveBeenCalledOnce()
    expect(await scheduler.evaluate()).toBe(false)

    now += 1
    scheduler.recordActivity('Update JSON snippet')
    now += 30_000
    expect(await scheduler.evaluate()).toBe(true)
    expect(checkpoint).toHaveBeenLastCalledWith('Update JSON snippet')
  })

  it('waits for editor autosave and supports the inactive threshold', async () => {
    let now = 10_000
    let dirty = true
    const checkpoint = vi.fn(async () => ({ success: true, message: 'Done' }))
    const scheduler = new VaultGitCheckpointScheduler({
      enabled: () => true,
      hasUnsavedEditorChanges: () => dirty,
      idleMilliseconds: () => 30_000,
      inactiveMilliseconds: () => 120_000,
      checkpoint,
      now: () => now
    })

    scheduler.recordActivity('Update Vault')
    scheduler.setWindowActive(false)
    now += 120_000
    expect(await scheduler.evaluate()).toBe(false)
    dirty = false
    expect(await scheduler.evaluate()).toBe(true)
    expect(checkpoint).toHaveBeenCalledOnce()
  })

  it('retries a failed checkpoint instead of consuming the activity', async () => {
    let now = 1_000
    const checkpoint = vi.fn()
      .mockResolvedValueOnce({ success: false, message: 'failed' })
      .mockResolvedValueOnce({ success: true, message: 'Done' })
    const scheduler = new VaultGitCheckpointScheduler({
      enabled: () => true,
      hasUnsavedEditorChanges: () => false,
      idleMilliseconds: () => 30_000,
      inactiveMilliseconds: () => 120_000,
      checkpoint,
      now: () => now
    })

    scheduler.recordActivity('Update Vault')
    now += 30_000
    expect(await scheduler.evaluate()).toBe(false)
    expect(await scheduler.evaluate()).toBe(true)
    expect(checkpoint).toHaveBeenCalledTimes(2)
  })
})
