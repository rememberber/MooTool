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
})
