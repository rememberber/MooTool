import { describe, expect, it } from 'vitest'
import { describeJsonSession } from './jsonSession'

describe('JSON WebView session', () => {
  it('tracks editor, formatting and JSONPath state without storing full content in the digest', () => {
    const result = describeJsonSession({
      content: '{"message":"你好"}',
      anchor: 16,
      head: 5,
      line: 1,
      scrollTop: 10.7,
      wrap: true,
      indent: 4,
      sortKeys: true,
      jsonPath: '$.message',
      compositionStarts: 1,
      compositionEnds: 1
    })

    expect(result).toMatchObject({ contentLength: 16, line: 1, selectionFrom: 5, selectionTo: 16 })
    expect(JSON.parse(result.digest)).toMatchObject({
      contentLength: 16,
      selection: [5, 16],
      scrollTop: 11,
      wrap: true,
      indent: 4,
      sortKeys: true,
      jsonPath: '$.message',
      composition: [1, 1]
    })
    expect(result.digest).not.toContain('你好')
  })
})
