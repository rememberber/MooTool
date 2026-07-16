import DOMPurify from 'dompurify'
import {
  CopyPlus,
  Download,
  FilePlus2,
  FolderOpen,
  FolderPlus,
  GitBranch,
  ImagePlus,
  Info,
  List,
  ListOrdered,
  MoreHorizontal,
  PanelLeftClose,
  PanelLeftOpen,
  Replace,
  Save,
  Search,
  Trash2,
  WandSparkles,
  WrapText,
  X
} from 'lucide-react'
import { marked } from 'marked'
import { useCallback, useEffect, useMemo, useReducer, useRef } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { VaultGitDialog } from '@/features/json/VaultGitDialog'
import { formatCode } from '@/features/reformat/reformatTools'
import { Dialog } from '@/shared/components/Dialog'
import { Tooltip } from '@/shared/components/Tooltip'
import type { QuickNoteFile, QuickNoteMetadata, QuickNoteNode, QuickNoteSort } from '@/shared/contracts/quickNote'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import type { MessageKey } from '@/shared/i18n/messages'
import { QuickNoteTree } from './QuickNoteTree'
import { quickReplaceActionIds, runQuickReplace, type QuickReplaceActionId } from './quickReplace'

type ViewMode = 'editor' | 'split' | 'preview'
type EntryKind = '' | 'file' | 'directory'
type ActionDialogMode = 'createNote' | 'createFolder' | 'rename' | 'move' | 'delete' | null

type QuickNoteState = {
  nodes: QuickNoteNode[]
  selectedPath: string
  selectedKind: EntryKind
  note: QuickNoteFile | null
  content: string
  expanded: Set<string>
  query: string
  includeContent: boolean
  sort: QuickNoteSort
  treeOpen: boolean
  quickReplaceOpen: boolean
  findOpen: boolean
  findText: string
  replaceText: string
  viewMode: ViewMode
  actionMode: ActionDialogMode
  actionValue: string
  gitOpen: boolean
  infoOpen: boolean
  busy: boolean
  metadataDirty: boolean
}

const initialState: QuickNoteState = {
  nodes: [],
  selectedPath: '',
  selectedKind: '',
  note: null,
  content: '',
  expanded: new Set(),
  query: '',
  includeContent: true,
  sort: 'modified',
  treeOpen: true,
  quickReplaceOpen: false,
  findOpen: false,
  findText: '',
  replaceText: '',
  viewMode: 'editor',
  actionMode: null,
  actionValue: '',
  gitOpen: false,
  infoOpen: false,
  busy: false,
  metadataDirty: false
}

function updateState(state: QuickNoteState, patch: Partial<QuickNoteState>): QuickNoteState {
  return { ...state, ...patch }
}

const syntaxOptions = [
  ['text/plain', 'Text'],
  ['text/markdown', 'Markdown'],
  ['application/json', 'JSON'],
  ['text/java', 'Java'],
  ['text/javascript', 'JavaScript'],
  ['text/typescript', 'TypeScript'],
  ['text/python', 'Python'],
  ['text/xml', 'XML'],
  ['text/yaml', 'YAML'],
  ['text/sql', 'SQL']
] as const

const noteColors = [
  ['default', 'var(--text-muted)'],
  ['coral', '#d97868'],
  ['yellow', '#c99535'],
  ['green', '#4e9275'],
  ['blue', '#4f83cc'],
  ['purple', '#8a72b5'],
  ['red', '#c96761']
] as const

