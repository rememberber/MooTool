import { messages, type Language, type MessageKey } from '@/shared/i18n/messages'
import type { TextTask, TextTaskReply } from './textTask'

function translate(language: Language) {
  return (key: MessageKey, params?: Record<string, string>) => {
    let message: string = messages[language][key] ?? messages['zh-CN'][key]
    for (const [name, value] of Object.entries(params ?? {})) message = message.replaceAll(`{${name}}`, value)
    return message
  }
}

self.onmessage = async ({ data: task }: MessageEvent<TextTask>) => {
  try {
    let result: TextTaskReply & { ok: true }
    if (task.kind === 'diff') {
      const { compareText } = await import('@/features/diff/diffTools')
      result = { ok: true, result: compareText(task.left, task.right, task.ignoreWhitespace) }
    } else {
      const tools = await import('@/features/json/jsonTools')
      const t = translate(task.language)
      if (task.kind === 'validateJson') result = { ok: true, result: tools.validateJson(task.input, t) }
      else {
        let output: string
        switch (task.operation) {
          case 'format': output = tools.formatJson(task.input, t, 2); break
          case 'compress': output = tools.compressJson(task.input, t); break
          case 'advancedFormat': output = tools.formatJsonAdvanced(task.input, t, task.options!); break
          case 'toXml': output = tools.jsonToXml(task.input, t); break
          case 'toBean': output = tools.jsonToJavaBean(task.input, t, task.className!); break
          case 'swap': output = tools.swapJsonKeysAndValues(task.input, t); break
          case 'escape': output = tools.escapeJsonString(task.input); break
          case 'unescape': output = tools.unescapeJsonString(task.input, t); break
          case 'escapeText': output = tools.escapeJavaString(task.input); break
          case 'unescapeText': output = tools.unescapeJsonText(task.input); break
          case 'query': output = tools.queryJsonPath(task.input, task.path!, t); break
          case 'fromXml': output = tools.xmlToJson(task.input, t); break
          case 'fromBean': output = tools.javaBeanToJson(task.input, t); break
        }
        result = { ok: true, result: output }
      }
    }
    self.postMessage(result)
  } catch (error) {
    self.postMessage({ ok: false, error: error instanceof Error ? error.message : String(error) } satisfies TextTaskReply)
  }
}
