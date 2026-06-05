import { useMemo, useState } from 'react'
import { CheckCircle2, Copy, Eraser, FileJson, Minimize2, Sparkles, WrapText, XCircle } from 'lucide-react'
import { compressJson, escapeJsonString, formatJson, unescapeJsonString, validateJson } from './jsonTools'

const sampleJson = `{
  "name": "MooTool Next",
  "stack": ["Electron", "Vite", "React", "TypeScript"],
  "desktop": {
    "style": "quiet macOS workspace",
    "theme": "light"
  }
}`

type CopyState = 'idle' | 'copied' | 'failed'

export function JsonTool() {
  const [content, setContent] = useState(sampleJson)
  const [wrap, setWrap] = useState(true)
  const [copyState, setCopyState] = useState<CopyState>('idle')
  const [notice, setNotice] = useState('')
  const status = useMemo(() => validateJson(content), [content])

  function runTransform(transform: (value: string) => string, success: string): void {
    try {
      setContent(transform(content))
      setNotice(success)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : '处理失败')
    }
  }

  async function copyContent(): Promise<void> {
    try {
      await navigator.clipboard.writeText(content)
      setCopyState('copied')
      setNotice('已复制到剪贴板')
    } catch {
      setCopyState('failed')
      setNotice('复制失败')
    }

    window.setTimeout(() => setCopyState('idle'), 1400)
  }

  return (
    <section className="tool-page json-tool">
      <div className="tool-page__header">
        <div>
          <div className="tool-eyebrow">
            <FileJson size={16} />
            JSON
          </div>
          <h1>JSON 工作台</h1>
        </div>
        <div className={status.kind === 'valid' ? 'status-pill status-pill--valid' : status.kind === 'error' ? 'status-pill status-pill--error' : 'status-pill'}>
          {status.kind === 'valid' ? <CheckCircle2 size={16} /> : status.kind === 'error' ? <XCircle size={16} /> : <FileJson size={16} />}
          {status.message}
        </div>
      </div>

      <div className="json-layout">
        <div className="editor-shell">
          <div className="editor-toolbar">
            <button className="toolbar-button toolbar-button--primary" onClick={() => runTransform((value) => formatJson(value, 2), '已格式化')}>
              <Sparkles size={16} />
              格式化
            </button>
            <button className="toolbar-button" onClick={() => runTransform(compressJson, '已压缩')}>
              <Minimize2 size={16} />
              压缩
            </button>
            <button className="toolbar-button" onClick={() => setWrap((value) => !value)}>
              <WrapText size={16} />
              {wrap ? '换行' : '单行'}
            </button>
            <button className="toolbar-button" onClick={copyContent}>
              <Copy size={16} />
              {copyState === 'copied' ? '已复制' : '复制'}
            </button>
            <button className="toolbar-button toolbar-button--quiet" onClick={() => setContent('')}>
              <Eraser size={16} />
              清空
            </button>
          </div>

          <textarea
            className={wrap ? 'json-editor' : 'json-editor json-editor--nowrap'}
            spellCheck={false}
            value={content}
            onChange={(event) => {
              setContent(event.target.value)
              setNotice('')
              if (copyState !== 'idle') {
                setCopyState('idle')
              }
            }}
          />
        </div>

        <aside className="inspector-panel">
          <div className="inspector-card">
            <span className="inspector-kicker">操作</span>
            <button className="inspector-action" onClick={() => runTransform(escapeJsonString, '已转为 JSON 字符串')}>
              JSON 字符串转义
            </button>
            <button className="inspector-action" onClick={() => runTransform(unescapeJsonString, '已还原 JSON 字符串')}>
              JSON 字符串还原
            </button>
          </div>

          <div className="inspector-card">
            <span className="inspector-kicker">结果</span>
            <p className={status.kind === 'error' ? 'result-text result-text--error' : 'result-text'}>{notice || status.message}</p>
          </div>

          <div className="inspector-card inspector-card--muted">
            <span className="inspector-kicker">后续</span>
            <p>保存片段、JsonPath、JSON/XML、排序和重复 key 检查会继续放在这个右侧区域里。</p>
          </div>
        </aside>
      </div>
    </section>
  )
}
