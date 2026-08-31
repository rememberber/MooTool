import {
  ArrowLeft,
  ArrowRight,
  ArrowUpDown,
  CheckCircle2,
  Clipboard,
  Copy,
  Eraser,
  TriangleAlert
} from 'lucide-react'
import { useMemo, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey, type MessageValues } from '../../app/localizedMessages'
import { clipboardApi } from '../../platform/api/clipboardApi'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { parseOperationMetadata, useOperationRestore } from '../history/operationRestore'
import {
  convertEncoding,
  encodeTabs,
  EncodeToolError,
  type AsciiFormat,
  type ConversionDirection,
  type EncodeTab,
  type UrlCharset
} from './encodeTools'
import { encodeMessages } from './encodeMessages'

interface Pair {
  left: string
  right: string
}
type EncodeMessageKey = LocalizedMessageKey<typeof encodeMessages>
type EncodeNotice = { key: EncodeMessageKey; values?: MessageValues } | { raw: string }

const tabLabels: Record<EncodeTab, string> = {
  unicode: 'Unicode',
  url: 'URL',
  base64: 'Base64',
  hex: 'Hex',
  ascii: 'ASCII'
}

export function EncodeSurface() {
  const { t } = useLocalizedMessages(encodeMessages)
  const [tab, setTab] = useState<EncodeTab>('unicode')
  const [pairs, setPairs] = useState<Record<EncodeTab, Pair>>(() => ({
    unicode: { left: t('sample.unicode'), right: '' },
    url: { left: t('sample.url'), right: '' },
    base64: { left: t('sample.base64'), right: '' },
    hex: { left: t('sample.hex'), right: '' },
    ascii: { left: t('sample.ascii'), right: '' }
  }))
  const [charset, setCharset] = useState<UrlCharset>('utf-8')
  const [asciiFormat, setAsciiFormat] = useState<AsciiFormat>('decimal')
  const [notice, setNotice] = useState<EncodeNotice>({ key: 'notice.ready', values: { tab: 'Unicode' } })
  const [failed, setFailed] = useState(false)
  const [copiedSide, setCopiedSide] = useState<'left' | 'right' | ''>('')
  const pair = pairs[tab]
  const labels = labelsFor(tab, t)
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({
      tab,
      charset,
      asciiFormat,
      leftLength: pair.left.length,
      leftHash: contentFingerprint(pair.left),
      rightLength: pair.right.length,
      rightHash: contentFingerprint(pair.right)
    }),
    summary: t('session.summary', { tab: tabLabels[tab], left: pair.left.length, right: pair.right.length })
  }), [asciiFormat, charset, pair.left, pair.right, t, tab])
  const { sessionId, reportError } = useToolSessionReport('encode', session.digest, session.summary)
  const recordOperation = useOperationHistory('encode')

  useOperationRestore('encode', (entry) => {
    const metadata = parseOperationMetadata(entry)
    const nextTab = typeof metadata.tab === 'string' && encodeTabs.includes(metadata.tab as EncodeTab) ? metadata.tab as EncodeTab : 'unicode'
    const direction = metadata.direction === 'reverse' ? 'reverse' : 'forward'
    setTab(nextTab)
    if (metadata.charset === 'utf-8' || metadata.charset === 'gb2312') setCharset(metadata.charset)
    if (metadata.asciiFormat === 'decimal' || metadata.asciiFormat === 'hex') setAsciiFormat(metadata.asciiFormat)
    setPairs((current) => ({
      ...current,
      [nextTab]: direction === 'forward'
        ? { left: entry.inputText, right: entry.outputText }
        : { left: entry.outputText, right: entry.inputText }
    }))
    setFailed(false)
  })

  function updatePair(patch: Partial<Pair>): void {
    setPairs((current) => ({ ...current, [tab]: { ...current[tab], ...patch } }))
  }

  function convert(direction: ConversionDirection): void {
    try {
      const input = direction === 'forward' ? pair.left : pair.right
      const output = convertEncoding(tab, direction, input, { charset, asciiFormat })
      updatePair(direction === 'forward' ? { right: output } : { left: output })
      setNotice({ key: 'notice.done', values: { action: direction === 'forward' ? labels.forward : labels.reverse } })
      setFailed(false)
      recordOperation(direction === 'forward' ? labels.forward : labels.reverse, `${tabLabels[tab]} · ${input.length} → ${output.length}`, 'success', {
        inputText: input, outputText: output, metadata: { tab, direction, charset, asciiFormat }
      })
    } catch (cause) {
      setNotice(cause instanceof EncodeToolError
        ? { key: `error.${cause.code}`, values: cause.values }
        : { raw: cause instanceof Error ? cause.message : String(cause) })
      setFailed(true)
      recordOperation(direction === 'forward' ? labels.forward : labels.reverse, cause instanceof Error ? cause.message : String(cause), 'error', {
        inputText: direction === 'forward' ? pair.left : pair.right,
        metadata: { tab, direction, charset, asciiFormat }
      })
    }
  }

  async function copy(side: 'left' | 'right'): Promise<void> {
    try {
      await clipboardApi.writeText(pair[side])
      setCopiedSide(side)
      window.setTimeout(() => setCopiedSide(''), 1200)
    } catch {
      setNotice({ key: 'error.clipboard' })
      setFailed(true)
    }
  }

  function activateTab(next: EncodeTab): void {
    setTab(next)
    setNotice({ key: 'notice.ready', values: { tab: tabLabels[next] } })
    setFailed(false)
  }

  return (
    <main className="utility-workbench encode-workbench">
      <header className="utility-header">
        <div>
          <span className="eyebrow">TAURI ENCODE &amp; DECODE</span>
          <h1>{t('title')}</h1>
        </div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>

      <section className="utility-toolbar encode-toolbar" aria-label={t('aria.types')}>
        <div className="utility-segments" role="tablist">
          {encodeTabs.map((item) => (
            <button
              className={tab === item ? 'utility-segment utility-segment--active' : 'utility-segment'}
              type="button"
              role="tab"
              aria-selected={tab === item}
              key={item}
              onClick={() => activateTab(item)}
            >
              {tabLabels[item]}
            </button>
          ))}
        </div>
        {tab === 'url' && (
          <label className="utility-select">
            {t('field.charset')}
            <select value={charset} onChange={(event) => setCharset(event.target.value as UrlCharset)}>
              <option value="utf-8">UTF-8</option>
              <option value="gb2312">GB2312</option>
            </select>
          </label>
        )}
        {tab === 'ascii' && (
          <label className="utility-select">
            {t('field.codePointFormat')}
            <select
              value={asciiFormat}
              onChange={(event) => setAsciiFormat(event.target.value as AsciiFormat)}
            >
              <option value="decimal">{t('option.decimal')}</option>
              <option value="hex">{t('option.hexadecimal')}</option>
            </select>
          </label>
        )}
        <button
          className="icon-button encode-clear"
          type="button"
          aria-label={t('action.clear')}
          onClick={() => updatePair({ left: '', right: '' })}
        >
          <Eraser />
        </button>
      </section>

      <section className="encode-layout">
        <EditorPane
          label={labels.left}
          value={pair.left}
          copied={copiedSide === 'left'}
          onChange={(left) => updatePair({ left })}
          onCopy={() => void copy('left')}
        />
        <div className="encode-actions">
          <ArrowUpDown className="encode-actions__mark" />
          <button
            className="primary-button"
            type="button"
            disabled={!pair.left}
            onClick={() => convert('forward')}
          >
            <ArrowRight />{labels.forward}
          </button>
          <button
            className="secondary-button"
            type="button"
            disabled={!pair.right}
            onClick={() => convert('reverse')}
          >
            <ArrowLeft />{labels.reverse}
          </button>
        </div>
        <EditorPane
          label={labels.right}
          value={pair.right}
          copied={copiedSide === 'right'}
          onChange={(right) => updatePair({ right })}
          onCopy={() => void copy('right')}
        />
      </section>

      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
    </main>
  )
}

function EditorPane({ label, value, copied, onChange, onCopy }: {
  label: string
  value: string
  copied: boolean
  onChange(value: string): void
  onCopy(): void
}) {
  const { t } = useLocalizedMessages(encodeMessages)
  return (
    <section className="utility-editor-card encode-pane">
      <header>
        <span>{label}</span>
        <button className="utility-copy" type="button" onClick={onCopy}>
          {copied ? <Clipboard /> : <Copy />}{copied ? t('action.copied') : t('action.copy')}
        </button>
      </header>
      <CodeEditor
        ariaLabel={label}
        value={value}
        onChange={onChange}
        className="utility-code-editor"
      />
      <footer>{t('pane.metrics', { characters: value.length, bytes: new TextEncoder().encode(value).length })}</footer>
    </section>
  )
}

function labelsFor(tab: EncodeTab, t: (key: EncodeMessageKey) => string): {
  left: string
  right: string
  forward: string
  reverse: string
} {
  return {
    left: t(`${tab}.left` as EncodeMessageKey),
    right: t(`${tab}.right` as EncodeMessageKey),
    forward: t(`${tab}.forward` as EncodeMessageKey),
    reverse: t(`${tab}.reverse` as EncodeMessageKey)
  }
}
