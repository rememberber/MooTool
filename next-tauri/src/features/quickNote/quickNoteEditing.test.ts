import { describe, expect, it } from 'vitest'
import { formatNoteContent, prefixSelectedLines } from './quickNoteEditing'

describe('quick note editing', () => {
  it('prefixes the selected lines the way Electron does', () => {
    expect(prefixSelectedLines('keep\nalpha\nbeta\nend', 6, 14, 'numbered')).toBe('keep\n1. alpha\n2. beta\nend')
    expect(prefixSelectedLines('alpha\nbeta', 0, 10, 'bullet')).toBe('- alpha\n- beta')
  })

  it('formats SQL keywords with the selected dialect', async () => {
    const formatted = await formatNoteContent('select id from users where id = 1', 'sql', 'PostgreSQL')
    expect(formatted).toContain('SELECT')
    expect(formatted).toContain('FROM')
  })

  it('formats JSON with a four-space indent', async () => {
    await expect(formatNoteContent('{"a":1}', 'json')).resolves.toBe('{\n    "a": 1\n}')
  })
})
