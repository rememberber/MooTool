import { describe, expect, it } from 'vitest'
import { AdapterRegistry } from '../../electron/main/ai/adapterRegistry'

describe('AdapterRegistry', () => {
  it('preserves registration order and rejects ambiguous ids', () => {
    const registry = new AdapterRegistry([{ id: 'codex' }, { id: 'claudeCode' }])

    expect(registry.list().map((adapter) => adapter.id)).toEqual(['codex', 'claudeCode'])
    expect(registry.get('codex')).toEqual({ id: 'codex' })
    expect(() => registry.register({ id: 'codex' })).toThrow('Duplicate adapter id')
    expect(() => registry.register({ id: '  ' })).toThrow('Adapter id is required')
  })
})
