import { AlignCenter, AlignLeft, Maximize2, Minimize2, Sparkles, Sun } from 'lucide-react'
import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { useToolActivity } from '@/shared/components/ToolActivity'
import { ToolPageHeader } from '@/shared/components/ToolPage'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'

const storageKey = 'mootool.message-board.preferences.v1'
const maxMessageLength = 80

const presets: Array<{ key: MessageKey; theme: BoardTheme }> = [
  { key: 'messageBoard.preset.away', theme: 'sunbeam' },
  { key: 'messageBoard.preset.closed', theme: 'coral' },
  { key: 'messageBoard.preset.rest', theme: 'paper' },
  { key: 'messageBoard.preset.busy', theme: 'cobalt' },
  { key: 'messageBoard.preset.meeting', theme: 'forest' },
  { key: 'messageBoard.preset.quiet', theme: 'midnight' },
  { key: 'messageBoard.preset.maintenance', theme: 'cobalt' },
  { key: 'messageBoard.preset.call', theme: 'forest' }
]

const themes: Array<{ id: BoardTheme; labelKey: MessageKey; colors: [string, string] }> = [
  { id: 'sunbeam', labelKey: 'messageBoard.theme.sunbeam', colors: ['#f4ce57', '#183832'] },
  { id: 'coral', labelKey: 'messageBoard.theme.coral', colors: ['#f36b55', '#fff7ec'] },
  { id: 'cobalt', labelKey: 'messageBoard.theme.cobalt', colors: ['#3459d4', '#f3f5ff'] },
  { id: 'forest', labelKey: 'messageBoard.theme.forest', colors: ['#0f4a3a', '#e8f0c2'] },
  { id: 'paper', labelKey: 'messageBoard.theme.paper', colors: ['#efe9dc', '#29241f'] },
  { id: 'midnight', labelKey: 'messageBoard.theme.midnight', colors: ['#151821', '#e8f0ff'] }
]

type BoardTheme = 'sunbeam' | 'coral' | 'cobalt' | 'forest' | 'paper' | 'midnight'
type BoardAlignment = 'left' | 'center'
type StoredPreferences = {
  message: string
  theme: BoardTheme
  alignment: BoardAlignment
  size: number
}

