export type SensitiveFindingKind = 'privateKey' | 'credentialAssignment' | 'knownTokenFormat'

export type SensitiveFinding = {
  id: string
  kind: SensitiveFindingKind
  line: number
  severity: 'warning' | 'blocking'
}

const assignmentPattern = /^\s*["']?(?:api[_-]?key|access[_-]?token|auth[_-]?token|secret|client[_-]?secret|password|authorization)["']?\s*[:=]\s*["']?([^"'#,\s][^"'#,]*)/i
const knownTokenPatterns = [
  /\bsk-[A-Za-z0-9_-]{20,}\b/,
  /\bgh[opusr]_[A-Za-z0-9]{20,}\b/,
  /\bAKIA[0-9A-Z]{16}\b/
]

export function scanSensitiveContent(content: string): SensitiveFinding[] {
  if (typeof content !== 'string') return []
  const findings: SensitiveFinding[] = []
  const seen = new Set<string>()
  const lines = content.slice(0, 1024 * 1024).split(/\r?\n/)
  for (let index = 0; index < lines.length && findings.length < 100; index += 1) {
    const line = lines[index]
    if (line.includes('-----BEGIN') && line.includes('PRIVATE KEY-----')) addFinding('privateKey', index + 1, 'blocking')
    const assignment = line.match(assignmentPattern)?.[1]?.trim()
    if (assignment && !isPlaceholder(assignment)) addFinding('credentialAssignment', index + 1, 'warning')
    if (knownTokenPatterns.some((pattern) => pattern.test(line))) addFinding('knownTokenFormat', index + 1, 'blocking')
  }
  return findings

  function addFinding(kind: SensitiveFindingKind, line: number, severity: SensitiveFinding['severity']): void {
    const id = `${kind}:${line}`
    if (seen.has(id)) return
    seen.add(id)
    findings.push({ id, kind, line, severity })
  }
}

export function redactSensitiveContent(content: string): string {
  const output: string[] = []
  let privateKeyBlock = false
  for (const line of content.split(/\r?\n/)) {
    if (line.includes('-----BEGIN') && line.includes('PRIVATE KEY-----')) {
      privateKeyBlock = true
      output.push('[REDACTED PRIVATE KEY]')
      continue
    }
    if (privateKeyBlock) {
      if (line.includes('-----END') && line.includes('PRIVATE KEY-----')) privateKeyBlock = false
      continue
    }
    let redacted = line
    const assignedValue = line.match(assignmentPattern)?.[1]?.trim()
    if (assignedValue && !isPlaceholder(assignedValue)) redacted = redacted.replace(assignedValue, '[REDACTED]')
    for (const pattern of knownTokenPatterns) {
      redacted = redacted.replace(new RegExp(pattern.source, `${pattern.flags.replace('g', '')}g`), '[REDACTED TOKEN]')
    }
    output.push(redacted)
  }
  return output.join('\n')
}

function isPlaceholder(value: string): boolean {
  const normalized = value.replace(/["',\]]+$/, '').trim()
  return !normalized
    || normalized === 'null'
    || normalized === 'undefined'
    || /^\$\{[A-Z0-9_]+}$/.test(normalized)
    || /^\$[A-Z0-9_]+$/.test(normalized)
    || /^env:[A-Z0-9_]+$/i.test(normalized)
    || /^<[^>]+>$/.test(normalized)
    || /^(replace|insert|your)[_-]?(me|key|token|secret|password)?$/i.test(normalized)
}
