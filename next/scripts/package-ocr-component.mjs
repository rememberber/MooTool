import { createHash } from 'node:crypto'
import { createReadStream, createWriteStream } from 'node:fs'
import { mkdir, readFile, stat, writeFile } from 'node:fs/promises'
import { dirname, join, resolve } from 'node:path'
import { pipeline } from 'node:stream/promises'
import { createGzip } from 'node:zlib'
import { fileURLToPath } from 'node:url'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
export async function packageOcrComponent(resources, platform, arch, output = join(root, 'dist/optional-components')) {
  const target = `${platform}-${arch}`
  const original = await readFile(join(resources, 'manifest.json'))
  const manifest = JSON.parse(original)
  if (manifest.target !== target) throw new Error('OCR component target mismatch')
  const version = createHash('sha256').update(original).digest('hex')
  const files = [...manifest.files, { path: 'manifest.json', bytes: original.length, sha256: version }]
  const fileName = `MooTool-OCR-${version}-${target}.bin.gz`
  await mkdir(output, { recursive: true })
  const bundle = join(output, fileName)
  await pipeline(async function* () {
    for (const file of files) {
      const hash = createHash('sha256'); let bytes = 0
      for await (const chunk of createReadStream(join(resources, file.path))) { hash.update(chunk); bytes += chunk.length; yield chunk }
      if (bytes !== file.bytes || hash.digest('hex') !== file.sha256) throw new Error(`Corrupt component input: ${file.path}`)
    }
  }, createGzip({ level: 9 }), createWriteStream(bundle))
  const hash = createHash('sha512')
  for await (const chunk of createReadStream(bundle)) hash.update(chunk)
  const app = JSON.parse(await readFile(join(root, 'package.json'), 'utf8'))
  const descriptor = {
    schema: 1, target, version, files,
    download: { fileName, packageType: 'component', size: (await stat(bundle)).size, sha512: hash.digest('base64'),
      url: `https://github.com/rememberber/MooTool/releases/download/next-electron-v${app.version}/${fileName}` }
  }
  const metadata = join(root, 'out/optional-components', `${target}.json`)
  await mkdir(dirname(metadata), { recursive: true })
  await writeFile(metadata, JSON.stringify(descriptor, null, 2) + '\n')
  await writeFile(join(output, `${target}.json`), JSON.stringify(descriptor, null, 2) + '\n')
  console.log(`Optional OCR ${target}: ${(descriptor.download.size / 1024 / 1024).toFixed(2)} MiB (separate release asset)`)
  return { bundle, metadata }
}
