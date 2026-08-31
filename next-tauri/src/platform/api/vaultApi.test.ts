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
    await api.disconnect()
    await api.read('example.json')
    await api.save(save)
    await api.createDirectory('examples')
    await api.move('example.json', 'examples/example.json', 'fingerprint')
    await api.duplicate('examples/example.json', 'examples/example-copy.json', 'fingerprint')
    await api.delete('example.json', 'fingerprint')
    await api.deleteEntry('examples', null)
    await api.gitStatus()
    await api.gitDetails('examples/example.json')
    await api.configureGitRemote('https://example.com/mootool.git')
    await api.runGit(git)
    await api.cancelGit('request-1')

    expect(invoke.mock.calls).toEqual([
      ['get_vault_snapshot'],
      ['configure_vault', { rootDirectory: '/tmp/vault' }],
      ['disconnect_vault'],
      ['read_vault_document', { relativePath: 'example.json' }],
      ['save_vault_document', { request: save }],
      ['create_vault_directory', { relativePath: 'examples' }],
      ['move_vault_entry', { relativePath: 'example.json', destinationPath: 'examples/example.json', expectedFingerprint: 'fingerprint' }],
      ['duplicate_vault_document', { relativePath: 'examples/example.json', destinationPath: 'examples/example-copy.json', expectedFingerprint: 'fingerprint' }],
      ['delete_vault_document', { relativePath: 'example.json', expectedFingerprint: 'fingerprint' }],
      ['delete_vault_entry', { relativePath: 'examples', expectedFingerprint: null }],
      ['get_vault_git_status'],
      ['get_vault_git_details', { relativePath: 'examples/example.json' }],
      ['configure_vault_git_remote', { remote: 'https://example.com/mootool.git' }],
      ['run_vault_git', { request: git }],
      ['cancel_vault_git', { requestId: 'request-1' }]
    ])
  })
})
