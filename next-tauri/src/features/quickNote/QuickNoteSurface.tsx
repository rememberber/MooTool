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
  ListOrdered,
  FileInput,
  FilePlus2,
  FoldVertical,
  Folder,
  FolderOpen,
  FolderPlus,
  GitBranch,
  LocateFixed,
  NotebookPen,
  PanelLeftClose,
  PanelLeftOpen,
  Paperclip,
  Palette,
  Pencil,
  Pin,
  PinOff,
  Replace,
  Save,
  Search,
  Sparkles,
  Tag,
  Trash2,
  TriangleAlert,
  UnfoldVertical,
  WandSparkles,
  X
} from 'lucide-react'
import { getCurrentWebview } from '@tauri-apps/api/webview'
import { open } from '@tauri-apps/plugin-dialog'
import { crosshairCursor, rectangularSelection, type EditorView } from '@codemirror/view'
import { useEffect, useMemo, useRef, useState, type ReactElement } from 'react'
import {
  defaultFindReplaceOptions,
  findAllMatches,
  findNextMatch,
  findNextMatchWithWrap,
  replaceAllMatches,
  replaceCurrentMatch,
  type FindReplaceOptions
} from '../../shared/findReplace'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import { quickNoteGitApi } from '../../platform/api/quickNoteGitApi'
import { quickNoteAttachmentApi } from '../../platform/api/quickNoteAttachmentApi'
import { userFilesApi } from '../../platform/api/userFilesApi'
import { normalizeNoteSyntax, type QuickNote, type QuickNoteAttachment, type QuickNoteFolder } from '../../platform/contracts/localData'
import { CodeEditor } from '../../shared/CodeEditor'
import { FontSelect } from '../../shared/components/FontSelect'
import { cssFontFamily } from '../../shared/fonts/systemFonts'
import { ResizableColumns } from '../../shared/ResizableColumns'
import { ResizableThreeColumns } from '../../shared/ResizableThreeColumns'
import { useDesktopDialog } from '../../shared/DesktopDialogProvider'
import { contentFingerprint } from '../../shared/fingerprint'
import { useToolSessionReport } from '../toolWebview/useToolSessionReport'
import { useOperationHistory } from '../history/useOperationHistory'
import { useOperationRestore } from '../history/operationRestore'
import { useSettings } from '../settings/SettingsProvider'
import { recordQuickNoteActivity, setQuickNoteEditorDirty } from '../settings/vaultCheckpoints'
import { formatNoteContent, prefixSelectedLines } from './quickNoteEditing'
import { quickNoteLanguage } from './quickNoteLanguage'
import { quickReplaceActionIds, runQuickReplace, type QuickReplaceActionId } from './quickReplace'
import { quickNoteMessages } from './quickNoteMessages'
import { QuickNoteGitPanel } from './QuickNoteGitPanel'
import { buildNoteTree, canMoveToDirectory, QuickNoteTree, type NoteSort, type NoteTreeAction, type NoteTreeNode } from './QuickNoteTree'

interface Draft {
  content: string
  title: string
  tags: string[]
  color: QuickNote['color']
  folderPath: string
  editorFont: QuickNote['editorFont']
  lineHeight: QuickNote['lineHeight']
  fontSize: number
  fontName: string
  lineSpacing: number
  lineWrapping: boolean
  syntax: QuickNote['syntax']
}
type ViewMode = 'editor' | 'split' | 'preview'
type QuickNoteMessageKey = LocalizedMessageKey<typeof quickNoteMessages>
type Notice = { key: QuickNoteMessageKey; values?: MessageValues } | { raw: string }

function QuickNoteWorkspace({ treeOpen, replaceOpen, replace, children }: { treeOpen: boolean; replaceOpen: boolean; replace: ReactElement; children: [ReactElement, ReactElement] }) {
  if (treeOpen && replaceOpen) {
    return (
      <ResizableThreeColumns id="quick-note-tree-replace" className="quick-note-layout" initialLeft={220} initialRight={218} defaultSizes={[220, 560, 218]} minLeft={170} minCenter={320} minRight={170}>
        {children[0]}
        {children[1]}
        {replace}
      </ResizableThreeColumns>
    )
  }
  if (treeOpen) {
    return (
      <ResizableColumns id="quick-note-tree-no-replace" legacyId="quick-note-list" className="quick-note-layout" initialPrimary={220} defaultSizes={[220, 780]} minPrimary={170} minSecondary={320}>
        {children[0]}
        {children[1]}
      </ResizableColumns>
    )
  }
  if (replaceOpen) {
    return (
      <ResizableColumns id="quick-note-no-tree-replace" className="quick-note-layout" initialPrimary={780} defaultSizes={[780, 218]} minPrimary={320} minSecondary={170}>
        {children[1]}
        {replace}
      </ResizableColumns>
    )
  }
  return <section className="quick-note-layout quick-note-layout--editor-only">{children[1]}</section>
}

