import { spawn } from 'node:child_process'
import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { describe, expect, it } from 'vitest'
import {
  buildJavaRegexStdin,
  javaRegexSource,
  parseJavaRegexStdout,
  type JavaRegexInput,
  type JavaRegexResult
} from './javaRegex'

const options = { global: true, ignoreCase: false, multiline: false, dotAll: false }

describe('Java regex protocol', () => {
  it('rejects oversized input before a process starts', () => {
    expect(() => buildJavaRegexStdin({ pattern: 'a'.repeat(16_001), source: 'a', options })).toThrow('Invalid regex')
  })

  it('reads match rows, empty captures and the result limit', () => {
    const text = parseJavaRegexStdout('M\t2\tbW9v\tbW9v\t\nLIMIT\n')
    expect(text).toEqual({ matches: [{ index: 2, value: 'moo', groups: ['moo', ''] }], limited: true })
    expect(() => parseJavaRegexStdout(`ERROR\t${btoa('Unclosed character class')}\n`)).toThrow('Unclosed')
  })
})

describe('Java regex engine', () => {
  it('uses Java quoting, intersections, possessive quantifiers and inline flags', async () => {
    expect((await match({ pattern: '\\Qfoo.bar\\E', source: 'foo.bar fooXbar', options })).matches).toEqual([{ index: 0, value: 'foo.bar', groups: [] }])
    expect((await match({ pattern: '[a-z&&[^aeiou]]+', source: 'abcdef', options })).matches.map((item) => item.value)).toEqual(['bcd', 'f'])
    expect((await match({ pattern: 'a++a', source: 'aaa', options })).matches).toEqual([])
    expect((await match({ pattern: '(?i)moo', source: 'MOO', options })).matches[0].value).toBe('MOO')
  }, 20_000)

  it('retains UTF-16 offsets, empty captures and zero-width matches', async () => {
    const result = await match({ pattern: '(moo)(x)?', source: '😀moo', options })
    expect(result.matches).toEqual([{ index: 2, value: 'moo', groups: ['moo', ''] }])
    expect((await match({ pattern: '(?=a)', source: 'aa', options })).matches.map((item) => item.index)).toEqual([0, 1])
  }, 15_000)

  it('reports syntax errors and limits large result sets', async () => {
    await expect(match({ pattern: '[', source: 'text', options })).rejects.toThrow('Unclosed')
    const result = await match({ pattern: 'a', source: 'a'.repeat(6_000), options })
    expect(result.matches).toHaveLength(5_000)
    expect(result.limited).toBe(true)
  }, 15_000)
})

async function match(input: JavaRegexInput): Promise<JavaRegexResult> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-regex-'))
  try {
    const file = join(directory, 'MooToolRegex.java')
    await writeFile(file, javaRegexSource, 'utf8')
    const stdout = await new Promise<string>((resolve, reject) => {
      const child = spawn('java', ['-Xmx128m', file], { stdio: ['pipe', 'pipe', 'pipe'] })
      let output = ''
      let error = ''
      const timer = setTimeout(() => {
        child.kill('SIGKILL')
        reject(new Error('REGEX_TIMEOUT'))
      }, 5_000)
      child.stdout.on('data', (chunk: Buffer) => { output += chunk.toString('utf8') })
      child.stderr.on('data', (chunk: Buffer) => { error = (error + chunk.toString('utf8')).slice(0, 4_096) })
      child.once('error', (cause) => { clearTimeout(timer); reject(cause) })
      child.once('close', (code) => {
        clearTimeout(timer)
        if (code !== 0) reject(new Error(error || 'JDK 11+ is required'))
        else resolve(output)
      })
      child.stdin.end(buildJavaRegexStdin(input))
    })
    return parseJavaRegexStdout(stdout)
  } finally {
    await rm(directory, { recursive: true, force: true })
  }
}
