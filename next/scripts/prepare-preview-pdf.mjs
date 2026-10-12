import { cp, mkdir, rm } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const source = join(root, 'node_modules/pdfjs-dist')
const target = join(root, 'public/pdfjs')
await rm(target, { recursive: true, force: true })
await mkdir(target, { recursive: true })
for (const entry of ['cmaps', 'standard_fonts', 'wasm', 'LICENSE']) await cp(join(source, entry), join(target, entry), { recursive: true })
