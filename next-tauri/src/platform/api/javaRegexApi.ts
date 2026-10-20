import { invoke } from '@tauri-apps/api/core'
import {
  buildJavaRegexStdin,
  parseJavaRegexStdout,
  type JavaRegexInput,
  type JavaRegexResult
} from '../../features/regex/javaRegex'

interface JavaRegexRun {
  stdout: string
  stderr: string
  exitCode: number | null
  timedOut: boolean
  cancelled: boolean
}

export interface JavaRegexApi {
  match(input: JavaRegexInput): Promise<JavaRegexResult>
  cancel(): Promise<boolean>
}

const browserApi: JavaRegexApi = {
  match: async () => {
    throw new Error('浏览器预览不能运行 Java 正则，请使用 Tauri 桌面应用')
  },
  cancel: async () => false
}

const desktopApi: JavaRegexApi = {
  match: async (input) => {
    const requestId = `java-regex-${Date.now()}-${crypto.randomUUID()}`
    const result = await invoke<JavaRegexRun>('match_java_regex', {
      request: { requestId, stdin: buildJavaRegexStdin(input) }
    })
    if (result.cancelled) throw new Error('REGEX_CANCELLED')
    if (result.timedOut) throw new Error('REGEX_TIMEOUT')
    if (result.exitCode !== 0) throw new Error(result.stderr.trim() || 'JDK 11+ is required')
    return parseJavaRegexStdout(result.stdout)
  },
  cancel: () => invoke<boolean>('cancel_java_regex', { requestId: '' })
}

export const javaRegexApi: JavaRegexApi = typeof window !== 'undefined' && window.__TAURI_INTERNALS__
  ? desktopApi
  : browserApi
