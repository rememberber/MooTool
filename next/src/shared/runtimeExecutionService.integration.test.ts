// @vitest-environment node
import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { RuntimeExecutionService } from '../../electron/main/runtimeExecutionService'

const directories: string[] = []

afterEach(async () => {
  await Promise.all(directories.splice(0).map((directory) => rm(directory, { recursive: true, force: true })))
})

async function service(): Promise<RuntimeExecutionService> {
  const directory = await mkdtemp(join(tmpdir(), 'mootool-runtime-test-'))
  directories.push(directory)
  return new RuntimeExecutionService(directory)
}

describe('RuntimeExecutionService', () => {
  it('streams and returns Node.js output in an isolated directory', async () => {
    const runtime = await service()
    const streamed: string[] = []
    const result = await runtime.run(
      {
        requestId: 'node-test-001',
        runtime: 'node',
        code: 'console.log(process.cwd().includes("mootool-runtime-test"), 6 * 7, process.argv.slice(2).join("|"))',
        arguments: ['Moo Tool', '--flag']
      },
      { node: process.execPath },
      (event) => streamed.push(event.text)
    )
    expect(result.exitCode).toBe(0)
    expect(result.stdout).toContain('true 42 Moo Tool|--flag')
    expect(streamed.join('')).toContain('42')
    expect(result.cancelled).toBe(false)
  }, 15_000)

  it('cancels a running process tree', async () => {
    const runtime = await service()
    const execution = runtime.run(
      { requestId: 'node-test-002', runtime: 'node', code: 'setInterval(() => console.log("tick"), 50)', timeoutMs: 10_000 },
      { node: process.execPath }
    )
    await new Promise((resolve) => setTimeout(resolve, 180))
    expect(runtime.cancel('node-test-002')).toBe(true)
    const result = await execution
    expect(result.cancelled).toBe(true)
    expect(result.durationMs).toBeLessThan(3000)
  }, 15_000)

  it('rejects oversized and malformed requests', async () => {
    const runtime = await service()
    await expect(runtime.run({ requestId: '../bad', runtime: 'node', code: '' }, { node: process.execPath })).rejects.toThrow('request id')
  })
})
