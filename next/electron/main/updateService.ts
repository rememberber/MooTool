import type {
  UpdateCheckResult,
  UpdateDownload,
  UpdateProductId,
  UpdateTarget
} from '../../src/shared/contracts/update'

type UpdateManifest = {
  schemaVersion?: unknown
  products?: unknown
}

type ProductRelease = {
  version: string
  title: string
  notes: string
  prerelease: boolean
  releaseUrl: string
  assets: UpdateAsset[]
}

type UpdateProduct = {
  displayName: string
  releases: ProductRelease[]
}

type UpdateAsset = UpdateDownload & {
  platform: string
  architecture: string
  priority: number
}

export type UpdateClientIdentity = UpdateTarget & {
  productId: UpdateProductId
  packageType?: string
}

type UpdateFetch = (url: string, init: RequestInit) => Promise<Pick<Response, 'ok' | 'status' | 'text'>>

export const currentUpdateProductId: UpdateProductId = 'next-electron'
export const defaultUpdateFeedUrl = 'https://raw.githubusercontent.com/rememberber/MooTool/master/update-manifest.json'
export const defaultReleaseUrl = 'https://github.com/rememberber/MooTool/releases'

export class UpdateService {
  constructor(
    private readonly feedUrl = defaultUpdateFeedUrl,
    private readonly identity: UpdateClientIdentity = {
      productId: currentUpdateProductId,
      platform: process.platform,
      architecture: process.arch
    },
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
    const product = parseManifest(raw, this.identity.productId)
    const normalizedCurrent = normalizeVersion(currentVersion)
    const allowPrerelease = parseVersion(normalizedCurrent).prerelease.length > 0
    const eligibleReleases = product.releases.filter((release) => allowPrerelease || !release.prerelease)
    if (eligibleReleases.length === 0) throw new Error(`Update product has no eligible releases: ${this.identity.productId}`)
    const latestRelease = eligibleReleases.reduce((latest, release) => (
      compareVersions(release.version, latest.version) > 0 ? release : latest
    ))
    const normalizedLatest = latestRelease.version
    const available = compareVersions(normalizedLatest, normalizedCurrent) > 0
    return {
      status: available ? 'available' : 'latest',
      productId: this.identity.productId,
      productName: product.displayName,
      currentVersion: normalizedCurrent,
      latestVersion: normalizedLatest,
      releaseUrl: latestRelease.releaseUrl,
      releaseNotes: available ? releaseNotesAfter(eligibleReleases, normalizedCurrent) : '',
      target: {
        platform: this.identity.platform,
        architecture: normalizeArchitecture(this.identity.architecture)
      },
      download: available ? selectUpdateAsset(latestRelease.assets, this.identity) : null,
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
  if (a.prerelease.length === 0 && b.prerelease.length === 0) return 0
  if (a.prerelease.length === 0) return 1
  if (b.prerelease.length === 0) return -1
  for (let index = 0; index < Math.max(a.prerelease.length, b.prerelease.length); index += 1) {
    const leftPart = a.prerelease[index]
    const rightPart = b.prerelease[index]
    if (leftPart === undefined) return -1
    if (rightPart === undefined) return 1
    if (leftPart === rightPart) continue
    const leftNumeric = /^\d+$/.test(leftPart)
    const rightNumeric = /^\d+$/.test(rightPart)
    if (leftNumeric && rightNumeric) return Number(leftPart) > Number(rightPart) ? 1 : -1
    if (leftNumeric !== rightNumeric) return leftNumeric ? -1 : 1
    return leftPart > rightPart ? 1 : -1
  }
  return 0
}

function parseManifest(raw: string, productId: UpdateProductId): UpdateProduct {
  let value: UpdateManifest
  try {
    value = JSON.parse(raw) as UpdateManifest
  } catch {
    throw new Error('Update response is not valid JSON')
  }
  if (value.schemaVersion !== 1 || !isRecord(value.products)) throw new Error('Unsupported update manifest')
  const productValue = value.products[productId]
  if (!isRecord(productValue)) throw new Error(`Update product is not registered: ${productId}`)
  if (productValue.status !== 'active') throw new Error(`Update product is not active: ${productId}`)
  if (typeof productValue.displayName !== 'string' || !Array.isArray(productValue.releases)) {
    throw new Error(`Invalid update product: ${productId}`)
  }
  const releases = productValue.releases.map(parseRelease)
  if (releases.length === 0) throw new Error(`Update product has no releases: ${productId}`)
  if (new Set(releases.map((release) => release.version)).size !== releases.length) {
    throw new Error(`Update product has duplicate release versions: ${productId}`)
  }
  return { displayName: productValue.displayName.slice(0, 120), releases }
}

function parseRelease(value: unknown): ProductRelease {
  if (!isRecord(value) || typeof value.version !== 'string' || typeof value.releaseUrl !== 'string') {
    throw new Error('Invalid release in update manifest')
  }
  const version = normalizeVersion(value.version)
  const prerelease = parseVersion(version).prerelease.length > 0
  if (value.prerelease !== undefined && typeof value.prerelease !== 'boolean') {
    throw new Error(`Invalid prerelease flag for version ${version}`)
  }
  if (typeof value.prerelease === 'boolean' && value.prerelease !== prerelease) {
    throw new Error(`Prerelease flag does not match version ${version}`)
  }
  return {
    version,
    title: typeof value.title === 'string' ? value.title.slice(0, 300) : '',
    notes: typeof value.notes === 'string' ? value.notes.slice(0, 5000) : '',
    prerelease,
    releaseUrl: normalizeHttpsUrl(value.releaseUrl, 'release'),
    assets: Array.isArray(value.assets) ? value.assets.map(parseAsset) : []
  }
}

function parseAsset(value: unknown): UpdateAsset {
  if (!isRecord(value)
    || typeof value.platform !== 'string'
    || typeof value.architecture !== 'string'
    || typeof value.packageType !== 'string'
    || typeof value.fileName !== 'string'
    || typeof value.url !== 'string') {
    throw new Error('Invalid asset in update manifest')
  }
  const fileName = value.fileName.trim()
  if (!fileName || fileName.length > 240 || /[/\\]/.test(fileName)) throw new Error('Invalid update asset file name')
  return {
    platform: value.platform.trim().toLowerCase(),
    architecture: normalizeArchitecture(value.architecture),
    packageType: value.packageType.trim().toLowerCase().slice(0, 40),
    fileName,
    url: normalizeHttpsUrl(value.url, 'asset'),
    priority: typeof value.priority === 'number' && Number.isSafeInteger(value.priority) ? value.priority : 100
  }
}

function selectUpdateAsset(assets: UpdateAsset[], target: UpdateTarget & { packageType?: string }): UpdateDownload | null {
  const platform = target.platform.trim().toLowerCase()
  const architecture = normalizeArchitecture(target.architecture)
  const packageType = target.packageType?.trim().toLowerCase()
  const match = assets
    .filter((asset) => asset.platform === platform && (asset.architecture === architecture || asset.architecture === 'universal'))
    .sort((left, right) => {
      const architectureOrder = Number(left.architecture !== architecture) - Number(right.architecture !== architecture)
      if (architectureOrder !== 0) return architectureOrder
      const packageTypeOrder = Number(Boolean(packageType) && left.packageType !== packageType)
        - Number(Boolean(packageType) && right.packageType !== packageType)
      if (packageTypeOrder !== 0) return packageTypeOrder
      if (left.priority !== right.priority) return left.priority - right.priority
      return packagePreference(platform, left.packageType) - packagePreference(platform, right.packageType)
    })[0]
  return match ? { fileName: match.fileName, packageType: match.packageType, url: match.url } : null
}

function releaseNotesAfter(releases: ProductRelease[], currentVersion: string): string {
  const sections = releases
    .filter((release) => compareVersions(release.version, currentVersion) > 0)
    .sort((left, right) => compareVersions(right.version, left.version))
    .map((release) => {
      const heading = release.title.includes(release.version)
        ? release.title
        : [release.version, release.title].filter(Boolean).join(' — ')
      return [heading, release.notes].filter(Boolean).join('\n')
    })
  const selected: string[] = []
  let length = 0
  for (const section of sections) {
    const addedLength = section.length + (selected.length > 0 ? 2 : 0)
    if (selected.length > 0 && length + addedLength > 12_000) break
    selected.unshift(section.slice(0, 12_000))
    length += addedLength
  }
  return selected.join('\n\n')
}

function packagePreference(platform: string, packageType: string): number {
  const preferences: Record<string, string[]> = {
    darwin: ['zip', 'dmg', 'pkg'],
    win32: ['nsis', 'msi', 'portable', 'zip'],
    linux: ['appimage', 'deb', 'rpm', 'tar.gz']
  }
  const index = (preferences[platform] ?? []).indexOf(packageType)
  return index < 0 ? 100 : index
}

function normalizeArchitecture(value: string): string {
  const normalized = value.trim().toLowerCase()
  if (normalized === 'aarch64') return 'arm64'
  if (normalized === 'amd64') return 'x64'
  return normalized
}

function normalizeHttpsUrl(value: string, kind: string): string {
  let url: URL
  try {
    url = new URL(value)
  } catch {
    throw new Error(`Invalid update ${kind} URL`)
  }
  if (url.protocol !== 'https:') throw new Error(`Update ${kind} URL must use HTTPS`)
  return url.toString()
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function normalizeVersion(value: string): string {
  const normalized = value.trim().replace(/^v/i, '')
  parseVersion(normalized)
  return normalized
}

function parseVersion(value: string): { numbers: [number, number, number]; prerelease: string[] } {
  const match = /^(0|[1-9]\d*)(?:\.(0|[1-9]\d*))?(?:\.(0|[1-9]\d*))?(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/.exec(value.trim().replace(/^v/i, ''))
  if (!match) throw new Error(`Invalid version: ${value}`)
  const prerelease = match[4]?.split('.') ?? []
  if (prerelease.some((part) => /^0\d+$/.test(part))) throw new Error(`Invalid version: ${value}`)
  return {
    numbers: [Number(match[1]), Number(match[2] ?? 0), Number(match[3] ?? 0)],
    prerelease
  }
}
