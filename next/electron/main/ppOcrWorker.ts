import { createRequire } from 'node:module'
import { readFile } from 'node:fs/promises'
import { join } from 'node:path'
import { recognizePpOcr, type OcrRuntime } from './ppOcrPipeline'
import { verifyOcrResources } from './ocrResources'
import { ocrErrorCodes } from '../../src/shared/contracts/ocr'

// No runtime downloading or HTTP services. Prevent accidental JS network access
// in future runtime changes as well. Native CPU inference uses only local files.
const requireLocal = createRequire(join(__dirname, 'ppOcrWorker.js'))
const denyNetwork = () => { throw new Error('Offline OCR process') }
for (const name of ['node:http', 'node:https']) { const module = requireLocal(name); module.request = denyNetwork; module.get = denyNetwork }
const net = requireLocal('node:net'); net.connect = denyNetwork; net.createConnection = denyNetwork; net.Socket.prototype.connect = denyNetwork
globalThis.fetch = denyNetwork

let running = false
process.parentPort.on('message', async ({ data }: { data: { operation: string; resources: string; image?: string; language?: string } }) => {
  if (running) return
  running = true
  try {
    if (!data || !['status', 'recognize'].includes(data.operation) || typeof data.resources !== 'string') throw new Error('image.ocr.error.input')
    await verifyOcrResources(data.resources)
    const local = createRequire(join(data.resources, 'runtime/node_modules/onnxruntime-node/package.json'))
    const ort = local('onnxruntime-node') as OcrRuntime & { listSupportedBackends(): { name: string }[] }
    if (!ort.listSupportedBackends().some(backend => backend.name === 'cpu')) throw new Error('image.ocr.error.unavailable')
    const cv = local('@techstark/opencv-js')
    // The OpenCV module is a self-resolving thenable; Promise.resolve(cv) would
    // recursively assimilate it. Wait for initialization without resolving to cv.
    await new Promise<void>((resolve, reject) => {
      if (cv.Mat) resolve()
      else { cv.onRuntimeInitialized = () => resolve(); cv.onAbort = () => reject(new Error('image.ocr.error.unavailable')) }
    })
    if (data.operation === 'status') { process.parentPort.postMessage({ ready: true }); return }
    if (!data.image || !['auto', 'zh-Hans', 'zh-Hant', 'en-US', 'ja-JP'].includes(data.language ?? '')) throw new Error('image.ocr.error.input')
    const characters = JSON.parse(await readFile(join(data.resources, 'characters.json'), 'utf8'))
    if (!Array.isArray(characters) || characters.length > 30002 || characters[0] !== '' || characters.some((character: unknown) => typeof character !== 'string' || character.length > 8)) throw new Error('image.ocr.error.unavailable')
    const { PNG } = local('pngjs')
    const bytes = await readFile(data.image)
    if (bytes.length > 32 * 1024 * 1024) throw new Error('image.ocr.error.tooLarge')
    const image = PNG.sync.read(bytes, { checkCRC: true })
    const result = await recognizePpOcr({ cv, clipper: local('clipper-lib'), ort }, {
      detector: join(data.resources, 'models/PP-OCRv6_det_small.onnx'), recognizer: join(data.resources, 'models/PP-OCRv6_rec_small.onnx'), characters
    }, image)
    process.parentPort.postMessage({ result })
  } catch (error) {
    const code = ocrErrorCodes.find(code => error instanceof Error && error.message === `image.ocr.error.${code}`) ?? 'failed'
    process.parentPort.postMessage({ error: code })
  }
  // The main process kills this short-lived utility process after receiving its
  // response and waits for exit before returning, so native model memory is freed.
})
