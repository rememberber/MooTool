import type { UpdateCheckResult } from '../../src/shared/contracts/update'

type VersionSummary = {
  currentVersion?: unknown
  versionDetailList?: unknown
}

type VersionDetail = {
  version: string
  title: string
  log: string
}

type UpdateFetch = (url: string, init: RequestInit) => Promise<Pick<Response, 'ok' | 'status' | 'text'>>

export const defaultUpdateFeedUrl = 'https://raw.githubusercontent.com/rememberber/MooTool/master/src/main/resources/version_summary.json'
export const defaultReleaseUrl = 'https://github.com/rememberber/MooTool/releases'

export class UpdateService {
  constructor(
    private readonly feedUrl = defaultUpdateFeedUrl,
    private readonly releaseUrl = defaultReleaseUrl,
    private readonly fetcher: UpdateFetch = fetch
  ) {}

  async check(currentVersion: string): Promise<UpdateCheckResult> {
    const response = await this.fetcher(this.feedUrl, {
      headers: { Accept: 'application/json', 'User-Agent': 'MooTool-Next' },
      signal: AbortSignal.timeout(10_000)
    })
    if (!response.ok) throw new Error(`Update server returned HTTP ${response.status}`)
    const raw = await response.text()
    if (!raw || raw.length > 2 * 1024 * 1024) throw new Error('Invalid update response')
    const summary = parseSummary(raw)
    const normalizedCurrent = normalizeVersion(currentVersion)
    const normalizedLatest = normalizeVersion(summary.latestVersion)
    const available = compareVersions(normalizedLatest, normalizedCurrent) > 0
    return {
      status: available ? 'available' : 'latest',
      currentVersion: normalizedCurrent,
      latestVersion: normalizedLatest,
      releaseUrl: this.releaseUrl,
      releaseNotes: available ? releaseNotesAfter(summary.details, normalizedCurrent) : '',
      checkedAt: new Date().toISOString()
    }
  }
}

export function compareVersions(left: string, right: string): number {
  const a = parseVersion(left)
  const b = parseVersion(right)
  for (let index = 0; index < 3; index += 1) {
    if (a.numbers[index] !== b.numbers[index]) return a.numbers[index] > b.numbers[index] ? 1 : -1
  }
  if (!a.prerelease && !b.prerelease) return 0
  if (!a.prerelease) return 1
  if (!b.prerelease) return -1
  return a.prerelease.localeCompare(b.prerelease, undefined, { numeric: true })
}

function parseSummary(raw: string): { latestVersion: string; details: VersionDetail[] } {
  let value: VersionSummary
  try {
    value = JSON.parse(raw) as VersionSummary
  } catch {
    throw new Error('Update response is not valid JSON')
  }
  if (typeof value.currentVersion !== 'string') throw new Error('Update response is missing a version')
  const details = Array.isArray(value.versionDetailList)
    ? value.versionDetailList.flatMap((item): VersionDetail[] => {
        if (!item || typeof item !== 'object') return []
        const record = item as Record<string, unknown>
        if (typeof record.version !== 'string') return []
        return [{
          version: normalizeVersion(record.version),
          title: typeof record.title === 'string' ? record.title.slice(0, 300) : '',
          log: typeof record.log === 'string' ? record.log.slice(0, 5000) : ''
        }]
      })
    : []
  return { latestVersion: value.currentVersion, details }
}

function releaseNotesAfter(details: VersionDetail[], currentVersion: string): string {
  return details
    .filter((detail) => compareVersions(detail.version, currentVersion) > 0)
    .sort((left, right) => compareVersions(left.version, right.version))
    .map((detail) => [detail.version, detail.title, detail.log].filter(Boolean).join('\n'))
    .join('\n\n')
    .slice(0, 12_000)
}

function normalizeVersion(value: string): string {
  const normalized = value.trim().replace(/^v/i, '')
  parseVersion(normalized)
  return normalized
}

function parseVersion(value: string): { numbers: [number, number, number]; prerelease: string } {
  const match = /^(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z.-]+))?$/.exec(value.trim().replace(/^v/i, ''))
  if (!match) throw new Error(`Invalid version: ${value}`)
  return {
    numbers: [Number(match[1]), Number(match[2] ?? 0), Number(match[3] ?? 0)],
    prerelease: match[4] ?? ''
  }
}
