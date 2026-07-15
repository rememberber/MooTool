import { useMemo, useState } from 'react'
import { CheckCircle2, Copy, Eraser, FileJson, Minimize2, Sparkles, WrapText, XCircle } from 'lucide-react'
import { compressJson, escapeJsonString, formatJson, unescapeJsonString, validateJson } from './jsonTools'
import { useI18n } from '@/shared/i18n/I18nProvider'

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
  const { t } = useI18n()
  const [content, setContent] = useState(sampleJson)
  const [wrap, setWrap] = useState(true)
  const [copyState, setCopyState] = useState<CopyState>('idle')
  const [notice, setNotice] = useState('')
  const status = useMemo(() => validateJson(content, t), [content, t])

  function runTransform(transform: (value: string) => string, success: string): void {
    try {
      setContent(transform(content))
      setNotice(success)
    } catch (error) {
      setNotice(error instanceof Error ? error.message : t('json.notice.failed'))
    }
  }

  async function copyContent(): Promise<void> {
    try {
      await navigator.clipboard.writeText(content)
      setCopyState('copied')
      setNotice(t('json.notice.copied'))
    } catch {
      setCopyState('failed')
      setNotice(t('json.notice.copyFailed'))
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
          <h1>{t('json.title')}</h1>
        </div>
        <div className={status.kind === 'valid' ? 'status-pill status-pill--valid' : status.kind === 'error' ? 'status-pill status-pill--error' : 'status-pill'}>
          {status.kind === 'valid' ? <CheckCircle2 size={16} /> : status.kind === 'error' ? <XCircle size={16} /> : <FileJson size={16} />}
          {status.message}
        </div>
      </div>

      <div className="json-layout">
        <div className="editor-shell">
          <div className="editor-toolbar">
            <button className="toolbar-button toolbar-button--primary" onClick={() => runTransform((value) => formatJson(value, t, 2), t('json.notice.formatted'))}>
              <Sparkles size={16} />
              {t('json.action.format')}
            </button>
            <button className="toolbar-button" onClick={() => runTransform((value) => compressJson(value, t), t('json.notice.compressed'))}>
              <Minimize2 size={16} />
              {t('json.action.compress')}
            </button>
            <button className="toolbar-button" onClick={() => setWrap((value) => !value)}>
              <WrapText size={16} />
              {wrap ? t('json.action.wrap') : t('json.action.nowrap')}
            </button>
            <button className="toolbar-button" onClick={copyContent}>
              <Copy size={16} />
              {copyState === 'copied' ? t('json.action.copied') : t('json.action.copy')}
            </button>
            <button className="toolbar-button toolbar-button--quiet" onClick={() => setContent('')}>
              <Eraser size={16} />
              {t('json.action.clear')}
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
            <span className="inspector-kicker">{t('json.panel.actions')}</span>
            <button className="inspector-action" onClick={() => runTransform(escapeJsonString, t('json.notice.escaped'))}>
              {t('json.action.escape')}
            </button>
            <button className="inspector-action" onClick={() => runTransform((value) => unescapeJsonString(value, t), t('json.notice.unescaped'))}>
              {t('json.action.unescape')}
            </button>
          </div>

          <div className="inspector-card">
            <span className="inspector-kicker">{t('json.panel.result')}</span>
            <p className={status.kind === 'error' ? 'result-text result-text--error' : 'result-text'}>{notice || status.message}</p>
          </div>

          <div className="inspector-card inspector-card--muted">
            <span className="inspector-kicker">{t('json.panel.next')}</span>
            <p>{t('json.panel.nextDesc')}</p>
          </div>
        </aside>
      </div>
    </section>
  )
}
