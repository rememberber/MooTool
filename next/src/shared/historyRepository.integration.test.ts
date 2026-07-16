import { mkdtempSync, rmSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { afterEach, describe, expect, it } from 'vitest'
import { HistoryRepository } from '../../electron/main/historyRepository'

const tempDirectories: string[] = []

afterEach(() => {
  for (const directory of tempDirectories.splice(0)) {
    rmSync(directory, { recursive: true, force: true })
  }
})

function createRepository(): HistoryRepository {
  const directory = mkdtempSync(join(tmpdir(), 'mootool-history-'))
  tempDirectories.push(directory)
  return new HistoryRepository(join(directory, 'MooToolNext.db'))
}

describe('HistoryRepository', () => {
  it('saves, searches, deletes, and clears records by function type', () => {
    const repository = createRepository()
    const first = repository.save({ funcType: 'json', summary: 'Format', inputText: '{"a":1}', outputText: '{\n  "a": 1\n}' })
    repository.save({ funcType: 'http', summary: 'Request', inputText: 'GET /', outputText: '200' })

    expect(first?.id).toBeTypeOf('number')
    expect(repository.list({ funcType: 'json' })).toHaveLength(1)
    expect(repository.list({ funcType: 'json', keyword: 'Format' })[0]?.summary).toBe('Format')
    repository.delete(first!.id)
    expect(repository.list({ funcType: 'json' })).toEqual([])
    repository.clear('http')
    expect(repository.list({ funcType: 'http' })).toEqual([])
    repository.close()
  })

  it('keeps only the latest 200 records per function type', () => {
    const repository = createRepository()
    for (let index = 0; index < 205; index++) {
      repository.save({ funcType: 'json', summary: `Record ${index}`, inputText: String(index), outputText: String(index) })
    }

    const records = repository.list({ funcType: 'json' })
    expect(records).toHaveLength(200)
    expect(records[0]?.summary).toBe('Record 204')
    expect(records.at(-1)?.summary).toBe('Record 5')
    repository.close()
  })
})
