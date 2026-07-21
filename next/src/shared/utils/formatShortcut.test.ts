import { describe, expect, it } from 'vitest'
import { isFormatShortcut } from './formatShortcut'

function keyEvent(partial: Partial<KeyboardEvent> & Pick<KeyboardEvent, 'code'>): KeyboardEvent {
  return {
    key: partial.key ?? '',
    code: partial.code,
    metaKey: partial.metaKey ?? false,
    ctrlKey: partial.ctrlKey ?? false,
    shiftKey: partial.shiftKey ?? false,
    altKey: partial.altKey ?? false
  } as KeyboardEvent
}

describe('isFormatShortcut', () => {
  it('matches Ctrl/Cmd+Shift+F', () => {
    expect(isFormatShortcut(keyEvent({ code: 'KeyF', key: 'F', ctrlKey: true, shiftKey: true }))).toBe(true)
    expect(isFormatShortcut(keyEvent({ code: 'KeyF', key: 'f', metaKey: true, shiftKey: true }))).toBe(true)
  })

  it('matches Cmd+Option+L even when Option remaps key', () => {
    expect(isFormatShortcut(keyEvent({ code: 'KeyL', key: 'l', metaKey: true, altKey: true }))).toBe(true)
    // macOS US layout: Option+L produces ¬
    expect(isFormatShortcut(keyEvent({ code: 'KeyL', key: '¬', metaKey: true, altKey: true }))).toBe(true)
  })

  it('rejects find and unrelated shortcuts', () => {
    expect(isFormatShortcut(keyEvent({ code: 'KeyF', key: 'f', metaKey: true }))).toBe(false)
    expect(isFormatShortcut(keyEvent({ code: 'KeyL', key: 'l', ctrlKey: true, altKey: true }))).toBe(false)
    expect(isFormatShortcut(keyEvent({ code: 'KeyL', key: 'l', metaKey: true }))).toBe(false)
  })
})
