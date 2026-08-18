import { useEffect, useState } from 'react'

export const fallbackEditorFonts = [
  'ui-monospace',
  'Menlo',
  'Monaco',
  'Consolas',
  'Courier New',
  'JetBrains Mono',
  'PingFang SC',
  'Hiragino Sans GB',
  'Microsoft YaHei',
  '等线',
  'Songti SC',
  'SimSun',
  'Georgia',
  'Times New Roman',
  'SF Pro Text',
  'Helvetica Neue',
  'Arial',
  'system-ui'
]

type LocalFontData = {
  family: string
}

export async function listSystemFontFamilies(): Promise<string[]> {
  const families = new Set(fallbackEditorFonts)
  try {
    for (const font of await queryInstalledFonts()) {
      const family = font.family.trim()
      if (family) families.add(family)
    }
  } catch {
    // Local Font Access may be unavailable in tests or locked-down environments.
  }
  return sortFontFamilies([...families])
}

export function cssFontFamily(fontName: string): string {
  const value = fontName.trim()
  if (!value || value === 'system-ui') return 'var(--app-font-family), system-ui, sans-serif'
  if (value === 'ui-monospace') return 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace'
  const quoted = /[\s,"'\\]/.test(value) ? `"${value.replaceAll('\\', '\\\\').replaceAll('"', '\\"')}"` : value
  return `${quoted}, ui-monospace, SFMono-Regular, Menlo, Consolas, monospace`
}

export function fontSelectOptions(fonts: string[], current = ''): string[] {
  const unique = new Set(fonts.filter(Boolean))
  if (current.trim()) unique.add(current.trim())
  const rest = sortFontFamilies([...unique].filter((font) => font !== 'ui-monospace'))
  return unique.has('ui-monospace') ? ['ui-monospace', ...rest] : rest
}

export function useSystemFontFamilies(): string[] {
  const [fonts, setFonts] = useState(fallbackEditorFonts)

  useEffect(() => {
    let cancelled = false
    void listSystemFontFamilies().then((next) => {
      if (!cancelled) setFonts(next)
    })
    return () => {
      cancelled = true
    }
  }, [])

  return fonts
}

function sortFontFamilies(fonts: string[]): string[] {
  return [...fonts].sort((left, right) => left.localeCompare(right, undefined, { sensitivity: 'base' }))
}

async function queryInstalledFonts(): Promise<LocalFontData[]> {
  const query = (globalThis as { queryLocalFonts?: () => Promise<LocalFontData[]> }).queryLocalFonts
  if (typeof query !== 'function') return []
  return query()
}
