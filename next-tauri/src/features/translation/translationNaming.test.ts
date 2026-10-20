import { describe, expect, it } from 'vitest'
import { formatTranslationName, splitIdentifiers } from './translationNaming'

describe('developer translation helpers', () => {
  it('splits acronym boundaries and separators without flattening paragraphs', () => {
    expect(splitIdentifiers('getHTTPResponse user_id\nXMLParser foo-bar version2Value')).toBe('get HTTP Response user id\nXML Parser foo bar version2 Value')
    expect(splitIdentifiers('Hello world!\n获取用户资料')).toBe('Hello world!\n获取用户资料')
  })
  it('formats a translated English phrase consistently', () => {
    expect(formatTranslationName('Get user profile.', 'camelCase')).toBe('getUserProfile')
    expect(formatTranslationName('getHTTPResponse', 'PascalCase')).toBe('GetHttpResponse')
    expect(formatTranslationName('Get user profile.', 'snake_case')).toBe('get_user_profile')
    expect(formatTranslationName('Get user profile.', 'UPPER_SNAKE_CASE')).toBe('GET_USER_PROFILE')
    expect(formatTranslationName('Get user profile.', 'kebab-case')).toBe('get-user-profile')
    expect(formatTranslationName('2 factor auth', 'camelCase')).toBe('_2FactorAuth')
  })
  it('does not turn non-English or multiline prose into misleading names', () => {
    for (const text of ['', '!!!', '获取用户资料', 'Get user\nprofile', 'a'.repeat(201)]) {
      expect(formatTranslationName(text, 'camelCase')).toBe('')
    }
  })
})
