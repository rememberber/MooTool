import { describe, expect, it, vi } from 'vitest'
import { createVaultApi } from './vaultApi'

describe('Vault API', () => {
  it('maps the restricted Vault boundary to owned commands', async () => {
    const invoke = vi.fn().mockResolvedValue({})
    const api = createVaultApi(invoke)
    const save = {
      relativePath: 'example.json',
      content: '{}',
      expectedFingerprint: null
    }
    const git = {
      requestId: 'request-1',
      operation: 'pull' as const,
      editorDirty: false
    }

    await api.snapshot()
    await api.configure('/tmp/vault')
    await api.read('example.json')
    await api.save(save)
    await api.delete('example.json', 'fingerprint')
    await api.runGit(git)
    await api.cancelGit('request-1')

    expect(invoke.mock.calls).toEqual([
      ['get_vault_snapshot'],
      ['configure_vault', { rootDirectory: '/tmp/vault' }],
      ['read_vault_document', { relativePath: 'example.json' }],
      ['save_vault_document', { request: save }],
      ['delete_vault_document', { relativePath: 'example.json', expectedFingerprint: 'fingerprint' }],
      ['run_vault_git', { request: git }],
      ['cancel_vault_git', { requestId: 'request-1' }]
    ])
  })
})
