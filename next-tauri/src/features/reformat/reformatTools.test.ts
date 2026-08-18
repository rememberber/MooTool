import { describe, expect, it } from 'vitest'
import { formatCode, formatNginx, ReformatToolError } from './reformatTools'

describe('reformat tools', () => {
  it('formats nested Nginx blocks without splitting quoted semicolons', () => {
    expect(formatNginx('server { set $value "a;b"; location / { return 200; } }', 2)).toBe(
      'server {\n  set $value "a;b";\n  location / {\n    return 200;\n  }\n}'
    )
  })

  it('rejects unbalanced Nginx blocks', () => {
    expect(() => formatNginx('server { listen 80;')).toThrowError(
      expect.objectContaining<Partial<ReformatToolError>>({ code: 'unclosedBlock' })
    )
    expect(() => formatNginx('server }')).toThrowError(
      expect.objectContaining<Partial<ReformatToolError>>({ code: 'unexpectedClosingBrace' })
    )
  })

  it('formats XML, HTML and Java through isolated formatter plugins', async () => {
    await expect(formatCode('<root><name>MooTool</name></root>', 'xml', 2))
      .resolves.toContain('\n  <name>MooTool</name>')
    await expect(formatCode('<main><section><p>MooTool</p><p>Tauri</p></section></main>', 'html', 2))
      .resolves.toContain('\n    <p>MooTool</p>')
    await expect(formatCode('class Demo{int value=1;}', 'java', 2))
      .resolves.toContain('int value = 1;')
  })
})
