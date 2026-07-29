import { describe, expect, it, vi } from 'vitest'
import { createRuntimeApi } from './runtimeApi'

describe('runtime API adapter', () => {
  it('maps the domain call to the single owned Tauri command', async () => {
    const runtimeInfo = {
      productId: 'next-tauri' as const,
      productName: 'MooTool Next Tauri',
      version: '0.1.0',
      platform: 'macos',
      architecture: 'x86_64',
      runtime: 'tauri' as const
    }
    const invoke = vi.fn().mockResolvedValue(runtimeInfo)

    await expect(createRuntimeApi(invoke).getInfo()).resolves.toEqual(runtimeInfo)
    expect(invoke).toHaveBeenCalledOnce()
    expect(invoke).toHaveBeenCalledWith('get_runtime_info')
  })
})
