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
    const latestRelease = product.releases.reduce((latest, release) => (
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
      releaseNotes: available ? releaseNotesAfter(product.releases, normalizedCurrent) : '',
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
  if (!a.prerelease && !b.prerelease) return 0
  if (!a.prerelease) return 1
  if (!b.prerelease) return -1
  return a.prerelease.localeCompare(b.prerelease, undefined, { numeric: true })
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
  return { displayName: productValue.displayName.slice(0, 120), releases }
}

function parseRelease(value: unknown): ProductRelease {
  if (!isRecord(value) || typeof value.version !== 'string' || typeof value.releaseUrl !== 'string') {
    throw new Error('Invalid release in update manifest')
  }
  return {
    version: normalizeVersion(value.version),
    title: typeof value.title === 'string' ? value.title.slice(0, 300) : '',
    notes: typeof value.notes === 'string' ? value.notes.slice(0, 5000) : '',
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

function selectUpdateAsset(assets: UpdateAsset[], target: UpdateTarget): UpdateDownload | null {
  const platform = target.platform.trim().toLowerCase()
  const architecture = normalizeArchitecture(target.architecture)
  const match = assets
    .filter((asset) => asset.platform === platform && (asset.architecture === architecture || asset.architecture === 'universal'))
    .sort((left, right) => {
      const architectureOrder = Number(left.architecture !== architecture) - Number(right.architecture !== architecture)
      if (architectureOrder !== 0) return architectureOrder
      if (left.priority !== right.priority) return left.priority - right.priority
      return packagePreference(platform, left.packageType) - packagePreference(platform, right.packageType)
    })[0]
  return match ? { fileName: match.fileName, packageType: match.packageType, url: match.url } : null
}

function releaseNotesAfter(releases: ProductRelease[], currentVersion: string): string {
  return releases
    .filter((release) => compareVersions(release.version, currentVersion) > 0)
    .sort((left, right) => compareVersions(left.version, right.version))
    .map((release) => [release.version, release.title, release.notes].filter(Boolean).join('\n'))
    .join('\n\n')
    .slice(0, 12_000)
}

function packagePreference(platform: string, packageType: string): number {
  const preferences: Record<string, string[]> = {
    darwin: ['dmg', 'pkg', 'zip'],
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

function parseVersion(value: string): { numbers: [number, number, number]; prerelease: string } {
  const match = /^(\d+)(?:\.(\d+))?(?:\.(\d+))?(?:-([0-9A-Za-z.-]+))?$/.exec(value.trim().replace(/^v/i, ''))
  if (!match) throw new Error(`Invalid version: ${value}`)
  return {
    numbers: [Number(match[1]), Number(match[2] ?? 0), Number(match[3] ?? 0)],
    prerelease: match[4] ?? ''
  }
}
