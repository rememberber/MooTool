/** Split identifier boundaries while preserving ordinary prose and line breaks. */
export function splitIdentifiers(text: string): string {
  return text.replace(/[A-Za-z_$][A-Za-z0-9_$]*(?:-[A-Za-z0-9_$]+)*/g, (identifier) => identifier
    .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/[_$-]+/g, ' ')
    .trim())
}

export const namingFormats = ['camelCase', 'PascalCase', 'snake_case', 'UPPER_SNAKE_CASE', 'kebab-case'] as const
export type NamingFormat = (typeof namingFormats)[number]

export function formatTranslationName(text: string, format: NamingFormat): string {
  // Naming is intentionally limited to a short English phrase, not arbitrary translated prose.
  if (!text.trim() || text.includes('\n') || text.length > 200 || /[^\x00-\x7F]/.test(text)) return ''
  const words = splitIdentifiers(text).match(/[A-Za-z0-9]+/g)?.map((word) => word.toLowerCase()) ?? []
  if (!words.length || words.length > 20) return ''
  const title = (word: string) => word.charAt(0).toUpperCase() + word.slice(1)
  let result: string
  switch (format) {
    case 'camelCase': result = words[0] + words.slice(1).map(title).join(''); break
    case 'PascalCase': result = words.map(title).join(''); break
    case 'snake_case': result = words.join('_'); break
    case 'UPPER_SNAKE_CASE': result = words.join('_').toUpperCase(); break
    case 'kebab-case': result = words.join('-'); break
  }
  return /^\d/.test(result) && format !== 'kebab-case' ? `_${result}` : result
}
