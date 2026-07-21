export type FindReplaceOptions = {
  matchCase: boolean
  wholeWord: boolean
  regex: boolean
}

export type FindMatch = {
  start: number
  end: number
}

export const defaultFindReplaceOptions: FindReplaceOptions = {
  matchCase: false,
  wholeWord: false,
  regex: false
}

export function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/** Build a global RegExp for find/highlight. Returns null when the pattern is empty or invalid. */
export function buildSearchRegExp(query: string, options: FindReplaceOptions): RegExp | null {
  if (!query) return null
  try {
    const source = options.regex ? query : escapeRegExp(query)
    const wrapped = options.wholeWord ? `\\b(?:${source})\\b` : source
    const flags = options.matchCase ? 'gu' : 'giu'
    return new RegExp(wrapped, flags)
  } catch {
    return null
  }
}

export function findAllMatches(content: string, query: string, options: FindReplaceOptions): FindMatch[] {
  const expression = buildSearchRegExp(query, options)
  if (!expression) return []
  const matches: FindMatch[] = []
  for (const match of content.matchAll(expression)) {
    if (match.index === undefined) continue
    const text = match[0]
    if (!text.length) {
      // Avoid zero-width infinite loops (e.g. empty-group regex).
      expression.lastIndex = match.index + 1
      continue
    }
    matches.push({ start: match.index, end: match.index + text.length })
  }
  return matches
}

export function findNextMatch(
  content: string,
  query: string,
  options: FindReplaceOptions,
  fromIndex: number,
  forward: boolean
): FindMatch | null {
  const matches = findAllMatches(content, query, options)
  if (!matches.length) return null
  if (forward) {
    return matches.find((match) => match.start >= fromIndex) ?? matches[0]
  }
  for (let index = matches.length - 1; index >= 0; index -= 1) {
    const match = matches[index]
    if (match.end <= fromIndex) return match
  }
  return matches[matches.length - 1]
}

function applyReplacement(matchedText: string, replaceWith: string, expression: RegExp): string {
  const single = new RegExp(expression.source, expression.flags.replace('g', ''))
  return matchedText.replace(single, replaceWith)
}

/**
 * Replace the current selection when it is an exact match; otherwise find the next match and replace it.
 * Returns replaced=false when nothing was replaced (caller may then navigate to next).
 */
export function replaceCurrentMatch(
  content: string,
  query: string,
  replaceWith: string,
  options: FindReplaceOptions,
  selection: { start: number; end: number } | null
): { content: string; nextFrom: number; replaced: boolean; match: FindMatch | null } {
  if (!query) return { content, nextFrom: selection?.end ?? 0, replaced: false, match: null }
  const expression = buildSearchRegExp(query, options)
  if (!expression) return { content, nextFrom: selection?.end ?? 0, replaced: false, match: null }

  if (selection && selection.end > selection.start) {
    const selected = content.slice(selection.start, selection.end)
    const selectedMatches = findAllMatches(selected, query, options)
    const exact = selectedMatches.length === 1
      && selectedMatches[0].start === 0
      && selectedMatches[0].end === selected.length
    if (exact) {
      const replacedSlice = applyReplacement(selected, replaceWith, expression)
      const next = `${content.slice(0, selection.start)}${replacedSlice}${content.slice(selection.end)}`
      return {
        content: next,
        nextFrom: selection.start + replacedSlice.length,
        replaced: true,
        match: { start: selection.start, end: selection.start + replacedSlice.length }
      }
    }
  }

  const from = selection?.end ?? 0
  const match = findNextMatch(content, query, options, from, true)
  if (!match) return { content, nextFrom: from, replaced: false, match: null }
  const slice = content.slice(match.start, match.end)
  const replacedSlice = applyReplacement(slice, replaceWith, expression)
  const next = `${content.slice(0, match.start)}${replacedSlice}${content.slice(match.end)}`
  return {
    content: next,
    nextFrom: match.start + replacedSlice.length,
    replaced: true,
    match: { start: match.start, end: match.start + replacedSlice.length }
  }
}

export function replaceAllMatches(
  content: string,
  query: string,
  replaceWith: string,
  options: FindReplaceOptions
): { content: string; count: number } {
  const expression = buildSearchRegExp(query, options)
  if (!expression) return { content, count: 0 }
  let count = 0
  const next = content.replace(expression, (match) => {
    if (!match.length) return match
    count += 1
    return applyReplacement(match, replaceWith, expression)
  })
  return { content: next, count }
}