export function QuickNoteTool() {
  const { t } = useI18n()
  const { settings } = useSettings()
  const toast = useToast()
  const editorRef = useRef<HTMLTextAreaElement>(null)
  const lineNumbersRef = useRef<HTMLPreElement>(null)
  const findIndexRef = useRef(0)
  const [state, update] = useReducer(updateState, initialState)
  const dirty = state.note !== null && (state.content !== state.note.content || state.metadataDirty)
  const directories = useMemo(() => flattenDirectories(state.nodes), [state.nodes])
  const stats = useMemo(() => documentStats(state.content), [state.content])
  const attachmentPaths = useMemo(() => extractAttachmentPaths(state.content), [state.content])
  const attachmentKey = attachmentPaths.join('\n')
  const [attachmentUrls, setAttachmentUrls] = useReducer(
    (current: Record<string, string>, next: Record<string, string>) => ({ ...current, ...next }),
    {}
  )
  const previewHtml = useMemo(() => renderPreview(state.content, state.note?.metadata.syntax, attachmentUrls), [attachmentUrls, state.content, state.note?.metadata.syntax])

  const loadTree = useCallback(async (): Promise<QuickNoteNode[]> => {
    const nodes = await window.mootool.listQuickNotes({
      keyword: state.query,
      includeContent: state.includeContent,
      sort: state.sort,
      hideIgnored: settings.vault.hideGitignoredFiles
    })
    update({ nodes })
    return nodes
  }, [settings.vault.hideGitignoredFiles, state.includeContent, state.query, state.sort])

  useEffect(() => {
    let cancelled = false
    void loadTree().then(async (nodes) => {
      if (cancelled || state.selectedPath) return
      const first = firstFile(nodes)
      if (!first) return
      const note = await window.mootool.readQuickNote(first.relativePath)
      if (!cancelled) update({ selectedPath: note.relativePath, selectedKind: 'file', note, content: note.content, metadataDirty: false })
    }).catch((error) => toast.error(errorMessage(error)))
    return () => { cancelled = true }
  }, [loadTree, state.selectedPath, toast])

  useEffect(() => window.mootool.onQuickNoteVaultChange(() => {
    void loadTree()
    if (!dirty && state.note) {
      void window.mootool.readQuickNote(state.note.relativePath)
        .then((note) => update({ note, content: note.content, metadataDirty: false }))
        .catch(() => undefined)
    }
  }), [dirty, loadTree, state.note])

  useEffect(() => {
    if (!attachmentKey) return
    const missing = attachmentPaths.filter((path) => !attachmentUrls[path])
    if (!missing.length) return
    let cancelled = false
    void Promise.all(missing.map(async (path) => [path, await window.mootool.readQuickNoteAttachment(path)] as const))
      .then((entries) => { if (!cancelled) setAttachmentUrls(Object.fromEntries(entries)) })
      .catch(() => undefined)
    return () => { cancelled = true }
  }, [attachmentKey, attachmentPaths, attachmentUrls])

  useEffect(() => {
    const handleShortcut = (event: KeyboardEvent) => {
      if (!(event.metaKey || event.ctrlKey)) return
      if (event.key.toLocaleLowerCase() === 's') {
        event.preventDefault()
        void saveCurrent()
      } else if (event.key.toLocaleLowerCase() === 'f') {
        event.preventDefault()
        update({ findOpen: true })
      }
    }
    window.addEventListener('keydown', handleShortcut)
    return () => window.removeEventListener('keydown', handleShortcut)
  })

  async function saveCurrent(showToast = true): Promise<QuickNoteFile | null> {
    if (!state.note) return null
    update({ busy: true })
    try {
      const note = await window.mootool.saveQuickNote({
        relativePath: state.note.relativePath,
        content: state.content,
        metadata: state.note.metadata
      })
      update({ note, content: note.content, busy: false, metadataDirty: false })
      await loadTree()
      if (showToast) toast.success(t('quickNote.saved'))
      return note
    } catch (error) {
      update({ busy: false })
      toast.error(errorMessage(error))
      return null
    }
  }

  async function selectNode(node: QuickNoteNode): Promise<void> {
    if (node.kind === 'directory') {
      if (dirty) await saveCurrent(false)
      update({ selectedPath: node.relativePath, selectedKind: 'directory', note: null, content: '', metadataDirty: false })
      return
    }
    if (node.relativePath === state.note?.relativePath) return
    if (dirty && !await saveCurrent(false)) return
    update({ busy: true })
    try {
      const note = await window.mootool.readQuickNote(node.relativePath)
      update({ selectedPath: note.relativePath, selectedKind: 'file', note, content: note.content, busy: false, metadataDirty: false })
    } catch (error) {
      update({ busy: false })
      toast.error(errorMessage(error))
    }
  }

  function toggleDirectory(path: string): void {
    const expanded = new Set(state.expanded)
    if (expanded.has(path)) expanded.delete(path)
    else expanded.add(path)
    update({ expanded })
  }

  function openAction(mode: Exclude<ActionDialogMode, null>): void {
    const defaultValue = mode === 'rename'
      ? state.note?.metadata.title ?? directoryLeaf(state.selectedPath)
      : mode === 'move'
        ? parentDirectory(state.selectedPath)
        : ''
    update({ actionMode: mode, actionValue: defaultValue })
  }

  async function runActionDialog(): Promise<void> {
    const mode = state.actionMode
    if (!mode) return
    update({ busy: true })
    try {
      if (mode === 'createNote') {
        const note = await window.mootool.createQuickNote({
          title: state.actionValue,
          parentPath: selectedDirectory(state.selectedPath, state.selectedKind),
          fontSize: settings.editor.quickNoteFontSize,
          lineWrap: settings.editor.softWrap
        })
        await loadTree()
        update({ actionMode: null, actionValue: '', selectedPath: note.relativePath, selectedKind: 'file', note, content: note.content, busy: false, metadataDirty: false })
        return
      }
      if (mode === 'createFolder') {
        const parent = selectedDirectory(state.selectedPath, state.selectedKind)
        const path = [parent, state.actionValue.trim()].filter(Boolean).join('/')
        const created = await window.mootool.createQuickNoteFolder(path)
        const expanded = new Set(state.expanded).add(parent).add(created)
        await loadTree()
        update({ actionMode: null, actionValue: '', selectedPath: created, selectedKind: 'directory', note: null, content: '', expanded, busy: false, metadataDirty: false })
        return
      }
      if (mode === 'rename') {
        const nextPath = await window.mootool.renameQuickNoteEntry({ relativePath: state.selectedPath, name: state.actionValue })
        await loadTree()
        if (state.selectedKind === 'file') {
          const note = await window.mootool.readQuickNote(nextPath)
          update({ actionMode: null, selectedPath: nextPath, note, content: note.content, busy: false, metadataDirty: false })
        } else {
          update({ actionMode: null, selectedPath: nextPath, busy: false })
        }
        return
      }
      if (mode === 'move') {
        const nextPath = await window.mootool.moveQuickNoteEntry({ relativePath: state.selectedPath, targetDirectory: state.actionValue })
        await loadTree()
        if (state.selectedKind === 'file') {
          const note = await window.mootool.readQuickNote(nextPath)
          update({ actionMode: null, selectedPath: nextPath, note, content: note.content, busy: false, metadataDirty: false })
        } else {
          update({ actionMode: null, selectedPath: nextPath, busy: false })
        }
        return
      }
      await window.mootool.deleteQuickNoteEntry(state.selectedPath)
      const nodes = await loadTree()
      const first = firstFile(nodes)
      if (first) {
        const note = await window.mootool.readQuickNote(first.relativePath)
        update({ actionMode: null, selectedPath: note.relativePath, selectedKind: 'file', note, content: note.content, busy: false, metadataDirty: false })
      } else {
        update({ actionMode: null, selectedPath: '', selectedKind: '', note: null, content: '', busy: false, metadataDirty: false })
      }
    } catch (error) {
      update({ busy: false })
      toast.error(errorMessage(error))
    }
  }

  async function duplicateNote(): Promise<void> {
    if (!state.note) return
    if (dirty && !await saveCurrent(false)) return
    try {
      const note = await window.mootool.duplicateQuickNote(state.note.relativePath)
      await loadTree()
      update({ selectedPath: note.relativePath, selectedKind: 'file', note, content: note.content, metadataDirty: false })
    } catch (error) {
      toast.error(errorMessage(error))
    }
  }

  async function moveTreeEntry(node: Pick<QuickNoteNode, 'relativePath' | 'kind'>, targetDirectory: string): Promise<void> {
    const affectsSelection = state.selectedPath === node.relativePath || (node.kind === 'directory' && state.selectedPath.startsWith(`${node.relativePath}/`))
    if (affectsSelection && dirty && !await saveCurrent(false)) return
    try {
      const nextPath = await window.mootool.moveQuickNoteEntry({ relativePath: node.relativePath, targetDirectory })
      const nextSelectedPath = affectsSelection ? `${nextPath}${state.selectedPath.slice(node.relativePath.length)}` : state.selectedPath
      const expanded = new Set([...state.expanded].map((path) => path === node.relativePath || path.startsWith(`${node.relativePath}/`)
        ? `${nextPath}${path.slice(node.relativePath.length)}`
        : path))
      await loadTree()
      if (affectsSelection && state.selectedKind === 'file') {
        const note = await window.mootool.readQuickNote(nextSelectedPath)
        update({ selectedPath: nextSelectedPath, note, content: note.content, expanded, metadataDirty: false })
      } else {
        update({ selectedPath: nextSelectedPath, expanded })
      }
      toast.success(t('quickNote.move'))
    } catch (error) {
      toast.error(errorMessage(error))
    }
  }

  async function importAttachment(): Promise<void> {
    if (!state.note) return
    try {
      const attachment = await window.mootool.importQuickNoteAttachment()
      if (!attachment) return
      setAttachmentUrls({ [attachment.relativePath]: attachment.dataUrl })
      insertText(attachment.markdown)
    } catch (error) {
      toast.error(errorMessage(error))
    }
  }

  async function exportNote(): Promise<void> {
    if (!state.note) return
    const path = await window.mootool.saveTextFile({
      kind: 'text',
      defaultName: `${state.note.metadata.title}.txt`,
      content: state.content
    })
    if (path) toast.success(t('quickNote.export'))
  }

  function insertText(text: string): void {
    const editor = editorRef.current
    const start = editor?.selectionStart ?? state.content.length
    const end = editor?.selectionEnd ?? start
    update({ content: `${state.content.slice(0, start)}${text}${state.content.slice(end)}` })
    requestAnimationFrame(() => {
      editorRef.current?.focus()
      editorRef.current?.setSelectionRange(start + text.length, start + text.length)
    })
  }

  function prefixSelectedLines(prefix: 'bullet' | 'numbered'): void {
    const editor = editorRef.current
    const start = editor?.selectionStart ?? 0
    const end = editor?.selectionEnd ?? state.content.length
    const lineStart = state.content.lastIndexOf('\n', Math.max(0, start - 1)) + 1
    const nextLine = state.content.indexOf('\n', end)
    const lineEnd = nextLine < 0 ? state.content.length : nextLine
    const selected = state.content.slice(lineStart, lineEnd)
    const transformed = selected.split('\n').map((line, index) => `${prefix === 'bullet' ? '- ' : `${index + 1}. `}${line}`).join('\n')
    update({ content: `${state.content.slice(0, lineStart)}${transformed}${state.content.slice(lineEnd)}` })
  }

  async function formatCurrent(): Promise<void> {
    if (!state.note) return
    try {
      const syntax = state.note.metadata.syntax
      let content = state.content
      if (syntax === 'application/json') content = JSON.stringify(JSON.parse(content), null, 2)
      else if (syntax === 'text/java') content = await formatCode(content, 'java')
      else if (syntax === 'text/xml') content = await formatCode(content, 'xml')
      else content = content.split(/\r?\n/).map((line) => line.trimEnd()).join('\n')
      update({ content })
    } catch (error) {
      toast.error(errorMessage(error))
    }
  }

  function patchMetadata(patch: Partial<QuickNoteMetadata>): void {
    if (!state.note) return
    update({ note: { ...state.note, metadata: { ...state.note.metadata, ...patch } }, metadataDirty: true })
  }

  function findNext(): void {
    if (!state.findText) return
    const haystack = state.content.toLocaleLowerCase()
    const needle = state.findText.toLocaleLowerCase()
    let index = haystack.indexOf(needle, findIndexRef.current)
    if (index < 0) index = haystack.indexOf(needle)
    if (index < 0) {
      toast.info(t('quickNote.noMatches'))
      return
    }
    findIndexRef.current = index + needle.length
    editorRef.current?.focus()
    editorRef.current?.setSelectionRange(index, index + needle.length)
  }

  function replaceCurrent(all: boolean): void {
    if (!state.findText) return
    if (all) {
      const expression = new RegExp(escapeRegExp(state.findText), 'gi')
      const next = state.content.replace(expression, state.replaceText)
      if (next === state.content) toast.info(t('quickNote.noMatches'))
      else update({ content: next })
      return
    }
    const editor = editorRef.current
    if (editor && state.content.slice(editor.selectionStart, editor.selectionEnd).toLocaleLowerCase() === state.findText.toLocaleLowerCase()) {
      const start = editor.selectionStart
      const end = editor.selectionEnd
      update({ content: `${state.content.slice(0, start)}${state.replaceText}${state.content.slice(end)}` })
      findIndexRef.current = start + state.replaceText.length
      requestAnimationFrame(findNext)
    } else {
      findNext()
    }
  }

  function applyQuickReplace(action: QuickReplaceActionId): void {
    const editor = editorRef.current
    const start = editor?.selectionStart ?? 0
    const end = editor?.selectionEnd ?? 0
    const hasSelection = end > start
    const source = hasSelection ? state.content.slice(start, end) : state.content
    try {
      const transformed = runQuickReplace(source, action)
      update({ content: hasSelection ? `${state.content.slice(0, start)}${transformed}${state.content.slice(end)}` : transformed })
    } catch (error) {
      toast.error(errorMessage(error))
    }
  }

  const currentName = state.note?.metadata.title ?? directoryLeaf(state.selectedPath)
  return (
    <section className="tool-page quick-note-tool">
      <div className="tool-page__header quick-note-page-header">
        <h1>{t('quickNote.title')}</h1>
        <div className="quick-note-view-switch segmented" role="tablist">
          {(['editor', 'split', 'preview'] as const).map((mode) => (
            <button className={state.viewMode === mode ? 'segmented__item segmented__item--active' : 'segmented__item'} type="button" role="tab" aria-selected={state.viewMode === mode} key={mode} onClick={() => update({ viewMode: mode })}>
              {t(`quickNote.view.${mode}` as MessageKey)}
            </button>
          ))}
        </div>
      </div>

      <div className={[state.treeOpen ? '' : 'quick-note-layout--tree-closed', state.quickReplaceOpen ? 'quick-note-layout--replace-open' : ''].filter(Boolean).join(' ') || undefined}>
        <div className="quick-note-layout">
          {state.treeOpen && (
            <aside className="quick-note-sidebar">
              <div className="quick-note-search">
                <Search size={14} />
                <input aria-label={t('quickNote.search')} placeholder={t('quickNote.search')} value={state.query} onChange={(event) => update({ query: event.target.value })} />
              </div>
              <div className="quick-note-list-controls">
                <label><input type="checkbox" checked={state.includeContent} onChange={(event) => update({ includeContent: event.target.checked })} />{t('quickNote.searchContent')}</label>
                <select aria-label={t('quickNote.sort')} value={state.sort} onChange={(event) => update({ sort: event.target.value as QuickNoteSort })}>
                  <option value="modified">{t('quickNote.sort.modified')}</option>
                  <option value="created">{t('quickNote.sort.created')}</option>
                  <option value="name">{t('quickNote.sort.name')}</option>
                </select>
              </div>
              <div className="quick-note-tree-actions">
                <IconButton label={t('quickNote.newNote')} icon={FilePlus2} onClick={() => openAction('createNote')} />
                <IconButton label={t('quickNote.newFolder')} icon={FolderPlus} onClick={() => openAction('createFolder')} />
                <IconButton label={t('quickNote.rename')} icon={MoreHorizontal} disabled={!state.selectedPath} onClick={() => openAction('rename')} />
                <IconButton label={t('quickNote.move')} icon={FolderOpen} disabled={!state.selectedPath} onClick={() => openAction('move')} />
              </div>
              <div className="quick-note-tree-scroll">
                {state.nodes.length
                  ? <QuickNoteTree nodes={state.nodes} selectedPath={state.selectedPath} expanded={state.expanded} onSelect={(node) => { void selectNode(node) }} onToggle={toggleDirectory} onMove={(node, targetDirectory) => { void moveTreeEntry(node, targetDirectory) }} />
                  : <div className="quick-note-empty">{t('quickNote.empty')}</div>}
              </div>
            </aside>
          )}

          <section className="quick-note-editor-shell">
            <div className="quick-note-toolbar">
              <IconButton label={state.treeOpen ? t('quickNote.openVault') : t('quickNote.newNote')} icon={state.treeOpen ? PanelLeftClose : PanelLeftOpen} onClick={() => update({ treeOpen: !state.treeOpen })} />
              <div className="quick-note-colors" aria-label={t('quickNote.color')}>
                {noteColors.map(([id, color]) => <button className={state.note?.metadata.color === id ? 'quick-note-color quick-note-color--active' : 'quick-note-color'} type="button" aria-label={`${t('quickNote.color')} ${id}`} key={id} disabled={!state.note} style={{ background: color }} onClick={() => patchMetadata({ color: id })} />)}
              </div>
              <select aria-label={t('quickNote.syntax')} disabled={!state.note} value={state.note?.metadata.syntax ?? 'text/plain'} onChange={(event) => patchMetadata({ syntax: event.target.value })}>
                {syntaxOptions.map(([value, label]) => <option value={value} key={value}>{label}</option>)}
              </select>
              <select aria-label={t('quickNote.font')} disabled={!state.note} value={state.note?.metadata.fontName ?? ''} onChange={(event) => patchMetadata({ fontName: event.target.value })}>
                <option value="">{t('quickNote.font.system')}</option>
                <option value="ui-monospace">{t('quickNote.font.mono')}</option>
                <option value="Georgia">{t('quickNote.font.serif')}</option>
              </select>
              <input className="quick-note-font-size" aria-label={t('quickNote.fontSize')} type="number" min={8} max={48} disabled={!state.note} value={state.note?.metadata.fontSize ?? settings.editor.quickNoteFontSize} onChange={(event) => patchMetadata({ fontSize: Number(event.target.value) })} />
              <IconButton label={t('quickNote.wrap')} icon={WrapText} active={state.note?.metadata.lineWrap} disabled={!state.note} onClick={() => patchMetadata({ lineWrap: !state.note?.metadata.lineWrap })} />
              <IconButton label={t('quickNote.bulletList')} icon={List} disabled={!state.note} onClick={() => prefixSelectedLines('bullet')} />
              <IconButton label={t('quickNote.numberedList')} icon={ListOrdered} disabled={!state.note} onClick={() => prefixSelectedLines('numbered')} />
              <span className="quick-note-toolbar__spacer" />
              <IconButton label={t('quickNote.find')} icon={Search} disabled={!state.note} active={state.findOpen} onClick={() => update({ findOpen: !state.findOpen })} />
              <IconButton label={t('quickNote.save')} icon={Save} disabled={!state.note || state.busy} active={dirty} onClick={() => { void saveCurrent() }} />
              <IconButton label={t('quickNote.duplicate')} icon={CopyPlus} disabled={!state.note} onClick={() => { void duplicateNote() }} />
              <IconButton label={t('quickNote.attachment')} icon={ImagePlus} disabled={!state.note} onClick={() => { void importAttachment() }} />
              <IconButton label={t('quickNote.export')} icon={Download} disabled={!state.note} onClick={() => { void exportNote() }} />
              <IconButton label={t('quickNote.quickReplace')} icon={Replace} active={state.quickReplaceOpen} onClick={() => update({ quickReplaceOpen: !state.quickReplaceOpen })} />
              <IconButton label={t('quickNote.info')} icon={Info} disabled={!state.note} onClick={() => update({ infoOpen: true })} />
              <IconButton label={t('quickNote.git')} icon={GitBranch} onClick={() => update({ gitOpen: true })} />
              <IconButton label={t('quickNote.openVault')} icon={FolderOpen} onClick={() => { void window.mootool.openQuickNoteVault() }} />
              <IconButton label={t('quickNote.delete')} icon={Trash2} disabled={!state.selectedPath} onClick={() => openAction('delete')} />
            </div>

            {state.findOpen && (
              <div className="quick-note-find-bar">
                <input aria-label={t('quickNote.findPlaceholder')} placeholder={t('quickNote.findPlaceholder')} value={state.findText} onChange={(event) => { findIndexRef.current = 0; update({ findText: event.target.value }) }} onKeyDown={(event) => { if (event.key === 'Enter') findNext() }} />
                <input aria-label={t('quickNote.replacePlaceholder')} placeholder={t('quickNote.replacePlaceholder')} value={state.replaceText} onChange={(event) => update({ replaceText: event.target.value })} />
                <button type="button" onClick={findNext}>{t('quickNote.nextMatch')}</button>
                <button type="button" onClick={() => replaceCurrent(false)}>{t('quickNote.replace')}</button>
                <button type="button" onClick={() => replaceCurrent(true)}>{t('quickNote.replaceAll')}</button>
                <button className="icon-ghost" type="button" aria-label={t('common.close')} onClick={() => update({ findOpen: false })}><X size={14} /></button>
              </div>
            )}

            {state.note ? (
              <div className={`quick-note-content quick-note-content--${state.viewMode}`}>
                {state.viewMode !== 'preview' && (
                  <div className="quick-note-editor-pane">
                    <pre ref={lineNumbersRef} className="quick-note-line-numbers" aria-hidden="true">{lineNumbers(state.content)}</pre>
                    <textarea
                      ref={editorRef}
                      className={state.note.metadata.lineWrap ? 'quick-note-editor' : 'quick-note-editor quick-note-editor--nowrap'}
                      aria-label={t('quickNote.editorLabel')}
                      spellCheck={false}
                      value={state.content}
                      style={{ fontFamily: editorFont(state.note.metadata.fontName), fontSize: state.note.metadata.fontSize }}
                      onChange={(event) => update({ content: event.target.value })}
                      onScroll={(event) => { if (lineNumbersRef.current) lineNumbersRef.current.scrollTop = event.currentTarget.scrollTop }}
                    />
                  </div>
                )}
                {state.viewMode !== 'editor' && (
                  <article className="quick-note-preview" dangerouslySetInnerHTML={{ __html: previewHtml }} />
                )}
              </div>
            ) : <div className="quick-note-select-empty">{t('quickNote.select')}</div>}

            <footer className="quick-note-statusbar">
              <span>{(state.note?.relativePath ?? state.selectedPath) || t('quickNote.select')}</span>
              <span>{dirty ? t('quickNote.unsaved') : state.note ? t('quickNote.saved') : ''}</span>
              <span>{t('quickNote.status', { lines: String(stats.lines), characters: String(stats.characters) })}</span>
            </footer>
          </section>

          {state.quickReplaceOpen && (
            <aside className="quick-replace-panel">
              <header><strong>{t('quickNote.quickReplace')}</strong><button className="icon-ghost" type="button" aria-label={t('common.close')} onClick={() => update({ quickReplaceOpen: false })}><X size={14} /></button></header>
              <div className="quick-replace-actions">
                {quickReplaceActionIds.map((action) => <button type="button" key={action} disabled={!state.note} onClick={() => applyQuickReplace(action)}>{t(`quickNote.quick.${action}` as MessageKey)}</button>)}
              </div>
            </aside>
          )}
        </div>
      </div>

      <ActionDialog
        mode={state.actionMode}
        value={state.actionValue}
        currentName={currentName}
        selectedPath={state.selectedPath}
        directories={directories}
        busy={state.busy}
        onChange={(actionValue) => update({ actionValue })}
        onClose={() => update({ actionMode: null })}
        onSubmit={() => { void runActionDialog() }}
      />
      <DocumentInfoDialog open={state.infoOpen} note={state.note} stats={stats} onClose={() => update({ infoOpen: false })} />
      <VaultGitDialog scope="quickNote" open={state.gitOpen} onClose={() => update({ gitOpen: false })} onVaultChange={() => { void loadTree() }} />
    </section>
  )
}

