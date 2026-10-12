import { createHash } from 'node:crypto'
import { createReadStream } from 'node:fs'
import { readFile, stat } from 'node:fs/promises'
import { join } from 'node:path'

export const ppOcrPlatforms = ['win32-x64', 'win32-arm64', 'linux-x64', 'linux-arm64', 'darwin-arm64'] as const
const modelHashes: Record<string, string> = {
  'models/PP-OCRv6_det_small.onnx': '090f04abcd9d9a7498bc4ebf677e4cb9bdce1fe4197ddb7e529f1ef44e1ff94f',
  'models/PP-OCRv6_rec_small.onnx': '6f327246b50388f3c176ae304bd95767ea6dc0c9ae92153ef8cbe210b3c14884'
}
export async function verifyOcrResources(root: string, platform = process.platform, arch = process.arch): Promise<void> {
  const manifestPath = join(root, 'manifest.json')
  if ((await stat(manifestPath)).size > 100000) throw new Error('Invalid OCR bundle')
  const manifest = JSON.parse(await readFile(manifestPath, 'utf8'))
  if (manifest.schema !== 1 || manifest.target !== `${platform}-${arch}` || manifest.engine !== 'pp-ocrv6-small' || manifest.runtimeVersion !== '1.30.0'
    || manifest.modelVersion !== 'RapidOCR-v3.10.0' || !Array.isArray(manifest.files) || manifest.files.length > 256) throw new Error('Invalid OCR bundle')
  const seen = new Set<string>(); let total = 0
  for (const entry of manifest.files) {
    if (typeof entry.path !== 'string' || entry.path.length > 200 || !/^[\w@./-]+$/.test(entry.path) || entry.path.startsWith('/')
      || entry.path.split('/').some((part: string) => !part || part === '.' || part === '..') || seen.has(entry.path)
      || !Number.isInteger(entry.bytes) || entry.bytes < 1 || entry.bytes > 128 * 1024 * 1024 || !/^[a-f0-9]{64}$/.test(entry.sha256)) throw new Error('Invalid OCR bundle')
    total += entry.bytes
    if (total > 256 * 1024 * 1024 || (modelHashes[entry.path] && modelHashes[entry.path] !== entry.sha256)) throw new Error('Invalid OCR bundle')
    seen.add(entry.path)
    const path = join(root, entry.path)
    if ((await stat(path)).size !== entry.bytes) throw new Error('Incomplete OCR bundle')
    const hash = createHash('sha256')
    for await (const chunk of createReadStream(path)) hash.update(chunk)
    if (hash.digest('hex') !== entry.sha256) throw new Error('Corrupt OCR bundle')
  }
  for (const path of [...Object.keys(modelHashes), 'characters.json', `runtime/node_modules/onnxruntime-node/bin/napi-v6/${platform}/${arch}/onnxruntime_binding.node`, 'runtime/node_modules/@techstark/opencv-js/dist/opencv.js']) {
    if (!seen.has(path)) throw new Error('Incomplete OCR bundle')
  }
}
