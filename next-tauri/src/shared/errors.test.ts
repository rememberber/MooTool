import { describe, expect, it } from 'vitest'
import { toProductError } from './errors'

describe('product error normalization', () => {
  it('preserves structured command errors', () => {
    expect(toProductError({
      code: 'permission_denied',
      message: 'Access denied',
      retryable: false
    })).toEqual({
      code: 'permission_denied',
      message: 'Access denied',
      retryable: false,
      stack: undefined
    })
  })

  it('parses serialized Tauri command errors', () => {
    expect(toProductError('{"code":"timeout","message":"Timed out","retryable":true}'))
      .toMatchObject({ code: 'timeout', message: 'Timed out', retryable: true })
  })

  it('falls back safely for native errors', () => {
    expect(toProductError(new Error('broken'))).toMatchObject({
      code: 'frontend_error',
      message: 'broken',
      retryable: false
    })
  })
})
