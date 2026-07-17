// @vitest-environment node
import { createHash } from 'node:crypto'
import { mkdtemp, readFile, readdir, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it, vi } from 'vitest'
import type { UpdateDownload } from '../../src/shared/contracts/update'
import { downloadUpdateFile } from './updateDownloader'

const temporaryDirectories: string[] = []

afterEach(async () => {
  await Promise.all(temporaryDirectories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

describe('downloadUpdateFile', () => {
  it('downloads a DMG atomically and verifies its size and SHA-512', async () => {
    const directory = await temporaryDirectory()
    const payload = new TextEncoder().encode('signed release payload')
    const download = updateDownload(payload)
    const onProgress = vi.fn()

    const destination = await downloadUpdateFile(download, directory, onProgress, async () => new Response(payload))

    expect(destination).toBe(join(directory, download.fileName))
    expect(await readFile(destination)).toEqual(Buffer.from(payload))
    expect(onProgress).toHaveBeenLastCalledWith({ percent: 100, transferred: payload.byteLength, total: payload.byteLength })
    expect(await readdir(directory)).toEqual([download.fileName])
  })

  it('removes the partial file when checksum verification fails', async () => {
    const directory = await temporaryDirectory()
    const payload = new TextEncoder().encode('corrupted payload')
    const download = { ...updateDownload(payload), sha512: Buffer.alloc(64).toString('base64') }

    await expect(downloadUpdateFile(download, directory, () => undefined, async () => new Response(payload)))
      .rejects.toThrow('checksum mismatch')
    expect(await readdir(directory)).toEqual([])
  })
})

async function temporaryDirectory(): Promise<string> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-update-test-'))
  temporaryDirectories.push(directory)
  return directory
}

function updateDownload(payload: Uint8Array): UpdateDownload {
  return {
    fileName: 'MooTool-Next-Electron-1.1.0-mac-arm64.dmg',
    packageType: 'dmg',
    url: 'https://downloads.example.test/MooTool.dmg',
    sha512: createHash('sha512').update(payload).digest('base64'),
    size: payload.byteLength
  }
}
