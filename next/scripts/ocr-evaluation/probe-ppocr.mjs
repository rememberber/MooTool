import { _electron } from '@playwright/test'
import { mkdtemp, readFile, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const output = resolve(process.argv[2] ?? '/tmp/mootool-ppocr-electron-results.json')
const samples = JSON.parse(await readFile(join(root, 'doc/ocr-evaluation/samples.json'), 'utf8'))
const directory = await mkdtemp(join(tmpdir(), 'mootool-ppocr-evaluation-'))
const app = await _electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: root, env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0', MOOTOOL_OCR_BACKEND: 'pp-ocrv6-small' } })
function distance(a, b) {
  let previous = Array.from({ length: b.length + 1 }, (_, i) => i)
  for (let i = 0; i < a.length; i++) { const current = [i + 1]; for (let j = 0; j < b.length; j++) current.push(Math.min(current[j] + 1, previous[j + 1] + 1, previous[j] + (a[i] === b[j] ? 0 : 1))); previous = current }
  return previous[b.length]
}
const normalize = (text, han) => Array.from(text).filter(character => han ? /\p{Script=Han}/u.test(character) : !/\s/u.test(character))
try {
  const page = await app.firstWindow()
  const status = await page.evaluate(() => window.mootool.getOcrStatus())
  if (!status.available || status.engine !== 'pp-ocrv6-small') throw new Error(`PP-OCR unavailable: ${JSON.stringify(status)}`)
  const runs = []
  for (let pass = 0; pass < 2; pass++) for (const sample of samples) {
    const dataUrl = `data:image/png;base64,${(await readFile(join(root, 'doc/ocr-evaluation', `${sample.id}.png`))).toString('base64')}`
    const result = await page.evaluate(({ id, dataUrl }) => window.mootool.recognizeImageText({ requestId: crypto.randomUUID(), language: 'auto', source: { name: `${id}.png`, dataUrl } }), { id: sample.id, dataUrl })
    runs.push({ id: sample.id, pass, ms: result.durationMs, text: result.text, lines: result.lines })
    console.log(sample.id, pass, `${result.durationMs} ms`, `${result.lines.length} lines`)
  }
  const scores = { han: { edits: 0, characters: 0 }, all: { edits: 0, characters: 0 } }
  for (const sample of samples) for (const key of ['han', 'all']) {
    const expected = normalize(sample.expected, key === 'han'), actual = normalize(runs.find(run => run.id === sample.id && run.pass === 0).text, key === 'han')
    scores[key].edits += distance(expected, actual); scores[key].characters += expected.length
  }
  const environment = await app.evaluate(() => ({ electron: process.versions.electron, node: process.versions.node, platform: process.platform, arch: process.arch }))
  await writeFile(output, JSON.stringify({ engine: 'PP-OCRv6 small Electron CPU', environment, status, lifecycle: 'Fresh isolated process per image; duration includes checksums, runtime/model initialization, inference, cleanup and process exit.', scores, runs }, null, 2) + '\n')
  console.log(scores)
} finally { await app.close(); await rm(directory, { recursive: true, force: true }) }
