import { describe, expect, it } from 'vitest'
import { PdfOutputAccess } from './pdfOutputAccess'

describe('PDF output access', () => {
  it('permits only a renderer’s latest successful results and known actions', () => {
    const access = new PdfOutputAccess()
    access.remember(1, ['/tmp/first.pdf'])
    expect(access.authorize(1, '/tmp/first.pdf', 'open')).toEqual({ path: '/tmp/first.pdf', action: 'open' })
    expect(() => access.authorize(2, '/tmp/first.pdf', 'open')).toThrow('Invalid PDF')
    expect(() => access.authorize(1, '/tmp/unknown.pdf', 'open')).toThrow('Invalid PDF')
    expect(() => access.authorize(1, '/tmp/first.pdf', 'execute')).toThrow('Invalid PDF')
    access.remember(1, ['/tmp/second.pdf'])
    expect(() => access.authorize(1, '/tmp/first.pdf', 'open')).toThrow('Invalid PDF')
    expect(access.authorize(1, '/tmp/second.pdf', 'reveal').action).toBe('reveal')
    access.forget(1)
    expect(() => access.authorize(1, '/tmp/second.pdf', 'open')).toThrow('Invalid PDF')
  })
})
