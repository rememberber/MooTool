import type { FuncHistoryRecord, SaveFuncHistoryInput } from '../contracts/localData'

export type { FuncHistoryRecord, SaveFuncHistoryInput }

const storageKey = 'mootool.func-history.v1'

type DraftStorage = Pick<Storage, 'getItem' | 'setItem'>

export function createFuncHistoryStore(storage: DraftStorage) {
  return {
    list(funcType: string, keyword = ''): FuncHistoryRecord[] {
      return listFrom(storage, funcType, keyword)
    },
    save(input: SaveFuncHistoryInput, now = new Date()): FuncHistoryRecord | null {
      return saveTo(storage, input, now)
    },
    delete(id: number): boolean {
      return deleteFrom(storage, id)
    },
    clear(funcType: string): number {
      return clearFrom(storage, funcType)
    }
  }
}

export function listFuncHistory(funcType: string, keyword = ''): FuncHistoryRecord[] {
  return listFrom(window.localStorage, funcType, keyword)
}

function listFrom(storage: DraftStorage, funcType: string, keyword: string): FuncHistoryRecord[] {
  const type = normalizeFuncType(funcType)
  const needle = keyword.slice(0, 200).trim().toLowerCase()
  return readRecords(storage)
    .filter((record) => record.funcType === type)
    .filter((record) => !needle || [record.summary, record.inputText, record.outputText, record.extraData ?? ''].some((value) => matchesLike(value, needle)))
    .sort((left, right) => right.createTime.localeCompare(left.createTime) || right.id - left.id)
}

export function saveFuncHistory(input: SaveFuncHistoryInput, now = new Date()): FuncHistoryRecord | null {
  return saveTo(window.localStorage, input, now)
}

function saveTo(storage: DraftStorage, input: SaveFuncHistoryInput, now: Date): FuncHistoryRecord | null {
  const funcType = normalizeFuncType(input.funcType)
  const inputText = truncate(input.inputText, 20 * 1024 * 1024)
  const outputText = truncate(input.outputText, 20 * 1024 * 1024)
  if (!inputText.trim() && !outputText.trim()) return null
  const requested = truncate(input.summary ?? '', 200).trim()
  const summary = requested || preview(inputText, 40)
  const extra = input.extraData == null ? '' : truncate(input.extraData, 100_000)
  const records = readRecords(storage)
  const saved: FuncHistoryRecord = {
    id: records.reduce((max, record) => Math.max(max, record.id), 0) + 1,
    funcType,
    summary,
    inputText,
    outputText,
    extraData: extra || null,
    createTime: sqliteDate(now)
  }
  const next = [saved, ...records.filter((record) => record.funcType === funcType)].slice(0, 200)
  writeRecords(storage, [...records.filter((record) => record.funcType !== funcType), ...next])
  return saved
}

export function deleteFuncHistory(id: number): boolean {
  return deleteFrom(window.localStorage, id)
}

function deleteFrom(storage: DraftStorage, id: number): boolean {
  if (!Number.isSafeInteger(id) || id <= 0) throw new Error('Invalid history id')
  const records = readRecords(storage)
  const next = records.filter((record) => record.id !== id)
  writeRecords(storage, next)
  return next.length !== records.length
}

export function clearFuncHistory(funcType: string): number {
  return clearFrom(window.localStorage, funcType)
}

function clearFrom(storage: DraftStorage, funcType: string): number {
  const type = normalizeFuncType(funcType)
  const records = readRecords(storage)
  const removed = records.filter((record) => record.funcType === type).length
  writeRecords(storage, records.filter((record) => record.funcType !== type))
  return removed
}

function readRecords(storage: DraftStorage): FuncHistoryRecord[] {
  try {
    const value = JSON.parse(storage.getItem(storageKey) ?? '[]') as unknown
    return Array.isArray(value) ? value.filter(isRecord) : []
  } catch {
    return []
  }
}

function writeRecords(storage: DraftStorage, records: FuncHistoryRecord[]): void {
  storage.setItem(storageKey, JSON.stringify(records))
}

function isRecord(value: unknown): value is FuncHistoryRecord {
  if (!value || typeof value !== 'object') return false
  const record = value as Partial<FuncHistoryRecord>
  return typeof record.id === 'number'
    && typeof record.funcType === 'string'
    && typeof record.summary === 'string'
    && typeof record.inputText === 'string'
    && typeof record.outputText === 'string'
    && typeof record.createTime === 'string'
}

function matchesLike(value: string, keyword: string): boolean {
  const pattern = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/%/g, '.*').replace(/_/g, '.')
  return new RegExp(pattern, 'i').test(value)
}

function normalizeFuncType(value: string): string {
  const funcType = value.trim()
  if (!/^[a-zA-Z][a-zA-Z0-9_-]{0,63}$/.test(funcType)) throw new Error('Invalid function type')
  return funcType
}

function truncate(value: string, maxChars: number): string {
  return [...value].slice(0, maxChars).join('')
}

function preview(value: string, maxChars: number): string {
  const normalized = value.replace(/[\r\n]+/g, ' ').trim()
  return [...normalized].length <= maxChars ? normalized : `${[...normalized].slice(0, maxChars).join('')}...`
}

function sqliteDate(date: Date): string {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
