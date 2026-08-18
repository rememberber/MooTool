import { createHash } from 'node:crypto'
import { cp, mkdir, readFile, readdir, stat, writeFile } from 'node:fs/promises'
import { basename, join, relative } from 'node:path'
import { pathToFileURL } from 'node:url'

export const UPDATE_CHANNEL_TAG = 'next-tauri-updater'
export const UPDATE_MANIFEST_URL = `https://github.com/rememberber/MooTool/releases/download/${UPDATE_CHANNEL_TAG}/latest.json`
const PRODUCT_ID = 'next-tauri'
const PRODUCT_NAME = 'MooTool Next Tauri'
const REPOSITORY = 'rememberber/MooTool'
const SEMVER = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?(?:\+[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?$/

const targetRules = [
  {
    artifact: 'darwin-x86_64',
    updateKey: 'darwin-x86_64',
    platform: 'darwin',
    architecture: 'x64',
    installers: [
      { packageType: 'dmg', suffix: 'mac-x64.dmg', match: (name) => name.endsWith('.dmg') }
    ],
    updater: { suffix: 'mac-x64.app.tar.gz', match: (name) => name.endsWith('.app.tar.gz') }
  },
  {
    artifact: 'darwin-aarch64',
    updateKey: 'darwin-aarch64',
    platform: 'darwin',
    architecture: 'arm64',
    installers: [
      { packageType: 'dmg', suffix: 'mac-arm64.dmg', match: (name) => name.endsWith('.dmg') }
    ],
    updater: { suffix: 'mac-arm64.app.tar.gz', match: (name) => name.endsWith('.app.tar.gz') }
  },
  {
    artifact: 'windows-x86_64',
    updateKey: 'windows-x86_64',
    platform: 'win32',
    architecture: 'x64',
    installers: [
      { packageType: 'nsis', suffix: 'win-x64-setup.exe', match: (name) => name.endsWith('-setup.exe') },
      { packageType: 'msi', suffix: 'win-x64.msi', optional: true, match: (name) => name.endsWith('.msi') }
    ],
    updater: { suffix: 'win-x64-setup.exe', match: (name) => name.endsWith('-setup.exe') }
  },
  {
    artifact: 'linux-x86_64',
    updateKey: 'linux-x86_64',
    platform: 'linux',
    architecture: 'x64',
    installers: [
      { packageType: 'appimage', suffix: 'linux-x64.AppImage', match: (name) => name.endsWith('.AppImage') },
      { packageType: 'deb', suffix: 'linux-x64.deb', match: (name) => name.endsWith('.deb') },
      { packageType: 'rpm', suffix: 'linux-x64.rpm', optional: true, match: (name) => name.endsWith('.rpm') }
    ],
    updater: { suffix: 'linux-x64.AppImage', match: (name) => name.endsWith('.AppImage') }
  }
]

export async function prepareReleaseAssets({ artifactsDirectory, outputDirectory, version, tag, notesPath, publishedAt = new Date().toISOString() }) {
  validateVersionAndTag(version, tag)
  const notes = await readReleaseNotes(notesPath, version)
  const parsedDate = new Date(publishedAt)
  if (Number.isNaN(parsedDate.valueOf())) throw new Error(`Invalid release publication date: ${publishedAt}`)
  await mkdir(outputDirectory, { recursive: true })

  const releaseAssets = []
  const updatePlatforms = {}
  const copied = new Map()

  for (const rule of targetRules) {
    const artifactDirectory = await findArtifactDirectory(artifactsDirectory, rule.artifact)
    const files = await walkFiles(artifactDirectory)
    for (const installer of rule.installers) {
      const source = findOne(files, installer.match, `${rule.artifact} ${installer.packageType}`, installer.optional)
      if (!source) continue
      const fileName = releaseFileName(version, installer.suffix)
      const destination = join(outputDirectory, fileName)
      await copyOnce(source, destination, copied)
      releaseAssets.push({
        platform: rule.platform,
        architecture: rule.architecture,
        packageType: installer.packageType,
        priority: installer.optional ? 20 : 10,
        fileName,
        url: releaseAssetUrl(tag, fileName),
        sha512: await fileSha512(destination),
        size: (await stat(destination)).size
      })
    }

    const updaterSource = findOne(files, rule.updater.match, `${rule.artifact} updater`)
    const signatureSource = findOne(
      files,
      (name, path) => path === `${updaterSource}.sig`,
      `${rule.artifact} updater signature`
    )
    const updaterName = releaseFileName(version, rule.updater.suffix)
    const updaterDestination = join(outputDirectory, updaterName)
    await copyOnce(updaterSource, updaterDestination, copied)
    const signature = validateSignature((await readFile(signatureSource, 'utf8')).trim(), signatureSource)
    await writeFile(join(outputDirectory, `${updaterName}.sig`), `${signature}\n`, { mode: 0o644 })
    updatePlatforms[rule.updateKey] = {
      signature,
      url: releaseAssetUrl(tag, updaterName)
    }
  }

  releaseAssets.sort(compareAssets)
  const releaseUrl = `https://github.com/${REPOSITORY}/releases/tag/${tag}`
  const updaterManifest = {
    version,
    notes,
    pub_date: parsedDate.toISOString(),
    release_url: releaseUrl,
    platforms: updatePlatforms
  }
  const registryRelease = {
    version,
    title: `${PRODUCT_NAME} ${version}`,
    notes,
    prerelease: true,
    releaseUrl,
    updaterManifestUrl: UPDATE_MANIFEST_URL,
    assets: releaseAssets
  }
  await writeJson(join(outputDirectory, 'latest.json'), updaterManifest)
  await writeJson(join(outputDirectory, 'next-tauri-release.json'), registryRelease)
  return { updaterManifest, registryRelease }
}

export async function updateProductManifest({ manifestPath, releasePath }) {
  const manifest = JSON.parse(await readFile(manifestPath, 'utf8'))
  const release = JSON.parse(await readFile(releasePath, 'utf8'))
  if (manifest?.schemaVersion !== 1 || !isRecord(manifest.products)) {
    throw new Error('Unsupported product update manifest schema')
  }
  const product = manifest.products[PRODUCT_ID]
  if (!isRecord(product) || product.displayName !== PRODUCT_NAME || !Array.isArray(product.releases)) {
    throw new Error(`Invalid ${PRODUCT_ID} product registry`)
  }
  validateRegistryRelease(release)
  const existingReleases = product.releases.filter(isRecord)
  const latestExisting = existingReleases
    .filter((item) => typeof item.version === 'string')
    .sort((left, right) => compareVersions(left.version, right.version))
    .at(-1)
  if (latestExisting && compareVersions(release.version, latestExisting.version) < 0) {
    throw new Error(`Refusing to downgrade the Tauri update channel from ${latestExisting.version} to ${release.version}`)
  }
  product.status = 'active'
  product.updaterManifestUrl = UPDATE_MANIFEST_URL
  product.releases = [
    ...existingReleases.filter((item) => item.version !== release.version),
    release
  ].sort((left, right) => compareVersions(left.version, right.version))
  await writeJson(manifestPath, manifest)
  return release
}

export async function validatePromotion({ latestPath, releasePath, tag }) {
  const latest = JSON.parse(await readFile(latestPath, 'utf8'))
  const release = JSON.parse(await readFile(releasePath, 'utf8'))
  validateVersionAndTag(String(latest?.version ?? ''), tag)
  validateRegistryRelease(release)
  if (release.version !== latest.version) throw new Error('Updater and registry release versions differ')
  const expectedKeys = targetRules.map((rule) => rule.updateKey).sort()
  const actualKeys = isRecord(latest.platforms) ? Object.keys(latest.platforms).sort() : []
  if (JSON.stringify(actualKeys) !== JSON.stringify(expectedKeys)) {
    throw new Error(`Updater platforms are incomplete: ${actualKeys.join(', ')}`)
  }
  for (const [target, platform] of Object.entries(latest.platforms)) {
    if (!isRecord(platform)) throw new Error(`Invalid updater platform: ${target}`)
    validateSignature(String(platform.signature ?? ''), latestPath)
    const prefix = `https://github.com/${REPOSITORY}/releases/download/${tag}/MooTool-Next-Tauri-${latest.version}-`
    if (typeof platform.url !== 'string' || !platform.url.startsWith(prefix)) {
      throw new Error(`Updater target ${target} does not use the independent version release`)
    }
  }
  if (latest.release_url !== release.releaseUrl) throw new Error('Updater release URL is inconsistent')
  return { latest, release }
}

function validateRegistryRelease(release) {
  if (!isRecord(release) || !SEMVER.test(String(release.version ?? ''))) throw new Error('Invalid Tauri release version')
  if (release.title !== `${PRODUCT_NAME} ${release.version}`) throw new Error('Invalid Tauri release title')
  if (release.releaseUrl !== `https://github.com/${REPOSITORY}/releases/tag/next-tauri-v${release.version}`) {
    throw new Error('Invalid Tauri release URL')
  }
  if (release.updaterManifestUrl !== UPDATE_MANIFEST_URL) throw new Error('Invalid Tauri updater manifest URL')
  if (!Array.isArray(release.assets) || release.assets.length < 6) throw new Error('Tauri release assets are incomplete')
}

function validateVersionAndTag(version, tag) {
  if (!SEMVER.test(version)) throw new Error(`Invalid semantic version: ${version}`)
  const expected = `next-tauri-v${version}`
  if (tag !== expected) throw new Error(`Tauri release tag must be ${expected}, got ${tag}`)
}

async function readReleaseNotes(notesPath, version) {
  const contents = (await readFile(notesPath, 'utf8')).trim()
  const [heading, ...body] = contents.split(/\r?\n/)
  if (heading !== `# ${PRODUCT_NAME} ${version}`) {
    throw new Error(`Release notes must start with "# ${PRODUCT_NAME} ${version}"`)
  }
  const notes = body.join('\n').trim()
  if (!notes || notes.length > 64 * 1024) throw new Error('Release notes are empty or too large')
  return notes
}

async function findArtifactDirectory(root, target) {
  const entries = await readdir(root, { withFileTypes: true })
  const matches = entries.filter((entry) => entry.isDirectory() && entry.name.endsWith(target))
  if (matches.length !== 1) {
    throw new Error(`Expected one ${target} artifact directory, found ${matches.length}`)
  }
  return join(root, matches[0].name)
}

async function walkFiles(directory) {
  const files = []
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const entryPath = join(directory, entry.name)
    if (entry.isDirectory()) files.push(...await walkFiles(entryPath))
    else if (entry.isFile()) files.push(entryPath)
  }
  return files.sort()
}

function findOne(files, predicate, label, optional = false) {
  const matches = files.filter((file) => predicate(basename(file), file))
  if (matches.length === 0 && optional) return undefined
  if (matches.length !== 1) throw new Error(`Expected one ${label} file, found ${matches.length}`)
  return matches[0]
}

async function copyOnce(source, destination, copied) {
  const existing = copied.get(destination)
  if (existing && existing !== source) throw new Error(`Two sources resolve to ${destination}`)
  if (!existing) {
    await cp(source, destination)
    copied.set(destination, source)
  }
}

function releaseFileName(version, suffix) {
  return `MooTool-Next-Tauri-${version}-${suffix}`
}

function releaseAssetUrl(tag, fileName) {
  return `https://github.com/${REPOSITORY}/releases/download/${tag}/${fileName}`
}

function validateSignature(signature, source) {
  if (signature.length < 64 || signature.length > 16 * 1024 || !/^[A-Za-z0-9+/=]+$/.test(signature)) {
    throw new Error(`Invalid updater signature: ${relative(process.cwd(), source)}`)
  }
  return signature
}

async function fileSha512(path) {
  return createHash('sha512').update(await readFile(path)).digest('base64')
}

function compareAssets(left, right) {
  return `${left.platform}:${left.architecture}:${left.priority}:${left.packageType}`
    .localeCompare(`${right.platform}:${right.architecture}:${right.priority}:${right.packageType}`)
}

function compareVersions(left, right) {
  const a = parseVersion(left)
  const b = parseVersion(right)
  for (const key of ['major', 'minor', 'patch']) {
    if (a[key] !== b[key]) return a[key] - b[key]
  }
  if (!a.prerelease.length && b.prerelease.length) return 1
  if (a.prerelease.length && !b.prerelease.length) return -1
  const length = Math.max(a.prerelease.length, b.prerelease.length)
  for (let index = 0; index < length; index += 1) {
    const leftPart = a.prerelease[index]
    const rightPart = b.prerelease[index]
    if (leftPart === undefined) return -1
    if (rightPart === undefined) return 1
    if (leftPart === rightPart) continue
    const leftNumber = /^\d+$/.test(leftPart) ? Number(leftPart) : null
    const rightNumber = /^\d+$/.test(rightPart) ? Number(rightPart) : null
    if (leftNumber !== null && rightNumber !== null) return leftNumber - rightNumber
    if (leftNumber !== null) return -1
    if (rightNumber !== null) return 1
    return leftPart.localeCompare(rightPart)
  }
  return 0
}

function parseVersion(value) {
  const match = SEMVER.exec(String(value))
  if (!match) throw new Error(`Invalid semantic version in product registry: ${value}`)
  return {
    major: Number(match[1]),
    minor: Number(match[2]),
    patch: Number(match[3]),
    prerelease: match[4]?.split('.') ?? []
  }
}

function isRecord(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

async function writeJson(path, value) {
  await writeFile(path, `${JSON.stringify(value, null, 2)}\n`, 'utf8')
}

async function main(argv) {
  const [command, ...rest] = argv
  const options = parseOptions(rest)
  if (command === 'prepare') {
    await prepareReleaseAssets({
      artifactsDirectory: required(options, 'artifacts'),
      outputDirectory: required(options, 'output'),
      version: required(options, 'version'),
      tag: required(options, 'tag'),
      notesPath: required(options, 'notes'),
      publishedAt: options['pub-date']
    })
    return
  }
  if (command === 'update-registry') {
    await updateProductManifest({
      manifestPath: required(options, 'manifest'),
      releasePath: required(options, 'release')
    })
    return
  }
  if (command === 'validate-promotion') {
    await validatePromotion({
      latestPath: required(options, 'latest'),
      releasePath: required(options, 'release'),
      tag: required(options, 'tag')
    })
    return
  }
  throw new Error('Usage: prepare-release-assets.mjs <prepare|validate-promotion|update-registry> [options]')
}

function parseOptions(values) {
  const options = {}
  for (let index = 0; index < values.length; index += 2) {
    const key = values[index]
    const value = values[index + 1]
    if (!key?.startsWith('--') || value === undefined) throw new Error(`Invalid option: ${key ?? ''}`)
    options[key.slice(2)] = value
  }
  return options
}

function required(options, key) {
  if (!options[key]) throw new Error(`Missing --${key}`)
  return options[key]
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  main(process.argv.slice(2)).catch((error) => {
    console.error(error instanceof Error ? error.message : error)
    process.exitCode = 1
  })
}
