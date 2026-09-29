import { matchRegex } from './regexTools'
import type { RegexInput } from '@/shared/contracts/regex'
self.onmessage = (event: MessageEvent<RegexInput>) => {
  try {
    const matches = matchRegex(event.data.pattern, event.data.source, event.data.options)
    self.postMessage({ matches, limited: matches.length === 5000 })
  } catch (error) { self.postMessage({ error: String(error) }) }
}
