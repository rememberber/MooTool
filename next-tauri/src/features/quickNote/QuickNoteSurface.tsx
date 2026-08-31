import {
  Bold,
  CheckCircle2,
  Code2,
  Columns2,
  CopyPlus,
  Download,
  Eye,
  Italic,
  List,
  FileInput,
  FilePlus2,
  NotebookPen,
  Paperclip,
  Palette,
  Pin,
  PinOff,
  Save,
  Search,
  Tag,
  Trash2,
  TriangleAlert,
  X
} from 'lucide-react'
import type { EditorView } from '@codemirror/view'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import { quickNoteAttachmentApi } from '../../platform/api/quickNoteAttachmentApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import type { QuickNote, QuickNoteAttachment } from '../../platform/contracts/localData'
import { CodeEditor } from '../../shared/CodeEditor'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { quickNoteMessages } from './quickNoteMessages'

interface Draft { content: string; title: string; tags: string[]; color: QuickNote['color'] }
type ViewMode = 'editor' | 'split' | 'preview'
type QuickNoteMessageKey = LocalizedMessageKey<typeof quickNoteMessages>
type Notice = { key: QuickNoteMessageKey; values?: MessageValues } | { raw: string }

export function QuickNoteSurface() {
  const dialog = useDesktopDialog()
  const { t, locale } = useLocalizedMessages(quickNoteMessages)
  const [notes, setNotes] = useState<QuickNote[]>([])
  const [activeId, setActiveId] = useState('')
  const [draft, setDraft] = useState<Draft>({ title: '', content: '', tags: [], color: 'default' })
  const [query, setQuery] = useState('')
  const [tagFilter, setTagFilter] = useState('')
  const [tagText, setTagText] = useState('')
  const [viewMode, setViewMode] = useState<ViewMode>('editor')
  const [dirty, setDirty] = useState(false)
  const [notice, setNotice] = useState<Notice>({ key: 'notice.loading' })
  const [failed, setFailed] = useState(false)
  const [busy, setBusy] = useState(false)
  const [findVisible, setFindVisible] = useState(false)
  const [find, setFind] = useState('')
  const [replace, setReplace] = useState('')
  const [attachments, setAttachments] = useState<QuickNoteAttachment[]>([])
  const editorViewRef = useRef<EditorView | undefined>(undefined)
  const revisionRef = useRef(0)
  const activeNote = notes.find((note) => note.id === activeId)
  const visibleNotes = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return notes.filter((note) => (!tagFilter || note.tags.includes(tagFilter))
      && (!needle || `${note.title}\n${note.content}\n${note.tags.join(' ')}`.toLowerCase().includes(needle)))
  }, [notes, query, tagFilter])
  const allTags = useMemo(() => [...new Set(notes.flatMap((note) => note.tags))].sort((left, right) => left.localeCompare(right)), [notes])
  const matchCount = useMemo(() => countTextMatches(draft.content, find), [draft.content, find])
  const stats = useMemo(() => documentStats(draft.content), [draft.content])
  const noticeText = 'raw' in notice ? notice.raw : t(notice.key, notice.values)
  const session = useMemo(() => ({
    digest: JSON.stringify({ count: notes.length, activeId, titleHash: contentFingerprint(draft.title), contentHash: contentFingerprint(draft.content), tagHash: contentFingerprint(tagText), dirty }),
    summary: t('session.summary', { count: notes.length, state: t(dirty ? 'session.unsaved' : 'session.saved') })
  }), [activeId, dirty, draft.content, draft.title, notes.length, t, tagText])
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
    if (!activeId) { setAttachments([]); return }
    void quickNoteAttachmentApi.list(activeId).then(setAttachments).catch(fail)
  }, [activeId])

  useEffect(() => {
    if (!dirty || busy || (!draft.title.trim() && !draft.content.trim())) return
    const timer = window.setTimeout(() => { void persistDraft(false) }, 650)
    return () => window.clearTimeout(timer)
  }, [activeId, busy, dirty, draft.content, draft.title, tagText])

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
    setDraft({ title: note.title, content: note.content, tags: note.tags, color: note.color })
    setTagText(note.tags.join(', '))
    setDirty(false)
  }

  function applyEmptyDraft(): void {
    revisionRef.current += 1
    setActiveId('')
    setDraft({ title: '', content: '', tags: [], color: 'default' })
    setTagText('')
    setAttachments([])
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

  function patchTags(value: string): void {
    revisionRef.current += 1
    setTagText(value)
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
        tags: parseTags(tagText),
        color: snapshot.color,
        pinned: activeNote?.pinned ?? false,
        createdAt: activeNote?.createdAt ?? now,
        updatedAt: now
      }
      const saved = await localDataApi.saveNote(note)
      setNotes((items) => sortNotes([saved, ...items.filter((item) => item.id !== saved.id)]))
      setActiveId(saved.id)
      const unchanged = revisionRef.current === revision
      if (unchanged) {
        setDraft({ title: saved.title, content: saved.content, tags: saved.tags, color: saved.color })
        setTagText(saved.tags.join(', '))
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
        tags: parseTags(tagText),
        color: snapshot.color,
        pinned: !activeNote.pinned,
        updatedAt: Date.now()
      })
      setNotes((items) => sortNotes([saved, ...items.filter((item) => item.id !== saved.id)]))
      const unchanged = revisionRef.current === revision
      if (unchanged) {
        setDraft({ title: saved.title, content: saved.content, tags: saved.tags, color: saved.color })
        setTagText(saved.tags.join(', '))
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
        tags: parseTags(tagText),
        color: draft.color,
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
    if (!activeNote || !await dialog.confirm(t('confirm.delete', { title: activeNote.title }), { dangerous: true })) return
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
      setDraft({ title: file.name.replace(/\.[^.]+$/, '') || t('import.title'), content: file.content, tags: [], color: 'default' })
      setTagText('')
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

  function insertMarkdown(prefix: string, suffix: string, placeholder: string): void {
    const view = editorViewRef.current
    const selection = view?.state.selection.main
    const from = selection?.from ?? draft.content.length
    const to = selection?.to ?? from
    const selected = draft.content.slice(from, to) || placeholder
    const insertion = `${prefix}${selected}${suffix}`
    patchDraft({ content: `${draft.content.slice(0, from)}${insertion}${draft.content.slice(to)}` })
    window.requestAnimationFrame(() => {
      const editor = editorViewRef.current
      if (!editor) return
      const anchor = from + prefix.length
      editor.dispatch({ selection: { anchor, head: anchor + selected.length }, scrollIntoView: true })
      editor.focus()
    })
  }

  async function importAttachment(): Promise<void> {
    if (!activeNote || busy) return
    setBusy(true)
    try {
      const attachment = await quickNoteAttachmentApi.chooseAndImport(activeNote.id)
      if (!attachment) return
      setAttachments((items) => [attachment, ...items])
      insertMarkdown('\n[', `](attachment:${attachment.id})\n`, attachment.name)
      succeed('notice.attachmentImported', { name: attachment.name })
      recordOperation(t('operation.attach'), `${attachment.name} · ${formatBytes(attachment.sizeBytes)}`, 'success')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function exportAttachment(attachment: QuickNoteAttachment): Promise<void> {
    try {
      const path = await quickNoteAttachmentApi.exportFile(attachment)
      if (path) succeed('notice.attachmentExported', { path })
    } catch (cause) { fail(cause) }
  }

  async function removeAttachment(attachment: QuickNoteAttachment): Promise<void> {
    if (!await dialog.confirm(t('confirm.deleteAttachment', { name: attachment.name }), { dangerous: true })) return
    try {
      await quickNoteAttachmentApi.delete(attachment.id)
      setAttachments((items) => items.filter((item) => item.id !== attachment.id))
      succeed('notice.attachmentDeleted', { name: attachment.name })
    } catch (cause) { fail(cause) }
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
      <ResizableColumns id="quick-note-list" className="quick-note-layout" initialPrimary={260} minPrimary={190} minSecondary={420}>
        <aside className="quick-note-sidebar">
          <div className="quick-note-search">
            <Search />
            <input value={query} placeholder={t('search.placeholder')} onChange={(event) => setQuery(event.target.value)} />
            <button type="button" aria-label={t('search.new')} onClick={() => void createDraft()}><FilePlus2 /></button>
          </div>
          <label className="quick-note-tag-filter"><Tag /><select value={tagFilter} aria-label={t('tags.filter')} onChange={(event) => setTagFilter(event.target.value)}><option value="">{t('tags.all')}</option>{allTags.map((tag) => <option key={tag} value={tag}>{tag}</option>)}</select></label>
          <div className="quick-note-list">
            {visibleNotes.map((note) => (
              <button className={note.id === activeId ? 'quick-note-item quick-note-item--active' : 'quick-note-item'} type="button" key={note.id} onClick={() => void selectNote(note)}>
                <strong><i data-color={note.color} />{note.title || t('note.untitled')}</strong>
                <span>{note.content.replace(/\s+/g, ' ').slice(0, 70) || t('note.empty')}</span>
                {!!note.tags.length && <span className="quick-note-item__tags">{note.tags.map((tag) => <em key={tag}>#{tag}</em>)}</span>}
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
          <div className="quick-note-editor-tools">
            <button type="button" title={t('format.bold')} onClick={() => insertMarkdown('**', '**', t('format.text'))}><Bold /></button>
            <button type="button" title={t('format.italic')} onClick={() => insertMarkdown('_', '_', t('format.text'))}><Italic /></button>
            <button type="button" title={t('format.code')} onClick={() => insertMarkdown('`', '`', t('format.codeText'))}><Code2 /></button>
            <button type="button" title={t('format.list')} onClick={() => insertMarkdown('- ', '', t('format.listItem'))}><List /></button>
            <button type="button" title={t('attachment.add')} disabled={!activeNote || busy} onClick={() => void importAttachment()}><Paperclip /></button>
            <label><Tag /><input value={tagText} placeholder={t('tags.placeholder')} onChange={(event) => patchTags(event.target.value)} /></label>
            <label><Palette /><select value={draft.color} aria-label={t('color.label')} onChange={(event) => patchDraft({ color: event.target.value as QuickNote['color'] })}>{NOTE_COLORS.map((color) => <option key={color} value={color}>{t(`color.${color}` as QuickNoteMessageKey)}</option>)}</select></label>
          </div>
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
            {viewMode !== 'preview' && <CodeEditor ariaLabel={t('editor.content')} value={draft.content} onChange={(content) => patchDraft({ content })} onReady={(view) => { editorViewRef.current = view }} className="utility-code-editor" lineWrapping />}
            {viewMode !== 'editor' && <MarkdownPreview value={draft.content} />}
          </div>
          {!!attachments.length && <div className="quick-note-attachments"><strong><Paperclip />{t('attachment.title', { count: attachments.length })}</strong><div>{attachments.map((attachment) => <span key={attachment.id}><button type="button" title={t('attachment.export')} onClick={() => void exportAttachment(attachment)}><Download />{attachment.name}<small>{formatBytes(attachment.sizeBytes)}</small></button><button type="button" aria-label={t('attachment.delete', { name: attachment.name })} onClick={() => void removeAttachment(attachment)}><X /></button></span>)}</div></div>}
          <footer>
            <span>{t('stats.lines', { count: stats.lines })}</span>
            <span>{t('stats.words', { count: stats.words })}</span>
            <span>{t('stats.characters', { count: stats.characters })}</span>
            <span>{t(dirty ? 'stats.pending' : 'stats.saved')}</span>
          </footer>
        </section>
      </ResizableColumns>
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
const NOTE_COLORS: QuickNote['color'][] = ['default', 'coral', 'yellow', 'green', 'blue', 'purple', 'red']
function parseTags(value: string): string[] { return normalizeTags(value.split(/[,，]/)) }
function normalizeTags(values: string[]): string[] { return [...new Set(values.map((value) => value.trim().replace(/^#/, '')).filter(Boolean))].slice(0, 32) }
function formatBytes(value: number): string { return value < 1024 ? `${value} B` : value < 1024 * 1024 ? `${(value / 1024).toFixed(1)} KB` : `${(value / (1024 * 1024)).toFixed(1)} MB` }
