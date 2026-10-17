import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { mkdtemp, readFile, readdir, rm, writeFile } from 'node:fs/promises'
import { join } from 'node:path'
import { tmpdir } from 'node:os'
import { PdfTaskService } from './pdfTaskService'

let directory: string
let service: PdfTaskService
beforeEach(async () => {
  directory = await mkdtemp(join(tmpdir(), 'mootool-pdf-task-test-'))
  const workerPath = join(directory, 'worker.cjs')
  await writeFile(workerPath, `
    const { parentPort, workerData: { task, directory } } = require('node:worker_threads');
    const { writeFile } = require('node:fs/promises');
    const { join } = require('node:path');
    (async () => {
      if (task.paths?.[0] === 'crash') process.exit(7);
      const stagedPath = join(directory, '0.pdf');
      await writeFile(stagedPath, 'new PDF');
      parentPort.postMessage({ type: 'progress', progress: { phase: 'processing', completed: 1, total: 1, name: directory } });
      if (task.paths?.[0] === 'wait') { setInterval(() => {}, 1000); return; }
      parentPort.postMessage({ type: 'done', result: { outputs: [task.outputPath], pageCount: 2 }, staged: [{ path: task.outputPath, stagedPath }] });
    })().catch(error => { throw error });
  `)
  service = new PdfTaskService(workerPath)
})
afterEach(async () => { service.dispose(); await rm(directory, { recursive: true, force: true }) })

describe('PDF background task lifecycle', () => {
  it('commits staged output and reports job-scoped progress', async () => {
    const outputPath = join(directory, 'output.pdf')
    await writeFile(outputPath, 'existing PDF')
    const phases: string[] = []
    const result = await service.run(1, 'job', progress => {
      expect(progress.jobId).toBe('job')
      phases.push(progress.phase)
    }, job => job.execute({ kind: 'merge', sources: [], outputPath }))
    expect(result).toEqual({ outputs: [outputPath], pageCount: 2 })
    expect(await readFile(outputPath, 'utf8')).toBe('new PDF')
    expect(phases).toContain('processing')
    expect(phases).toContain('saving')
    expect((await readdir(directory)).some(name => name.endsWith('.tmp'))).toBe(false)
  })

  it('cancels CPU work, cleans staging and preserves existing destination files', async () => {
    const outputPath = join(directory, 'output.pdf')
    await writeFile(outputPath, 'existing PDF')
    let staging = ''
    await expect(service.run(1, 'job', progress => {
      staging = progress.name
      expect(service.cancel(1, 'job')).toBe(true)
    }, job => job.execute({ kind: 'merge', sources: [], outputPath }))).rejects.toMatchObject({ name: 'AbortError' })
    expect(await readFile(outputPath, 'utf8')).toBe('existing PDF')
    await expect(readdir(staging)).rejects.toMatchObject({ code: 'ENOENT' })
  })

  it('isolates jobs by owner and identifier and rejects concurrent work for one owner', async () => {
    let started!: () => void
    const ready = new Promise<void>(resolve => { started = resolve })
    const work = service.run(1, 'first', () => started(), job => job.execute({ kind: 'inspect', paths: ['wait'] }))
    const rejection = expect(work).rejects.toMatchObject({ name: 'AbortError' })
    await ready
    await expect(service.run(1, 'second', () => {}, async () => null)).rejects.toThrow('already running')
    expect(service.cancel(2, 'first')).toBe(false)
    expect(service.cancel(1, 'second')).toBe(false)
    expect(service.cancel(1, 'first')).toBe(true)
    await rejection
    await expect(service.run(1, 'third', () => {}, async () => 'reusable')).resolves.toBe('reusable')
  })

  it('handles premature worker exit and cancellation while a native dialog is open', async () => {
    await expect(service.run(1, 'crash', () => {}, job => job.execute({ kind: 'inspect', paths: ['crash'] }))).rejects.toThrow('exited (7)')
    await expect(service.run(1, 'dialog', () => {}, async job => {
      service.cancel(1, 'dialog')
      return job.execute({ kind: 'inspect', paths: [] })
    })).rejects.toMatchObject({ name: 'AbortError' })
  })
})
