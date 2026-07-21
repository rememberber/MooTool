import { describe, expect, it } from 'vitest'
import { formatCodeEditorContent } from './codeEditorFormatting'

describe('formatCodeEditorContent', () => {
  it('formats JSON content from a MIME type', async () => {
    await expect(formatCodeEditorContent('{"tool":"MooTool","ready":true}', 'application/json')).resolves.toBe(
      '{\n  "tool": "MooTool",\n  "ready": true\n}'
    )
  })

  it('formats JavaScript and trims plain text', async () => {
    await expect(formatCodeEditorContent('const value={ready:true}', 'text/javascript')).resolves.toBe(
      'const value = { ready: true };'
    )
    await expect(formatCodeEditorContent('first  \nsecond\t\n', 'text/plain')).resolves.toBe('first\nsecond')
  })

  it('formats SQL with the configured dialect', async () => {
    await expect(formatCodeEditorContent(
      'select id,name from users where active=1 and role in (select role from roles)',
      'text/sql',
      { tabWidth: 4, sqlDialect: 'MySQL' }
    )).resolves.toBe(
      'SELECT\n    id,\n    name\nFROM\n    users\nWHERE\n    active = 1\n    AND role IN (\n        SELECT\n            role\n        FROM\n            roles\n    )'
    )
  })

  it('formats Python by expanding tabs and trimming trailing spaces', async () => {
    await expect(formatCodeEditorContent('\tdef greet():  \n\t\tprint("moo")\t\n', 'text/python', { tabWidth: 4 })).resolves.toBe(
      '    def greet():\n        print("moo")'
    )
  })
})
