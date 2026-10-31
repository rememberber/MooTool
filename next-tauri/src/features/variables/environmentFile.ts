export interface EnvironmentEntry {
  key: string
  value: string
}

const environmentAssignment = /^\s*(?:export\s+)?([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/

export function parseEnvironmentContent(content: string): EnvironmentEntry[] {
  const values = new Map<string, string>()
  for (const line of content.split(/\r?\n/)) {
    const match = line.match(environmentAssignment)
    if (!match) continue
    values.set(match[1], unquoteEnvironmentValue(match[2].trim()))
  }
  return [...values].map(([key, value]) => ({ key, value })).sort(compareEntries)
}

export function updateEnvironmentContent(content: string, key: string, value: string | undefined, shellExport: boolean): string {
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(key)) throw new Error('Invalid environment variable name')
  if (value !== undefined && (value.length > 65_535 || /[\0\r\n]/.test(value))) throw new Error('Invalid environment variable value')
  const replacement = value === undefined ? undefined : serializeEnvironmentValue(key, value, shellExport)
  const lines = content.split(/\r?\n/)
  const result: string[] = []
  let replaced = false
  for (const line of lines) {
    const match = line.match(environmentAssignment)
    if (match?.[1] === key) {
      if (!replaced && replacement !== undefined) {
        result.push(replacement)
        replaced = true
      }
      continue
    }
    result.push(line)
  }
  while (result.at(-1) === '') result.pop()
  if (replacement !== undefined && !replaced) result.push(replacement)
  return result.length ? `${result.join('\n')}\n` : ''
}

export function formatEnvironment(snapshot: {
  user: EnvironmentEntry[]
  system: EnvironmentEntry[]
  environment: EnvironmentEntry[]
  runtime: EnvironmentEntry[]
}): string {
  return [
    '------------Persistent user environment---------------',
    ...snapshot.user.map((entry) => `${entry.key}=${entry.value}`),
    '',
    '------------Persistent system environment---------------',
    ...snapshot.system.map((entry) => `${entry.key}=${entry.value}`),
    '',
    '------------Current process environment---------------',
    ...snapshot.environment.map((entry) => `${entry.key}=${entry.value}`),
    '',
    '------------Electron runtime---------------',
    ...snapshot.runtime.map((entry) => `${entry.key}=${entry.value}`)
  ].join('\n')
}

function serializeEnvironmentValue(key: string, value: string, shellExport: boolean): string {
  if (shellExport) return `export ${key}='${value.replace(/'/g, `'\\''`)}'`
  return `${key}="${value.replace(/\\/g, '\\\\').replace(/"/g, '\\"')}"`
}

function unquoteEnvironmentValue(value: string): string {
  if (value.length >= 2 && value.startsWith("'") && value.endsWith("'")) {
    return value.slice(1, -1).replace(/'\\''/g, "'")
  }
  if (value.length >= 2 && value.startsWith('"') && value.endsWith('"')) {
    return value.slice(1, -1).replace(/\\"/g, '"').replace(/\\\\/g, '\\')
  }
  return value
}

function compareEntries(left: EnvironmentEntry, right: EnvironmentEntry): number {
  return left.key.localeCompare(right.key)
}
