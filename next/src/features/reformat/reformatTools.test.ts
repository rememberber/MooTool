import { describe, expect, it } from 'vitest'
import { formatCode, formatNginx } from './reformatTools'

describe('reformat tools', () => {
  it('formats nginx blocks and statements', () => {
    expect(formatNginx('server { listen 80; location / { proxy_pass http://app; } }', 2)).toBe(
      'server {\n  listen 80;\n  location / {\n    proxy_pass http://app;\n  }\n}'
    )
  })

  it('formats Java using the Java parser', async () => {
    const output = await formatCode('class Demo{public static void main(String[] args){System.out.println("moo");}}', 'java', 2)
    expect(output).toContain('class Demo')
    expect(output).toContain('System.out.println("moo");')
  })

  it('formats XML and HTML', async () => {
    await expect(formatCode('<root><item id="1">moo</item></root>', 'xml', 2)).resolves.toContain('\n  <item')
    await expect(formatCode('<main><strong>Moo</strong></main>', 'html', 2)).resolves.toContain('<strong>Moo</strong>')
  })
})
