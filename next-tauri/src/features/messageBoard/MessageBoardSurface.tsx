import {
  AlignCenter,
  AlignLeft,
  CheckCircle2,
  Maximize2,
  Minimize2,
  Sparkles,
  Sun,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import { nativeDesktopApi } from '../../platform/api/nativeDesktopApi'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { messageBoardMessages } from './messageBoardMessages'

type BoardTheme = 'sunbeam' | 'coral' | 'cobalt' | 'forest' | 'paper' | 'midnight'
type BoardAlignment = 'left' | 'center'
type BoardMessageKey = LocalizedMessageKey<typeof messageBoardMessages>
type BoardNotice = { key: BoardMessageKey } | { raw: string }

interface BoardPreferences {
  message: string
  theme: BoardTheme
  alignment: BoardAlignment
  size: number
}

const storageKey = 'mootool.message-board.preferences.v1'
const legacyStorageKey = 'mootool-next-tauri.message-board.v1'
const maximumMessageLength = 80
const defaultPreferences: BoardPreferences = {
  message: '',
  theme: 'sunbeam',
  alignment: 'center',
  size: 100
}
const presets: Array<{ messageKey: BoardMessageKey; theme: BoardTheme }> = [
  { messageKey: 'preset.away', theme: 'sunbeam' },
  { messageKey: 'preset.closed', theme: 'coral' },
  { messageKey: 'preset.rest', theme: 'paper' },
  { messageKey: 'preset.busy', theme: 'cobalt' },
  { messageKey: 'preset.meeting', theme: 'forest' },
  { messageKey: 'preset.quiet', theme: 'midnight' },
  { messageKey: 'preset.maintenance', theme: 'cobalt' },
  { messageKey: 'preset.call', theme: 'forest' }
]
const themes: Array<{ id: BoardTheme; labelKey: BoardMessageKey; colors: [string, string] }> = [
  { id: 'sunbeam', labelKey: 'theme.sunbeam', colors: ['#f4ce57', '#183832'] },
  { id: 'coral', labelKey: 'theme.coral', colors: ['#f36b55', '#fff7ec'] },
  { id: 'cobalt', labelKey: 'theme.cobalt', colors: ['#3459d4', '#f3f5ff'] },
  { id: 'forest', labelKey: 'theme.forest', colors: ['#0f4a3a', '#e8f0c2'] },
  { id: 'paper', labelKey: 'theme.paper', colors: ['#efe9dc', '#29241f'] },
  { id: 'midnight', labelKey: 'theme.midnight', colors: ['#151821', '#e8f0ff'] }
]

export function MessageBoardSurface() {
  const { t } = useLocalizedMessages(messageBoardMessages)
  const initial = useRef(readPreferences(t('preset.away')))
  const [message, setMessage] = useState(initial.current.message)
  const [theme, setTheme] = useState(initial.current.theme)
  const [alignment, setAlignment] = useState(initial.current.alignment)
  const [size, setSize] = useState(initial.current.size)
  const [presenting, setPresenting] = useState(false)
  const [displayAwake, setDisplayAwake] = useState(false)
  const [notice, setNotice] = useState<BoardNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const frameRef = useRef<HTMLDivElement>(null)
  const labelRef = useRef<HTMLDivElement>(null)
  const visibleMessage = message.trim() || t('display.empty')
  const selectedTheme = themes.find((item) => item.id === theme) ?? themes[0]
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key)
  const session = useMemo(() => ({
    digest: JSON.stringify({
      messageHash: contentFingerprint(message),
      theme,
      alignment,
      size,
      presenting
    }),
    summary: `${theme} · ${alignment} · ${message.length}/${maximumMessageLength}`
  }), [alignment, message, presenting, size, theme])
  const { sessionId, reportError } = useToolSessionReport('message-board', session.digest, session.summary)

  useEffect(() => {
    window.localStorage.setItem(storageKey, JSON.stringify({ message, theme, alignment, size }))
  }, [alignment, message, size, theme])

  useLayoutEffect(() => {
    const frame = frameRef.current
    const label = labelRef.current
    if (!frame || !label) return
    const fit = () => fitMessage(frame, label, size)
    fit()
    const observer = new ResizeObserver(fit)
    observer.observe(frame)
    return () => observer.disconnect()
  }, [alignment, presenting, size, visibleMessage])

  useEffect(() => {
    const onFullscreenChange = () => {
      if (presenting && !document.fullscreenElement) setPresenting(false)
    }
    document.addEventListener('fullscreenchange', onFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', onFullscreenChange)
  }, [presenting])

  useEffect(() => {
    if (!presenting || !window.__TAURI_INTERNALS__) {
      setDisplayAwake(false)
      return
    }
    let disposed = false
    void nativeDesktopApi.setDisplaySleepPrevention('message-board-presentation', true)
      .then((status) => {
        if (!disposed) setDisplayAwake(status.active)
      })
      .catch(() => {
        if (!disposed) setDisplayAwake(false)
      })
    return () => {
      disposed = true
      setDisplayAwake(false)
      void nativeDesktopApi.setDisplaySleepPrevention('message-board-presentation', false)
    }
  }, [presenting])

  async function enterPresentation(): Promise<void> {
    setPresenting(true)
    setFailed(false)
    setNotice({ key: 'notice.presenting' })
    try {
      if (!document.fullscreenElement) await document.documentElement.requestFullscreen()
    } catch {
      // The tool WebView still enters an edge-to-edge presentation if native fullscreen is denied.
    }
  }

  async function exitPresentation(): Promise<void> {
    setPresenting(false)
    if (document.fullscreenElement) {
      try {
        await document.exitFullscreen()
      } catch {
        // State is already restored even if the host rejects the fullscreen exit request.
      }
    }
  }

  if (presenting) {
    return (
      <main
        className={`message-display message-display--${theme} message-display--${alignment}`}
        style={{ background: selectedTheme.colors[0], color: selectedTheme.colors[1] }}
        onDoubleClick={() => void exitPresentation()}
      >
        <div className="message-display-frame" ref={frameRef}>
          <div ref={labelRef}>{visibleMessage}</div>
        </div>
        <div className="message-display-meta">
          <span>{displayAwake ? t('display.awake') : t('display.mode')}</span>
          <button type="button" onClick={() => void exitPresentation()}><Minimize2 />{t('action.exit')}</button>
        </div>
      </main>
    )
  }

  return (
    <main className="utility-workbench message-board-workbench message-board-workbench--display">
      <header className="utility-header">
        <h1 className="visually-hidden">{t('title')}</h1>
        <div className="message-display-header-actions">
          <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
          <button className="primary-button" type="button" onClick={() => void enterPresentation()}>
            <Maximize2 />{t('action.start')}
          </button>
        </div>
      </header>

      <section className="message-display-layout">
        <aside className="message-display-controls">
          <section>
            <header><Sparkles /><strong>{t('content.title')}</strong></header>
            <textarea
              value={message}
              maxLength={maximumMessageLength}
              placeholder={t('content.placeholder')}
              onChange={(event) => setMessage(event.target.value)}
            />
            <small>{message.length} / {maximumMessageLength}</small>
            <div className="message-display-preset-grid">
              {presets.map((preset) => (
                <button type="button" key={preset.messageKey} onClick={() => {
                  setMessage(t(preset.messageKey))
                  setTheme(preset.theme)
                }}>{t(preset.messageKey)}</button>
              ))}
            </div>
          </section>

          <section>
            <header><Sun /><strong>{t('style.title')}</strong></header>
            <div className="message-display-themes">
              {themes.map((item) => (
                <button
                  type="button"
                  key={item.id}
                  title={t(item.labelKey)}
                  aria-label={t(item.labelKey)}
                  aria-pressed={theme === item.id}
                  style={{ background: item.colors[0], color: item.colors[1] }}
                  onClick={() => setTheme(item.id)}
                >{theme === item.id ? '✓' : ''}</button>
              ))}
            </div>
            <div className="message-display-options">
              <button type="button" aria-pressed={alignment === 'left'} onClick={() => setAlignment('left')}><AlignLeft />{t('align.left')}</button>
              <button type="button" aria-pressed={alignment === 'center'} onClick={() => setAlignment('center')}><AlignCenter />{t('align.center')}</button>
            </div>
            <label className="message-display-size">{t('size.label')}
              <input type="range" min="70" max="130" value={size} onChange={(event) => setSize(Number(event.target.value))} />
              <span>{size}%</span>
            </label>
          </section>
        </aside>

        <section className="message-display-preview" style={{ background: selectedTheme.colors[0], color: selectedTheme.colors[1] }}>
          <div className={`message-display-frame message-display-frame--${alignment}`} ref={frameRef}>
            <div ref={labelRef}>{visibleMessage}</div>
          </div>
          <span>{t('preview.hint')}</span>
        </section>

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

function fitMessage(frame: HTMLElement, label: HTMLElement, size: number): void {
  const availableHeight = frame.clientHeight
  const availableWidth = frame.clientWidth
  if (availableHeight <= 0 || availableWidth <= 0) return
  let low = 26
  let high = Math.max(low, Math.min(280, Math.round(availableHeight * 0.72 * size / 100)))
  while (low < high) {
    const candidate = Math.ceil((low + high) / 2)
    label.style.fontSize = `${candidate}px`
    if (label.scrollHeight <= availableHeight && label.scrollWidth <= availableWidth) low = candidate
    else high = candidate - 1
  }
  label.style.fontSize = `${low}px`
}

function readPreferences(fallbackMessage: string): BoardPreferences {
  const fallback: BoardPreferences = { ...defaultPreferences, message: fallbackMessage }
  try {
    const stored = window.localStorage.getItem(storageKey) ?? window.localStorage.getItem(legacyStorageKey)
    const value = JSON.parse(stored ?? '') as Partial<BoardPreferences>
    return {
      message: typeof value.message === 'string' ? value.message.slice(0, maximumMessageLength) : fallback.message,
      theme: themes.some((item) => item.id === value.theme) ? value.theme as BoardTheme : fallback.theme,
      alignment: value.alignment === 'left' || value.alignment === 'center' ? value.alignment : fallback.alignment,
      size: typeof value.size === 'number' ? Math.min(130, Math.max(70, value.size)) : fallback.size
    }
  } catch {
    return fallback
  }
}

