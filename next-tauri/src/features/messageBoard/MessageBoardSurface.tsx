import {
  AlignCenter,
  AlignLeft,
  CheckCircle2,
  Maximize2,
  Minimize2,
  Save,
  Sparkles,
  Sun,
  Trash2,
  TriangleAlert
} from 'lucide-react'
import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useLocalizedMessages, type LocalizedMessageKey } from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import { nativeDesktopApi } from '../../platform/api/nativeDesktopApi'
import type { BoardMessage, BoardMessageColor } from '../../platform/contracts/localData'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
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

const storageKey = 'mootool-next-tauri.message-board.v1'
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
  { messageKey: 'preset.break', theme: 'paper' },
  { messageKey: 'preset.focus', theme: 'cobalt' },
  { messageKey: 'preset.meeting', theme: 'forest' },
  { messageKey: 'preset.quiet', theme: 'midnight' }
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
  const dialog = useDesktopDialog()
  const { t } = useLocalizedMessages(messageBoardMessages)
  const initial = useRef(readPreferences())
  const [message, setMessage] = useState(initial.current.message)
  const [theme, setTheme] = useState(initial.current.theme)
  const [alignment, setAlignment] = useState(initial.current.alignment)
  const [size, setSize] = useState(initial.current.size)
  const [savedMessages, setSavedMessages] = useState<BoardMessage[]>([])
  const [presenting, setPresenting] = useState(false)
  const [displayAwake, setDisplayAwake] = useState(false)
  const [notice, setNotice] = useState<BoardNotice>({ key: 'notice.ready' })
  const [failed, setFailed] = useState(false)
  const [busy, setBusy] = useState(false)
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
      presenting,
      saved: savedMessages.length
    }),
    summary: `${theme} · ${alignment} · ${message.length}/${maximumMessageLength}`
  }), [alignment, message, presenting, savedMessages.length, size, theme])
  const { sessionId, reportError } = useToolSessionReport('message-board', session.digest, session.summary)

  useEffect(() => {
    void localDataApi.listMessages()
      .then(setSavedMessages)
      .catch((cause) => fail(cause))
  }, [])

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

  async function savePreset(): Promise<void> {
    if (!message.trim()) {
      setFailed(true)
      setNotice({ key: 'error.empty' })
      return
    }
    const duplicate = savedMessages.find((item) => item.content === message.trim())
    if (duplicate) {
      setFailed(false)
      setNotice({ key: 'notice.duplicate' })
      return
    }
    setBusy(true)
    try {
      const now = Date.now()
      const saved = await localDataApi.saveMessage({
        id: crypto.randomUUID(),
        content: message.trim(),
        color: themeToColor(theme),
        pinned: false,
        createdAt: now,
        updatedAt: now
      })
      setSavedMessages([saved, ...savedMessages])
      succeed('notice.saved')
    } catch (cause) {
      fail(cause)
    } finally {
      setBusy(false)
    }
  }

  async function removePreset(item: BoardMessage): Promise<void> {
    if (!await dialog.confirm(t('confirm.delete'), { dangerous: true })) return
    setBusy(true)
    try {
      await localDataApi.deleteMessage(item.id)
      setSavedMessages(savedMessages.filter((saved) => saved.id !== item.id))
      succeed('notice.deleted')
    } catch (cause) {
      fail(cause)
    } finally {
      setBusy(false)
    }
  }

  function useSaved(item: BoardMessage): void {
    setMessage(item.content.slice(0, maximumMessageLength))
    setTheme(colorToTheme(item.color))
    succeed('notice.applied')
  }

  function succeed(key: BoardMessageKey): void {
    setNotice({ key })
    setFailed(false)
  }

  function fail(cause: unknown): void {
    setNotice({ raw: cause instanceof Error ? cause.message : String(cause) })
    setFailed(true)
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
              <input type="range" min="60" max="140" value={size} onChange={(event) => setSize(Number(event.target.value))} />
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

        <aside className="message-display-saved">
          <header>
            <div><strong>{t('saved.title')}</strong><span>{savedMessages.length}</span></div>
            <button className="secondary-button" type="button" disabled={busy || !message.trim()} onClick={() => void savePreset()}><Save />{t('action.saveCurrent')}</button>
          </header>
          <div>
            {savedMessages.map((item) => (
              <article key={item.id}>
                <button type="button" onClick={() => useSaved(item)}>{item.content}</button>
                <button type="button" title={t('action.delete')} disabled={busy} onClick={() => void removePreset(item)}><Trash2 /></button>
              </article>
            ))}
            {!savedMessages.length && <p>{t('saved.empty')}</p>}
          </div>
        </aside>
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
  let low = 24
  let high = Math.max(low, Math.min(300, Math.round(availableHeight * 0.72 * size / 100)))
  while (low < high) {
    const candidate = Math.ceil((low + high) / 2)
    label.style.fontSize = `${candidate}px`
    if (label.scrollHeight <= availableHeight && label.scrollWidth <= availableWidth) low = candidate
    else high = candidate - 1
  }
  label.style.fontSize = `${low}px`
}

function readPreferences(): BoardPreferences {
  try {
    const value = JSON.parse(window.localStorage.getItem(storageKey) ?? '{}') as Partial<BoardPreferences>
    return {
      message: typeof value.message === 'string' ? value.message.slice(0, maximumMessageLength) : defaultPreferences.message,
      theme: themes.some((item) => item.id === value.theme) ? value.theme as BoardTheme : defaultPreferences.theme,
      alignment: value.alignment === 'left' || value.alignment === 'center' ? value.alignment : defaultPreferences.alignment,
      size: typeof value.size === 'number' ? Math.min(140, Math.max(60, value.size)) : defaultPreferences.size
    }
  } catch {
    return defaultPreferences
  }
}

function themeToColor(theme: BoardTheme): BoardMessageColor {
  const colors: Record<BoardTheme, BoardMessageColor> = {
    sunbeam: 'yellow',
    coral: 'pink',
    cobalt: 'blue',
    forest: 'green',
    paper: 'gray',
    midnight: 'purple'
  }
  return colors[theme]
}

function colorToTheme(color: BoardMessageColor): BoardTheme {
  const themesByColor: Record<BoardMessageColor, BoardTheme> = {
    yellow: 'sunbeam',
    pink: 'coral',
    blue: 'cobalt',
    green: 'forest',
    gray: 'paper',
    purple: 'midnight'
  }
  return themesByColor[color]
}
