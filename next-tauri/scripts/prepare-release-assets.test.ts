import { mkdtemp, mkdir, readFile, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { afterEach, describe, expect, it } from 'vitest'
import { prepareReleaseAssets, updateProductManifest, UPDATE_MANIFEST_URL } from './prepare-release-assets.mjs'

const temporaryDirectories: string[] = []

afterEach(async () => {
  const { rm } = await import('node:fs/promises')
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('Tauri release asset preparation', () => {
  it('creates a complete signed updater manifest and isolated registry release', async () => {
    const root = await temporaryRoot()
    const artifacts = join(root, 'artifacts')
    const output = join(root, 'output')
    const notes = join(root, 'notes.md')
    await createArtifacts(artifacts)
    await writeFile(notes, '# MooTool Next Tauri 1.2.3\n\nIndependent Tauri release.\n')

    const result = await prepareReleaseAssets({
      artifactsDirectory: artifacts,
      outputDirectory: output,
      version: '1.2.3',
      tag: 'next-tauri-v1.2.3',
      notesPath: notes,
      publishedAt: '2026-08-16T00:00:00.000Z'
    })

    expect(Object.keys(result.updaterManifest.platforms).sort()).toEqual([
      'darwin-aarch64',
      'darwin-x86_64',
      'linux-x86_64',
      'windows-x86_64'
    ])
    expect(result.updaterManifest.platforms['windows-x86_64'].url).toContain('next-tauri-v1.2.3')
    expect(result.registryRelease.assets).toHaveLength(7)
    expect(result.registryRelease.assets.every((asset) => asset.url.includes('next-tauri-v1.2.3'))).toBe(true)
    expect(JSON.parse(await readFile(join(output, 'latest.json'), 'utf8')).version).toBe('1.2.3')
  })

  it('updates only products.next-tauri and preserves other product nodes', async () => {
    const root = await temporaryRoot()
    const manifestPath = join(root, 'update-manifest.json')
    const releasePath = join(root, 'release.json')
    const electron = { displayName: 'MooTool Next Electron', status: 'active', releases: [{ version: '9.9.9' }] }
    await writeFile(manifestPath, JSON.stringify({
      schemaVersion: 1,
      products: {
        java: { status: 'legacy' },
        'next-electron': electron,
        'next-tauri': { displayName: 'MooTool Next Tauri', status: 'planned', updaterManifestUrl: UPDATE_MANIFEST_URL, releases: [] }
      }
    }))
    await writeFile(releasePath, JSON.stringify({
      version: '1.0.0',
      title: 'MooTool Next Tauri 1.0.0',
      notes: 'Tauri only',
      prerelease: true,
      releaseUrl: 'https://github.com/rememberber/MooTool/releases/tag/next-tauri-v1.0.0',
      updaterManifestUrl: UPDATE_MANIFEST_URL,
      assets: Array.from({ length: 6 }, (_, index) => ({ fileName: `tauri-${index}` }))
    }))

    await updateProductManifest({ manifestPath, releasePath })
    const updated = JSON.parse(await readFile(manifestPath, 'utf8'))
    expect(updated.products['next-electron']).toEqual(electron)
    expect(updated.products.java).toEqual({ status: 'legacy' })
    expect(updated.products['next-tauri'].status).toBe('active')
    expect(updated.products['next-tauri'].releases).toHaveLength(1)
  })

  it('rejects a missing signature before publishing metadata', async () => {
    const root = await temporaryRoot()
    const artifacts = join(root, 'artifacts')
    await createArtifacts(artifacts, { omitLinuxSignature: true })
    const notes = join(root, 'notes.md')
    await writeFile(notes, '# MooTool Next Tauri 1.0.0\n\nTauri only.\n')

    await expect(prepareReleaseAssets({
      artifactsDirectory: artifacts,
      outputDirectory: join(root, 'output'),
      version: '1.0.0',
      tag: 'next-tauri-v1.0.0',
      notesPath: notes
    })).rejects.toThrow('linux-x86_64 updater signature')
  })

  it('refuses to move the independent updater channel to an older version', async () => {
    const root = await temporaryRoot()
    const manifestPath = join(root, 'update-manifest.json')
    const releasePath = join(root, 'release.json')
    await writeFile(manifestPath, JSON.stringify({
      schemaVersion: 1,
      products: {
        'next-tauri': {
          displayName: 'MooTool Next Tauri',
          status: 'active',
          updaterManifestUrl: UPDATE_MANIFEST_URL,
          releases: [{ version: '2.0.0', title: 'existing release' }]
        }
      }
    }))
    await writeFile(releasePath, JSON.stringify(registryRelease('1.9.9')))

    await expect(updateProductManifest({ manifestPath, releasePath }))
      .rejects.toThrow('Refusing to downgrade the Tauri update channel from 2.0.0 to 1.9.9')
    expect(JSON.parse(await readFile(manifestPath, 'utf8')).products['next-tauri'].releases)
      .toEqual([{ version: '2.0.0', title: 'existing release' }])
  })
})

function registryRelease(version: string): Record<string, unknown> {
  return {
    version,
    title: `MooTool Next Tauri ${version}`,
    notes: 'Tauri only',
    prerelease: true,
    releaseUrl: `https://github.com/rememberber/MooTool/releases/tag/next-tauri-v${version}`,
    updaterManifestUrl: UPDATE_MANIFEST_URL,
    assets: Array.from({ length: 6 }, (_, index) => ({ fileName: `tauri-${index}` }))
  }
}

async function temporaryRoot(): Promise<string> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-tauri-release-'))
  temporaryDirectories.push(directory)
  return directory
}

async function createArtifacts(root: string, options: { omitLinuxSignature?: boolean } = {}): Promise<void> {
  const signature = 'A'.repeat(160)
  const targets: Record<string, string[]> = {
    'darwin-x86_64': ['MooTool Next Tauri.dmg', 'MooTool Next Tauri.app.tar.gz'],
    'darwin-aarch64': ['MooTool Next Tauri.dmg', 'MooTool Next Tauri.app.tar.gz'],
    'windows-x86_64': ['MooTool_Next_Tauri_x64-setup.exe', 'MooTool_Next_Tauri_x64.msi'],
    'linux-x86_64': ['MooTool_Next_Tauri.AppImage', 'MooTool_Next_Tauri.deb', 'MooTool_Next_Tauri.rpm']
  }
  for (const [target, files] of Object.entries(targets)) {
    const directory = join(root, `MooTool-Next-Tauri-${target}`, 'bundle')
    await mkdir(directory, { recursive: true })
    for (const file of files) await writeFile(join(directory, file), `payload:${target}:${file}`)
    const updater = target.startsWith('darwin')
      ? files.find((file) => file.endsWith('.app.tar.gz'))
      : target.startsWith('windows')
        ? files.find((file) => file.endsWith('-setup.exe'))
        : files.find((file) => file.endsWith('.AppImage'))
    if (!(options.omitLinuxSignature && target === 'linux-x86_64')) {
      await writeFile(join(directory, `${updater}.sig`), `${signature}\n`)
    }
  }
}
