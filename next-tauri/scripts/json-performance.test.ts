import { mkdir, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'
import { performance } from 'node:perf_hooks'
import { expect, test } from 'vitest'

import { formatJson, validateJson } from '../src/features/json/jsonTools'

const reportPath = process.env.MOOTOOL_JSON_PERFORMANCE_REPORT
const targetBytes = 10 * 1024 * 1024

test.runIf(Boolean(reportPath))('records the 10 MiB JSON validation and formatting baseline', async () => {
  const envelopeBytes = Buffer.byteLength('{"payload":""}')
  const input = JSON.stringify({ payload: 'x'.repeat(targetBytes - envelopeBytes) })
  expect(Buffer.byteLength(input)).toBe(targetBytes)

  const validationStartedAt = performance.now()
  const validation = validateJson(input)
  const validationMs = performance.now() - validationStartedAt
  expect(validation.kind).toBe('valid')

  const formatStartedAt = performance.now()
  const output = formatJson(input, { indent: 2, sortKeys: false })
  const formatMs = performance.now() - formatStartedAt
  expect(output.startsWith('{\n')).toBe(true)

  const report = {
    inputBytes: Buffer.byteLength(input),
    outputBytes: Buffer.byteLength(output),
    validationMs: rounded(validationMs),
    formatMs: rounded(formatMs)
  }
  await mkdir(dirname(reportPath!), { recursive: true })
  await writeFile(reportPath!, `${JSON.stringify(report, null, 2)}\n`, 'utf8')
})

function rounded(value: number) {
  return Math.round(value * 1_000) / 1_000
}
