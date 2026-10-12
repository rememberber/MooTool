import { mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { verifyOcrResources } from './ocrResources'

describe('offline OCR resource integrity', () => {
  let directory: string
  beforeEach(async () => { directory = await mkdtemp(join(tmpdir(), 'mootool-ocr-resource-test-')) })
  afterEach(async () => { await rm(directory, { recursive: true, force: true }) })
  const manifest = (files: unknown[] = []) => ({ schema: 1, target: 'linux-x64', engine: 'pp-ocrv6-small', runtimeVersion: '1.30.0', modelVersion: 'RapidOCR-v3.10.0', files })
  async function write(value: unknown) { await writeFile(join(directory, 'manifest.json'), JSON.stringify(value)) }
  it('rejects missing, incompatible and incomplete bundles', async () => {
    await expect(verifyOcrResources(directory, 'linux', 'x64')).rejects.toThrow()
    await write(manifest()); await expect(verifyOcrResources(directory, 'win32', 'x64')).rejects.toThrow('Invalid OCR bundle')
    await expect(verifyOcrResources(directory, 'linux', 'x64')).rejects.toThrow('Incomplete OCR bundle')
  })
  it('rejects path traversal, model substitution and corrupted resources', async () => {
    for (const path of ['../outside', '/private/file', 'models/PP-OCRv6_rec_small.onnx']) {
      await write(manifest([{ path, bytes: 1, sha256: 'a'.repeat(64) }]))
      await expect(verifyOcrResources(directory, 'linux', 'x64')).rejects.toThrow('Invalid OCR bundle')
    }
    await writeFile(join(directory, 'characters.json'), 'x')
    await write(manifest([{ path: 'characters.json', bytes: 1, sha256: 'a'.repeat(64) }]))
    await expect(verifyOcrResources(directory, 'linux', 'x64')).rejects.toThrow('Corrupt OCR bundle')
  })
})
