import { describe, expect, it } from 'vitest'
import { productToolCatalog, toolCatalog } from './toolCatalog'

describe('Tauri product feature baseline', () => {
  it('freezes 25 product tools independently from engineering labs', () => {
    expect(productToolCatalog).toHaveLength(25)
    expect(new Set(productToolCatalog.map((tool) => tool.id)).size).toBe(25)
    expect(productToolCatalog.every((tool) => !tool.engineeringOnly)).toBe(true)
  })

  it('keeps compatibility labs outside the release feature count', () => {
    expect(toolCatalog.filter((tool) => tool.engineeringOnly).map((tool) => tool.id)).toEqual([
      'editor-lab',
      'webview-lab'
    ])
  })

  it('tracks the currently delivered product tools explicitly', () => {
    expect(productToolCatalog.filter((tool) => tool.ready).map((tool) => tool.id)).toEqual([
      'quick-note',
      'text-diff',
      'reformat',
      'json',
      'config',
      'runtime',
      'protobuf',
      'variables',
      'http',
      'host',
      'network',
      'ua',
      'encode',
      'crypto',
      'regex',
      'cron',
      'qrcode',
      'timestamp',
      'message-board',
      'translation',
      'calculator',
      'color',
      'image',
      'pdf',
      'system'
    ])
  })
})
