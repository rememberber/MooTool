import { writeFile } from 'node:fs/promises'
import { join, posix } from 'node:path'
import { extensionForSyntax } from './quickNoteVaultRepository'

/** Export bodies only (without note frontmatter), never overwrite an existing file. */
export async function exportVaultFiles(
  directory: string,
  files: { relativePath: string; content: string; metadata?: { syntax: string } }[],
  kind: 'json' | 'quickNote'
): Promise<number> {
  for (const file of files) {
    const path = file.relativePath.replaceAll('\\', '/')
    const { base: fileName, ext: sourceExtension } = posix.parse(path)
    const name = kind === 'json' ? fileName : path.replaceAll('/', '-')
    const base = name.slice(0, name.length - sourceExtension.length).replace(/[<>:"/\\|?*\x00-\x1f]/g, '_') || 'untitled'
    let extension = sourceExtension || (kind === 'json' ? '.json' : '.txt')
    // Legacy notes can have a .txt path even when their metadata specifies a language.
    if (kind === 'quickNote' && extension.toLowerCase() === '.txt' && file.metadata) {
      const syntaxExtension = extensionForSyntax(file.metadata.syntax)
      if (syntaxExtension !== '.txt') extension = syntaxExtension
    }
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
