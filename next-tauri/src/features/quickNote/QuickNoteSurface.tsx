import {
  CheckCircle2,
  Columns2,
  CopyPlus,
  Download,
  Eye,
  FileInput,
  FilePlus2,
  NotebookPen,
  Pin,
  PinOff,
  Save,
  Search,
  Trash2,
  TriangleAlert,
  X
} from 'lucide-react'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import type { QuickNote } from '../../platform/contracts/localData'
import { CodeEditor } from '../../shared/CodeEditor'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { quickNoteMessages } from './quickNoteMessages'

interface Draft { content: string; title: string }
type ViewMode = 'editor' | 'split' | 'preview'
type QuickNoteMessageKey = LocalizedMessageKey<typeof quickNoteMessages>
type Notice = { key: QuickNoteMessageKey; values?: MessageValues } | { raw: string }

export function QuickNoteSurface() {
  const { t, locale } = useLocalizedMessages(quickNoteMessages)
  const [notes, setNotes] = useState<QuickNote[]>([])
  const [activeId, setActiveId] = useState('')
  const [draft, setDraft] = useState<Draft>({ title: '', content: '' })
  const [query, setQuery] = useState('')
  const [viewMode, setViewMode] = useState<ViewMode>('editor')
  const [dirty, setDirty] = useState(false)
  const [notice, setNotice] = useState<Notice>({ key: 'notice.loading' })
  const [failed, setFailed] = useState(false)
  const [busy, setBusy] = useState(false)
  const [findVisible, setFindVisible] = useState(false)
  const [find, setFind] = useState('')
  const [replace, setReplace] = useState('')
  const revisionRef = useRef(0)
  const activeNote = notes.find((note) => note.id === activeId)
  const visibleNotes = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return needle ? notes.filter((note) => `${note.title}\n${note.content}`.toLowerCase().includes(needle)) : notes
  }, [notes, query])
  const matchCount = useMemo(() => countTextMatches(draft.content, find), [draft.content, find])
  const stats = useMemo(() => documentStats(draft.content), [draft.content])
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({ count: notes.length, activeId, titleHash: contentFingerprint(draft.title), contentHash: contentFingerprint(draft.content), dirty }),
    summary: t('session.summary', { count: notes.length, state: t(dirty ? 'session.unsaved' : 'session.saved') })
  }), [activeId, dirty, draft.content, draft.title, notes.length, t])
  const { sessionId, reportError } = useToolSessionReport('quick-note', session.digest, session.summary)
  const recordOperation = useOperationHistory('quick-note')

  useEffect(() => {
    void localDataApi.listNotes().then((loaded) => {
      setNotes(loaded)
      if (loaded[0]) applyNote(loaded[0])
      else applyEmptyDraft()
      setNotice({ key: loaded.length ? 'notice.loaded' : 'notice.first', values: loaded.length ? { count: loaded.length } : undefined })
      setFailed(false)
    }).catch(fail)
  }, [])

  useEffect(() => {
    if (!dirty || busy || (!draft.title.trim() && !draft.content.trim())) return
    const timer = window.setTimeout(() => { void persistDraft(false) }, 650)
    return () => window.clearTimeout(timer)
  }, [activeId, busy, dirty, draft.content, draft.title])

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 's') {
        event.preventDefault()
        void persistDraft(true)
      }
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'f') {
        event.preventDefault()
        setFindVisible(true)
      }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  })

  function applyNote(note: QuickNote): void {
    revisionRef.current += 1
    setActiveId(note.id)
    setDraft({ title: note.title, content: note.content })
    setDirty(false)
  }

  function applyEmptyDraft(): void {
    revisionRef.current += 1
    setActiveId('')
    setDraft({ title: '', content: '' })
    setDirty(false)
    setViewMode('editor')
  }

  async function selectNote(note: QuickNote): Promise<void> {
    if (dirty && !await persistDraft(false)) return
    applyNote(note)
  }

  async function createDraft(): Promise<void> {
    if (dirty && !await persistDraft(false)) return
    applyEmptyDraft()
    succeed('notice.created')
  }

  function patchDraft(patch: Partial<Draft>): void {
    revisionRef.current += 1
    setDraft((current) => ({ ...current, ...patch }))
    setDirty(true)
  }

  async function persistDraft(announce: boolean): Promise<boolean> {
    if (busy) return false
    if (!draft.title.trim() && !draft.content.trim()) {
      if (announce) failMessage('notice.empty')
      return false
    }
    setBusy(true)
    try {
      const revision = revisionRef.current
      const snapshot = draft
      const now = Date.now()
      const note: QuickNote = {
        id: activeNote?.id ?? crypto.randomUUID(),
        title: snapshot.title.trim() || firstContentLine(snapshot.content) || t('note.untitled'),
        content: snapshot.content,
        pinned: activeNote?.pinned ?? false,
        createdAt: activeNote?.createdAt ?? now,
        updatedAt: now
      }
      const saved = await localDataApi.saveNote(note)
      setNotes((items) => sortNotes([saved, ...items.filter((item) => item.id !== saved.id)]))
      setActiveId(saved.id)
      const unchanged = revisionRef.current === revision
      if (unchanged) {
        setDraft({ title: saved.title, content: saved.content })
        setDirty(false)
      }
      if (announce) {
        succeed(unchanged ? 'notice.saved' : 'notice.savedPending')
        recordOperation(t('operation.save'), saved.title, 'success')
      } else {
        succeed(unchanged ? 'notice.autoSaved' : 'notice.autoSavedPending')
      }
      return true
    } catch (cause) {
      fail(cause)
      return false
    } finally { setBusy(false) }
  }

  async function togglePinned(): Promise<void> {
    if (!activeNote || busy) return
    setBusy(true)
    try {
      const revision = revisionRef.current
      const snapshot = draft
      const saved = await localDataApi.saveNote({
        ...activeNote,
        title: snapshot.title.trim() || firstContentLine(snapshot.content) || t('note.untitled'),
        content: snapshot.content,
        pinned: !activeNote.pinned,
        updatedAt: Date.now()
      })
      setNotes((items) => sortNotes([saved, ...items.filter((item) => item.id !== saved.id)]))
      const unchanged = revisionRef.current === revision
      if (unchanged) {
        setDraft({ title: saved.title, content: saved.content })
        setDirty(false)
      }
      succeed(saved.pinned
        ? unchanged ? 'notice.pinned' : 'notice.pinnedPending'
        : unchanged ? 'notice.unpinned' : 'notice.unpinnedPending')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function duplicate(): Promise<void> {
    if (!activeNote) return
    try {
      const now = Date.now()
      const saved = await localDataApi.saveNote({
        id: crypto.randomUUID(),
        title: t('duplicate.title', { title: draft.title || activeNote.title || t('note.untitled') }),
        content: draft.content,
        pinned: false,
        createdAt: now,
        updatedAt: now
      })
      setNotes((items) => sortNotes([saved, ...items]))
      applyNote(saved)
      succeed('notice.duplicated')
    } catch (cause) { fail(cause) }
  }

  async function remove(): Promise<void> {
    if (!activeNote || !window.confirm(t('confirm.delete', { title: activeNote.title }))) return
    setBusy(true)
    try {
      await localDataApi.deleteNote(activeNote.id)
      const next = notes.filter((note) => note.id !== activeNote.id)
      setNotes(next)
      if (next[0]) applyNote(next[0]); else applyEmptyDraft()
      succeed('notice.deleted')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function importNote(): Promise<void> {
    if (dirty && !await persistDraft(false)) return
    try {
      const file = await userFilesApi.pickText()
      if (!file) return
      revisionRef.current += 1
      setActiveId('')
      setDraft({ title: file.name.replace(/\.[^.]+$/, '') || t('import.title'), content: file.content })
      setDirty(true)
      setViewMode('editor')
      succeed('notice.imported', { file: file.name })
    } catch (cause) { fail(cause) }
  }

  async function exportNote(): Promise<void> {
    if (!draft.content && !draft.title) return
    try {
      const path = await userFilesApi.exportText(`${safeFileName(draft.title || 'note')}.md`, draft.content)
      if (path) succeed('notice.exported', { path })
    } catch (cause) { fail(cause) }
  }

  function replaceText(all: boolean): void {
    if (!find) return
    if (all) {
      if (!matchCount) return failMessage('notice.noMatches')
      patchDraft({ content: draft.content.split(find).join(replace) })
      succeed('notice.replaced', { count: matchCount })
      return
    }
    const index = draft.content.indexOf(find)
    if (index < 0) return failMessage('notice.noMatches')
    patchDraft({ content: `${draft.content.slice(0, index)}${replace}${draft.content.slice(index + find.length)}` })
    succeed('notice.replaced', { count: 1 })
  }

  function succeed(key: QuickNoteMessageKey, values?: MessageValues): void { setNotice({ key, values }); setFailed(false) }
  function failMessage(key: QuickNoteMessageKey, values?: MessageValues): void { setNotice({ key, values }); setFailed(true) }
  function fail(cause: unknown): void { setNotice({ raw: cause instanceof Error ? cause.message : String(cause) }); setFailed(true) }

  return (
    <main className="utility-workbench quick-note-workbench">
      <header className="utility-header">
        <div><span className="eyebrow">TAURI LOCAL NOTEBOOK</span><h1>{t('title')}</h1></div>
        <span className="utility-session">{t('session.label')} <code>{sessionId}</code></span>
      </header>
      <section className="quick-note-main-toolbar">
        <button type="button" onClick={() => void importNote()}><FileInput />{t('toolbar.import')}</button>
        <button type="button" disabled={!draft.title && !draft.content} onClick={() => void exportNote()}><Download />{t('toolbar.export')}</button>
        <button type="button" disabled={!activeNote} onClick={() => void duplicate()}><CopyPlus />{t('toolbar.duplicate')}</button>
        <span />
        <button type="button" onClick={() => setFindVisible((value) => !value)}><Search />{t('toolbar.findReplace')}</button>
      </section>
      <section className="quick-note-layout">
        <aside className="quick-note-sidebar">
          <div className="quick-note-search">
            <Search />
            <input value={query} placeholder={t('search.placeholder')} onChange={(event) => setQuery(event.target.value)} />
            <button type="button" aria-label={t('search.new')} onClick={() => void createDraft()}><FilePlus2 /></button>
          </div>
          <div className="quick-note-list">
            {visibleNotes.map((note) => (
              <button className={note.id === activeId ? 'quick-note-item quick-note-item--active' : 'quick-note-item'} type="button" key={note.id} onClick={() => void selectNote(note)}>
                <strong>{note.title || t('note.untitled')}</strong>
                <span>{note.content.replace(/\s+/g, ' ').slice(0, 70) || t('note.empty')}</span>
                <small>{note.pinned && <Pin />} {formatTime(note.updatedAt, locale)}</small>
              </button>
            ))}
            {!visibleNotes.length && <div className="quick-note-empty"><NotebookPen /><span>{t(query ? 'note.noMatches' : 'note.none')}</span></div>}
          </div>
        </aside>
        <section className="quick-note-editor">
          <header>
            <input aria-label={t('editor.title')} value={draft.title} placeholder={t('note.untitled')} onChange={(event) => patchDraft({ title: event.target.value })} />
            <div className="utility-segments">
              <button className={viewMode === 'editor' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setViewMode('editor')}>{t('view.editor')}</button>
              <button className={viewMode === 'split' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setViewMode('split')}><Columns2 />{t('view.split')}</button>
              <button className={viewMode === 'preview' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setViewMode('preview')}><Eye />{t('view.preview')}</button>
            </div>
            <button className="secondary-button" type="button" disabled={!activeNote || busy} onClick={() => void togglePinned()}>{activeNote?.pinned ? <PinOff /> : <Pin />}{t(activeNote?.pinned ? 'action.unpin' : 'action.pin')}</button>
            <button className="secondary-button quick-note-delete" type="button" disabled={!activeNote || busy} onClick={() => void remove()}><Trash2 />{t('action.delete')}</button>
            <button className="primary-button" type="button" disabled={!dirty || busy} onClick={() => void persistDraft(true)}><Save />{t(busy ? 'action.saving' : 'action.save')}</button>
          </header>
          {findVisible && (
            <div className="quick-note-find">
              <input autoFocus value={find} placeholder={t('find.placeholder')} onChange={(event) => setFind(event.target.value)} />
              <input value={replace} placeholder={t('replace.placeholder')} onChange={(event) => setReplace(event.target.value)} />
              <span>{find ? t('find.matches', { count: matchCount }) : ''}</span>
              <button type="button" disabled={!find} onClick={() => replaceText(false)}>{t('action.replace')}</button>
              <button type="button" disabled={!find} onClick={() => replaceText(true)}>{t('action.replaceAll')}</button>
              <button type="button" aria-label={t('action.closeFind')} onClick={() => setFindVisible(false)}><X /></button>
            </div>
          )}
          <div className={`quick-note-content quick-note-content--${viewMode}`}>
            {viewMode !== 'preview' && <CodeEditor ariaLabel={t('editor.content')} value={draft.content} onChange={(content) => patchDraft({ content })} className="utility-code-editor" lineWrapping />}
            {viewMode !== 'editor' && <MarkdownPreview value={draft.content} />}
          </div>
          <footer>
            <span>{t('stats.lines', { count: stats.lines })}</span>
            <span>{t('stats.words', { count: stats.words })}</span>
            <span>{t('stats.characters', { count: stats.characters })}</span>
            <span>{t(dirty ? 'stats.pending' : 'stats.saved')}</span>
          </footer>
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

function MarkdownPreview({ value }: { value: string }) {
  const blocks = markdownBlocks(value)
  return <article className="quick-note-preview">{blocks.map((block, index) => {
    if (block.kind === 'code') return <pre key={index}><code>{block.value}</code></pre>
    const heading = /^(#{1,3})\s+(.+)$/.exec(block.value)
    if (heading) { const Tag = `h${heading[1].length}` as 'h1' | 'h2' | 'h3'; return <Tag key={index}>{heading[2]}</Tag> }
    if (/^\s*[-*]\s+/.test(block.value)) return <li key={index}>{block.value.replace(/^\s*[-*]\s+/, '')}</li>
    if (/^>\s?/.test(block.value)) return <blockquote key={index}>{block.value.replace(/^>\s?/, '')}</blockquote>
    return block.value ? <p key={index}>{block.value}</p> : <br key={index} />
  })}</article>
}

function markdownBlocks(value: string): Array<{ kind: 'text' | 'code'; value: string }> {
  const blocks: Array<{ kind: 'text' | 'code'; value: string }> = []
  let code: string[] | null = null
  for (const line of value.split(/\r?\n/)) {
    if (/^```/.test(line)) {
      if (code) { blocks.push({ kind: 'code', value: code.join('\n') }); code = null } else code = []
    } else if (code) code.push(line)
    else blocks.push({ kind: 'text', value: line })
  }
  if (code) blocks.push({ kind: 'code', value: code.join('\n') })
  return blocks
}

function sortNotes(notes: QuickNote[]): QuickNote[] { return notes.sort((left, right) => Number(right.pinned) - Number(left.pinned) || right.updatedAt - left.updatedAt) }
function firstContentLine(content: string): string { return content.split(/\r?\n/).find((line) => line.trim())?.replace(/^#+\s*/, '').slice(0, 80) ?? '' }
function formatTime(value: number, locale: string): string { return new Intl.DateTimeFormat(locale, { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value) }
function countTextMatches(value: string, query: string): number { if (!query) return 0; let count = 0; let from = 0; while ((from = value.indexOf(query, from)) >= 0) { count += 1; from += Math.max(1, query.length) } return count }
function documentStats(value: string) { return { lines: value ? value.split(/\r?\n/).length : 1, words: value.trim() ? value.trim().split(/\s+/u).length : 0, characters: [...value].length } }
function safeFileName(value: string): string { return value.trim().replace(/[\\/:*?"<>|\u0000-\u001f]/g, '-').replace(/^\.+/, '').slice(0, 120) || 'note' }
