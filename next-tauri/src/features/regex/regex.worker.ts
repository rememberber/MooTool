import { matchRegex, type RegexOptions } from './regexTools'

export interface JavaScriptRegexRequest {
  pattern: string
  source: string
  options: RegexOptions
}

self.onmessage = (event: MessageEvent<JavaScriptRegexRequest>) => {
  try {
    const matches = matchRegex(event.data.pattern, event.data.source, event.data.options)
    self.postMessage({ matches, limited: matches.length === 5000 })
  } catch (error) {
    self.postMessage({ error: String(error) })
  }
}
