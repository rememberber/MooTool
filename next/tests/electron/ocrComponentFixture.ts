import { cp, mkdir, readFile, writeFile } from 'node:fs/promises'
import { join, resolve } from 'node:path'

// Recognition/lifecycle regressions start with an already downloaded component.
// First-use installation is covered separately in ocr-component.spec.ts.
export async function seedOcrComponent(userData: string): Promise<void> {
  if (!process.env.MOOTOOL_OCR_EXECUTABLE || process.platform === 'darwin') return
  const target = `${process.platform}-${process.arch}`
  const metadata = JSON.parse(await readFile(resolve('out/optional-components', `${target}.json`), 'utf8'))
  const destination = join(userData, 'components/ocr', metadata.version, target)
  await mkdir(destination, { recursive: true })
  await cp(resolve('out/ocr', target), destination, { recursive: true })
  await writeFile(join(destination, '.complete'), metadata.version)
}
