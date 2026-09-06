import {
  Bold,
  CheckCircle2,
  Code2,
  Columns2,
  CopyPlus,
  Download,
  Eye,
  Info,
  Italic,
  List,
  FileInput,
  FilePlus2,
  Folder,
  FolderPlus,
  NotebookPen,
  Paperclip,
  Palette,
  Pencil,
  Pin,
  PinOff,
  Save,
  Search,
  Sparkles,
  Tag,
  Trash2,
  TriangleAlert,
  X
} from 'lucide-react'
import { getCurrentWebview } from '@tauri-apps/api/webview'
import { json } from '@codemirror/lang-json'
import { crosshairCursor, rectangularSelection, type EditorView } from '@codemirror/view'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import { quickNoteAttachmentApi } from '../../platform/api/quickNoteAttachmentApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import type { QuickNote, QuickNoteAttachment, QuickNoteFolder } from '../../platform/contracts/localData'
import { CodeEditor } from '../../shared/CodeEditor'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { useOperationRestore } from '../history/operationRestore'
import { useSettings } from '../settings/SettingsProvider'
import { quickNoteMessages } from './quickNoteMessages'

interface Draft {
  content: string
  title: string
  tags: string[]
  color: QuickNote['color']
  folderPath: string
  editorFont: QuickNote['editorFont']
  lineHeight: QuickNote['lineHeight']
  lineWrapping: boolean
  syntax: QuickNote['syntax']
}
type ViewMode = 'editor' | 'split' | 'preview'
type QuickNoteMessageKey = LocalizedMessageKey<typeof quickNoteMessages>
type Notice = { key: QuickNoteMessageKey; values?: MessageValues } | { raw: string }

