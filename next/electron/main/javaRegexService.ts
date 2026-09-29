import { spawn } from 'node:child_process'
import { mkdtemp, writeFile, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import type { RegexInput, RegexResult } from '../../src/shared/contracts/regex'
import { getRuntimeEnvironment, resolveRuntimeCommand } from './runtimeDiscovery'

// Pattern and source are stdin data, never Java source or command-line arguments.
const javaSource = `import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.nio.charset.StandardCharsets;
class MooToolRegex {
 static String decode(String s) { return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8); }
 static String encode(String s) { return Base64.getEncoder().encodeToString((s == null ? "" : s).getBytes(StandardCharsets.UTF_8)); }
 public static void main(String[] args) throws Exception {
  BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
  try {
   String pattern = decode(in.readLine()), source = decode(in.readLine());
   int flags = Integer.parseInt(in.readLine());
   boolean global = Boolean.parseBoolean(in.readLine());
   Matcher matcher = Pattern.compile(pattern, flags).matcher(source);
   int count = 0, size = 0;
   while (matcher.find()) {
    if (count++ >= 5000) { System.out.println("LIMIT"); return; }
    StringBuilder row = new StringBuilder("M\\t" + matcher.start() + "\\t" + encode(matcher.group()));
    for (int i = 1; i <= matcher.groupCount(); i++) row.append("\\t").append(encode(matcher.group(i)));
    size += row.length();
    if (size > 1500000) { System.out.println("LIMIT"); return; }
    System.out.println(row);
    if (!global) break;
   }
  } catch (Exception e) { System.out.println("ERROR\\t" + encode(e.getMessage())); }
 }
}`

export class JavaRegexService {
  private readonly requests = new Map<number, AbortController>()

  cancel(owner: number): void { this.requests.get(owner)?.abort(); this.requests.delete(owner) }
  dispose(): void { for (const owner of this.requests.keys()) this.cancel(owner) }

  async match(owner: number, value: unknown, javaPath?: string): Promise<RegexResult> {
    validate(value)
    this.cancel(owner)
    const controller = new AbortController()
    this.requests.set(owner, controller)
    let directory: string | undefined
    try {
      const env = await getRuntimeEnvironment()
      const command = await resolveRuntimeCommand('java', javaPath ?? '', env)
      controller.signal.throwIfAborted()
      directory = await mkdtemp(join(tmpdir(), 'mootool-regex-'))
      const file = join(directory, 'MooToolRegex.java')
      await writeFile(file, javaSource, 'utf8')
      controller.signal.throwIfAborted()
      const flags = (value.options.ignoreCase ? 2 : 0) | (value.options.multiline ? 8 : 0) | (value.options.dotAll ? 32 : 0)
      const input = [Buffer.from(value.pattern).toString('base64'), Buffer.from(value.source).toString('base64'), flags, value.options.global].join('\n') + '\n'
      const stdout = await new Promise<string>((resolve, reject) => {
        const child = spawn(command, ['-Xmx128m', file], { env, windowsHide: true, stdio: ['pipe', 'pipe', 'pipe'] })
        let output = '', error = '', bytes = 0, failure = ''
        const kill = () => { failure ||= 'REGEX_CANCELLED'; child.kill('SIGKILL') }
        controller.signal.addEventListener('abort', kill, { once: true })
        const timer = setTimeout(() => { failure = 'REGEX_TIMEOUT'; child.kill('SIGKILL') }, 5000)
        const cleanup = () => { clearTimeout(timer); controller.signal.removeEventListener('abort', kill) }
        child.stdout.on('data', (chunk: Buffer) => {
          bytes += chunk.length
          if (bytes > 2 * 1024 * 1024) { failure = 'REGEX_OUTPUT_LIMIT'; child.kill('SIGKILL') }
          else output += chunk.toString('utf8')
        })
        child.stderr.on('data', (chunk: Buffer) => { error = (error + chunk.toString('utf8')).slice(0, 4096) })
        child.stdin.on('error', () => { /* Process exit is handled below. */ })
        child.once('error', err => { cleanup(); reject(err) })
        child.once('close', code => { cleanup(); failure || code !== 0 ? reject(new Error(failure || error || 'JDK 11+ is required')) : resolve(output) })
        child.stdin.end(input)
      })
      const result: RegexResult = { matches: [], limited: false }
      for (const line of stdout.split('\n')) {
        if (line === 'LIMIT') { result.limited = true; continue }
        const [kind, index, ...groups] = line.replace(/\r$/, '').split('\t')
        if (kind === 'ERROR') throw new Error(Buffer.from(index, 'base64').toString('utf8'))
        if (kind !== 'M') continue
        const [text = '', ...captures] = groups.map(value => Buffer.from(value, 'base64').toString('utf8'))
        result.matches.push({ index: Number(index), value: text, groups: captures })
      }
      return result
    } finally {
      if (this.requests.get(owner) === controller) this.requests.delete(owner)
      if (directory) await rm(directory, { recursive: true, force: true })
    }
  }
}

function validate(value: unknown): asserts value is RegexInput {
  const input = value as RegexInput | null
  if (!input || typeof input.pattern !== 'string' || typeof input.source !== 'string' || !input.options
    || input.pattern.length > 16000 || input.source.length > 1_000_000
    || ['global', 'ignoreCase', 'multiline', 'dotAll'].some(key => typeof input.options[key as keyof RegexInput['options']] !== 'boolean')) throw new Error('Invalid regex input (pattern ≤ 16000, text ≤ 1000000 characters)')
}