type IconButtonProps = {
  label: string
  icon: typeof Save
  disabled?: boolean
  active?: boolean
  onClick: () => void
}

function IconButton({ label, icon: Icon, disabled, active, onClick }: IconButtonProps) {
  return (
    <Tooltip content={label} side="bottom">
      <button className={active ? 'quick-note-icon-button quick-note-icon-button--active' : 'quick-note-icon-button'} type="button" aria-label={label} disabled={disabled} onClick={onClick}>
        <Icon size={14} />
      </button>
    </Tooltip>
  )
}

type ActionDialogProps = {
  mode: ActionDialogMode
  value: string
  currentName: string
  selectedPath: string
  directories: string[]
  busy: boolean
  onChange: (value: string) => void
  onClose: () => void
  onSubmit: () => void
}

function ActionDialog({ mode, value, currentName, selectedPath, directories, busy, onChange, onClose, onSubmit }: ActionDialogProps) {
  const { t } = useI18n()
  const titleKey: MessageKey = mode === 'createNote'
    ? 'quickNote.dialog.createNote'
    : mode === 'createFolder'
      ? 'quickNote.dialog.createFolder'
      : mode === 'rename'
        ? 'quickNote.dialog.rename'
        : mode === 'move'
          ? 'quickNote.dialog.move'
          : 'quickNote.delete'
  return (
    <Dialog
      title={t(titleKey)}
      open={mode !== null}
      onClose={onClose}
      footer={(
        <>
          <button className="dialog-button" type="button" onClick={onClose}>{t('common.cancel')}</button>
          <button className={mode === 'delete' ? 'dialog-button dialog-button--danger' : 'dialog-button dialog-button--primary'} type="button" disabled={busy || (mode !== 'delete' && !value.trim())} onClick={onSubmit}>
            {mode === 'delete' ? t('quickNote.delete') : mode === 'createNote' || mode === 'createFolder' ? t('quickNote.create') : t('quickNote.apply')}
          </button>
        </>
      )}
    >
      {mode === 'delete' ? <p className="quick-note-confirm">{t('quickNote.confirmDelete', { name: currentName })}</p> : mode === 'move' ? (
        <label className="dialog-field">
          <span>{t('quickNote.dialog.target')}</span>
          <select value={value} onChange={(event) => onChange(event.target.value)}>
            <option value="">{t('quickNote.dialog.root')}</option>
            {directories.filter((path) => path !== selectedPath && !path.startsWith(`${selectedPath}/`)).map((path) => <option value={path} key={path}>{path}</option>)}
          </select>
        </label>
      ) : (
        <label className="dialog-field">
          <span>{t('quickNote.dialog.name')}</span>
          <input autoFocus value={value} onChange={(event) => onChange(event.target.value)} onKeyDown={(event) => { if (event.key === 'Enter' && value.trim()) onSubmit() }} />
        </label>
      )}
    </Dialog>
  )
}

