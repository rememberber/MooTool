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
  Folder,
  FolderOpen,
  FolderPlus,
  GitBranch,
  NotebookPen,
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
  WandSparkles,
  X
} from 'lucide-react'
import { getCurrentWebview } from '@tauri-apps/api/webview'
import { crosshairCursor, rectangularSelection, type EditorView } from '@codemirror/view'
import { useEffect, useMemo, useRef, useState } from 'react'
import {
  useLocalizedMessages,
  type LocalizedMessageKey,
  type MessageValues
} from '../../app/localizedMessages'
import { localDataApi } from '../../platform/api/localDataApi'
import { quickNoteGitApi } from '../../platform/api/quickNoteGitApi'
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
import { formatNoteContent, prefixSelectedLines } from './quickNoteEditing'
import { quickNoteLanguage } from './quickNoteLanguage'
import { quickReplaceActionIds, runQuickReplace, type QuickReplaceActionId } from './quickReplace'
import { quickNoteMessages } from './quickNoteMessages'
import { QuickNoteGitPanel } from './QuickNoteGitPanel'
import { buildNoteTree, canMoveToDirectory, QuickNoteTree, type NoteTreeAction, type NoteTreeNode } from './QuickNoteTree'

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

export function QuickNoteSurface() {
  const dialog = useDesktopDialog()
  const { settings } = useSettings()
  const { t, locale } = useLocalizedMessages(quickNoteMessages)
  const [notes, setNotes] = useState<QuickNote[]>([])
  const [folders, setFolders] = useState<QuickNoteFolder[]>([])
  const [folderFilter, setFolderFilter] = useState('')
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
  const [gitOpen, setGitOpen] = useState(false)
  const [quickReplaceOpen, setQuickReplaceOpen] = useState(false)
  const [nativeDragActive, setNativeDragActive] = useState(false)
  const [attachments, setAttachments] = useState<QuickNoteAttachment[]>([])
  const [attachmentUrls, setAttachmentUrls] = useState<Record<string, string>>({})
  const editorViewRef = useRef<EditorView | undefined>(undefined)
  const revisionRef = useRef(0)
  const activeNote = notes.find((note) => note.id === activeId)
  const noteTree = useMemo(() => buildNoteTree(notes, folders, query, tagFilter), [folders, notes, query, tagFilter])
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
        fontSize: restored.fontSize ?? settings.editor.quickNoteFontSize, fontName: restored.fontName ?? '',
        lineSpacing: restored.lineSpacing ?? 1, lineWrapping: restored.lineWrapping ?? true, syntax: restored.syntax ?? 'markdown'
      })
      setTagText((restored.tags ?? []).join(', '))
      setFolderFilter(restored.folderPath ?? '')
      setDirty(true)
      setViewMode('split')
    } catch { /* Ignore legacy records without a note snapshot. */ }
  })

  useEffect(() => {
    if (!settings.vault.autoCommit || !window.__TAURI_INTERNALS__) return
    const timer = window.setTimeout(() => {
      void quickNoteGitApi.action({ action: 'commit', message: 'MooTool Quick Note checkpoint' }).catch(() => undefined)
    }, settings.vault.autoCommitIdleSeconds * 1000)
    return () => window.clearTimeout(timer)
  }, [notes, settings.vault.autoCommit, settings.vault.autoCommitIdleSeconds])

  useEffect(() => {
    if (!settings.vault.autoCommit || !window.__TAURI_INTERNALS__) return
    let timer = 0
    const onHide = () => {
      window.clearTimeout(timer)
      if (document.hidden) {
        timer = window.setTimeout(() => {
          void quickNoteGitApi.action({ action: 'commit', message: 'MooTool Quick Note checkpoint' }).catch(() => undefined)
        }, settings.vault.autoCommitInactiveSeconds * 1000)
      }
    }
    document.addEventListener('visibilitychange', onHide)
    return () => { document.removeEventListener('visibilitychange', onHide); window.clearTimeout(timer) }
  }, [settings.vault.autoCommit, settings.vault.autoCommitInactiveSeconds])

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
        fontSize: draft.fontSize,
        fontName: draft.fontName,
        lineSpacing: draft.lineSpacing,
        relativePath: '',
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
    const initial = folderFilter && folderFilter !== '*' ? `${folderFilter}/` : ''
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

  async function runTreeAction(node: NoteTreeNode, action: NoteTreeAction): Promise<void> {
    if (node.kind === 'directory') setFolderFilter(node.path)
    else if (node.note && node.note.id !== activeId) await selectNote(node.note)
    if (action === 'rename') return renameTreeNode(node)
    if (action === 'move') return promptMove(node)
    if (action === 'duplicate' && node.note) return node.note.id === activeId ? duplicate() : duplicateNote(node.note)
    if (action === 'export' && node.note) return exportTreeNote(node.note)
    if (action === 'info' && node.note) { setInfoVisible(true); return }
    if (action === 'delete') return deleteTreeNode(node)
    if (action === 'reveal') {
      try { await localDataApi.revealNoteEntry(node.path) }
      catch (cause) { fail(cause) }
    }
  }

  async function renameTreeNode(node: NoteTreeNode): Promise<void> {
    const currentName = node.kind === 'file' ? (node.note?.id === activeId ? draft.title : node.note?.title ?? node.name) : node.name
    const entered = await dialog.prompt(t('tree.renamePrompt'), currentName)
    const name = entered?.trim()
    if (!name || name === currentName) return
    if (node.kind === 'file' && node.note) {
      const movingActive = node.note.id === activeId
      const source = movingActive ? {
        ...node.note,
        title: name,
        content: draft.content,
        tags: parseTags(tagText),
        color: draft.color,
        folderPath: draft.folderPath,
        editorFont: draft.editorFont,
        lineHeight: draft.lineHeight,
        fontSize: draft.fontSize,
        fontName: draft.fontName,
        lineSpacing: draft.lineSpacing,
        lineWrapping: draft.lineWrapping,
        syntax: draft.syntax
      } : { ...node.note, title: name }
      try {
        const saved = await localDataApi.saveNote({ ...source, updatedAt: Date.now() })
        setNotes((items) => sortNotes([saved, ...items.filter((item) => item.id !== saved.id)]))
        if (movingActive) { setDraft(noteDraft(saved)); setTagText(saved.tags.join(', ')); setDirty(false) }
        succeed('notice.renamed', { name: saved.title })
      } catch (cause) { fail(cause) }
      return
    }
    const parent = node.path.includes('/') ? node.path.slice(0, node.path.lastIndexOf('/')) : ''
    const nextPath = parent ? `${parent}/${normalizeFolderPath(name)}` : normalizeFolderPath(name)
    if (!nextPath || nextPath === node.path) return
    try {
      await localDataApi.renameNoteFolder(node.path, nextPath, Date.now())
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

  async function promptMove(node: NoteTreeNode): Promise<void> {
    const current = node.kind === 'file' ? node.note?.folderPath ?? '' : (node.path.includes('/') ? node.path.slice(0, node.path.lastIndexOf('/')) : '')
    const entered = await dialog.prompt(t('tree.movePrompt'), current)
    if (entered === null) return
    await moveTreeNode(node, normalizeFolderPath(entered))
  }

  async function duplicateNote(note: QuickNote): Promise<void> {
    const now = Date.now()
    try {
      const saved = await localDataApi.saveNote({ ...note, id: crypto.randomUUID(), title: t('duplicate.title', { title: note.title || t('note.untitled') }), pinned: false, createdAt: now, updatedAt: now })
      setNotes((items) => sortNotes([saved, ...items]))
      applyNote(saved)
      succeed('notice.duplicated')
    } catch (cause) { fail(cause) }
  }

  async function exportTreeNote(note: QuickNote): Promise<void> {
    const content = note.id === activeId ? draft.content : note.content
    const title = note.id === activeId ? draft.title : note.title
    try {
      const path = await userFilesApi.exportText(`${safeFileName(title || 'note')}.md`, content)
      if (path) succeed('notice.exported', { path })
    } catch (cause) { fail(cause) }
  }

  async function deleteTreeNode(node: NoteTreeNode): Promise<void> {
    if (node.kind === 'file') {
      if (node.note?.id === activeId) return remove()
      if (!node.note || !await dialog.confirm(t('confirm.delete', { title: node.note.title }), { dangerous: true })) return
      try {
        await localDataApi.deleteNote(node.note.id)
        setNotes((items) => items.filter((item) => item.id !== node.note?.id))
        succeed('notice.deleted')
      } catch (cause) { fail(cause) }
      return
    }
    if (!await dialog.confirm(t('confirm.deleteFolder', { path: node.path }), { dangerous: true })) return
    try {
      const moved = await localDataApi.deleteNoteFolder(node.path)
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
        const source = movingActive ? {
          ...node.note,
          title: draft.title.trim() || firstContentLine(draft.content) || node.note.title,
          content: draft.content,
          tags: parseTags(tagText),
          color: draft.color,
          editorFont: draft.editorFont,
          lineHeight: draft.lineHeight,
          fontSize: draft.fontSize,
          fontName: draft.fontName,
          lineSpacing: draft.lineSpacing,
          lineWrapping: draft.lineWrapping,
          syntax: draft.syntax
        } : node.note
        const saved = await localDataApi.saveNote({ ...source, folderPath: targetDirectory, updatedAt: Date.now() })
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
      fontName: settings.editor.quickNoteFontFamily === 'mono' ? 'ui-monospace' : '',
      lineSpacing: 1,
      relativePath: '',
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
        <button type="button" disabled={!draft.title && !draft.content} onClick={() => void exportNote()}><Download />{t('toolbar.export')}</button>
        <button type="button" disabled={!activeNote} onClick={() => void duplicate()}><CopyPlus />{t('toolbar.duplicate')}</button>
        <span />
        <button type="button" onClick={() => setFindVisible((value) => !value)}><Search />{t('toolbar.findReplace')}</button>
        <button type="button" onClick={() => setGitOpen(true)}><GitBranch />{t('tree.git')}</button>
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
                <button type="button" aria-label={t('folder.rename')} disabled={!folderFilter} onClick={() => void renameFolder()}><Pencil /></button>
                <button type="button" aria-label={t('folder.delete')} disabled={!folderFilter} onClick={() => void removeFolder()}><Trash2 /></button>
              </span>
            </header>
          </section>
          <label className="quick-note-tag-filter"><Tag /><select value={tagFilter} aria-label={t('tags.filter')} onChange={(event) => setTagFilter(event.target.value)}><option value="">{t('tags.all')}</option>{allTags.map((tag) => <option key={tag} value={tag}>{tag}</option>)}</select></label>
          <div className="quick-note-tree-scroll">
            <QuickNoteTree
              nodes={noteTree}
              activeId={activeId}
              selectedFolder={folderFilter}
              onOpenNote={(note) => {
                setFolderFilter(note.folderPath)
                void selectNote(note)
              }}
              onSelectFolder={setFolderFilter}
              onMove={(node, targetDirectory) => { void moveTreeNode(node, targetDirectory) }}
              onAction={(node, action) => { void runTreeAction(node, action) }}
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
        <section className={quickReplaceOpen ? 'quick-note-editor quick-note-editor--replace' : 'quick-note-editor'}>
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
            <button type="button" title={t('format.list')} onClick={() => prefixLines('bullet')}><List /></button>
            <button type="button" title={t('format.numbered')} onClick={() => prefixLines('numbered')}><ListOrdered /></button>
            <button type="button" title={t('format.document')} disabled={!draft.content.trim() || busy} onClick={() => void formatCurrent()}><WandSparkles /></button>
            <button type="button" title={t('replace.title')} onClick={() => setQuickReplaceOpen((value) => !value)}><Replace /></button>
            <button type="button" title={t('replace.openVault')} onClick={() => void openVault()}><FolderOpen /></button>
            <button type="button" title={t('attachment.add')} disabled={!activeNote || busy} onClick={() => void importAttachment()}><Paperclip /></button>
            <label className="quick-note-transform"><Sparkles /><select aria-label={t('transform.label')} value={transform} onChange={(event) => setTransform(event.target.value)}><option value="uppercase">{t('transform.uppercase')}</option><option value="lowercase">{t('transform.lowercase')}</option><option value="trimLines">{t('transform.trimLines')}</option><option value="sortLines">{t('transform.sortLines')}</option><option value="uniqueLines">{t('transform.uniqueLines')}</option><option value="jsonPretty">{t('transform.jsonPretty')}</option></select><button type="button" onClick={applyQuickTransform}>{t('transform.apply')}</button></label>
            <label><Tag /><input value={tagText} placeholder={t('tags.placeholder')} onChange={(event) => patchTags(event.target.value)} /></label>
            <label><Palette /><select value={draft.color} aria-label={t('color.label')} onChange={(event) => patchDraft({ color: event.target.value as QuickNote['color'] })}>{NOTE_COLORS.map((color) => <option key={color} value={color}>{t(`color.${color}` as QuickNoteMessageKey)}</option>)}</select></label>
            <label><Folder /><select value={draft.folderPath} aria-label={t('folder.move')} onChange={(event) => patchDraft({ folderPath: event.target.value })}><option value="">{t('folder.root')}</option>{folders.map((folder) => <option key={folder.path} value={folder.path}>{folder.path}</option>)}</select></label>
            <label>{t('editor.syntax')}<select value={draft.syntax} onChange={(event) => patchDraft({ syntax: event.target.value as QuickNote['syntax'] })}>{NOTE_SYNTAX.map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label>
            <label>{t('editor.font')}<select value={draft.editorFont} onChange={(event) => patchDraft({ editorFont: event.target.value as QuickNote['editorFont'] })}><option value="default">{t('editor.fontDefault')}</option><option value="mono">{t('editor.fontMono')}</option><option value="serif">{t('editor.fontSerif')}</option></select></label>
            <label>{t('editor.font')}<select value={draft.fontName} onChange={(event) => patchDraft({ fontName: event.target.value, editorFont: event.target.value === 'ui-monospace' ? 'mono' : 'default' })}><option value="">{t('editor.fontDefault')}</option><option value="ui-monospace">{t('editor.fontMono')}</option></select></label>
            <label>{t('editor.fontSize')}<input aria-label={t('editor.fontSize')} type="number" min={8} max={48} value={draft.fontSize} onChange={(event) => { const next = event.target.valueAsNumber; if (Number.isFinite(next)) patchDraft({ fontSize: Math.min(48, Math.max(8, Math.round(next))) }) }} /></label>
            <label>{t('editor.lineHeight')}<select value={draft.lineSpacing} onChange={(event) => { const lineSpacing = Number(event.target.value); patchDraft({ lineSpacing, lineHeight: lineSpacing <= 1.05 ? 'compact' : lineSpacing <= 1.45 ? 'normal' : 'relaxed' }) }}>{[1, 1.2, 1.4, 1.6, 1.8, 2].map((value) => <option key={value} value={value}>{value.toFixed(1)}×</option>)}</select></label>
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
          {infoVisible && <aside className="quick-note-info"><strong><Info />{t('info.title')}</strong><span>{t('info.id')} <code>{activeId || t('info.unsaved')}</code></span><span>{t('info.path')} <code>{activeNote?.relativePath || '—'}</code></span><span>{t('info.folder')} <code>{draft.folderPath || t('folder.root')}</code></span><span>{t('info.created')} <time>{activeNote ? new Date(activeNote.createdAt).toLocaleString(locale) : '—'}</time></span><span>{t('info.updated')} <time>{activeNote ? new Date(activeNote.updatedAt).toLocaleString(locale) : '—'}</time></span><span>{t('info.attachments')} <b>{attachments.length}</b></span></aside>}
          <div
            className={`quick-note-content quick-note-content--${viewMode}${nativeDragActive ? ' quick-note-content--drag-active' : ''}`}
            style={{ ['--quick-note-note-font-size' as string]: `${draft.fontSize}px`, ['--quick-note-note-font-family' as string]: draft.fontName || 'inherit', ['--quick-note-note-line-spacing' as string]: String(draft.lineSpacing) }}
            onDragOver={window.__TAURI_INTERNALS__ ? undefined : (event) => event.preventDefault()}
            onDrop={window.__TAURI_INTERNALS__ ? undefined : (event) => { event.preventDefault(); void attachImageFiles([...event.dataTransfer.files]) }}
            onPaste={(event) => { const images = [...event.clipboardData.files].filter((file) => file.type.startsWith('image/')); if (images.length) { event.preventDefault(); void attachImageFiles(images) } }}
          >
            {nativeDragActive && <div className="quick-note-image-drop"><Paperclip />{t('attachment.dropImages')}</div>}
            {viewMode !== 'preview' && <CodeEditor key={draft.syntax} ariaLabel={t('editor.content')} value={draft.content} onChange={(content) => patchDraft({ content })} onReady={(view) => { editorViewRef.current = view }} className={`utility-code-editor quick-note-code--font-${draft.editorFont}`} lineWrapping={draft.lineWrapping} extensions={[...quickNoteLanguage(draft.syntax), ...QUICK_NOTE_EDITOR_EXTENSIONS]} />}
            {viewMode !== 'editor' && <MarkdownPreview value={draft.content} images={attachmentUrls} />}
          </div>
          {!!attachments.length && <div className="quick-note-attachments"><strong><Paperclip />{t('attachment.title', { count: attachments.length })}</strong><div>{attachments.map((attachment) => <span key={attachment.id}><button type="button" title={t('attachment.export')} onClick={() => void exportAttachment(attachment)}><Download />{attachment.name}<small>{formatBytes(attachment.sizeBytes)}</small></button><button type="button" aria-label={t('attachment.delete', { name: attachment.name })} onClick={() => void removeAttachment(attachment)}><X /></button></span>)}</div></div>}
          {quickReplaceOpen && (
            <aside className="quick-replace-panel">
              <header><strong>{t('replace.title')}</strong><button type="button" aria-label={t('action.closeFind')} onClick={() => setQuickReplaceOpen(false)}><X /></button></header>
              <div>
                {quickReplaceActionIds.map((action) => <button type="button" key={action} onClick={() => applyQuickReplace(action)}>{t(`replace.${action}` as QuickNoteMessageKey)}</button>)}
              </div>
            </aside>
          )}
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
    fontSize: note.fontSize,
    fontName: note.fontName,
    lineSpacing: note.lineSpacing,
    lineWrapping: note.lineWrapping,
    syntax: note.syntax
  }
}

const NOTE_SYNTAX: Array<[QuickNote['syntax'], string]> = [
  ['plain', 'Text'],
  ['markdown', 'Markdown'],
  ['json', 'JSON'],
  ['java', 'Java'],
  ['javascript', 'JavaScript'],
  ['typescript', 'TypeScript'],
  ['python', 'Python'],
  ['html', 'HTML'],
  ['xml', 'XML'],
  ['yaml', 'YAML'],
  ['sql', 'SQL']
]

function inferSyntax(name: string): QuickNote['syntax'] {
  if (/\.json$/i.test(name)) return 'json'
  if (/\.java$/i.test(name)) return 'java'
  if (/\.js$/i.test(name)) return 'javascript'
  if (/\.ts$/i.test(name)) return 'typescript'
  if (/\.py$/i.test(name)) return 'python'
  if (/\.html?$/i.test(name)) return 'html'
  if (/\.xml$/i.test(name)) return 'xml'
  if (/\.ya?ml$/i.test(name)) return 'yaml'
  if (/\.sql$/i.test(name)) return 'sql'
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
