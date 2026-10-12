import { createHash } from 'node:crypto'
import { spawnSync } from 'node:child_process'
import { createReadStream } from 'node:fs'
import { access, copyFile, cp, mkdir, mkdtemp, readFile, readdir, rename, rm, writeFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const require = createRequire(join(root, 'package.json'))
const models = JSON.parse(await readFile(join(root, 'doc/ocr-evaluation/sizes.json'), 'utf8')).ppOcrModels.filter(model => model.name.endsWith('_small.onnx'))
const supported = new Set(['win32-x64', 'win32-arm64', 'linux-x64', 'linux-arm64', 'darwin-arm64'])
const digest = async path => { const hash = createHash('sha256'); for await (const chunk of createReadStream(path)) hash.update(chunk); return hash.digest('hex') }
async function exists(path) { try { await access(path); return true } catch { return false } }
async function inventory(directory, prefix = '') {
  const files = []
  for (const entry of await readdir(join(directory, prefix), { withFileTypes: true })) {
    const relative = prefix ? `${prefix}/${entry.name}` : entry.name
    if (entry.isDirectory()) files.push(...await inventory(directory, relative))
    else { const bytes = (await readFile(join(directory, relative))).length; files.push({ path: relative, bytes, sha256: await digest(join(directory, relative)) }) }
  }
  return files.sort((a, b) => a.path.localeCompare(b.path))
}
export async function prepareOcrResources(platform = process.platform, arch = process.arch) {
  const target = `${platform}-${arch}`
  if (!supported.has(target)) throw new Error(`No offline OCR runtime for ${target}`)
  const cache = join(root, 'out/ocr-cache'), install = join(cache, 'runtime-install'), output = join(root, 'out/ocr', target)
  await mkdir(install, { recursive: true }); await mkdir(join(cache, 'models'), { recursive: true }); await mkdir(dirname(output), { recursive: true })
  const lock = await readFile(join(root, 'scripts/ocr-runtime/package-lock.json'))
  const lockHash = createHash('sha256').update(lock).digest('hex')
  const marker = join(install, 'installed-lock.sha256')
  let installedHash = ''
  try { installedHash = await readFile(marker, 'utf8') } catch { /* Fresh cache. */ }
  if (installedHash !== lockHash || !await exists(join(install, 'node_modules/onnxruntime-node/package.json'))) {
    await copyFile(join(root, 'scripts/ocr-runtime/package.json'), join(install, 'package.json'))
    await writeFile(join(install, 'package-lock.json'), lock)
    const args = ['ci', '--ignore-scripts', '--no-audit', '--no-fund']
    const npmCli = process.env.npm_execpath
    const result = npmCli ? spawnSync(process.execPath, [npmCli, ...args], { cwd: install, stdio: 'inherit' })
      : spawnSync(platform === 'win32' && process.platform === 'win32' ? 'npm.cmd' : 'npm', args, { cwd: install, stdio: 'inherit', shell: process.platform === 'win32' })
    if (result.error || result.status !== 0) throw result.error ?? new Error('OCR runtime installation failed')
    await writeFile(marker, lockHash)
  }
  const temporary = await mkdtemp(join(dirname(output), `${target}-prepare-`))
  try {
    const modules = join(install, 'node_modules')
    async function copyPackage(name, paths) {
      // Electron Builder always excludes a root node_modules in extraResources.
      // Nest the private runtime so it is copied without dependency discovery.
      const source = join(modules, name), destination = join(temporary, 'runtime/node_modules', name)
      await mkdir(destination, { recursive: true }); await copyFile(join(source, 'package.json'), join(destination, 'package.json'))
      for (const path of paths) { await mkdir(dirname(join(destination, path)), { recursive: true }); await cp(join(source, path), join(destination, path), { recursive: true, filter: file => !file.endsWith('.map') && !file.endsWith('.ts') }) }
    }
    await copyPackage('onnxruntime-node', ['dist', `bin/napi-v6/${platform}/${arch}`])
    if (platform === 'darwin') await rm(join(temporary, 'runtime/node_modules/onnxruntime-node/bin/napi-v6/darwin/arm64/libonnxruntime.1.30.0.dylib'), { force: true })
    await copyPackage('onnxruntime-common', ['dist/cjs'])
    // onnxruntime-common's entrypoint refers to dist/cjs/index.js; no installer
    // modules, GPU downloads, other architectures, maps or typings are shipped.
    await copyPackage('@techstark/opencv-js', ['dist/opencv.js', 'LICENSE'])
    await copyPackage('pngjs', ['lib', 'LICENSE'])
    await copyPackage('clipper-lib', ['clipper.js'])
    await mkdir(join(temporary, 'models'))
    for (const model of models) {
      const path = join(cache, 'models', model.name)
      if (!await exists(path) || await digest(path) !== model.sha256) {
        const localSource = process.env.MOOTOOL_OCR_MODEL_SOURCE
        const pending = `${path}.partial`
        try {
          if (localSource) await copyFile(join(resolve(localSource), model.name), pending)
          else {
            console.log(`Downloading ${model.name} for the offline installation bundle…`)
            const response = await fetch(model.url, { signal: AbortSignal.timeout(180000) })
            if (!response.ok || !response.body) throw new Error(`Model download failed (${response.status})`)
            const chunks = []; let bytes = 0
            for await (const chunk of response.body) { bytes += chunk.length; if (bytes > model.bytes) throw new Error('Model exceeds expected size'); chunks.push(chunk) }
            if (bytes !== model.bytes) throw new Error('Incomplete model download')
            await writeFile(pending, Buffer.concat(chunks))
          }
          if ((await readFile(pending)).length !== model.bytes || await digest(pending) !== model.sha256) throw new Error(`Model checksum mismatch: ${model.name}`)
          await rename(pending, path)
        } finally { await rm(pending, { force: true }) }
      }
      await copyFile(path, join(temporary, 'models', model.name))
    }
    // ONNX's ModelProto stores custom metadata at field 14. Extract the CTC
    // alphabet from the verified model itself rather than an unrelated dictionary.
    const protobuf = require('protobufjs')
    const schema = protobuf.parse('syntax="proto2"; message Entry { optional string key=1; optional string value=2; } message Model { repeated Entry metadata=14; }').root.lookupType('Model')
    const metadata = schema.decode(await readFile(join(temporary, 'models/PP-OCRv6_rec_small.onnx'))).metadata
    const alphabet = metadata.find(entry => entry.key === 'character')?.value?.split(/\r?\n/)
    if (!alphabet || alphabet.length < 6000 || alphabet.length > 30000 || alphabet.some(character => !character || character.length > 8)) throw new Error('Invalid model alphabet')
    await writeFile(join(temporary, 'characters.json'), JSON.stringify(['', ...alphabet, ' ']))
    await cp(join(root, 'resources/ocr'), join(temporary, 'attribution'), { recursive: true })
    const files = await inventory(temporary)
    await writeFile(join(temporary, 'manifest.json'), JSON.stringify({ schema: 1, target, engine: 'pp-ocrv6-small', modelVersion: 'RapidOCR-v3.10.0', runtimeVersion: '1.30.0', runtimeLockSha256: lockHash, files }, null, 2) + '\n')
    await rm(output, { recursive: true, force: true }); await rename(temporary, output)
    console.log(`Prepared offline OCR ${target}: ${(files.reduce((sum, file) => sum + file.bytes, 0) / 1024 / 1024).toFixed(2)} MiB`)
    return output
  } finally { await rm(temporary, { recursive: true, force: true }) }
}
if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const option = name => { const index = process.argv.indexOf(name); return index >= 0 ? process.argv[index + 1] : undefined }
  await prepareOcrResources(option('--platform') ?? process.platform, option('--arch') ?? process.arch)
}
