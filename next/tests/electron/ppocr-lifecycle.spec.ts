import { _electron as electron, expect, test } from '@playwright/test'
import { mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { seedOcrComponent } from './ocrComponentFixture'

test.skip(process.platform === 'darwin' && process.arch !== 'arm64', 'ONNX Runtime has no Intel macOS binary.')
test('kills real OCR work, removes its private snapshot and can recognize again without retaining a utility process', async ({}, testInfo) => {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-ppocr-lifecycle-'))
  await seedOcrComponent(directory)
  const executablePath = process.env.MOOTOOL_OCR_EXECUTABLE
  const app = await electron.launch({ executablePath, args: [...(executablePath ? [] : ['.']), `--user-data-dir=${directory}`], cwd: process.cwd(), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0', MOOTOOL_OCR_BACKEND: 'pp-ocrv6-small' } })
  try {
    const page = await app.firstWindow()
    const status = await page.evaluate(() => window.mootool.getOcrStatus())
    expect(status).toMatchObject({ available: true, engine: 'pp-ocrv6-small' })
    const temporaryRoot = await app.evaluate(({ app }) => app.getPath('temp'))
    const snapshots = () => readdir(temporaryRoot).then(names => names.filter(name => name.startsWith('mootool-ocr-')))
    const before = new Set(await snapshots())
    const dataUrl = `data:image/png;base64,${(await readFile('doc/ocr-evaluation/retina-12x2.png')).toString('base64')}`
    await page.evaluate(dataUrl => {
      const state = window as typeof window & { __ocrPending?: Promise<unknown> }
      state.__ocrPending = window.mootool.recognizeImageText({ requestId: 'real-cancel', language: 'auto', source: { name: 'retina.png', dataUrl } }).catch(error => ({ error: String(error) }))
    }, dataUrl)
    const metrics = () => app.evaluate(({ app }) => app.getAppMetrics().filter(metric => metric.name === 'MooTool Offline OCR'))
    await expect.poll(metrics).not.toHaveLength(0)
    expect(await page.evaluate(() => window.mootool.cancelImageTextRecognition('real-cancel'))).toBe(true)
    const cancelled = await page.evaluate(() => (window as typeof window & { __ocrPending?: Promise<unknown> }).__ocrPending)
    expect(cancelled).toMatchObject({ error: expect.stringContaining('image.ocr.error.cancelled') })
    await expect.poll(metrics).toHaveLength(0)
    expect((await snapshots()).filter(name => !before.has(name))).toEqual([])
    let finished = false, peakWorkingSetKiB = 0
    const pending = page.evaluate(dataUrl => window.mootool.recognizeImageText({ requestId: 'real-retry', language: 'auto', source: { name: 'retina.png', dataUrl } }), dataUrl)
    void pending.then(() => { finished = true }, () => { finished = true })
    while (!finished) {
      for (const metric of await metrics()) peakWorkingSetKiB = Math.max(peakWorkingSetKiB, metric.memory.workingSetSize)
      await new Promise(resolve => setTimeout(resolve, 20))
    }
    const result = await pending
    expect(result.text).toContain('请先选择文件')
    expect(result).toMatchObject({ width: 2000, height: 480, engine: 'pp-ocrv6-small' })
    await expect.poll(metrics).toHaveLength(0)
    expect((await snapshots()).filter(name => !before.has(name))).toEqual([])
    const memoryPath = testInfo.outputPath('ppocr-memory.json')
    await writeFile(memoryPath, JSON.stringify({ platform: process.platform, arch: process.arch, peakSampledWorkingSetBytes: peakWorkingSetKiB * 1024, image: 'retina-12x2', durationMs: result.durationMs, note: '20 ms sampling; not an exact process peak or worst-case memory bound.' }, null, 2) + '\n')
    await testInfo.attach('ppocr-memory-sample', { path: memoryPath, contentType: 'application/json' })
  } finally { await app.close(); await rm(directory, { recursive: true, force: true }) }
})
