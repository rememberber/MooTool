import { createHash } from 'node:crypto'
import { createReadStream } from 'node:fs'
import { copyFile, readFile, stat } from 'node:fs/promises'
import { join, resolve } from 'node:path'

async function validate(directory, target) {
  const metadata = join(directory, `${target}.json`)
  const manifest = JSON.parse(await readFile(metadata, 'utf8'))
  const { fileName, size, sha512 } = manifest.download
  if (manifest.target !== target || fileName !== `MooTool-OCR-${manifest.version}-${target}.bin.gz` || !/^[a-f0-9]{64}$/.test(manifest.version)) throw new Error('Invalid OCR release metadata')
  const bundle = join(directory, fileName)
  if ((await stat(bundle)).size !== size) throw new Error('Incomplete OCR release asset')
  const hash = createHash('sha512')
  for await (const chunk of createReadStream(bundle)) hash.update(chunk)
  if (hash.digest('base64') !== sha512) throw new Error('Corrupt OCR release asset')
  return { metadata, bundle, fileName }
}
if (process.argv[2] === '--validate') {
  for (const target of ['win32-x64', 'linux-x64']) await validate(resolve(process.argv[3]), target)
} else {
  const target = { 'windows-x64': 'win32-x64', 'linux-x64': 'linux-x64' }[process.argv[2]]
  if (target) {
    const { metadata, bundle, fileName } = await validate(resolve('dist/optional-components'), target)
    await copyFile(bundle, join('release-assets', fileName))
    await copyFile(metadata, join('release-assets', `${target}.json`))
  }
}
