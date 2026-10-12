import { describe, expect, it } from 'vitest'
import { runOcrHelper, VisionOcrBackend } from './ocrBackend'

describe('native OCR process', () => {
  it('streams a bounded JSON request to an executable without a shell', async () => {
    const script = "let data='';process.stdin.on('data',chunk=>data+=chunk);process.stdin.on('end',()=>console.log(JSON.stringify({echo:JSON.parse(data)})))"
    expect(await runOcrHelper(process.execPath, ['-e', script], { path: 'name with spaces', language: 'auto' }, new AbortController().signal)).toEqual({ echo: { path: 'name with spaces', language: 'auto' } })
  })
  it('kills a timed-out helper and rejects a cancelled invocation', async () => {
    await expect(runOcrHelper(process.execPath, ['-e', 'setInterval(()=>{},1000)'], null, new AbortController().signal, 50)).rejects.toThrow('image.ocr.error.timeout')
    const controller = new AbortController()
    const running = runOcrHelper(process.execPath, ['-e', 'setInterval(()=>{},1000)'], null, controller.signal)
    controller.abort()
    await expect(running).rejects.toThrow('image.ocr.error.cancelled')
  })
  it('rejects invalid and excessive stdout rather than forwarding diagnostics', async () => {
    await expect(runOcrHelper(process.execPath, ['-e', "console.log('bad stdout')"], null, new AbortController().signal)).rejects.toThrow('image.ocr.error.failed')
    await expect(runOcrHelper(process.execPath, ['-e', "process.stdout.write('x'.repeat(3*1024*1024))"], null, new AbortController().signal)).rejects.toThrow('image.ocr.error.tooLarge')
  })
  it('reports unsupported systems and missing helper resources before recognition', async () => {
    expect(await new VisionOcrBackend('/missing/helper', 'linux').status()).toMatchObject({ available: false, reason: 'platform' })
    expect(await new VisionOcrBackend('/missing/helper', 'darwin', 12).status()).toMatchObject({ available: false, reason: 'system' })
    expect(await new VisionOcrBackend('/missing/helper', 'darwin', 13).status()).toMatchObject({ available: false, reason: 'component' })
  })
})
