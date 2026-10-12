const { _electron } = require('@playwright/test')
const fs = require('node:fs/promises')
const os = require('node:os')
const path = require('node:path')

;(async () => {
  const runtime = path.resolve(process.argv[2])
  const model = path.resolve(process.argv[3])
  const output = path.resolve(process.argv[4])
  const directory = await fs.mkdtemp(path.join(os.tmpdir(), 'mootool-onnx-probe-'))
  const app = await _electron.launch({ args: ['.', `--user-data-dir=${directory}`], cwd: path.resolve(__dirname, '../..'), env: { ...process.env, NODE_ENV: 'test', MOOTOOL_TOOL_VIEWS: '0' } })
  try {
    const result = await app.evaluate(async ({ app }, { runtime, model }) => {
      const requireLocal = process.getBuiltinModule('module').createRequire(pathForPackage(app.getAppPath()))
      function pathForPackage(root) { return root + '/package.json' }
      const ort = requireLocal(runtime)
      const start = performance.now()
      const session = await ort.InferenceSession.create(model, { executionProviders: ['cpu'], intraOpNumThreads: 4 })
      const loadMs = performance.now() - start
      const input = new ort.Tensor('float32', new Float32Array(3 * 48 * 320), [1, 3, 48, 320])
      const predicted = await session.run({ [session.inputNames[0]]: input })
      const shapes = Object.fromEntries(Object.entries(predicted).map(([name, tensor]) => [name, tensor.dims]))
      await session.release()
      return { electron: process.versions.electron, node: process.versions.node, arch: process.arch, platform: process.platform, onnxRuntime: ort.env.versions, loadMs, inputNames: session.inputNames, shapes, note: 'Native binding and one recognition-model inference only; not a complete JavaScript OCR pipeline.' }
    }, { runtime, model })
    await fs.writeFile(output, JSON.stringify(result, null, 2) + '\n')
    console.log(result)
  } finally { await app.close(); await fs.rm(directory, { recursive: true, force: true }) }
})().catch(error => { console.error(error); process.exitCode = 1 })
