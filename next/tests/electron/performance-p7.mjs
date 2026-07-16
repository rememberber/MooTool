import { _electron as electron } from '@playwright/test'
import { mkdir, mkdtemp, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { performance } from 'node:perf_hooks'

const root = process.cwd()
const executablePath = join(root, 'dist/mac-arm64/MooTool.app/Contents/MacOS/MooTool')
const userData = await mkdtemp(join(tmpdir(), 'mootool-p7-performance-'))
const startedAt = performance.now()
const app = await electron.launch({ executablePath, args: [`--user-data-dir=${userData}`, '--enable-precise-memory-info'], env: { ...process.env, NODE_ENV: 'test' } })

try {
  const page = await app.firstWindow()
  await page.waitForLoadState('domcontentloaded')
  await page.locator('.tool-button').first().waitFor()
  const startupMs = performance.now() - startedAt
  await page.evaluate(() => window.mootool.updateSettings({ general: { autoCheckUpdates: false } }))
  const baselineMemory = await memorySnapshot(app, page)

  await page.locator('.tool-button').filter({ hasText: 'JSON' }).click()
  const largeValue = Object.fromEntries(Array.from({ length: 55_000 }, (_, index) => [`key_${String(index).padStart(5, '0')}`, `MooTool-${index}-${'x'.repeat(28)}`]))
  const compactJson = JSON.stringify(largeValue)
  const jsonStartedAt = performance.now()
  await page.locator('.json-editor .cm-content').fill(compactJson)
  await page.locator('.editor-toolbar').getByRole('button', { name: '格式化', exact: true }).click()
  await page.locator('.status-pill--valid').waitFor({ timeout: 60_000 })
  const jsonFormatMs = performance.now() - jsonStartedAt
  const afterJsonMemory = await memorySnapshot(app, page)

  const noteContent = 'MooTool large note line\n'.repeat(230_000)
  const noteStartedAt = performance.now()
  await page.evaluate(async ({ content }) => {
    const now = new Date().toISOString()
    await window.mootool.saveQuickNote({
      relativePath: 'P7 Large Note.txt',
      content,
      metadata: {
        title: 'P7 Large Note',
        style: '',
        syntax: 'text/plain',
        fontName: '',
        fontSize: 14,
        color: 'default',
        lineWrap: true,
        createdAt: now,
        modifiedAt: now
      }
    })
    const saved = await window.mootool.readQuickNote('P7 Large Note.txt')
    if (saved.content.length !== content.length) throw new Error('Large Quick Note content changed during persistence')
    await window.mootool.deleteQuickNoteEntry('P7 Large Note.txt')
  }, { content: noteContent })
  const quickNoteRoundTripMs = performance.now() - noteStartedAt
  const afterQuickNoteMemory = await memorySnapshot(app, page)

  await page.locator('.tool-button').first().click()
  await page.locator('.home-panel').waitFor()
  await page.waitForTimeout(500)
  const beforeCollectionMemory = await memorySnapshot(app, page)
  const cdp = await page.context().newCDPSession(page)
  await cdp.send('HeapProfiler.collectGarbage')
  await page.waitForTimeout(1_000)
  const settledMemory = await memorySnapshot(app, page)
  const peakMemoryMb = Math.max(
    baselineMemory.totalMb,
    afterJsonMemory.totalMb,
    afterQuickNoteMemory.totalMb,
    beforeCollectionMemory.totalMb,
    settledMemory.totalMb
  )
  const peakMemoryIncreaseMb = peakMemoryMb - baselineMemory.totalMb
  const settledMemoryIncreaseMb = settledMemory.totalMb - baselineMemory.totalMb

  const result = {
    platform: process.platform,
    architecture: process.arch,
    packaged: true,
    measuredAt: new Date().toISOString(),
    startupMs: round(startupMs),
    baselineMemoryMb: baselineMemory.totalMb,
    jsonInputMb: round(Buffer.byteLength(compactJson) / 1024 / 1024),
    jsonFormatMs: round(jsonFormatMs),
    quickNoteInputMb: round(Buffer.byteLength(noteContent) / 1024 / 1024),
    quickNoteRoundTripMs: round(quickNoteRoundTripMs),
    peakMemoryMb: round(peakMemoryMb),
    peakMemoryIncreaseMb: round(peakMemoryIncreaseMb),
    settledMemoryIncreaseMb: round(settledMemoryIncreaseMb),
    memory: {
      baseline: baselineMemory,
      afterJson: afterJsonMemory,
      afterQuickNote: afterQuickNoteMemory,
      beforeCollection: beforeCollectionMemory,
      settled: settledMemory
    },
    thresholds: {
      startupMs: 8000,
      jsonFormatMs: 8000,
      quickNoteRoundTripMs: 8000,
      peakMemoryIncreaseMb: 400,
      settledMemoryIncreaseMb: 200
    }
  }
  await mkdir(join(root, 'doc/parity'), { recursive: true })
  await writeFile(join(root, 'doc/parity/p7-performance-results.json'), `${JSON.stringify(result, null, 2)}\n`)
  console.log(JSON.stringify(result, null, 2))
  if (result.startupMs > result.thresholds.startupMs
    || result.jsonFormatMs > result.thresholds.jsonFormatMs
    || result.quickNoteRoundTripMs > result.thresholds.quickNoteRoundTripMs
    || result.peakMemoryIncreaseMb > result.thresholds.peakMemoryIncreaseMb
    || result.settledMemoryIncreaseMb > result.thresholds.settledMemoryIncreaseMb) {
    throw new Error(`P7 performance threshold exceeded: ${JSON.stringify(result)}`)
  }
} finally {
  await app.close()
  await rm(userData, { recursive: true, force: true })
}

async function memorySnapshot(app, page) {
  const processes = await app.evaluate(({ app: electronApp }) => electronApp.getAppMetrics().map((metric) => ({
    pid: metric.pid,
    type: metric.type,
    serviceName: metric.serviceName ?? '',
    workingSetMb: Math.round(metric.memory.workingSetSize / 1024 * 100) / 100,
    peakWorkingSetMb: Math.round(metric.memory.peakWorkingSetSize / 1024 * 100) / 100
  })))
  const rendererHeap = await page.evaluate(() => {
    const memory = performance.memory
    return memory
      ? {
          usedMb: Math.round(memory.usedJSHeapSize / 1024 / 1024 * 100) / 100,
          totalMb: Math.round(memory.totalJSHeapSize / 1024 / 1024 * 100) / 100
        }
      : null
  })
  return {
    totalMb: round(processes.reduce((sum, process) => sum + process.workingSetMb, 0)),
    rendererHeap,
    processes: processes.sort((left, right) => right.workingSetMb - left.workingSetMb)
  }
}

function round(value) {
  return Math.round(value * 100) / 100
}
