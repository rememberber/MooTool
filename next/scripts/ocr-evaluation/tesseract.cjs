// Pass the temporary dependency prefix and fixture directory; no app dependencies change.
const fs = require('node:fs/promises')
const path = require('node:path')
const zlib = require('node:zlib')
const { performance } = require('node:perf_hooks')

;(async () => {
  const prefix = path.resolve(process.argv[2])
  const fixtures = path.resolve(process.argv[3])
  const { createWorker, OEM, PSM } = require(path.join(prefix, 'node_modules/tesseract.js'))
  const languages = process.argv[4] || 'chi_sim+chi_tra+eng'
  const modelDir = path.join(prefix, 'models'); await fs.mkdir(modelDir, { recursive: true })
  const sizes = []
  for (const language of languages.split('+')) {
    const source = path.join(prefix, 'node_modules/@tesseract.js-data', language, '4.0.0_best_int', `${language}.traineddata.gz`)
    const bytes = await fs.readFile(source)
    await fs.writeFile(path.join(modelDir, `${language}.traineddata.gz`), bytes)
    sizes.push({ name: `${language}.traineddata.gz`, compressedBytes: bytes.length, rawBytes: zlib.gunzipSync(bytes).length })
  }
  const started = performance.now()
  const worker = await createWorker(languages, OEM.LSTM_ONLY, { langPath: modelDir, cachePath: modelDir, corePath: path.join(prefix, 'node_modules/tesseract.js-core'), workerPath: path.join(prefix, 'node_modules/tesseract.js/src/worker-script/node/index.js') })
  const initMs = performance.now() - started
  await worker.setParameters({ tessedit_pageseg_mode: PSM.AUTO })
  const samples = JSON.parse(await fs.readFile(path.join(fixtures, 'samples.json'), 'utf8'))
  const runs = []
  try {
    for (let pass = 0; pass < 4; pass++) for (const sample of samples) {
      const start = performance.now()
      const { data } = await worker.recognize(path.join(fixtures, `${sample.id}.png`))
      runs.push({ id: sample.id, pass, ms: performance.now() - start, text: data.text, confidence: data.confidence })
      if (pass === 0) console.log(sample.id, runs.at(-1).ms.toFixed(1))
    }
    const filename = languages === 'chi_sim+eng' ? 'tesseract-simplified.json' : 'tesseract.json'
    await fs.writeFile(path.join(fixtures, filename), JSON.stringify({ engine: `Tesseract.js 7.0.0 (${languages})`, language: languages, parameters: { OEM: 'LSTM_ONLY', PSM: 'AUTO' }, initMs, models: sizes, rssBytes: process.memoryUsage().rss, runs }, null, 2) + '\n')
  } finally { await worker.terminate() }
})().catch(error => { console.error(error); process.exitCode = 1 })
