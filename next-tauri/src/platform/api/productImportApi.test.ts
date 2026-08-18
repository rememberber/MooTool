import { describe, expect, it, vi } from 'vitest'
import { createProductImportApi } from './productImportApi'

describe('product import API adapter', () => {
  it('keeps Java and Electron imports behind Tauri-owned commands', async () => {
    const preview = { sourceProduct: 'java', sourceDirectory: '/source', totalItems: 3 }
    const result = { preview, backupPath: '/backup', reportPath: '/report' }
    const invoke = vi.fn()
      .mockResolvedValueOnce(preview)
      .mockResolvedValueOnce(result)
    const api = createProductImportApi(invoke, vi.fn().mockResolvedValue('/source'))

    await expect(api.chooseSourceDirectory()).resolves.toBe('/source')
    await expect(api.preview('java', '/source')).resolves.toEqual(preview)
    await expect(api.run('nextElectron', '/source', 'a'.repeat(64))).resolves.toEqual(result)

    expect(invoke.mock.calls).toEqual([
      ['preview_product_import', { sourceProduct: 'java', sourceDirectory: '/source' }],
      ['run_product_import', {
        sourceProduct: 'nextElectron',
        sourceDirectory: '/source',
        expectedFingerprint: 'a'.repeat(64)
      }]
    ])
  })

  it('ignores multi-selection results at the directory boundary', async () => {
    const api = createProductImportApi(vi.fn(), vi.fn().mockResolvedValue(['/one', '/two']))
    await expect(api.chooseSourceDirectory()).resolves.toBeNull()
  })
})