export function QuickNoteSurface() {
  const dialog = useDesktopDialog()
  const { settings, save } = useSettings()
  const { t, locale } = useLocalizedMessages(quickNoteMessages)
  const [notes, setNotes] = useState<QuickNote[]>([])
  const [folders, setFolders] = useState<QuickNoteFolder[]>([])
  const [folderFilter, setFolderFilter] = useState('')
  const [activeId, setActiveId] = useState('')
  const [draft, setDraft] = useState<Draft>(() => emptyDraft())
  const [query, setQuery] = useState('')
  const [includeContent, setIncludeContent] = useState(true)
  const [noteSort, setNoteSort] = useState<NoteSort>('modified')
  const [locateRequest, setLocateRequest] = useState(0)
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
  const [findOptions, setFindOptions] = useState<FindReplaceOptions>(defaultFindReplaceOptions)
  const [replacedCount, setReplacedCount] = useState(0)
  const [transform, setTransform] = useState('uppercase')
  const [infoVisible, setInfoVisible] = useState(false)
  const [gitOpen, setGitOpen] = useState(false)
  const [treeOpen, setTreeOpen] = useState(true)
  const [quickReplaceOpen, setQuickReplaceOpen] = useState(false)
  const [nativeDragActive, setNativeDragActive] = useState(false)
  const [createMode, setCreateMode] = useState<'note' | 'folder' | null>(null)
  const [createName, setCreateName] = useState('')
  const [entryAction, setEntryAction] = useState<{ mode: 'rename' | 'move'; node: NoteTreeNode; value: string } | null>(null)
  const [attachments, setAttachments] = useState<QuickNoteAttachment[]>([])
  const [attachmentUrls, setAttachmentUrls] = useState<Record<string, string>>({})
  const editorViewRef = useRef<EditorView | undefined>(undefined)
  const revisionRef = useRef(0)
  const findIndexRef = useRef(0)
  const activeNote = notes.find((note) => note.id === activeId)
  const activeTreePath = activeNote
    ? activeNote.relativePath || (activeNote.folderPath ? `${activeNote.folderPath}/${activeNote.id}` : activeNote.id)
    : folderFilter === '*' ? '' : folderFilter
  const treeExpanded = settings.vault.quickNoteTreeExpandMode === 'collapseAll' ? 'collapseAll' : 'expandAll'
  const noteTree = useMemo(
    () => buildNoteTree(notes, folders, query, tagFilter, { includeContent, sort: noteSort }),
    [folders, includeContent, noteSort, notes, query, tagFilter]
  )
  const allTags = useMemo(() => [...new Set(notes.flatMap((note) => note.tags))].sort((left, right) => left.localeCompare(right)), [notes])
  const matchCount = useMemo(() => findAllMatches(draft.content, find, findOptions).length, [draft.content, find, findOptions])
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
        fontSize: restored.fontSize ?? settings.editor.quickNoteFontSize, fontName: restored.fontName ?? '',
        lineSpacing: restored.lineSpacing ?? 1, lineWrapping: restored.lineWrapping ?? true, syntax: normalizeNoteSyntax(restored.syntax)
      })
      setTagText((restored.tags ?? []).join(', '))
      setFolderFilter(restored.folderPath ?? '')
      setDirty(true)
      setViewMode('split')
    } catch { /* Ignore legacy records without a note snapshot. */ }
  })

  useEffect(() => { setQuickNoteEditorDirty(dirty) }, [dirty])

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
    void Promise.all([localDataApi.listNotes(), localDataApi.listNoteFolders()]).then(([loaded, loadedFolders]) => {
      setNotes(loaded)
      setFolders(loadedFolders)
    }).catch(fail)
  }, [settings.vault.hideGitignoredFiles])

  useEffect(() => {
    if (!activeId) { setAttachments([]); return }
    void quickNoteAttachmentApi.list(activeId).then(setAttachments).catch(fail)
  }, [activeId])

  const attachmentKey = useMemo(() => attachmentPaths(draft.content).join('\n'), [draft.content])
  useEffect(() => {
    const missing = (attachmentKey ? attachmentKey.split('\n') : []).filter((path) => path && !attachmentUrls[path])
    if (!missing.length || !window.__TAURI_INTERNALS__) return
    let cancelled = false
    void Promise.all(missing.map(async (path) => [path, await quickNoteAttachmentApi.read(path)] as const)).then((entries) => {
      if (cancelled) return
      setAttachmentUrls((current) => {
        const next = { ...current }
        for (const [path, url] of entries) if (url) next[path] = url
        return next
      })
    }).catch(() => undefined)
    return () => { cancelled = true }
  }, [attachmentKey, attachmentUrls])

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
      const paths = event.payload.paths.filter((path) => /\.(?:png|jpe?g|gif|bmp|webp)$/i.test(path)).slice(0, 20)
      if (paths.length) void attachImagePaths(paths)
    }).then((dispose) => {
      if (disposed) dispose()
      else unlisten = dispose
    }).catch((cause: unknown) => { if (!disposed) fail(cause) })
    return () => { disposed = true; unlisten?.() }
  }, [activeId, draft.content, draft.title])

  useEffect(() => {
    if (!dirty || busy || !activeNote) return
    const timer = window.setTimeout(() => { void persistDraft(false) }, 250)
    return () => window.clearTimeout(timer)
  }, [activeId, activeNote, busy, dirty, draft.color, draft.content, draft.editorFont, draft.folderPath, draft.lineHeight, draft.lineWrapping, draft.syntax, draft.title, tagText])

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if (!(event.metaKey || event.ctrlKey) || event.shiftKey || event.altKey) return
      const key = event.key.toLowerCase()
      if (key === 's') {
        event.preventDefault()
        void persistDraft(true)
      } else if (key === 'f' || key === 'r') {
        event.preventDefault()
        openFind()
      }
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  })

  function applyNote(note: QuickNote): void {
    revisionRef.current += 1
    setActiveId(note.id)
    setFolderFilter(note.folderPath)
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
    if (dirty && activeNote && !await persistDraft(false)) return
    applyNote(note)
  }

  async function selectFolder(path: string): Promise<void> {
    if (dirty && activeNote && !await persistDraft(false)) return
    revisionRef.current += 1
    setActiveId('')
    setFolderFilter(path)
    setDraft(emptyDraft(path === '*' ? '' : path))
    setTagText('')
    setAttachments([])
    setDirty(false)
    setInfoVisible(false)
    setFindVisible(false)
    setReplacedCount(0)
  }

  function openCreateNote(): void {
    setCreateName('')
    setCreateMode('note')
  }

  function openCreateFolder(): void {
    setCreateName('')
    setCreateMode('folder')
  }

  async function submitCreateNote(): Promise<void> {
    const title = createName.trim()
    if (!title || busy) return
    if (dirty && activeNote && !await persistDraft(false)) return
    setBusy(true)
    try {
      const created = await localDataApi.createNote({
        title,
        parentPath: folderFilter === '*' ? '' : folderFilter,
        fontName: settings.editor.quickNoteFontName,
        fontSize: settings.editor.quickNoteFontSize,
        lineWrap: settings.editor.softWrap
      })
      setNotes((items) => sortNotes([created, ...items.filter((note) => note.id !== created.id)]))
      setFolderFilter(created.folderPath)
      applyNote(created)
      setCreateMode(null)
      recordQuickNoteActivity('Create Quick Note')
      succeed('notice.created')
    } catch (cause) {
      fail(cause)
    } finally {
      setBusy(false)
    }
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
        fontSize: snapshot.fontSize,
        fontName: snapshot.fontName,
        lineSpacing: snapshot.lineSpacing,
        relativePath: activeNote?.relativePath ?? '',
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
      recordQuickNoteActivity(activeNote ? 'Update Quick Note' : 'Create Quick Note')
      if (announce) {
        succeed(unchanged ? 'notice.saved' : 'notice.savedPending')
        recordOperation(t('operation.save'), saved.title, 'success', {
          inputText: JSON.stringify({ ...saved, content: snapshot.content }), outputText: JSON.stringify(saved),
          metadata: { operation: 'save', folderPath: saved.folderPath }
        })
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
        fontSize: snapshot.fontSize,
        fontName: snapshot.fontName,
        lineSpacing: snapshot.lineSpacing,
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
      recordQuickNoteActivity('Update Quick Note')
      succeed(saved.pinned
        ? unchanged ? 'notice.pinned' : 'notice.pinnedPending'
        : unchanged ? 'notice.unpinned' : 'notice.unpinnedPending')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function duplicate(): Promise<void> {
    if (activeNote) return duplicateSavedNotes([activeNote])
    const saved = await persistDraft(false)
    if (!saved) return
    const current = (await localDataApi.listNotes()).find((note) => note.id === saved)
    if (current) await duplicateSavedNotes([current])
  }

  async function remove(): Promise<void> {
    if (!activeNote || !await dialog.confirm(t('confirm.delete', { title: activeNote.title }), { dangerous: true })) return
    setBusy(true)
    try {
      await localDataApi.deleteNote(activeNote.id)
      recordQuickNoteActivity('Delete Quick Note entry')
      const next = notes.filter((note) => note.id !== activeNote.id)
      setNotes(next)
      if (next[0]) applyNote(next[0]); else applyEmptyDraft()
      succeed('notice.deleted')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
  }

  async function importNote(): Promise<void> {
    if (dirty && activeNote && !await persistDraft(false)) return
    try {
      const file = await userFilesApi.pickText()
      if (!file) return
      const now = Date.now()
      const folderPath = folderFilter === '*' ? '' : folderFilter
      const saved = await localDataApi.saveNote({
        id: crypto.randomUUID(),
        title: file.name.replace(/\.[^.]+$/, '') || t('import.title'),
        content: file.content,
        tags: [],
        color: 'default',
        folderPath,
        editorFont: 'default',
        lineHeight: 'normal',
        fontSize: settings.editor.quickNoteFontSize,
        fontName: settings.editor.quickNoteFontName,
        lineSpacing: 1.2,
        relativePath: '',
        lineWrapping: settings.editor.softWrap,
        syntax: inferSyntax(file.name),
        pinned: false,
        createdAt: now,
        updatedAt: now
      })
      setNotes((items) => sortNotes([saved, ...items.filter((note) => note.id !== saved.id)]))
      applyNote(saved)
      recordQuickNoteActivity('Create Quick Note')
      succeed('notice.imported', { file: file.name })
    } catch (cause) { fail(cause) }
  }

  async function submitCreateFolder(): Promise<void> {
    const name = createName.trim()
    if (!name || busy) return
    setBusy(true)
    try {
      const parent = folderFilter === '*' ? '' : folderFilter
      const path = [parent, name].filter(Boolean).join('/')
      const now = Date.now()
      const saved = await localDataApi.saveNoteFolder({ path, createdAt: now, updatedAt: now })
      setFolders((current) => sortFolders([saved, ...current.filter((folder) => folder.path !== saved.path)]))
      setCreateMode(null)
      revisionRef.current += 1
      setActiveId('')
      setFolderFilter(saved.path)
      setDraft(emptyDraft(saved.path))
      setTagText('')
      setAttachments([])
      setDirty(false)
      recordQuickNoteActivity('Create Quick Note folder')
      succeed('notice.folderCreated', { path: saved.path })
    } catch (cause) {
      fail(cause)
    } finally {
      setBusy(false)
    }
  }

  async function duplicateSavedNotes(targets: QuickNote[]): Promise<void> {
    if (!targets.length) return
    const includesActive = targets.some((note) => note.id === activeId)
    const savedActiveId = dirty && includesActive ? await persistDraft(false) : activeId
    if (dirty && includesActive && !savedActiveId) return
    try {
      const listed = await localDataApi.listNotes()
      const copies: QuickNote[] = []
      for (const note of targets) {
        const id = note.id === activeId ? savedActiveId : note.id
        const current = listed.find((item) => item.id === id) ?? note
        copies.push(await localDataApi.duplicateNote(current.relativePath || current.id))
      }
      const last = copies.at(-1)
      if (!last) return
      setNotes((items) => sortNotes([...copies, ...items]))
      applyNote(last)
      recordQuickNoteActivity('Duplicate Quick Note')
      succeed('notice.duplicated')
    } catch (cause) { fail(cause) }
  }

  async function runTreeAction(node: NoteTreeNode, action: NoteTreeAction, files?: NoteTreeNode[]): Promise<void> {
    const targets = (files ?? []).flatMap((item) => item.note ? [item.note] : [])
    if (action === 'duplicate') return duplicateSavedNotes(targets.length ? targets : node.note ? [node.note] : [])
    if (action === 'export') return exportTreeNotes(targets.length ? targets : node.note ? [node.note] : [])
    if (node.kind === 'directory') setFolderFilter(node.path)
    else if (node.note && node.note.id !== activeId) await selectNote(node.note)
    if (action === 'rename') { openRename(node); return }
    if (action === 'move') { openMove(node); return }
    if (action === 'info' && node.note) { setInfoVisible(true); return }
    if (action === 'delete') return deleteTreeNode(node)
    if (action === 'reveal') {
      try { await localDataApi.revealNoteEntry(node.path) }
      catch (cause) { fail(cause) }
    }
  }

  function openRename(node: NoteTreeNode): void {
    const currentName = node.kind === 'file' ? (node.note?.id === activeId ? draft.title : node.note?.title ?? node.name) : node.name
    setEntryAction({ mode: 'rename', node, value: currentName })
  }

  function openRenameFolder(): void {
    if (!folderFilter || folderFilter === '*') return
    const name = folderFilter.split('/').pop() ?? folderFilter
    openRename({ name, path: folderFilter, kind: 'directory', children: [] })
  }

  function openMove(node: NoteTreeNode): void {
    const current = node.kind === 'file' ? node.note?.folderPath ?? '' : (node.path.includes('/') ? node.path.slice(0, node.path.lastIndexOf('/')) : '')
    setEntryAction({ mode: 'move', node, value: current })
  }

  async function submitEntryAction(): Promise<void> {
    if (!entryAction || busy) return
    const { mode, node, value } = entryAction
    setEntryAction(null)
    if (mode === 'move') {
      await moveTreeNode(node, value)
      return
    }
    await renameTreeNode(node, value)
  }

  async function renameTreeNode(node: NoteTreeNode, entered: string): Promise<void> {
    const name = entered.trim()
    const currentName = node.kind === 'file' ? (node.note?.id === activeId ? draft.title : node.note?.title ?? node.name) : node.name
    if (!name || name === currentName) return
    if (node.kind === 'file' && node.note) {
      const movingActive = node.note.id === activeId
      if (movingActive && dirty && !await persistDraft(false)) return
      try {
        const listed = await localDataApi.listNotes()
        const current = listed.find((item) => item.id === node.note?.id) ?? node.note
        const saved = await localDataApi.renameNoteEntry(current.relativePath || node.path, name)
        recordQuickNoteActivity('Rename Quick Note entry')
        setNotes((items) => sortNotes([saved, ...items.filter((item) => item.id !== saved.id)]))
        if (movingActive) { setDraft(noteDraft(saved)); setTagText(saved.tags.join(', ')); setDirty(false) }
        succeed('notice.renamed', { name: saved.title })
      } catch (cause) { fail(cause) }
      return
    }
    const parent = node.path.includes('/') ? node.path.slice(0, node.path.lastIndexOf('/')) : ''
    let leaf = ''
    try { leaf = sanitizeFolderName(name) } catch (cause) { fail(cause); return }
    const nextPath = parent ? `${parent}/${leaf}` : leaf
    if (!nextPath || nextPath === node.path) return
    try {
      await localDataApi.renameNoteFolder(node.path, nextPath, Date.now())
      recordQuickNoteActivity('Rename Quick Note entry')
      const [loadedNotes, loadedFolders] = await Promise.all([localDataApi.listNotes(), localDataApi.listNoteFolders()])
      setNotes(loadedNotes)
      setFolders(loadedFolders)
      setFolderFilter(nextPath)
      if (draft.folderPath === node.path || draft.folderPath.startsWith(`${node.path}/`)) {
        setDraft((current) => ({ ...current, folderPath: `${nextPath}${current.folderPath.slice(node.path.length)}` }))
      }
      succeed('notice.renamed', { name: nextPath })
    } catch (cause) { fail(cause) }
  }

  async function exportTreeNotes(targets: QuickNote[]): Promise<void> {
    if (!targets.length) return
    if (dirty && targets.some((note) => note.id === activeId) && !await persistDraft(false)) return
    try {
      const listed = await localDataApi.listNotes()
      const relativePaths = targets.map((note) => {
        const current = listed.find((item) => item.id === note.id) ?? note
        return current.relativePath || (current.folderPath ? `${current.folderPath}/${current.id}` : current.id)
      })
      const selected = await open({ directory: true, multiple: false, title: t('tree.export') })
      const directory = Array.isArray(selected) ? selected[0] : selected
      if (!directory) return
      const count = await localDataApi.exportNotes(directory, relativePaths)
      succeed('notice.exportedFiles', { count })
    } catch (cause) { fail(cause) }
  }

  async function deleteTreeNode(node: NoteTreeNode): Promise<void> {
    if (node.kind === 'file') {
      if (node.note?.id === activeId) return remove()
      if (!node.note || !await dialog.confirm(t('confirm.delete', { title: node.note.title }), { dangerous: true })) return
      try {
        await localDataApi.deleteNote(node.note.id)
        recordQuickNoteActivity('Delete Quick Note entry')
        setNotes((items) => items.filter((item) => item.id !== node.note?.id))
        succeed('notice.deleted')
      } catch (cause) { fail(cause) }
      return
    }
    if (!await dialog.confirm(t('confirm.deleteFolder', { path: node.path }), { dangerous: true })) return
    try {
      const moved = await localDataApi.deleteNoteFolder(node.path)
      recordQuickNoteActivity('Delete Quick Note entry')
      const [loadedNotes, loadedFolders] = await Promise.all([localDataApi.listNotes(), localDataApi.listNoteFolders()])
      setNotes(loadedNotes)
      setFolders(loadedFolders)
      if (folderFilter === node.path || folderFilter.startsWith(`${node.path}/`)) setFolderFilter('')
      if (draft.folderPath === node.path || draft.folderPath.startsWith(`${node.path}/`)) setDraft((current) => ({ ...current, folderPath: '' }))
      succeed('notice.folderDeleted', { count: moved })
    } catch (cause) { fail(cause) }
  }

  async function moveTreeNode(node: NoteTreeNode, targetDirectory: string): Promise<void> {
    if (!canMoveToDirectory(node.path, targetDirectory) || busy) return
    try {
      if (node.kind === 'file' && node.note) {
        const movingActive = node.note.id === activeId
        if (movingActive && dirty && !await persistDraft(false)) return
        const listed = await localDataApi.listNotes()
        const current = listed.find((item) => item.id === node.note?.id) ?? node.note
        const saved = await localDataApi.moveNoteEntry(current.relativePath || node.path, targetDirectory)
        recordQuickNoteActivity('Move Quick Note entry')
        setNotes((items) => sortNotes([saved, ...items.filter((item) => item.id !== saved.id)]))
        if (movingActive) {
          setDraft(noteDraft(saved))
          setTagText(saved.tags.join(', '))
          setDirty(false)
          setFolderFilter(saved.folderPath)
        }
        succeed('notice.moved', { path: targetDirectory || t('folder.root') })
        return
      }
      const nextPath = targetDirectory ? `${targetDirectory}/${node.name}` : node.name
      await localDataApi.renameNoteFolder(node.path, nextPath, Date.now())
      recordQuickNoteActivity('Move Quick Note entry')
      const [loadedNotes, loadedFolders] = await Promise.all([localDataApi.listNotes(), localDataApi.listNoteFolders()])
      setNotes(loadedNotes)
      setFolders(loadedFolders)
      setFolderFilter(nextPath)
      if (draft.folderPath === node.path || draft.folderPath.startsWith(`${node.path}/`)) {
        setDraft((current) => ({ ...current, folderPath: `${nextPath}${current.folderPath.slice(node.path.length)}` }))
      }
      succeed('notice.moved', { path: nextPath })
    } catch (cause) { fail(cause) }
  }

  async function renameFolder(): Promise<void> {
    openRenameFolder()
  }

  async function removeFolder(): Promise<void> {
    if (folderFilter === '*' || !folderFilter || !await dialog.confirm(t('confirm.deleteFolder', { path: folderFilter }), { dangerous: true })) return
    try {
      const moved = await localDataApi.deleteNoteFolder(folderFilter)
      recordQuickNoteActivity('Delete Quick Note entry')
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

  function editorSelection(): { start: number; end: number } | null {
    const selection = editorViewRef.current?.state.selection.main
    if (!selection) return null
    return { start: selection.from, end: selection.to }
  }

  function selectEditorRange(start: number, end: number): void {
    const editor = editorViewRef.current
    if (!editor) return
    editor.dispatch({ selection: { anchor: start, head: end }, scrollIntoView: true })
    editor.focus()
  }

  function openFind(): void {
    if (!activeNote) return
    const selection = editorSelection()
    const selected = selection && selection.end > selection.start ? draft.content.slice(selection.start, selection.end) : undefined
    findIndexRef.current = 0
    setReplacedCount(0)
    if (selected !== undefined) setFind(selected)
    setFindVisible(true)
  }

  function findAround(forward: boolean): void {
    if (!find) return
    const selection = editorSelection()
    const fromIndex = forward ? selection?.end ?? findIndexRef.current : selection?.start ?? findIndexRef.current
    const result = findNextMatchWithWrap(draft.content, find, findOptions, fromIndex, forward)
    if (!result) {
      failMessage('find.noMatches')
      return
    }
    if (result.wrapped) succeed(forward ? 'find.wrappedStart' : 'find.wrappedEnd')
    findIndexRef.current = forward ? result.match.end : result.match.start
    selectEditorRange(result.match.start, result.match.end)
  }

  function replaceCurrent(all: boolean): void {
    if (!find) return
    if (all) {
      const result = replaceAllMatches(draft.content, find, replace, findOptions)
      if (result.count === 0) failMessage('find.noMatches')
      else {
        patchDraft({ content: result.content })
        setReplacedCount(result.count)
        succeed('notice.replaced', { count: result.count })
      }
      return
    }
    const result = replaceCurrentMatch(draft.content, find, replace, findOptions, editorSelection())
    if (!result.replaced) {
      failMessage('find.noMatches')
      return
    }
    patchDraft({ content: result.content })
    setReplacedCount((count) => count + 1)
    findIndexRef.current = result.nextFrom
    const query = find
    const options = findOptions
    window.requestAnimationFrame(() => {
      const match = findNextMatch(result.content, query, options, result.nextFrom, true)
      if (match) {
        findIndexRef.current = match.end
        selectEditorRange(match.start, match.end)
      }
    })
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

  function applyQuickReplace(action: QuickReplaceActionId): void {
    const selection = editorViewRef.current?.state.selection.main
    const start = selection?.from ?? 0
    const end = selection?.to ?? start
    const hasSelection = end > start
    const source = hasSelection ? draft.content.slice(start, end) : draft.content
    try {
      const transformed = runQuickReplace(source, action)
      patchDraft({ content: hasSelection ? `${draft.content.slice(0, start)}${transformed}${draft.content.slice(end)}` : transformed })
    } catch (cause) { fail(cause) }
  }

  async function openVault(): Promise<void> {
    try { await localDataApi.openNoteVault() } catch (cause) { fail(cause) }
  }

  function prefixLines(prefix: 'bullet' | 'numbered'): void {
    const selection = editorViewRef.current?.state.selection.main
    const start = selection?.from ?? 0
    const end = selection?.to ?? draft.content.length
    patchDraft({ content: prefixSelectedLines(draft.content, start, end, prefix) })
  }

  async function formatCurrent(): Promise<void> {
    if (!draft.content.trim() || busy) return
    setBusy(true)
    try {
      patchDraft({ content: await formatNoteContent(draft.content, draft.syntax, settings.editor.sqlDialect) })
      succeed('notice.formatted')
    } catch (cause) { fail(cause) } finally { setBusy(false) }
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
      rememberAttachment(attachment)
      insertRawMarkdown(attachment.markdown || `![image](attachments/${attachment.name})`)
      recordQuickNoteActivity('Add Quick Note attachment')
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
      fontSize: settings.editor.quickNoteFontSize,
      fontName: settings.editor.quickNoteFontName,
      lineSpacing: 1,
      relativePath: '',
      lineWrapping: settings.editor.softWrap,
      syntax: 'text/markdown',
      pinned: false,
      createdAt: now,
      updatedAt: now
    }
    const saved = await localDataApi.saveNote(note)
    setNotes((items) => sortNotes([saved, ...items]))
    applyNote(saved)
    recordQuickNoteActivity('Create Quick Note')
    return saved.id
  }

  async function attachImageFiles(files: File[]): Promise<void> {
    const images = files.filter((file) => /^image\/(png|jpeg|gif|bmp|webp)$/.test(file.type)).slice(0, 20)
    if (!images.length || busy) return
    try {
      const noteId = await ensureAttachmentNote(images[0].name || 'Pasted-image.png')
      if (!noteId) return
      setBusy(true)
      const imported: QuickNoteAttachment[] = []
      for (const file of images) imported.push(await quickNoteAttachmentApi.importImageFile(noteId, file))
      setAttachments((items) => [...imported, ...items])
      imported.forEach(rememberAttachment)
      insertRawMarkdown(imported.map((attachment) => attachment.markdown || `![image](attachments/${attachment.name})`).join('\n'))
      recordQuickNoteActivity('Paste Quick Note attachment')
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
      imported.forEach(rememberAttachment)
      insertRawMarkdown(imported.map((attachment) => attachment.markdown || `![image](attachments/${attachment.name})`).join('\n'))
      recordQuickNoteActivity('Add Quick Note attachment')
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

  function rememberAttachment(attachment: QuickNoteAttachment): void {
    if (attachment.relativePath && attachment.dataUrl) {
      setAttachmentUrls((current) => ({ ...current, [attachment.relativePath!]: attachment.dataUrl! }))
    }
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
        <button type="button" disabled={!activeNote} onClick={() => { if (activeNote) void exportTreeNotes([activeNote]) }}><Download />{t('toolbar.export')}</button>
        <button type="button" disabled={!activeNote} onClick={() => void duplicate()}><CopyPlus />{t('toolbar.duplicate')}</button>
        <span />
        <button type="button" disabled={!activeNote} onClick={() => { if (findVisible) { setFindVisible(false); setReplacedCount(0) } else openFind() }}><Search />{t('toolbar.findReplace')}</button>
        <button type="button" onClick={() => setGitOpen(true)}><GitBranch />{t('tree.git')}</button>
      </section>
      <QuickNoteWorkspace treeOpen={treeOpen} replaceOpen={quickReplaceOpen} replace={(
        <aside className="quick-replace-panel">
          <header><strong>{t('replace.title')}</strong><button type="button" aria-label={t('action.closeFind')} onClick={() => setQuickReplaceOpen(false)}><X /></button></header>
          <div>
            {quickReplaceActionIds.map((action) => <button type="button" key={action} disabled={!activeNote} onClick={() => applyQuickReplace(action)}>{t(`replace.${action}` as QuickNoteMessageKey)}</button>)}
          </div>
        </aside>
      )}>
        <aside className="quick-note-sidebar">
          <div className="quick-note-search">
            <Search />
            <input aria-label={t('search.placeholder')} value={query} placeholder={t('search.placeholder')} onChange={(event) => setQuery(event.target.value)} />
            <button type="button" aria-label={t('search.new')} onClick={openCreateNote}><FilePlus2 /></button>
          </div>
          <div className="quick-note-list-controls">
            <label><input type="checkbox" checked={includeContent} onChange={(event) => setIncludeContent(event.target.checked)} />{t('search.content')}</label>
            <select aria-label={t('search.sort')} value={noteSort} onChange={(event) => setNoteSort(event.target.value as NoteSort)}>
              <option value="modified">{t('search.sort.modified')}</option>
              <option value="created">{t('search.sort.created')}</option>
              <option value="name">{t('search.sort.name')}</option>
            </select>
          </div>
          <section className="quick-note-folders" aria-label={t('folder.tree')}>
            <header>
              <strong><Folder />{t('folder.tree')}</strong>
              <span>
                <button type="button" aria-label={t('folder.create')} onClick={openCreateFolder}><FolderPlus /></button>
                <button type="button" aria-label={t('folder.rename')} disabled={!folderFilter} onClick={() => void renameFolder()}><Pencil /></button>
                <button type="button" aria-label={t('folder.delete')} disabled={!folderFilter} onClick={() => void removeFolder()}><Trash2 /></button>
                <button type="button" aria-label={t(treeExpanded === 'expandAll' ? 'tree.collapseAll' : 'tree.expandAll')} onClick={() => void save((current) => ({ ...current, vault: { ...current.vault, quickNoteTreeExpandMode: current.vault.quickNoteTreeExpandMode === 'expandAll' ? 'collapseAll' : 'expandAll' } }))}>{treeExpanded === 'expandAll' ? <FoldVertical /> : <UnfoldVertical />}</button>
                <button type="button" aria-label={t('tree.locate')} disabled={!activeNote} onClick={() => { setQuery(''); setLocateRequest((value) => value + 1) }}><LocateFixed /></button>
              </span>
            </header>
          </section>
          <label className="quick-note-tag-filter"><Tag /><select value={tagFilter} aria-label={t('tags.filter')} onChange={(event) => setTagFilter(event.target.value)}><option value="">{t('tags.all')}</option>{allTags.map((tag) => <option key={tag} value={tag}>{tag}</option>)}</select></label>
          <div className="quick-note-tree-scroll">
            <QuickNoteTree
              nodes={noteTree}
              activeId={activeId}
              selectedFolder={folderFilter}
              selectedPath={activeTreePath}
              expandMode={treeExpanded}
              locateRequest={locateRequest}
              onOpenNote={(note) => {
                setFolderFilter(note.folderPath)
                void selectNote(note)
              }}
              onSelectFolder={(path) => { void selectFolder(path) }}
              onMove={(node, targetDirectory) => { void moveTreeNode(node, targetDirectory) }}
              onAction={(node, action, files) => { void runTreeAction(node, action, files) }}
              selectedLabel={(count) => t('tree.selected', { count })}
              labels={{
                rename: t('tree.rename'),
                move: t('tree.move'),
                duplicate: t('tree.duplicate'),
                export: t('tree.export'),
                info: t('tree.info'),
                delete: t('tree.delete'),
                reveal: t('tree.reveal')
              }}
              onGit={() => setGitOpen(true)}
              gitLabel={t('tree.git')}
            />
            {!noteTree.length && <div className="quick-note-empty"><NotebookPen /><span>{t(query ? 'note.noMatches' : 'note.none')}</span></div>}
          </div>
        </aside>
        <section className="quick-note-editor">
          <header>
            <input aria-label={t('editor.title')} disabled={!activeNote} value={activeNote ? draft.title : ''} placeholder={t('note.untitled')} onChange={(event) => patchDraft({ title: event.target.value })} />
            <button className="icon-button" type="button" aria-pressed={treeOpen} aria-label={t(treeOpen ? 'sidebar.hide' : 'sidebar.show')} title={t(treeOpen ? 'sidebar.hide' : 'sidebar.show')} onClick={() => setTreeOpen((value) => !value)}>{treeOpen ? <PanelLeftClose /> : <PanelLeftOpen />}</button>
            <div className="utility-segments">
              <button className={viewMode === 'editor' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setViewMode('editor')}>{t('view.editor')}</button>
              <button className={viewMode === 'split' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setViewMode('split')}><Columns2 />{t('view.split')}</button>
              <button className={viewMode === 'preview' ? 'utility-segment utility-segment--active' : 'utility-segment'} type="button" onClick={() => setViewMode('preview')}><Eye />{t('view.preview')}</button>
            </div>
            <button className="secondary-button" type="button" disabled={!activeNote || busy} onClick={() => void togglePinned()}>{activeNote?.pinned ? <PinOff /> : <Pin />}{t(activeNote?.pinned ? 'action.unpin' : 'action.pin')}</button>
            <button className="secondary-button quick-note-delete" type="button" disabled={!activeNote || busy} onClick={() => void remove()}><Trash2 />{t('action.delete')}</button>
            <button className="primary-button" type="button" disabled={!activeNote || !dirty || busy} onClick={() => void persistDraft(true)}><Save />{t(busy ? 'action.saving' : 'action.save')}</button>
          </header>
          <div className="quick-note-editor-tools">
            <button type="button" title={t('format.bold')} disabled={!activeNote} onClick={() => insertMarkdown('**', '**', t('format.text'))}><Bold /></button>
            <button type="button" title={t('format.italic')} disabled={!activeNote} onClick={() => insertMarkdown('_', '_', t('format.text'))}><Italic /></button>
            <button type="button" title={t('format.code')} disabled={!activeNote} onClick={() => insertMarkdown('`', '`', t('format.codeText'))}><Code2 /></button>
            <button type="button" title={t('format.list')} disabled={!activeNote} onClick={() => prefixLines('bullet')}><List /></button>
            <button type="button" title={t('format.numbered')} disabled={!activeNote} onClick={() => prefixLines('numbered')}><ListOrdered /></button>
            <button type="button" title={t('format.document')} disabled={!activeNote || !draft.content.trim() || busy} onClick={() => void formatCurrent()}><WandSparkles /></button>
            <button type="button" title={t('replace.title')} onClick={() => setQuickReplaceOpen((value) => !value)}><Replace /></button>
            <button type="button" title={t('replace.openVault')} onClick={() => void openVault()}><FolderOpen /></button>
            <button type="button" title={t('attachment.add')} disabled={!activeNote || busy} onClick={() => void importAttachment()}><Paperclip /></button>
            <label className="quick-note-transform"><Sparkles /><select aria-label={t('transform.label')} disabled={!activeNote} value={transform} onChange={(event) => setTransform(event.target.value)}><option value="uppercase">{t('transform.uppercase')}</option><option value="lowercase">{t('transform.lowercase')}</option><option value="trimLines">{t('transform.trimLines')}</option><option value="sortLines">{t('transform.sortLines')}</option><option value="uniqueLines">{t('transform.uniqueLines')}</option><option value="jsonPretty">{t('transform.jsonPretty')}</option></select><button type="button" disabled={!activeNote} onClick={applyQuickTransform}>{t('transform.apply')}</button></label>
            <label><Tag /><input value={tagText} disabled={!activeNote} placeholder={t('tags.placeholder')} onChange={(event) => patchTags(event.target.value)} /></label>
            <label><Palette /><select value={draft.color} disabled={!activeNote} aria-label={t('color.label')} onChange={(event) => patchDraft({ color: event.target.value as QuickNote['color'] })}>{NOTE_COLORS.map((color) => <option key={color} value={color}>{t(`color.${color}` as QuickNoteMessageKey)}</option>)}</select></label>
            <label><Folder /><select value={draft.folderPath} disabled={!activeNote} aria-label={t('folder.move')} onChange={(event) => patchDraft({ folderPath: event.target.value })}><option value="">{t('folder.root')}</option>{folders.map((folder) => <option key={folder.path} value={folder.path}>{folder.path}</option>)}</select></label>
            <label>{t('editor.syntax')}<select value={draft.syntax} disabled={!activeNote} onChange={(event) => patchDraft({ syntax: event.target.value as QuickNote['syntax'] })}>{NOTE_SYNTAX.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
            <FontSelect ariaLabel={t('editor.font')} disabled={!activeNote} value={draft.fontName} emptyLabel={t('editor.fontDefault')} labels={{ 'ui-monospace': t('editor.fontMono') }} onChange={(fontName) => {
              patchDraft({ fontName })
              if (fontName) void save({ ...settings, editor: { ...settings.editor, quickNoteFontName: fontName } })
            }} />
            <label>{t('editor.fontSize')}<input aria-label={t('editor.fontSize')} type="number" min={8} max={48} disabled={!activeNote} value={draft.fontSize} onChange={(event) => { const next = event.target.valueAsNumber; if (Number.isFinite(next)) patchDraft({ fontSize: Math.min(48, Math.max(8, Math.round(next))) }) }} /></label>
            <label>{t('editor.lineHeight')}<select value={draft.lineSpacing} disabled={!activeNote} onChange={(event) => { const lineSpacing = Number(event.target.value); patchDraft({ lineSpacing, lineHeight: lineSpacing <= 1.05 ? 'compact' : lineSpacing <= 1.45 ? 'normal' : 'relaxed' }) }}>{[1, 1.2, 1.4, 1.6, 1.8, 2].map((value) => <option key={value} value={value}>{value.toFixed(1)}×</option>)}</select></label>
            <label className="quick-note-wrap"><input type="checkbox" disabled={!activeNote} checked={draft.lineWrapping} onChange={(event) => patchDraft({ lineWrapping: event.target.checked })} />{t('editor.wrap')}</label>
            <button type="button" title={t('tree.info')} aria-label={t('tree.info')} disabled={!activeNote} onClick={() => setInfoVisible(true)}><Info /></button>
          </div>
          {findVisible && activeNote && (
            <div className="quick-note-find" onKeyDown={(event) => { if (event.key === 'Escape') { event.preventDefault(); setFindVisible(false); setReplacedCount(0) } }}>
              <input aria-label={t('find.placeholder')} autoFocus value={find} placeholder={t('find.placeholder')} onChange={(event) => { findIndexRef.current = 0; setReplacedCount(0); setFind(event.target.value) }} onKeyDown={(event) => { if (event.key === 'Enter' && !event.nativeEvent.isComposing) { event.preventDefault(); findAround(event.shiftKey ? false : true) } }} />
              <button type="button" disabled={!find} onClick={() => findAround(false)}>{t('find.previous')}</button>
              <button type="button" disabled={!find} onClick={() => findAround(true)}>{t('find.next')}</button>
              <span>{find ? t('find.found', { count: matchCount }) : ''}</span>
              <label><input type="checkbox" checked={findOptions.matchCase} onChange={(event) => { findIndexRef.current = 0; setReplacedCount(0); setFindOptions({ ...findOptions, matchCase: event.target.checked }) }} />{t('find.matchCase')}</label>
              <label><input type="checkbox" checked={findOptions.wholeWord} onChange={(event) => { findIndexRef.current = 0; setReplacedCount(0); setFindOptions({ ...findOptions, wholeWord: event.target.checked }) }} />{t('find.wholeWord')}</label>
              <label><input type="checkbox" checked={findOptions.regex} onChange={(event) => { findIndexRef.current = 0; setReplacedCount(0); setFindOptions({ ...findOptions, regex: event.target.checked }) }} />{t('find.regex')}</label>
              <input aria-label={t('replace.placeholder')} value={replace} placeholder={t('replace.placeholder')} onChange={(event) => setReplace(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter') { event.preventDefault(); replaceCurrent(false) } }} />
              <button type="button" disabled={!find} onClick={() => replaceCurrent(false)}>{t('action.replace')}</button>
              <button type="button" disabled={!find} onClick={() => replaceCurrent(true)}>{t('action.replaceAll')}</button>
              <span>{t('find.replaced', { count: replacedCount })}</span>
              <button type="button" aria-label={t('action.closeFind')} onClick={() => { setFindVisible(false); setReplacedCount(0) }}><X /></button>
            </div>
          )}
          {infoVisible && (
            <div className="shared-dialog-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setInfoVisible(false) }}>
              <div className="shared-dialog" role="dialog" aria-modal="true" aria-labelledby="quick-note-info-title">
                <h2 id="quick-note-info-title">{t('tree.info')}</h2>
                <dl className="quick-note-info">
                  <div><dt>{t('info.row.path')}</dt><dd>{activeNote?.relativePath || '—'}</dd></div>
                  <div><dt>{t('info.created')}</dt><dd>{formatNoteDate(activeNote?.createdAt, locale)}</dd></div>
                  <div><dt>{t('info.row.modified')}</dt><dd>{formatNoteDate(activeNote?.updatedAt, locale)}</dd></div>
                  <div><dt>{t('info.row.lines')}</dt><dd>{String(stats.lines)}</dd></div>
                  <div><dt>{t('info.row.words')}</dt><dd>{String(stats.words)}</dd></div>
                  <div><dt>{t('info.row.characters')}</dt><dd>{String(stats.characters)}</dd></div>
                </dl>
                <footer>
                  <button className="secondary-button" type="button" onClick={() => setInfoVisible(false)}>{t('info.close')}</button>
                </footer>
              </div>
            </div>
          )}
          <div
            className={`quick-note-content quick-note-content--${viewMode}${nativeDragActive ? ' quick-note-content--drag-active' : ''}`}
            style={{ ['--quick-note-note-font-size' as string]: `${draft.fontSize}px`, ['--quick-note-note-font-family' as string]: cssFontFamily(draft.fontName), ['--quick-note-note-line-spacing' as string]: String(draft.lineSpacing) }}
            onDragOver={window.__TAURI_INTERNALS__ ? undefined : (event) => event.preventDefault()}
            onDrop={window.__TAURI_INTERNALS__ ? undefined : (event) => { event.preventDefault(); void attachImageFiles([...event.dataTransfer.files]) }}
            onPaste={(event) => { const images = [...event.clipboardData.files].filter((file) => file.type.startsWith('image/')); if (images.length) { event.preventDefault(); void attachImageFiles(images) } }}
          >
            {activeNote ? (
              <>
                {nativeDragActive && <div className="quick-note-image-drop"><Paperclip />{t('attachment.dropImages')}</div>}
                {viewMode === 'split' ? (
                  <ResizableColumns id="quick-note-editor-preview" className="quick-note-editor-preview" initialPrimary={1} defaultSizes={[1, 1]} minPrimary={260} minSecondary={260}>
                    <CodeEditor key={draft.syntax} ariaLabel={t('editor.content')} value={draft.content} onChange={(content) => patchDraft({ content })} onReady={(view) => { editorViewRef.current = view }} className="utility-code-editor" lineWrapping={draft.lineWrapping} extensions={[...quickNoteLanguage(draft.syntax), ...QUICK_NOTE_EDITOR_EXTENSIONS]} />
                    <MarkdownPreview value={draft.content} images={attachmentUrls} />
                  </ResizableColumns>
                ) : (
                  <>
                    {viewMode !== 'preview' && <CodeEditor key={draft.syntax} ariaLabel={t('editor.content')} value={draft.content} onChange={(content) => patchDraft({ content })} onReady={(view) => { editorViewRef.current = view }} className="utility-code-editor" lineWrapping={draft.lineWrapping} extensions={[...quickNoteLanguage(draft.syntax), ...QUICK_NOTE_EDITOR_EXTENSIONS]} />}
                    {viewMode !== 'editor' && <MarkdownPreview value={draft.content} images={attachmentUrls} />}
                  </>
                )}
              </>
            ) : <div className="quick-note-select-empty">{t('status.empty')}</div>}
          </div>
          {!!attachments.length && <div className="quick-note-attachments"><strong><Paperclip />{t('attachment.title', { count: attachments.length })}</strong><div>{attachments.map((attachment) => <span key={attachment.id}><button type="button" title={t('attachment.export')} onClick={() => void exportAttachment(attachment)}><Download />{attachment.name}<small>{formatBytes(attachment.sizeBytes)}</small></button><button type="button" aria-label={t('attachment.delete', { name: attachment.name })} onClick={() => void removeAttachment(attachment)}><X /></button></span>)}</div></div>}
          <footer className="quick-note-statusbar">
            <span>{activeNote ? activeNote.relativePath || activeTreePath : t('status.empty')}</span>
            <span>{activeNote ? t(dirty ? 'status.unsaved' : 'status.saved') : ''}</span>
            <span>{t('status.summary', { lines: stats.lines, characters: stats.characters })}</span>
          </footer>
        </section>
      </QuickNoteWorkspace>
      <footer className={failed ? 'utility-status utility-status--error' : 'utility-status'}>
        <span>{failed ? <TriangleAlert /> : <CheckCircle2 />}{noticeText}</span>
        <span>{t('footer.capabilities')}</span>
        <code>{session.summary}</code>
      </footer>
      {reportError && <p className="tool-surface-report-error">{t('report.error', { error: reportError })}</p>}
      <QuickNoteGitPanel
        open={gitOpen}
        onClose={() => setGitOpen(false)}
        onChanged={() => {
          void Promise.all([localDataApi.listNotes(), localDataApi.listNoteFolders()]).then(([loaded, loadedFolders]) => {
            setNotes(loaded)
            setFolders(loadedFolders)
          }).catch(fail)
        }}
      />
      {entryAction && (
        <div className="shared-dialog-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setEntryAction(null) }}>
          <form className="shared-dialog" role="dialog" aria-modal="true" aria-labelledby="quick-note-entry-title" onSubmit={(event) => { event.preventDefault(); void submitEntryAction() }}>
            <h2 id="quick-note-entry-title">{t(entryAction.mode === 'move' ? 'tree.moveTitle' : 'tree.renameTitle')}</h2>
            <p>{t(entryAction.mode === 'move' ? 'tree.moveTarget' : 'note.createName')}</p>
            {entryAction.mode === 'move' ? (
              <select aria-label={t('tree.moveTarget')} value={entryAction.value} onChange={(event) => setEntryAction({ ...entryAction, value: event.target.value })}>
                <option value="">{t('tree.moveRoot')}</option>
                {moveDestinations(entryAction.node, notes, folders).map((path) => <option key={path} value={path}>{path}</option>)}
              </select>
            ) : (
              <input autoFocus aria-label={t('note.createName')} value={entryAction.value} onChange={(event) => setEntryAction({ ...entryAction, value: event.target.value })} />
            )}
            <footer>
              <button className="secondary-button" type="button" onClick={() => setEntryAction(null)}>{t('note.createCancel')}</button>
              <button className="primary-button" type="submit" disabled={busy || (entryAction.mode === 'rename' && !entryAction.value.trim())}>{t('tree.moveApply')}</button>
            </footer>
          </form>
        </div>
      )}
      {createMode && (
        <div className="shared-dialog-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setCreateMode(null) }}>
          <form className="shared-dialog" role="dialog" aria-modal="true" aria-labelledby="quick-note-create-title" onSubmit={(event) => { event.preventDefault(); void (createMode === 'folder' ? submitCreateFolder() : submitCreateNote()) }}>
            <h2 id="quick-note-create-title">{t(createMode === 'folder' ? 'folder.createTitle' : 'note.createTitle')}</h2>
            <p>{t('note.createName')}</p>
            <input autoFocus aria-label={t('note.createName')} value={createName} onChange={(event) => setCreateName(event.target.value)} />
            <footer>
              <button className="secondary-button" type="button" onClick={() => setCreateMode(null)}>{t('note.createCancel')}</button>
              <button className="primary-button" type="submit" disabled={busy || !createName.trim()}>{t('note.createAction')}</button>
            </footer>
          </form>
        </div>
      )}
    </main>
  )
}

function MarkdownPreview({ value, images }: { value: string; images: Record<string, string> }) {
  const blocks = markdownBlocks(value)
  return <article className="quick-note-preview">{blocks.map((block, index) => {
    if (block.kind === 'code') return <pre key={index}><code>{block.value}</code></pre>
    const heading = /^(#{1,3})\s+(.+)$/.exec(block.value)
    if (heading) { const Tag = `h${heading[1].length}` as 'h1' | 'h2' | 'h3'; return <Tag key={index}>{heading[2]}</Tag> }
    if (/^\s*[-*]\s+/.test(block.value)) return <li key={index}>{renderInline(block.value.replace(/^\s*[-*]\s+/, ''), images)}</li>
    if (/^>\s?/.test(block.value)) return <blockquote key={index}>{renderInline(block.value.replace(/^>\s?/, ''), images)}</blockquote>
    return block.value ? <p key={index}>{renderInline(block.value, images)}</p> : <br key={index} />
  })}</article>
}

function renderInline(value: string, images: Record<string, string>) {
  const parts = value.split(/(!\[[^\]]*\]\([^)]+\))/g)
  return parts.map((part, index) => {
    const image = /^!\[([^\]]*)\]\(([^)]+)\)$/.exec(part)
    if (!image) return part
    const source = images[image[2]] || (image[2].startsWith('data:image/') ? image[2] : '')
    return source ? <img key={index} alt={image[1] || 'image'} src={source} /> : <code key={index}>{image[2]}</code>
  })
}

function attachmentPaths(content: string): string[] {
  return [...new Set([...content.matchAll(/(?:\(|["'])(attachments\/[A-Za-z0-9_.-]+)(?:\)|["'])/g)].map((match) => match[1]))]
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
    fontSize: 14,
    fontName: '',
    lineSpacing: 1,
    lineWrapping: true,
    syntax: 'text/plain'
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
    fontSize: note.fontSize,
    fontName: note.fontName,
    lineSpacing: note.lineSpacing,
    lineWrapping: note.lineWrapping,
    syntax: normalizeNoteSyntax(note.syntax)
  }
}

const NOTE_SYNTAX: Array<[QuickNote['syntax'], string]> = [
  ['text/plain', 'Text'],
  ['text/markdown', 'Markdown'],
  ['application/json', 'JSON'],
  ['text/java', 'Java'],
  ['text/javascript', 'JavaScript'],
  ['text/typescript', 'TypeScript'],
  ['text/python', 'Python'],
  ['text/html', 'HTML'],
  ['text/xml', 'XML'],
  ['text/yaml', 'YAML'],
  ['text/sql', 'SQL']
]

function inferSyntax(name: string): QuickNote['syntax'] {
  if (/\.json$/i.test(name)) return 'application/json'
  if (/\.java$/i.test(name)) return 'text/java'
  if (/\.js$/i.test(name)) return 'text/javascript'
  if (/\.ts$/i.test(name)) return 'text/typescript'
  if (/\.py$/i.test(name)) return 'text/python'
  if (/\.html?$/i.test(name)) return 'text/html'
  if (/\.xml$/i.test(name)) return 'text/xml'
  if (/\.ya?ml$/i.test(name)) return 'text/yaml'
  if (/\.sql$/i.test(name)) return 'text/sql'
  if (/\.md$/i.test(name)) return 'text/markdown'
  return 'text/plain'
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
function sanitizeFolderName(value: string): string {
  const title = value.trim()
  if (!title || title.length > 180 || /[\r\n\0]/.test(title)) throw new Error('Invalid title')
  const sanitized = title.replace(/[\\/:*?"<>|]/g, '_').replace(/^\.+/, '').trim().slice(0, 180)
  if (!sanitized) throw new Error('Invalid file name')
  return sanitized
}

function moveDestinations(node: NoteTreeNode, notes: QuickNote[], folders: QuickNoteFolder[]): string[] {
  const paths = new Set(folders.map((folder) => folder.path))
  for (const note of notes) {
    if (!note.folderPath) continue
    const parts = note.folderPath.split('/')
    for (let index = 1; index <= parts.length; index += 1) paths.add(parts.slice(0, index).join('/'))
  }
  return [...paths].filter((path) => path !== node.path && !path.startsWith(`${node.path}/`)).sort((left, right) => left.localeCompare(right))
}

function normalizeFolderPath(value: string): string { return value.trim().replace(/\\/g, '/').split('/').map((part) => part.trim()).filter((part) => part && part !== '.' && part !== '..').join('/').slice(0, 512) }
function firstContentLine(content: string): string { return content.split(/\r?\n/).find((line) => line.trim())?.replace(/^#+\s*/, '').slice(0, 80) ?? '' }
function formatTime(value: number, locale: string): string { return new Intl.DateTimeFormat(locale, { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }).format(value) }
function documentStats(value: string) { return { lines: value ? value.split(/\r?\n/).length : 0, words: value.trim() ? value.trim().split(/\s+/).length : 0, characters: value.length } }

function formatNoteDate(value: number | undefined, locale: string): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString(locale)
}
const NOTE_COLORS: QuickNote['color'][] = ['default', 'coral', 'yellow', 'green', 'blue', 'purple', 'red']
function parseTags(value: string): string[] { return normalizeTags(value.split(/[,，]/)) }
function normalizeTags(values: string[]): string[] { return [...new Set(values.map((value) => value.trim().replace(/^#/, '')).filter(Boolean))].slice(0, 32) }
function formatBytes(value: number): string { return value < 1024 ? `${value} B` : value < 1024 * 1024 ? `${(value / 1024).toFixed(1)} KB` : `${(value / (1024 * 1024)).toFixed(1)} MB` }
