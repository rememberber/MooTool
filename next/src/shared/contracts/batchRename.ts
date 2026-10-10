export type RenameOptions = {
  find: string; replacement: string; prefix: string; suffix: string
  caseSensitive: boolean; preserveExtension: boolean
  numbering: boolean; start: number; width: number; separator: string
}
export const defaultRenameOptions: RenameOptions = {
  find: '', replacement: '', prefix: '', suffix: '', caseSensitive: true,
  preserveExtension: true, numbering: false, start: 1, width: 3, separator: '_'
}
export type RenameFile = { id: string; name: string; path: string }
export type RenameIssue = 'invalidName' | 'duplicate' | 'occupied' | 'changed' | 'sameFile'
export type RenameRow = RenameFile & { newName: string; status: 'ready' | 'unchanged' | 'conflict'; issue?: RenameIssue }
export type RenamePreview = { token: string | null; rows: RenameRow[]; changedCount: number }
export type RenameStatus = { undoAvailable: boolean; recoveryRequired: boolean; journalPath: string }
export type RenameResult = { changedCount: number; files: RenameFile[]; status: RenameStatus }

export function normalizeRenameOptions(value: unknown): RenameOptions {
  if (!value || typeof value !== 'object') throw new Error('batchRename.error.options')
  const source = value as Record<string, unknown>
  for (const key of ['find', 'replacement', 'prefix', 'suffix', 'separator'] as const) {
    if (typeof source[key] !== 'string' || source[key].length > 120) throw new Error('batchRename.error.options')
  }
  for (const key of ['caseSensitive', 'preserveExtension', 'numbering'] as const) {
    if (typeof source[key] !== 'boolean') throw new Error('batchRename.error.options')
  }
  if (!Number.isInteger(source.start) || Number(source.start) < 0 || Number(source.start) > 1_000_000
    || !Number.isInteger(source.width) || Number(source.width) < 1 || Number(source.width) > 8) throw new Error('batchRename.error.options')
  return Object.fromEntries(Object.keys(defaultRenameOptions).map((key) => [key, source[key]])) as RenameOptions
}

export function renameCandidate(name: string, options: RenameOptions, index: number): string {
  const dot = name.lastIndexOf('.')
  const split = options.preserveExtension && dot > 0 ? dot : name.length
  const extension = name.slice(split)
  let stem = name.slice(0, split)
  if (options.find) {
    const escaped = options.find.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    // User text is escaped: replacement is literal and never evaluated as regex.
    stem = stem.replace(new RegExp(escaped, options.caseSensitive ? 'g' : 'gi'), () => options.replacement)
  }
  const number = options.numbering ? options.separator + String(options.start + index).padStart(options.width, '0') : ''
  return options.prefix + stem + options.suffix + number + extension
}

export function validRenameName(name: string): boolean {
  return Boolean(name) && name !== '.' && name !== '..'
    && !/[<>:"/\\|?*\u0000-\u001f]/.test(name) && !/[. ]$/.test(name)
    && !/^(?:CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\.|$)/i.test(name)
    && new TextEncoder().encode(name).length <= 255
}

export function renameNameKey(name: string): string { return name.normalize('NFC').toLowerCase() }
