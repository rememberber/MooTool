import { readdir, readFile } from 'node:fs/promises'
import { extname, join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const root = fileURLToPath(new URL('..', import.meta.url))
const featuresRoot = join(root, 'src', 'features')
const failures = []

for (const file of await sourceFiles(featuresRoot)) {
  const projectPath = relative(root, file)
  if (isAllowedCatalogOrEngineeringFile(projectPath)) continue
  const source = await readFile(file, 'utf8')
  source.split(/\r?\n/).forEach((line, index) => {
    if (/[\u3400-\u9fff\uf900-\ufaff]/u.test(line)) {
      failures.push(`${projectPath}:${index + 1}: ${line.trim()}`)
    }
  })
}

if (failures.length) {
  console.error('Localization boundary check failed. Move product UI text and samples into a *Messages.ts catalog:')
  failures.forEach((failure) => console.error(`- ${failure}`))
  process.exitCode = 1
} else {
  console.log('Localization boundary check passed: formal features contain no catalog-bypassing CJK text.')
}

async function sourceFiles(directory) {
  const output = []
  for (const entry of await readdir(directory, { withFileTypes: true })) {
    const path = join(directory, entry.name)
    if (entry.isDirectory()) output.push(...await sourceFiles(path))
    else if (['.ts', '.tsx'].includes(extname(entry.name))) output.push(path)
  }
  return output
}

function isAllowedCatalogOrEngineeringFile(path) {
  return path.endsWith('Messages.ts')
    || path.endsWith('.test.ts')
    || path.endsWith('.test.tsx')
    || path.includes('/editorLab/')
    || path.includes('/webviewLab/')
}