export function MessageBoardTool() {
  const { t } = useI18n()
  const toolActive = useToolActivity()
  const initialPreferences = useRef(readPreferences(t('messageBoard.preset.away')))
  const [message, setMessage] = useState(initialPreferences.current.message)
  const [theme, setTheme] = useState<BoardTheme>(initialPreferences.current.theme)
  const [alignment, setAlignment] = useState<BoardAlignment>(initialPreferences.current.alignment)
  const [size, setSize] = useState(initialPreferences.current.size)
  const [presenting, setPresenting] = useState(false)
  const [displayAwake, setDisplayAwake] = useState(false)
  const messageFrameRef = useRef<HTMLDivElement>(null)
  const messageRef = useRef<HTMLDivElement>(null)
  const visibleMessage = message.trim() || t('messageBoard.empty')

  useLayoutEffect(() => {
    const frame = messageFrameRef.current
    const label = messageRef.current
    if (!frame || !label) return

    const fitMessage = () => {
      const availableHeight = frame.clientHeight
      const availableWidth = frame.clientWidth
      if (availableHeight <= 0 || availableWidth <= 0) return

      let low = 26
      let high = Math.max(low, Math.min(280, Math.round(availableHeight * 0.72 * size / 100)))
      while (low < high) {
        const candidate = Math.ceil((low + high) / 2)
        label.style.fontSize = `${candidate}px`
        const fits = label.scrollHeight <= availableHeight && label.scrollWidth <= availableWidth
        if (fits) low = candidate
        else high = candidate - 1
      }
      label.style.fontSize = `${low}px`
    }

    fitMessage()
    const observer = new ResizeObserver(fitMessage)
    observer.observe(frame)
    return () => observer.disconnect()
  }, [alignment, presenting, size, visibleMessage])

  useEffect(() => {
    window.localStorage.setItem(storageKey, JSON.stringify({ message, theme, alignment, size } satisfies StoredPreferences))
  }, [alignment, message, size, theme])

  useEffect(() => {
    if (!presenting) return
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key !== 'Escape') return
      event.preventDefault()
      event.stopImmediatePropagation()
      setPresenting(false)
    }
    window.addEventListener('keydown', handleEscape, true)
    return () => window.removeEventListener('keydown', handleEscape, true)
  }, [presenting])

  useEffect(() => {
    if (!toolActive) setPresenting(false)
  }, [toolActive])

  useEffect(() => {
    if (!presenting || !toolActive) {
      setDisplayAwake(false)
      return
    }

    let disposed = false
    void window.mootool.setPreventDisplaySleep(true)
      .then((active) => {
        if (!disposed) setDisplayAwake(active)
      })
      .catch(() => {
        if (!disposed) setDisplayAwake(false)
      })

    return () => {
      disposed = true
      setDisplayAwake(false)
      void window.mootool.setPreventDisplaySleep(false)
    }
  }, [presenting, toolActive])

  function selectPreset(preset: (typeof presets)[number]): void {
    setMessage(t(preset.key))
    setTheme(preset.theme)
  }

  return (
    <section className={presenting ? 'tool-page message-board-tool message-board-tool--presenting' : 'tool-page message-board-tool'}>
      <ToolPageHeader
        title={t('messageBoard.title')}
        actions={(
          <button className="toolbar-button toolbar-button--primary" type="button" onClick={() => setPresenting((value) => !value)}>
            {presenting ? <Minimize2 size={15} /> : <Maximize2 size={15} />}
            {presenting ? t('messageBoard.exitDisplay') : t('messageBoard.display')}
          </button>
        )}
      />

      <div className="message-board-workspace">
        <aside className="message-board-controls">
          <section className="message-board-control-section message-board-composer">
            <header>
              <label htmlFor="message-board-input">{t('messageBoard.message')}</label>
              <span>{message.length}/{maxMessageLength}</span>
            </header>
            <textarea
              id="message-board-input"
              value={message}
              maxLength={maxMessageLength}
              rows={3}
              placeholder={t('messageBoard.placeholder')}
              onChange={(event) => setMessage(event.target.value)}
            />
            <p>{t('messageBoard.messageHint')}</p>
          </section>

          <section className="message-board-control-section">
            <header>
              <h2>{t('messageBoard.presets')}</h2>
              <Sparkles size={14} aria-hidden="true" />
            </header>
            <div className="message-board-presets">
              {presets.map((preset) => {
                const label = t(preset.key)
                return (
                  <button
                    className={message === label ? 'message-board-preset message-board-preset--active' : 'message-board-preset'}
                    type="button"
                    key={preset.key}
                    onClick={() => selectPreset(preset)}
                  >
                    <span className={`message-board-preset__dot message-board-preset__dot--${preset.theme}`} />
                    {label}
                  </button>
                )
              })}
            </div>
          </section>

          <section className="message-board-control-section">
            <header><h2>{t('messageBoard.style')}</h2></header>
            <div className="message-board-themes">
              {themes.map((item) => (
                <button
                  className={theme === item.id ? 'message-board-theme message-board-theme--active' : 'message-board-theme'}
                  type="button"
                  aria-label={t(item.labelKey)}
                  aria-pressed={theme === item.id}
                  key={item.id}
                  onClick={() => setTheme(item.id)}
                >
                  <span style={{ background: item.colors[0] }} />
                  <span style={{ background: item.colors[1] }} />
                </button>
              ))}
            </div>

            <div className="message-board-format-row">
              <label htmlFor="message-board-size">
                <span>{t('messageBoard.size')}</span>
                <output>{size}%</output>
              </label>
              <input
                id="message-board-size"
                type="range"
                min="70"
                max="130"
                step="5"
                value={size}
                onChange={(event) => setSize(Number(event.target.value))}
              />
              <div className="message-board-alignment" aria-label={t('messageBoard.alignment')}>
                <button
                  type="button"
                  className={alignment === 'left' ? 'message-board-alignment__active' : ''}
                  aria-label={t('messageBoard.alignLeft')}
                  aria-pressed={alignment === 'left'}
                  onClick={() => setAlignment('left')}
                >
                  <AlignLeft size={15} />
                </button>
                <button
                  type="button"
                  className={alignment === 'center' ? 'message-board-alignment__active' : ''}
                  aria-label={t('messageBoard.alignCenter')}
                  aria-pressed={alignment === 'center'}
                  onClick={() => setAlignment('center')}
                >
                  <AlignCenter size={15} />
                </button>
              </div>
            </div>
          </section>
        </aside>

        <main className={`message-board-stage message-board-stage--${theme} message-board-stage--${alignment}`}>
          <div className="message-board-stage__orb message-board-stage__orb--one" />
          <div className="message-board-stage__orb message-board-stage__orb--two" />
          <header className="message-board-stage__header">
            <span><i />{t('messageBoard.badge')}</span>
            {displayAwake ? (
              <small className="message-board-stage__awake" role="status">
                <Sun size={12} aria-hidden="true" />{t('messageBoard.keepAwake')}
              </small>
            ) : <small>{t('messageBoard.badgeEnglish')}</small>}
          </header>
          <div className="message-board-stage__message-frame" ref={messageFrameRef}>
            <div className="message-board-stage__message" ref={messageRef}>{visibleMessage}</div>
          </div>
          <footer>
            <span>{t('messageBoard.footer')}</span>
            <i />
          </footer>
          {presenting && (
            <button className="message-board-stage__exit" type="button" onClick={() => setPresenting(false)}>
              <Minimize2 size={15} />{t('messageBoard.exitHint')}
            </button>
          )}
        </main>
      </div>
    </section>
  )
}

function readPreferences(fallbackMessage: string): StoredPreferences {
  const fallback: StoredPreferences = { message: fallbackMessage, theme: 'sunbeam', alignment: 'center', size: 100 }
  try {
    const parsed = JSON.parse(window.localStorage.getItem(storageKey) ?? '') as Partial<StoredPreferences>
    const validTheme = themes.some((theme) => theme.id === parsed.theme)
    const validAlignment = parsed.alignment === 'left' || parsed.alignment === 'center'
    return {
      message: typeof parsed.message === 'string' ? parsed.message.slice(0, maxMessageLength) : fallback.message,
      theme: validTheme ? parsed.theme as BoardTheme : fallback.theme,
      alignment: validAlignment ? parsed.alignment as BoardAlignment : fallback.alignment,
      size: typeof parsed.size === 'number' ? Math.min(130, Math.max(70, parsed.size)) : fallback.size
    }
  } catch {
    return fallback
  }
}
