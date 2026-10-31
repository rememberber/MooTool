import type { SaveTranslationWordInput, TranslationHistory, TranslationWord } from '../../platform/contracts/localData'
import { sqliteDate } from '../host/hostProfileStore'

export function listStoredTranslationWords(words: TranslationWord[], keyword = ''): TranslationWord[] {
  const needle = keyword.trim()
  const visible = needle
    ? words.filter((word) => matchesLike(word.sourceText, needle) || matchesLike(word.targetText, needle) || matchesLike(word.remark, needle))
    : words
  return [...visible].sort((left, right) => right.modifiedTime.localeCompare(left.modifiedTime) || right.id - left.id)
}

export function saveStoredTranslationWord(words: TranslationWord[], input: SaveTranslationWordInput, now = new Date()): TranslationWord {
  const sourceText = input.sourceText.trim()
  if (!sourceText) throw new Error('Source text is required')
  if (sourceText.length > 50_000 || input.targetText.length > 50_000 || input.remark.length > 2_000) throw new Error('Invalid string value')
  const id = Number.isSafeInteger(input.id) && Number(input.id) > 0 ? Number(input.id) : undefined
  const existing = id
    ? words.find((word) => word.id === id)
    : words.find((word) => word.sourceText === sourceText && word.sourceLang === input.sourceLang && word.targetLang === input.targetLang)
  const stamp = sqliteDate(now)
  if (existing) return { ...existing, sourceText, targetText: input.targetText, sourceLang: input.sourceLang, targetLang: input.targetLang, remark: input.remark, modifiedTime: stamp }
  return {
    id: Math.max(0, ...words.map((word) => word.id)) + 1,
    sourceText,
    targetText: input.targetText,
    sourceLang: input.sourceLang,
    targetLang: input.targetLang,
    remark: input.remark,
    createTime: stamp,
    modifiedTime: stamp
  }
}

export function listStoredTranslationHistory(items: TranslationHistory[], keyword = ''): TranslationHistory[] {
  const needle = keyword.trim()
  const visible = needle
    ? items.filter((item) => [item.sourceText, item.targetText, item.sourceLang, item.targetLang].some((value) => matchesLike(value, needle)))
    : items
  return [...visible].sort((left, right) => right.id - left.id).slice(0, 500)
}

function matchesLike(value: string, keyword: string): boolean {
  const pattern = `%${keyword}%`.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replaceAll('%', '.*').replaceAll('_', '.')
  return new RegExp(`^${pattern}$`, 'i').test(value)
}
