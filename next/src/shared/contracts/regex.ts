export type RegexEngine = 'java' | 'javascript'
export type RegexOptions = { global: boolean; ignoreCase: boolean; multiline: boolean; dotAll: boolean }
export type RegexMatch = { index: number; value: string; groups: string[] }
export type RegexInput = { pattern: string; source: string; options: RegexOptions }
export type RegexResult = { matches: RegexMatch[]; limited: boolean }