export function QuickNoteSurface() {
  const dialog = useDesktopDialog()
  const { settings } = useSettings()
  const { t, locale } = useLocalizedMessages(quickNoteMessages)
  const [notes, setNotes] = useState<QuickNote[]>([])
  const [folders, setFolders] = useState<QuickNoteFolder[]>([])
  const [folderFilter, setFolderFilter] = useState('*')
  const [activeId, setActiveId] = useState('')
  const [draft, setDraft] = useState<Draft>(() => emptyDraft())
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
  const [transform, setTransform] = useState('uppercase')
  const [infoVisible, setInfoVisible] = useState(false)
  const [nativeDragActive, setNativeDragActive] = useState(false)
  const [attachments, setAttachments] = useState<QuickNoteAttachment[]>([])
  const editorViewRef = useRef<EditorView | undefined>(undefined)
  const revisionRef = useRef(0)
  const activeNote = notes.find((note) => note.id === activeId)
  const visibleNotes = useMemo(() => {
    const needle = query.trim().toLowerCase()
    return notes.filter((note) => (folderFilter === '*' || note.folderPath === folderFilter)
      && (!tagFilter || note.tags.includes(tagFilter))
      && (!needle || `${note.title}\n${note.content}\n${note.tags.join(' ')}`.toLowerCase().includes(needle)))
  }, [folderFilter, notes, query, tagFilter])
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

  useOperationRestore('quick-note', (entry) => {
    try {
      const restored = JSON.parse(entry.outputText || entry.inputText) as Partial<QuickNote>
      revisionRef.current += 1
      setActiveId('')
      setDraft({
        title: restored.title ?? '', content: restored.content ?? '', tags: restored.tags ?? [],
        color: restored.color ?? 'default', folderPath: restored.folderPath ?? '',
        editorFont: restored.editorFont ?? 'default', lineHeight: restored.lineHeight ?? 'normal',
        lineWrapping: restored.lineWrapping ?? true, syntax: restored.syntax ?? 'markdown'
      })
      setTagText((restored.tags ?? []).join(', '))
      setFolderFilter(restored.folderPath ?? '')
      setDirty(true)
      setViewMode('split')
    } catch { /* Ignore legacy records without a note snapshot. */ }
  })

  useEffect(() => {
    void Promise.all([localDataApi.listNotes(), localDataApi.listNoteFolders()]).then(([loaded, loadedFolders]) => {
      setNotes(loaded)
      setFolders(loadedFolders)
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
    if (!window.__TAURI_INTERNALS__) return
    let disposed = false
    let unlisten: (() => void) | undefined
    void getCurrentWebview().onDragDropEvent((event) => {
      if (event.payload.type === 'enter' || event.payload.type === 'over') {
        setNativeDragActive(true)
        return
      }
      setNativeDragActive(false)
      if (event.payload.type !== 'drop') return
      const paths = event.payload.paths.filter((path) => /\.(?:png|jpe?g|gif|webp)$/i.test(path)).slice(0, 20)
      if (paths.length) void attachImagePaths(paths)
    }).then((dispose) => {
      if (disposed) dispose()
      else unlisten = dispose
    }).catch((cause: unknown) => { if (!disposed) fail(cause) })
    return () => { disposed = true; unlisten?.() }
  }, [activeId, draft.content, draft.title])

  useEffect(() => {
    if (!dirty || busy || (!draft.title.trim() && !draft.content.trim())) return
    const timer = window.setTimeout(() => { void persistDraft(false) }, 650)
    return () => window.clearTimeout(timer)
  }, [activeId, busy, dirty, draft.color, draft.content, draft.editorFont, draft.folderPath, draft.lineHeight, draft.lineWrapping, draft.syntax, draft.title, tagText])

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
    setDraft(noteDraft(note))
    setTagText(note.tags.join(', '))
    setDirty(false)
  }

  function applyEmptyDraft(): void {
    revisionRef.current += 1
    setActiveId('')
    setDraft(emptyDraft(folderFilter === '*' ? '' : folderFilter))
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

  async function persistDraft(announce: boolean): Promise<string | false> {
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
        folderPath: snapshot.folderPath,
        editorFont: snapshot.editorFont,
        lineHeight: snapshot.lineHeight,
        lineWrapping: snapshot.lineWrapping,
        syntax: snapshot.syntax,
        pinned: activeNote?.pinned ?? false,
        createdAt: activeNote?.createdAt ?? now,
        updatedAt: now
      }
      const saved = await localDataApi.saveNote(note)
      setNotes((items) => sortNotes([saved, ...items.filter((item) => item.id !== saved.id)]))
      setActiveId(saved.id)
      const unchanged = revisionRef.current === revision
      if (unchanged) {
        setDraft(noteDraft(saved))
        setTagText(saved.tags.join(', '))
        setDirty(false)
      }
      if (announce) {
        succeed(unchanged ? 'notice.saved' : 'notice.savedPending')
        recordOperation(t('operation.save'), saved.title, 'success', {
          inputText: JSON.stringify({ ...saved, content: snapshot.content }), outputText: JSON.stringify(saved),
          metadata: { operation: 'save', folderPath: saved.folderPath }
        })
      } else {
        succeed(unchanged ? 'notice.autoSaved' : 'notice.autoSavedPending')
      }
      return saved.id
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
        folderPath: snapshot.folderPath,
        editorFont: snapshot.editorFont,
        lineHeight: snapshot.lineHeight,
        lineWrapping: snapshot.lineWrapping,
        syntax: snapshot.syntax,
        pinned: !activeNote.pinned,
        updatedAt: Date.now()
      })
      setNotes((items) => sortNotes([saved, ...items.filter((item) => item.id !== saved.id)]))
      const unchanged = revisionRef.current === revision
      if (unchanged) {
        setDraft(noteDraft(saved))
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
        folderPath: draft.folderPath,
        editorFont: draft.editorFont,
        lineHeight: draft.lineHeight,
        lineWrapping: draft.lineWrapping,
        syntax: draft.syntax,
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
      setDraft({ ...emptyDraft(folderFilter === '*' ? '' : folderFilter), title: file.name.replace(/\.[^.]+$/, '') || t('import.title'), content: file.content, syntax: inferSyntax(file.name) })
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

  async function createFolder(): Promise<void> {
    const initial = folderFilter === '*' ? '' : `${folderFilter}/`
    const entered = await dialog.prompt(t('folder.createPrompt'), initial)
    const path = normalizeFolderPath(entered ?? '')
    if (!path) return
    try {
      const now = Date.now()
      const parts = path.split('/')
      const known = new Set(folders.map((folder) => folder.path))
      const created = [...folders]
      for (let index = 1; index <= parts.length; index += 1) {
        const nextPath = parts.slice(0, index).join('/')
        if (known.has(nextPath)) continue
        const saved = await localDataApi.saveNoteFolder({ path: nextPath, createdAt: now, updatedAt: now })
        created.push(saved)
        known.add(nextPath)
      }
      setFolders(sortFolders(created))
      setFolderFilter(path)
      succeed('notice.folderCreated', { path })
    } catch (cause) { fail(cause) }
  }

  async function renameFolder(): Promise<void> {
    if (folderFilter === '*' || !folderFilter) return
    const entered = await dialog.prompt(t('folder.renamePrompt', { path: folderFilter }), folderFilter)
    const nextPath = normalizeFolderPath(entered ?? '')
    if (!nextPath || nextPath === folderFilter) return
    try {
      const previous = folderFilter
      await localDataApi.renameNoteFolder(previous, nextPath, Date.now())
      const [loadedNotes, loadedFolders] = await Promise.all([localDataApi.listNotes(), localDataApi.listNoteFolders()])
      setNotes(loadedNotes)
      setFolders(loadedFolders)
      setFolderFilter(nextPath)
      if (draft.folderPath === previous || draft.folderPath.startsWith(`${previous}/`)) {
        setDraft((current) => ({ ...current, folderPath: `${nextPath}${current.folderPath.slice(previous.length)}` }))
      }
      succeed('notice.folderRenamed', { path: nextPath })
    } catch (cause) { fail(cause) }
  }

  async function removeFolder(): Promise<void> {
    if (folderFilter === '*' || !folderFilter || !await dialog.confirm(t('confirm.deleteFolder', { path: folderFilter }), { dangerous: true })) return
    try {
      const moved = await localDataApi.deleteNoteFolder(folderFilter)
      const [loadedNotes, loadedFolders] = await Promise.all([localDataApi.listNotes(), localDataApi.listNoteFolders()])
      setNotes(loadedNotes)
      setFolders(loadedFolders)
      setFolderFilter('')
      if (draft.folderPath && (draft.folderPath === folderFilter || draft.folderPath.startsWith(`${folderFilter}/`))) {
        setDraft((current) => ({ ...current, folderPath: '' }))
      }
      succeed('notice.folderDeleted', { count: moved })
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

  function applyQuickTransform(): void {
    const view = editorViewRef.current
    const selection = view?.state.selection.main
    const from = selection?.from ?? 0
    const to = selection && !selection.empty ? selection.to : draft.content.length
    try {
      const selected = draft.content.slice(from, to)
      const replacement = transformNoteText(selected, transform)
      patchDraft({ content: `${draft.content.slice(0, from)}${replacement}${draft.content.slice(to)}` })
      window.requestAnimationFrame(() => {
        const editor = editorViewRef.current
        editor?.dispatch({ selection: { anchor: from, head: from + replacement.length }, scrollIntoView: true })
        editor?.focus()
      })
      succeed('notice.transformed')
    } catch (cause) { fail(cause) }
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

  async function ensureAttachmentNote(suggestedName: string): Promise<string | undefined> {
    if (activeId) return activeId
    if (draft.title.trim() || draft.content.trim()) {
      const savedId = await persistDraft(false)
      return savedId || undefined
    }
    const now = Date.now()
    const note: QuickNote = {
      id: crypto.randomUUID(),
      title: suggestedName.replace(/\.[^.]+$/, '').slice(0, 256) || t('note.untitled'),
      content: '',
      tags: [],
      color: 'default',
      folderPath: folderFilter === '*' ? '' : folderFilter,
      editorFont: 'default',
      lineHeight: 'normal',
      lineWrapping: true,
      syntax: 'markdown',
      pinned: false,
      createdAt: now,
      updatedAt: now
    }
    const saved = await localDataApi.saveNote(note)
    setNotes((items) => sortNotes([saved, ...items]))
    applyNote(saved)
    return saved.id
  }

  async function attachImageFiles(files: File[]): Promise<void> {
    const images = files.filter((file) => /^image\/(png|jpeg|gif|webp)$/.test(file.type)).slice(0, 20)
    if (!images.length || busy) return
    try {
      const noteId = await ensureAttachmentNote(images[0].name || 'Pasted-image.png')
      if (!noteId) return
      setBusy(true)
      const imported: QuickNoteAttachment[] = []
      for (const file of images) imported.push(await quickNoteAttachmentApi.importImageFile(noteId, file))
      setAttachments((items) => [...imported, ...items])
      insertRawMarkdown(imported.map((attachment) => `![${attachment.name}](attachment:${attachment.id})`).join('\n'))
      succeed('notice.imagesAttached', { count: imported.length })
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function attachImagePaths(paths: string[]): Promise<void> {
    if (!paths.length || busy) return
    try {
      const name = paths[0].split(/[\\/]/).at(-1) || 'Dropped-image.png'
      const noteId = await ensureAttachmentNote(name)
      if (!noteId) return
      setBusy(true)
      const imported: QuickNoteAttachment[] = []
      for (const path of paths) imported.push(await quickNoteAttachmentApi.importPath(noteId, path))
      setAttachments((items) => [...imported, ...items])
      insertRawMarkdown(imported.map((attachment) => `![${attachment.name}](attachment:${attachment.id})`).join('\n'))
      succeed('notice.imagesAttached', { count: imported.length })
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  function insertRawMarkdown(value: string): void {
    const selection = editorViewRef.current?.state.selection.main
    const from = selection?.from ?? draft.content.length
    const to = selection?.to ?? from
    const before = from > 0 && !draft.content.slice(0, from).endsWith('\n') ? '\n' : ''
    const after = to < draft.content.length && !draft.content.slice(to).startsWith('\n') ? '\n' : ''
    const insertion = `${before}${value}${after}`
    patchDraft({ content: `${draft.content.slice(0, from)}${insertion}${draft.content.slice(to)}` })
    window.requestAnimationFrame(() => editorViewRef.current?.dispatch({ selection: { anchor: from + insertion.length }, scrollIntoView: true }))
  }

  async function exportAttachment(attachment: QuickNoteAttachment): Promise<void> {
    try {
      const path = await quickNoteAttachmentApi.exportFile(attachment, settings.tools.exportDirectory)
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
        <h1 className="visually-hidden">{t('title')}</h1>
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
          <section className="quick-note-folders" aria-label={t('folder.tree')}>
            <header>
              <strong><Folder />{t('folder.tree')}</strong>
              <span>
                <button type="button" aria-label={t('folder.create')} onClick={() => void createFolder()}><FolderPlus /></button>
                <button type="button" aria-label={t('folder.rename')} disabled={folderFilter === '*' || !folderFilter} onClick={() => void renameFolder()}><Pencil /></button>
                <button type="button" aria-label={t('folder.delete')} disabled={folderFilter === '*' || !folderFilter} onClick={() => void removeFolder()}><Trash2 /></button>
              </span>
            </header>
            <button className={folderFilter === '*' ? 'quick-note-folder quick-note-folder--active' : 'quick-note-folder'} type="button" onClick={() => setFolderFilter('*')}><Folder />{t('folder.all')}<em>{notes.length}</em></button>
            <button className={folderFilter === '' ? 'quick-note-folder quick-note-folder--active' : 'quick-note-folder'} type="button" onClick={() => setFolderFilter('')}><Folder />{t('folder.root')}<em>{notes.filter((note) => !note.folderPath).length}</em></button>
            {folders.map((folder) => <button
              className={folderFilter === folder.path ? 'quick-note-folder quick-note-folder--active' : 'quick-note-folder'}
              style={{ paddingInlineStart: `${12 + folder.path.split('/').length * 12}px` }}
              type="button"
              key={folder.path}
              title={folder.path}
              onClick={() => setFolderFilter(folder.path)}
            ><Folder /><span>{folder.path.split('/').at(-1)}</span><em>{notes.filter((note) => note.folderPath === folder.path).length}</em></button>)}
          </section>
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
            <label className="quick-note-transform"><Sparkles /><select aria-label={t('transform.label')} value={transform} onChange={(event) => setTransform(event.target.value)}><option value="uppercase">{t('transform.uppercase')}</option><option value="lowercase">{t('transform.lowercase')}</option><option value="trimLines">{t('transform.trimLines')}</option><option value="sortLines">{t('transform.sortLines')}</option><option value="uniqueLines">{t('transform.uniqueLines')}</option><option value="jsonPretty">{t('transform.jsonPretty')}</option></select><button type="button" onClick={applyQuickTransform}>{t('transform.apply')}</button></label>
            <label><Tag /><input value={tagText} placeholder={t('tags.placeholder')} onChange={(event) => patchTags(event.target.value)} /></label>
            <label><Palette /><select value={draft.color} aria-label={t('color.label')} onChange={(event) => patchDraft({ color: event.target.value as QuickNote['color'] })}>{NOTE_COLORS.map((color) => <option key={color} value={color}>{t(`color.${color}` as QuickNoteMessageKey)}</option>)}</select></label>
            <label><Folder /><select value={draft.folderPath} aria-label={t('folder.move')} onChange={(event) => patchDraft({ folderPath: event.target.value })}><option value="">{t('folder.root')}</option>{folders.map((folder) => <option key={folder.path} value={folder.path}>{folder.path}</option>)}</select></label>
            <label>{t('editor.syntax')}<select value={draft.syntax} onChange={(event) => patchDraft({ syntax: event.target.value as QuickNote['syntax'] })}><option value="markdown">Markdown</option><option value="plain">{t('syntax.plain')}</option><option value="json">JSON</option><option value="yaml">YAML</option></select></label>
            <label>{t('editor.font')}<select value={draft.editorFont} onChange={(event) => patchDraft({ editorFont: event.target.value as QuickNote['editorFont'] })}><option value="default">{t('editor.fontDefault')}</option><option value="mono">{t('editor.fontMono')}</option><option value="serif">{t('editor.fontSerif')}</option></select></label>
            <label>{t('editor.lineHeight')}<select value={draft.lineHeight} onChange={(event) => patchDraft({ lineHeight: event.target.value as QuickNote['lineHeight'] })}><option value="compact">{t('lineHeight.compact')}</option><option value="normal">{t('lineHeight.normal')}</option><option value="relaxed">{t('lineHeight.relaxed')}</option></select></label>
            <label className="quick-note-wrap"><input type="checkbox" checked={draft.lineWrapping} onChange={(event) => patchDraft({ lineWrapping: event.target.checked })} />{t('editor.wrap')}</label>
            <button type="button" title={t('info.title')} aria-label={t('info.title')} onClick={() => setInfoVisible((value) => !value)}><Info /></button>
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
          {infoVisible && <aside className="quick-note-info"><strong><Info />{t('info.title')}</strong><span>{t('info.id')} <code>{activeId || t('info.unsaved')}</code></span><span>{t('info.folder')} <code>{draft.folderPath || t('folder.root')}</code></span><span>{t('info.created')} <time>{activeNote ? new Date(activeNote.createdAt).toLocaleString(locale) : '—'}</time></span><span>{t('info.updated')} <time>{activeNote ? new Date(activeNote.updatedAt).toLocaleString(locale) : '—'}</time></span><span>{t('info.attachments')} <b>{attachments.length}</b></span></aside>}
          <div
            className={`quick-note-content quick-note-content--${viewMode}${nativeDragActive ? ' quick-note-content--drag-active' : ''}`}
            onDragOver={window.__TAURI_INTERNALS__ ? undefined : (event) => event.preventDefault()}
            onDrop={window.__TAURI_INTERNALS__ ? undefined : (event) => { event.preventDefault(); void attachImageFiles([...event.dataTransfer.files]) }}
            onPaste={(event) => { const images = [...event.clipboardData.files].filter((file) => file.type.startsWith('image/')); if (images.length) { event.preventDefault(); void attachImageFiles(images) } }}
          >
            {nativeDragActive && <div className="quick-note-image-drop"><Paperclip />{t('attachment.dropImages')}</div>}
            {viewMode !== 'preview' && <CodeEditor key={draft.syntax} ariaLabel={t('editor.content')} value={draft.content} onChange={(content) => patchDraft({ content })} onReady={(view) => { editorViewRef.current = view }} className={`utility-code-editor quick-note-code--font-${draft.editorFont} quick-note-code--line-${draft.lineHeight}`} lineWrapping={draft.lineWrapping} extensions={draft.syntax === 'json' ? [json(), ...QUICK_NOTE_EDITOR_EXTENSIONS] : QUICK_NOTE_EDITOR_EXTENSIONS} />}
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

const QUICK_NOTE_EDITOR_EXTENSIONS = [rectangularSelection(), crosshairCursor()]

function emptyDraft(folderPath = ''): Draft {
  return {
    title: '',
    content: '',
    tags: [],
    color: 'default',
    folderPath,
    editorFont: 'default',
    lineHeight: 'normal',
    lineWrapping: true,
    syntax: 'markdown'
  }
}

function noteDraft(note: QuickNote): Draft {
  return {
    title: note.title,
    content: note.content,
    tags: note.tags,
    color: note.color,
    folderPath: note.folderPath,
    editorFont: note.editorFont,
    lineHeight: note.lineHeight,
    lineWrapping: note.lineWrapping,
    syntax: note.syntax
  }
}

function inferSyntax(name: string): QuickNote['syntax'] {
  if (/\.json$/i.test(name)) return 'json'
  if (/\.ya?ml$/i.test(name)) return 'yaml'
  if (/\.md$/i.test(name)) return 'markdown'
  return 'plain'
}

export function transformNoteText(value: string, operation: string): string {
  if (operation === 'uppercase') return value.toLocaleUpperCase()
  if (operation === 'lowercase') return value.toLocaleLowerCase()
  if (operation === 'trimLines') return value.split(/\r?\n/).map((line) => line.trim()).join('\n')
  if (operation === 'sortLines') return value.split(/\r?\n/).sort((left, right) => left.localeCompare(right)).join('\n')
  if (operation === 'uniqueLines') return [...new Set(value.split(/\r?\n/))].join('\n')
  if (operation === 'jsonPretty') return JSON.stringify(JSON.parse(value), null, 2)
  return value
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
function sortFolders(folders: QuickNoteFolder[]): QuickNoteFolder[] { return folders.sort((left, right) => left.path.localeCompare(right.path)) }
function normalizeFolderPath(value: string): string { return value.trim().replace(/\\/g, '/').split('/').map((part) => part.trim()).filter((part) => part && part !== '.' && part !== '..').join('/').slice(0, 512) }
function firstContentLine(content: string): string { return content.split(/\r?\n/).find((line) => line.trim())?.replace(/^#+\s*/, '').slice(0, 80) ?? '' }
function formatTime(value: number, locale: string): string { return new Intl.DateTimeFormat(locale, { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value) }
function countTextMatches(value: string, query: string): number { if (!query) return 0; let count = 0; let from = 0; while ((from = value.indexOf(query, from)) >= 0) { count += 1; from += Math.max(1, query.length) } return count }
function documentStats(value: string) { return { lines: value ? value.split(/\r?\n/).length : 1, words: value.trim() ? value.trim().split(/\s+/u).length : 0, characters: [...value].length } }
function safeFileName(value: string): string { return value.trim().replace(/[\\/:*?"<>|\u0000-\u001f]/g, '-').replace(/^\.+/, '').slice(0, 120) || 'note' }
const NOTE_COLORS: QuickNote['color'][] = ['default', 'coral', 'yellow', 'green', 'blue', 'purple', 'red']
function parseTags(value: string): string[] { return normalizeTags(value.split(/[,，]/)) }
function normalizeTags(values: string[]): string[] { return [...new Set(values.map((value) => value.trim().replace(/^#/, '')).filter(Boolean))].slice(0, 32) }
function formatBytes(value: number): string { return value < 1024 ? `${value} B` : value < 1024 * 1024 ? `${(value / 1024).toFixed(1)} KB` : `${(value / (1024 * 1024)).toFixed(1)} MB` }
