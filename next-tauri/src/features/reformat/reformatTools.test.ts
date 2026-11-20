import { describe, expect, it } from 'vitest'
import { formatCode, formatNginx } from './reformatTools'

describe('reformat tools', () => {
  it('formats nested Nginx blocks without splitting quoted semicolons', () => {
    expect(formatNginx('server { set $value "a;b"; location / { return 200; } }', 2)).toBe(
      'server {\n  set $value "a;b";\n  location / {\n    return 200;\n  }\n}'
    )
  })

  it('keeps unbalanced Nginx blocks instead of rejecting them', () => {
    expect(formatNginx('server { listen 80;')).toBe('server {\n    listen 80;')
    expect(formatNginx('server }')).toBe('server\n}')
    expect(formatNginx('set $value "a;')).toBe('set $value "a;')
  })

  it('formats XML, HTML and Java through isolated formatter plugins', async () => {
    await expect(formatCode('<root><name>MooTool</name></root>', 'xml', 2))
      .resolves.toContain('\n  <name>MooTool</name>')
    await expect(formatCode('<main><section><p>MooTool</p><p>Tauri</p></section></main>', 'html', 2))
      .resolves.toContain('\n    <p>MooTool</p>')
    const java = await formatCode('class Demo{public static void main(String[] args){System.out.println("moo");}}', 'java', 2)
    expect(java).toContain('class Demo')
    expect(java).toContain('System.out.println("moo");')
  })
})
