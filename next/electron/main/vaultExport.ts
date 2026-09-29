import { writeFile } from 'node:fs/promises'
import { join } from 'node:path'

/** Export bodies only (without note frontmatter), never overwrite an existing file. */
export async function exportVaultFiles(directory: string, files: { relativePath: string; content: string }[], kind: 'json' | 'quickNote'): Promise<number> {
  for (const file of files) {
    const extension = kind === 'json' ? '.json' : '.txt'
    const path = file.relativePath.replace(/\\/g, '/')
    const name = kind === 'json' ? path.slice(path.lastIndexOf('/') + 1) : path.replace(/\//g, '-')
    const base = name.replace(/\.(json|txt)$/i, '').replace(/[<>:"/\\|?*\x00-\x1f]/g, '_') || 'untitled'
    for (let suffix = 0; ; suffix++) {
      try {
        await writeFile(join(directory, `${base}${suffix ? `-${suffix}` : ''}${extension}`), file.content, { encoding: 'utf8', flag: 'wx' })
        break
      } catch (error) {
        if ((error as NodeJS.ErrnoException).code !== 'EEXIST') throw error
      }
    }
  }
  return files.length
}