function DocumentInfoDialog({ open, note, stats, onClose }: { open: boolean; note: QuickNoteFile | null; stats: ReturnType<typeof documentStats>; onClose: () => void }) {
  const { t } = useI18n()
  const rows = note ? [
    [t('quickNote.path'), note.relativePath],
    [t('quickNote.created'), formatDate(note.metadata.createdAt)],
    [t('quickNote.modified'), formatDate(note.metadata.modifiedAt)],
    [t('quickNote.lines'), String(stats.lines)],
    [t('quickNote.words'), String(stats.words)],
    [t('quickNote.characters'), String(stats.characters)]
  ] : []
  return (
    <Dialog title={t('quickNote.info')} open={open} onClose={onClose} footer={<button className="dialog-button" type="button" onClick={onClose}>{t('common.close')}</button>}>
      <dl className="quick-note-info">{rows.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>
    </Dialog>
  )
}

function firstFile(nodes: QuickNoteNode[]): QuickNoteNode | null {
  for (const node of nodes) {
    if (node.kind === 'file') return node
    const nested = firstFile(node.children ?? [])
    if (nested) return nested
  }
  return null
}

function flattenDirectories(nodes: QuickNoteNode[]): string[] {
  return nodes.flatMap((node) => node.kind === 'directory' ? [node.relativePath, ...flattenDirectories(node.children ?? [])] : [])
}

function selectedDirectory(path: string, kind: EntryKind): string {
  return kind === 'directory' ? path : parentDirectory(path)
}

function parentDirectory(path: string): string {
  const index = path.lastIndexOf('/')
  return index < 0 ? '' : path.slice(0, index)
}

function directoryLeaf(path: string): string {
  return path.slice(path.lastIndexOf('/') + 1)
}

function documentStats(content: string): { lines: number; words: number; characters: number } {
  const trimmed = content.trim()
  return {
    lines: content ? content.split(/\r?\n/).length : 0,
    words: trimmed ? trimmed.split(/\s+/).length : 0,
    characters: content.length
  }
}

function lineNumbers(content: string): string {
  const count = Math.max(1, content.split(/\r?\n/).length)
  return Array.from({ length: count }, (_value, index) => String(index + 1)).join('\n')
}

function extractAttachmentPaths(content: string): string[] {
  const matches = content.matchAll(/(?:\(|src=["'])(attachments\/[A-Za-z0-9_.-]+)(?:\)|["'])/g)
  return [...new Set([...matches].map((match) => match[1]))]
}

function renderPreview(content: string, syntax: string | undefined, attachmentUrls: Record<string, string>): string {
  if (syntax !== 'text/markdown') {
    return DOMPurify.sanitize(`<pre>${escapeHtml(content)}</pre>`)
  }
  let source = content
  for (const [path, url] of Object.entries(attachmentUrls)) source = source.replaceAll(path, url)
  return DOMPurify.sanitize(marked.parse(source, { async: false }) as string)
}

function editorFont(value: string): string {
  if (value === 'ui-monospace') return 'ui-monospace, SFMono-Regular, Menlo, Consolas, monospace'
  if (value === 'Georgia') return 'Georgia, "Times New Roman", serif'
  return 'var(--app-font-family), system-ui, sans-serif'
}

function formatDate(value: string): string {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

function escapeRegExp(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function escapeHtml(value: string): string {
  return value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;')
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : String(error)
}
