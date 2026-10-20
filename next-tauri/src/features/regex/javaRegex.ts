export const javaRegexSource = `import java.io.*;
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

export interface JavaRegexOptions {
  global: boolean
  ignoreCase: boolean
  multiline: boolean
  dotAll: boolean
}

export interface JavaRegexInput {
  pattern: string
  source: string
  options: JavaRegexOptions
}

export interface JavaRegexMatch {
  index: number
  value: string
  groups: string[]
}

export interface JavaRegexResult {
  matches: JavaRegexMatch[]
  limited: boolean
}

export function javaRegexFlags(options: JavaRegexOptions): number {
  return (options.ignoreCase ? 2 : 0) | (options.multiline ? 8 : 0) | (options.dotAll ? 32 : 0)
}

export function buildJavaRegexStdin(input: JavaRegexInput): string {
  validateJavaRegexInput(input)
  return [
    encodeBase64(input.pattern),
    encodeBase64(input.source),
    javaRegexFlags(input.options),
    input.options.global
  ].join('\n') + '\n'
}

export function parseJavaRegexStdout(stdout: string): JavaRegexResult {
  const result: JavaRegexResult = { matches: [], limited: false }
  for (const line of stdout.split('\n')) {
    if (line.replace(/\r$/, '') === 'LIMIT') {
      result.limited = true
      continue
    }
    const [kind, index, ...groups] = line.replace(/\r$/, '').split('\t')
    if (kind === 'ERROR') throw new Error(decodeBase64(index ?? ''))
    if (kind !== 'M') continue
    const [text = '', ...captures] = groups.map((value) => decodeBase64(value))
    result.matches.push({ index: Number(index), value: text, groups: captures })
  }
  return result
}

export function validateJavaRegexInput(value: JavaRegexInput): void {
  const options = value?.options
  if (!value || typeof value.pattern !== 'string' || typeof value.source !== 'string' || !options
    || value.pattern.length > 16_000 || value.source.length > 1_000_000
    || ['global', 'ignoreCase', 'multiline', 'dotAll'].some((key) => typeof options[key as keyof JavaRegexOptions] !== 'boolean')) {
    throw new Error('Invalid regex input (pattern ≤ 16000, text ≤ 1000000 characters)')
  }
}

function encodeBase64(value: string): string {
  const bytes = new TextEncoder().encode(value)
  let binary = ''
  for (const byte of bytes) binary += String.fromCharCode(byte)
  return btoa(binary)
}

function decodeBase64(value: string): string {
  const binary = atob(value)
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}
