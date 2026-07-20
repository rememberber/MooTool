import { createHash, timingSafeEqual } from 'node:crypto'
import { mkdir, open, rename, rm } from 'node:fs/promises'
import { join } from 'node:path'
import type { UpdateDownload } from '../../src/shared/contracts/update'

type DownloadResponse = Pick<Response, 'ok' | 'status' | 'body'>
type DownloadFetch = (url: string, init: RequestInit) => Promise<DownloadResponse>

export type DownloadProgress = {
  percent: number
  transferred: number
  total: number
}

export async function downloadUpdateFile(
  download: UpdateDownload,
  directory: string,
  onProgress: (progress: DownloadProgress) => void,
  fetcher: DownloadFetch = fetch
): Promise<string> {
  await mkdir(directory, { recursive: true })
  const destination = join(directory, download.fileName)
  const temporary = join(directory, `.${download.fileName}.${process.pid}.download`)
  const response = await fetcher(download.url, {
    headers: { Accept: 'application/octet-stream', 'User-Agent': 'MooTool-Next' },
    redirect: 'follow',
    signal: AbortSignal.timeout(30 * 60 * 1_000)
  })
  if (!response.ok) throw new Error(`Update download returned HTTP ${response.status}`)
  if (!response.body) throw new Error('Update download returned an empty response')

  const file = await open(temporary, 'w', 0o600)
  const hash = createHash('sha512')
  const reader = response.body.getReader()
  let transferred = 0
  let closed = false

  try {
    onProgress({ percent: 0, transferred, total: download.size })
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      await writeAll(file, value)
      hash.update(value)
      transferred += value.byteLength
      onProgress({
        percent: Number(Math.min(100, transferred / download.size * 100).toFixed(1)),
        transferred,
        total: download.size
      })
    }
    if (transferred !== download.size) {
      throw new Error(`Update download size mismatch: expected ${download.size}, received ${transferred}`)
    }
    const actualChecksum = hash.digest()
    const expectedChecksum = Buffer.from(download.sha512, 'base64')
    if (expectedChecksum.length !== actualChecksum.length || !timingSafeEqual(actualChecksum, expectedChecksum)) {
      throw new Error('Update download checksum mismatch')
    }
    await file.sync()
    await file.close()
    closed = true
    await rename(temporary, destination)
    return destination
  } catch (error) {
    if (!closed) await file.close().catch(() => undefined)
    await rm(temporary, { force: true }).catch(() => undefined)
    throw error
  } finally {
    reader.releaseLock()
  }
}

async function writeAll(file: Awaited<ReturnType<typeof open>>, value: Uint8Array): Promise<void> {
  let offset = 0
  while (offset < value.byteLength) {
    const { bytesWritten } = await file.write(value, offset, value.byteLength - offset)
    if (bytesWritten <= 0) throw new Error('Could not write the downloaded update')
    offset += bytesWritten
  }
}
