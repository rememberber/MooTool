import { describe, expect, it } from 'vitest'
import { formatRuntimeSource, parseRuntimeArguments, runtimeDisplayName } from './runtimeTools'

describe('runtime tools', () => {
  it('formats Node.js and normalizes script whitespace', async () => {
    expect(await formatRuntimeSource('const x={a:1};console.log(x)', 'node')).toContain('const x = { a: 1 }')
    expect(await formatRuntimeSource('\tprint("moo")  ', 'python')).toBe('    print("moo")')
  })

  it('labels runtimes', () => {
    expect(runtimeDisplayName('node')).toBe('Node.js')
    expect(runtimeDisplayName('groovy')).toBe('Groovy')
  })

  it('parses quoted arguments without invoking a shell', () => {
    expect(parseRuntimeArguments('--name "Moo Tool" --count 2')).toEqual(['--name', 'Moo Tool', '--count', '2'])
    expect(parseRuntimeArguments("'single value' escaped\\ value")).toEqual(['single value', 'escaped value'])
    expect(() => parseRuntimeArguments('"unfinished')).toThrow('Unterminated')
  })
})
