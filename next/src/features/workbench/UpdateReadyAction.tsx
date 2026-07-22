import DOMPurify from 'dompurify'
import { FolderOpen, RefreshCw } from 'lucide-react'
import { marked } from 'marked'
import {
  useCallback,
  useEffect,
  useEffectEvent,
  useId,
  useMemo,
  useRef,
  useState,
  type CSSProperties
} from 'react'
import { createPortal } from 'react-dom'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useUpdateState } from '@/shared/hooks/useUpdateState'
import { useI18n } from '@/shared/i18n/I18nProvider'

const notesGap = 12

type UpdateReadyActionProps = {
  onNotesVisibilityChange?: (visible: boolean) => void
}

export function UpdateReadyAction({ onNotesVisibilityChange }: UpdateReadyActionProps) {
  const { t } = useI18n()
  const toast = useToast()
  const update = useUpdateState()
  const [installing, setInstalling] = useState(false)
  const [notesOpen, setNotesOpen] = useState(false)
  const [position, setPosition] = useState<CSSProperties>({})
  const triggerRef = useRef<HTMLDivElement>(null)
  const hideTimerRef = useRef<number | null>(null)
  const notesId = useId()

  const releaseNotesHtml = useMemo(() => {
    if (!update.releaseNotes) return ''
    return DOMPurify.sanitize(marked.parse(update.releaseNotes, { async: false }) as string)
  }, [update.releaseNotes])

  const updatePosition = useCallback(() => {
    const trigger = triggerRef.current
    if (!trigger) return
    const rect = trigger.getBoundingClientRect()
    setPosition({
      left: rect.right + notesGap,
      bottom: Math.max(16, window.innerHeight - rect.bottom),
      maxHeight: Math.min(420, Math.max(180, window.innerHeight - 32))
    })
  }, [])

  const clearHideTimer = useCallback(() => {
    if (hideTimerRef.current !== null) {
      window.clearTimeout(hideTimerRef.current)
      hideTimerRef.current = null
    }
  }, [])

  const showNotes = useCallback(() => {
    if (!releaseNotesHtml) return
    clearHideTimer()
    updatePosition()
    setNotesOpen(true)
  }, [clearHideTimer, releaseNotesHtml, updatePosition])

  const hideNotes = useCallback(() => {
    clearHideTimer()
    hideTimerRef.current = window.setTimeout(() => {
      setNotesOpen(false)
      hideTimerRef.current = null
    }, 120)
  }, [clearHideTimer])

  const repositionNotes = useEffectEvent(updatePosition)

  useEffect(() => {
    if (!notesOpen) return
    const reposition = () => repositionNotes()
    window.addEventListener('resize', reposition)
    window.addEventListener('scroll', reposition, true)
    return () => {
      window.removeEventListener('resize', reposition)
      window.removeEventListener('scroll', reposition, true)
    }
  }, [notesOpen])

  useEffect(() => () => clearHideTimer(), [clearHideTimer])

  const notesVisible = notesOpen
    && update.status === 'ready'
    && Boolean(update.version)
    && Boolean(releaseNotesHtml)

  useEffect(() => {
    onNotesVisibilityChange?.(notesVisible)
  }, [notesVisible, onNotesVisibilityChange])

  if (update.status !== 'ready' || !update.version) return null

  function install(): void {
    setInstalling(true)
    void window.mootool.installUpdate().catch(() => {
      toast.error(t('settings.update.installFailed'))
    }).finally(() => setInstalling(false))
  }

  return (
    <div
      className="sidebar-update-ready"
      ref={triggerRef}
      onMouseEnter={showNotes}
      onMouseLeave={hideNotes}
      onFocusCapture={showNotes}
      onBlurCapture={hideNotes}
    >
      <button
        className="sidebar-update-action"
        type="button"
        disabled={installing}
        onClick={install}
        aria-describedby={notesOpen && releaseNotesHtml ? notesId : undefined}
      >
        {update.installMode === 'manual'
          ? <FolderOpen size={17} aria-hidden="true" />
          : <RefreshCw size={17} aria-hidden="true" />}
        <span>
          <strong>{t(update.installMode === 'manual' ? 'settings.update.manualInstall' : 'settings.update.installRestart')}</strong>
          <small>{t('settings.update.ready', { version: update.version })}</small>
        </span>
      </button>
      {notesOpen && releaseNotesHtml && createPortal(
        <aside
          className="sidebar-update-notes"
          id={notesId}
          role="tooltip"
          style={position}
          onMouseEnter={showNotes}
          onMouseLeave={hideNotes}
        >
          <header className="sidebar-update-notes__header">
            <strong>{t('settings.update.whatsNew')}</strong>
            <span>{t('settings.update.ready', { version: update.version })}</span>
          </header>
          <article
            className="sidebar-update-notes__body"
            dangerouslySetInnerHTML={{ __html: releaseNotesHtml }}
          />
        </aside>,
        document.body
      )}
    </div>
  )
}
