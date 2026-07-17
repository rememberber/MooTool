import {
  ChevronDown,
  ChevronRight,
  Copy,
  FileJson,
  FilePlus2,
  Folder,
  FolderOpen,
  FolderPlus,
  GitBranch,
  MoreHorizontal,
  Move,
  Pencil,
  RefreshCw,
  Save,
  Trash2
} from 'lucide-react'
import { useCallback, useEffect, useEffectEvent, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { useSettings } from '@/features/settings/SettingsProvider'
import { Dialog } from '@/shared/components/Dialog'
import { Tooltip } from '@/shared/components/Tooltip'
import { useToolActivity } from '@/shared/components/ToolActivity'
import type { JsonVaultNode } from '@/shared/contracts/jsonVault'
import type { VaultGitAction } from '@/shared/contracts/vaultGit'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { VaultGitDialog } from './VaultGitDialog'

type JsonVaultPanelProps = {
  content: string
  onOpen: (content: string) => void
}

type SelectedEntry = { path: string; kind: JsonVaultNode['kind'] }
type TextAction = { type: 'file' | 'folder' | 'rename'; value: string } | null
const jsonVaultPathType = 'application/x-mootool-vault-path'
const jsonVaultKindType = 'application/x-mootool-vault-kind'

type JsonVaultSessionState = {
  nodes: JsonVaultNode[]
  selectedEntry: SelectedEntry | null
  selectedPath: string
  savedContent: string
  expanded: Set<string>
  gitDialogOpen: boolean
  textAction: TextAction
  moveOpen: boolean
  moveTarget: string
  contextMenu: { entry: SelectedEntry; left: number; top: number } | null
  sort: 'name' | 'modified'
}

let jsonVaultSessionState: JsonVaultSessionState = {
  nodes: [],
  selectedEntry: null,
  selectedPath: '',
  savedContent: '',
  expanded: new Set(),
  gitDialogOpen: false,
  textAction: null,
  moveOpen: false,
  moveTarget: '',
  contextMenu: null,
  sort: 'name'
}
let jsonVaultTreeScrollTop = 0

export function JsonVaultPanel({ content, onOpen }: JsonVaultPanelProps) {
  const toolActive = useToolActivity()
  const { t } = useI18n()
  const { settings } = useSettings()
  const toast = useToast()
  const [nodes, setNodes] = useState<JsonVaultNode[]>(jsonVaultSessionState.nodes)
  const [selectedEntry, setSelectedEntry] = useState<SelectedEntry | null>(jsonVaultSessionState.selectedEntry)
  const [selectedPath, setSelectedPath] = useState(jsonVaultSessionState.selectedPath)
  const [savedContent, setSavedContent] = useState(jsonVaultSessionState.savedContent)
  const [expanded, setExpanded] = useState(() => new Set(jsonVaultSessionState.expanded))
  const [gitDialogOpen, setGitDialogOpen] = useState(jsonVaultSessionState.gitDialogOpen)
  const [gitChangeCount, setGitChangeCount] = useState(0)
  const [textAction, setTextAction] = useState<TextAction>(jsonVaultSessionState.textAction)
  const [moveOpen, setMoveOpen] = useState(jsonVaultSessionState.moveOpen)
  const [moveTarget, setMoveTarget] = useState(jsonVaultSessionState.moveTarget)
  const [contextMenu, setContextMenu] = useState<{ entry: SelectedEntry; left: number; top: number } | null>(jsonVaultSessionState.contextMenu)
  const contextMenuRef = useRef<HTMLDivElement>(null)
  const treeRef = useRef<HTMLDivElement>(null)
  const [sort, setSort] = useState<'name' | 'modified'>(jsonVaultSessionState.sort)
  const latestSelectionRef = useRef({ selectedPath, content })
  const saveQueueRef = useRef<Promise<void>>(Promise.resolve())
  const dirty = Boolean(selectedPath) && content !== savedContent
  latestSelectionRef.current = { selectedPath, content }
  const directories = useMemo(() => ['', ...flattenDirectories(nodes)], [nodes])

  useEffect(() => {
    jsonVaultSessionState = {
      nodes,
      selectedEntry,
      selectedPath,
      savedContent,
      expanded: new Set(expanded),
      gitDialogOpen,
      textAction,
      moveOpen,
      moveTarget,
      contextMenu,
      sort
    }
  }, [contextMenu, expanded, gitDialogOpen, moveOpen, moveTarget, nodes, savedContent, selectedEntry, selectedPath, sort, textAction])

  useLayoutEffect(() => {
    if (treeRef.current) treeRef.current.scrollTop = jsonVaultTreeScrollTop
  }, [nodes])

  const load = useCallback(async () => {
    try {
      setNodes(await window.mootool.listJsonVault({ hideIgnored: settings.vault.hideGitignoredFiles, sort }))
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t('json.notice.failed'))
    }
  }, [settings.vault.hideGitignoredFiles, sort, t, toast])

  const reloadSelectedFromDisk = useCallback(async () => {
    if (!selectedPath) return
    try {
      const file = await window.mootool.readJsonVaultFile(selectedPath)
      setSelectedEntry({ path: file.relativePath, kind: 'file' })
      setSavedContent(file.content)
      onOpen(file.content)
    } catch {
      setSelectedEntry(null)
      setSelectedPath('')
      setSavedContent('')
      onOpen('')
    }
  }, [onOpen, selectedPath])

  const refreshGitChangeCount = useCallback(async () => {
    try {
      const status = await window.mootool.getVaultGitStatus()
      setGitChangeCount(status.changes.length)
    } catch {
      setGitChangeCount(0)
    }
  }, [])

  useEffect(() => {
    void load()
    if (!dirty) void reloadSelectedFromDisk()
  }, [dirty, load, reloadSelectedFromDisk, settings.vault.jsonPath])

  useEffect(() => window.mootool.onJsonVaultChange(() => {
    void load()
    if (!dirty) void reloadSelectedFromDisk()
    void refreshGitChangeCount()
  }), [dirty, load, refreshGitChangeCount, reloadSelectedFromDisk])

  useEffect(() => {
    if (!toolActive) return
    void refreshGitChangeCount()
    const timer = window.setInterval(() => { void refreshGitChangeCount() }, 5_000)
    return () => window.clearInterval(timer)
  }, [refreshGitChangeCount, toolActive])

  useEffect(() => {
    void window.mootool.setJsonVaultEditorDirty(dirty)
  }, [dirty])

  const persistSelectedOnIdle = useEffectEvent((path: string, snapshot: string) => {
    void persistSelected(path, snapshot, false)
  })

  useEffect(() => {
    if (!dirty || !selectedPath) return
    const path = selectedPath
    const snapshot = content
    const timer = window.setTimeout(() => persistSelectedOnIdle(path, snapshot), 250)
    return () => window.clearTimeout(timer)
  }, [content, dirty, selectedPath])

  useEffect(() => {
    if (!contextMenu || !toolActive) return
    const focusFrame = window.requestAnimationFrame(() => contextMenuRef.current?.querySelector('button')?.focus())
    const close = () => setContextMenu(null)
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') close() }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', closeOnEscape)
    window.addEventListener('blur', close)
    return () => {
      window.cancelAnimationFrame(focusFrame)
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', closeOnEscape)
      window.removeEventListener('blur', close)
    }
  }, [contextMenu, toolActive])

  async function openFile(path: string): Promise<void> {
    if (dirty && path !== selectedPath && !await saveSelected(false)) return
    try {
      const file = await window.mootool.readJsonVaultFile(path)
      setSelectedEntry({ path: file.relativePath, kind: 'file' })
      setSelectedPath(file.relativePath)
      setSavedContent(file.content)
      onOpen(file.content)
    } catch (error) {
      reportError(error)
    }
  }

  async function saveSelected(showToast = true): Promise<boolean> {
    if (!selectedPath) {
      beginCreateFile()
      return false
    }
    return persistSelected(selectedPath, content, showToast)
  }

  async function persistSelected(path: string, snapshot: string, showToast: boolean): Promise<boolean> {
    let saved = false
    const operation = saveQueueRef.current.catch(() => undefined).then(async () => {
      const file = await window.mootool.saveJsonVaultFile({ relativePath: path, content: snapshot })
      saved = true
      const current = latestSelectionRef.current
      if (current.selectedPath === path) setSavedContent(file.content)
      if (showToast) toast.success(t('json.vault.saved'))
      await load()
      await refreshGitChangeCount()
    })
    saveQueueRef.current = operation.then(() => undefined, () => undefined)
    try {
      await operation
      return saved
    } catch (error) {
      reportError(error)
      return false
    }
  }

  async function prepareGitAction(action: VaultGitAction): Promise<boolean> {
    if (!dirty) return true
    if (action === 'pull' || action === 'continue-operation') return saveSelected()
    return true
  }

  async function refreshAfterGitAction(): Promise<void> {
    await Promise.all([load(), reloadSelectedFromDisk(), refreshGitChangeCount()])
  }

  function beginCreateFile(): void {
    const parent = selectedDirectory(selectedEntry)
    setTextAction({ type: 'file', value: [parent, 'snippet.json'].filter(Boolean).join('/') })
  }

  function beginCreateFolder(): void {
    const parent = selectedDirectory(selectedEntry)
    setTextAction({ type: 'folder', value: [parent, t('json.vault.defaultFolder')].filter(Boolean).join('/') })
  }

  function beginRename(entry = selectedEntry): void {
    if (!entry) return
    setSelectedEntry(entry)
    const name = leafName(entry.path).replace(/\.json$/i, '')
    setTextAction({ type: 'rename', value: name })
  }

  async function submitTextAction(): Promise<void> {
    if (!textAction?.value.trim()) return
    try {
      if (textAction.type === 'file') {
        const file = await window.mootool.saveJsonVaultFile({ relativePath: textAction.value, content: content || '{\n\n}' })
        setSelectedEntry({ path: file.relativePath, kind: 'file' })
        setSelectedPath(file.relativePath)
        setSavedContent(file.content)
        onOpen(file.content)
        toast.success(t('json.vault.created'))
      } else if (textAction.type === 'folder') {
        const path = await window.mootool.createJsonVaultFolder(textAction.value)
        setExpanded((current) => new Set(current).add(path))
        setSelectedEntry({ path, kind: 'directory' })
        toast.success(t('json.vault.folderCreated'))
      } else if (selectedEntry) {
        const before = selectedEntry.path
        const next = await window.mootool.renameJsonVaultEntry({ relativePath: before, name: textAction.value })
        updateSelectionAfterPathChange(before, next, selectedEntry.kind)
        toast.success(t('json.vault.renamed'))
      }
      setTextAction(null)
      await load()
    } catch (error) {
      reportError(error)
    }
  }

  function beginMove(entry = selectedEntry): void {
    if (!entry) return
    setSelectedEntry(entry)
    setMoveTarget(parentPath(entry.path))
    setMoveOpen(true)
  }

  async function moveEntry(targetDirectory = moveTarget): Promise<void> {
    if (!selectedEntry) return
    await moveSpecificEntry(selectedEntry, targetDirectory)
  }

  async function moveSpecificEntry(entry: SelectedEntry, targetDirectory: string): Promise<void> {
    try {
      const before = entry.path
      const next = await window.mootool.moveJsonVaultEntry({ relativePath: before, targetDirectory })
      updateSelectionAfterPathChange(before, next, entry.kind)
      setMoveOpen(false)
      toast.success(t('json.vault.moved'))
      await load()
    } catch (error) {
      reportError(error)
    }
  }

  async function duplicateSelected(): Promise<void> {
    if (selectedEntry?.kind !== 'file') return
    try {
      const file = await window.mootool.duplicateJsonVaultFile(selectedEntry.path)
      setSelectedEntry({ path: file.relativePath, kind: 'file' })
      setSelectedPath(file.relativePath)
      setSavedContent(file.content)
      onOpen(file.content)
      toast.success(t('json.vault.duplicated'))
      await load()
    } catch (error) {
      reportError(error)
    }
  }

  async function deleteSelected(): Promise<void> {
    if (!selectedEntry || !window.confirm(t('json.vault.confirmDelete', { name: selectedEntry.path }))) return
    try {
      await window.mootool.deleteJsonVaultFile(selectedEntry.path)
      if (selectedEntry.path === selectedPath) {
        setSelectedPath('')
        setSavedContent('')
      }
      setSelectedEntry(null)
      toast.success(t('json.vault.deleted'))
      await load()
    } catch (error) {
      reportError(error)
    }
  }

  function updateSelectionAfterPathChange(before: string, next: string, kind: JsonVaultNode['kind']): void {
    setSelectedEntry({ path: next, kind })
    if (selectedPath === before || (kind === 'directory' && selectedPath.startsWith(`${before}/`))) {
      setSelectedPath(`${next}${selectedPath.slice(before.length)}`)
    }
    setExpanded((current) => {
      const values = [...current].map((path) => path === before || path.startsWith(`${before}/`) ? `${next}${path.slice(before.length)}` : path)
      return new Set(values)
    })
  }

  function toggleDirectory(path: string): void {
    setExpanded((current) => {
      const next = new Set(current)
      if (next.has(path)) next.delete(path)
      else next.add(path)
      return next
    })
  }

  function reportError(error: unknown): void {
    toast.error(error instanceof Error ? error.message : t('json.notice.failed'))
  }

  const actionTitle = textAction?.type === 'folder'
    ? t('json.vault.newFolder')
    : textAction?.type === 'rename' ? t('json.vault.rename') : t('json.vault.new')

  return (
    <aside className="vault-panel">
      <header className="vault-panel__header">
        <h2>{t('json.vault.title')}</h2>
        <div className="vault-panel__actions">
          <VaultAction label={t('json.vault.new')} onClick={beginCreateFile}><FilePlus2 size={14} /></VaultAction>
          <VaultAction label={t('json.vault.newFolder')} onClick={beginCreateFolder}><FolderPlus size={14} /></VaultAction>
          <VaultAction label={t('json.vault.save')} onClick={() => { void saveSelected() }}><Save size={14} /></VaultAction>
          <VaultAction label={t('json.vault.delete')} disabled={!selectedEntry} onClick={() => { void deleteSelected() }}><Trash2 size={14} /></VaultAction>
          <VaultAction label={t('json.git.open')} badge={gitChangeCount} onClick={() => setGitDialogOpen(true)}><GitBranch size={14} /></VaultAction>
        </div>
      </header>
      <div className="vault-panel__options">
        <select aria-label={t('json.vault.sort')} value={sort} onChange={(event) => setSort(event.target.value as typeof sort)}>
          <option value="name">{t('json.vault.sortName')}</option>
          <option value="modified">{t('json.vault.sortModified')}</option>
        </select>
        <details className="vault-more-menu">
          <summary aria-label={t('json.vault.more')} title={t('json.vault.more')}><MoreHorizontal size={14} /></summary>
          <div>
            <MenuAction icon={Pencil} label={t('json.vault.rename')} disabled={!selectedEntry} onClick={() => beginRename()} />
            <MenuAction icon={Move} label={t('json.vault.move')} disabled={!selectedEntry} onClick={() => beginMove()} />
            <MenuAction icon={Copy} label={t('json.vault.duplicate')} disabled={selectedEntry?.kind !== 'file'} onClick={() => { void duplicateSelected() }} />
            <MenuAction icon={RefreshCw} label={t('json.vault.refresh')} onClick={() => { void load() }} />
            <MenuAction icon={FolderOpen} label={t('json.vault.openFolder')} onClick={() => { void window.mootool.openJsonVault() }} />
          </div>
        </details>
      </div>
      <div
        ref={treeRef}
        className="vault-tree"
        onScroll={(event) => { jsonVaultTreeScrollTop = event.currentTarget.scrollTop }}
        onDragOver={(event) => event.preventDefault()}
        onDrop={(event) => {
          event.preventDefault()
          const draggedEntry = readDraggedVaultEntry(event.dataTransfer)
          if (draggedEntry && canMoveJsonVaultEntry(draggedEntry.path, '')) void moveSpecificEntry(draggedEntry, '')
        }}
      >
        {nodes.length === 0 ? <div className="vault-empty">{t('json.vault.empty')}</div> : nodes.map((node) => (
          <VaultNode
            key={node.relativePath}
            node={node}
            depth={0}
            expanded={expanded}
            selectedEntryPath={selectedEntry?.path ?? ''}
            activePath={selectedPath}
            dirty={dirty}
            onToggle={toggleDirectory}
            onSelect={(entry) => setSelectedEntry({ path: entry.relativePath, kind: entry.kind })}
            onOpen={(path) => { void openFile(path) }}
            onOpenContextMenu={(entry, left, top) => {
              const selected = { path: entry.relativePath, kind: entry.kind }
              setSelectedEntry(selected)
              setContextMenu({ entry: selected, left, top })
            }}
            onDrop={(path, kind, target) => {
              void moveSpecificEntry({ path, kind }, target)
            }}
          />
        ))}
      </div>
      {contextMenu && toolActive && createPortal(
        <div
          ref={contextMenuRef}
          className="vault-tree-menu"
          role="menu"
          style={{ left: contextMenu.left, top: contextMenu.top }}
          onPointerDown={(event) => event.stopPropagation()}
        >
          <button type="button" role="menuitem" onClick={() => { beginRename(contextMenu.entry); setContextMenu(null) }}>{t('json.vault.rename')}</button>
          <button type="button" role="menuitem" onClick={() => { beginMove(contextMenu.entry); setContextMenu(null) }}>{t('json.vault.move')}</button>
        </div>,
        document.body
      )}
      {selectedEntry && <footer className="vault-panel__selection" title={selectedEntry.path}>{selectedEntry.path === selectedPath && dirty ? '• ' : ''}{selectedEntry.path}</footer>}
      <Dialog
        title={actionTitle}
        open={textAction !== null}
        width={440}
        onClose={() => setTextAction(null)}
        footer={(
          <>
            <button className="dialog-button" type="button" onClick={() => setTextAction(null)}>{t('common.cancel')}</button>
            <button className="dialog-button dialog-button--primary" type="button" disabled={!textAction?.value.trim()} onClick={() => { void submitTextAction() }}>{textAction?.type === 'rename' ? t('common.save') : t('json.vault.create')}</button>
          </>
        )}
      >
        <label className="vault-new-field">
          <span>{textAction?.type === 'rename' ? t('json.vault.renameName') : textAction?.type === 'folder' ? t('json.vault.folderName') : t('json.vault.fileName')}</span>
          <input autoFocus value={textAction?.value ?? ''} placeholder={t('json.vault.fileNameHint')} onChange={(event) => setTextAction((current) => current ? { ...current, value: event.target.value } : null)} onKeyDown={(event) => { if (event.key === 'Enter' && textAction?.value.trim()) void submitTextAction() }} />
        </label>
      </Dialog>
      <Dialog
        title={t('json.vault.move')}
        open={moveOpen}
        width={440}
        onClose={() => setMoveOpen(false)}
        footer={(
          <>
            <button className="dialog-button" type="button" onClick={() => setMoveOpen(false)}>{t('common.cancel')}</button>
            <button className="dialog-button dialog-button--primary" type="button" onClick={() => { void moveEntry() }}>{t('common.save')}</button>
          </>
        )}
      >
        <label className="vault-new-field">
          <span>{t('json.vault.moveTo')}</span>
          <select value={moveTarget} onChange={(event) => setMoveTarget(event.target.value)}>
            {directories.map((path) => <option value={path} key={path || '/'}>{path || t('json.vault.root')}</option>)}
          </select>
        </label>
      </Dialog>
      <VaultGitDialog
        open={gitDialogOpen}
        onClose={() => setGitDialogOpen(false)}
        beforeWorkingTreeChange={prepareGitAction}
        onVaultChange={refreshAfterGitAction}
      />
    </aside>
  )
}

type VaultNodeProps = {
  node: JsonVaultNode
  depth: number
  expanded: Set<string>
  selectedEntryPath: string
  activePath: string
  dirty: boolean
  onToggle: (path: string) => void
  onSelect: (node: JsonVaultNode) => void
  onOpen: (path: string) => void
  onOpenContextMenu: (node: JsonVaultNode, left: number, top: number) => void
  onDrop: (path: string, kind: JsonVaultNode['kind'], target: string) => void
}

function VaultNode({ node, depth, expanded, selectedEntryPath, activePath, dirty, onToggle, onSelect, onOpen, onOpenContextMenu, onDrop }: VaultNodeProps) {
  const isDirectoryOpen = expanded.has(node.relativePath)
  const selected = selectedEntryPath === node.relativePath
  return (
    <div>
      <button
        className={`${node.kind === 'directory' ? 'vault-node vault-node--directory' : 'vault-node'}${selected ? ' vault-node--selected' : ''}`}
        type="button"
        draggable
        style={{ paddingLeft: (node.kind === 'directory' ? 9 : 24) + depth * 14 }}
        onClick={() => {
          onSelect(node)
          if (node.kind === 'directory') onToggle(node.relativePath)
          else onOpen(node.relativePath)
        }}
        onContextMenu={(event) => {
          event.preventDefault()
          onOpenContextMenu(node, Math.min(event.clientX, window.innerWidth - 164), Math.min(event.clientY, window.innerHeight - 78))
        }}
        onDragStart={(event) => {
          event.dataTransfer.effectAllowed = 'move'
          event.dataTransfer.setData(jsonVaultPathType, node.relativePath)
          event.dataTransfer.setData(jsonVaultKindType, node.kind)
        }}
        onDragOver={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          event.dataTransfer.dropEffect = 'move'
        } : undefined}
        onDrop={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          const draggedEntry = readDraggedVaultEntry(event.dataTransfer)
          if (draggedEntry && canMoveJsonVaultEntry(draggedEntry.path, node.relativePath)) onDrop(draggedEntry.path, draggedEntry.kind, node.relativePath)
        } : undefined}
      >
        {node.kind === 'directory' ? <>{isDirectoryOpen ? <ChevronDown size={13} /> : <ChevronRight size={13} />}<Folder size={13} /></> : <FileJson size={13} />}
        <span>{node.name}</span>{activePath === node.relativePath && dirty && <i />}
      </button>
      {node.kind === 'directory' && isDirectoryOpen && node.children?.map((child) => (
        <VaultNode key={child.relativePath} node={child} depth={depth + 1} expanded={expanded} selectedEntryPath={selectedEntryPath} activePath={activePath} dirty={dirty} onToggle={onToggle} onSelect={onSelect} onOpen={onOpen} onOpenContextMenu={onOpenContextMenu} onDrop={onDrop} />
      ))}
    </div>
  )
}

function readDraggedVaultEntry(dataTransfer: DataTransfer): SelectedEntry | null {
  const path = dataTransfer.getData(jsonVaultPathType)
  const kind = dataTransfer.getData(jsonVaultKindType)
  if (!path || (kind !== 'file' && kind !== 'directory')) return null
  return { path, kind }
}

export function canMoveJsonVaultEntry(path: string, targetDirectory: string): boolean {
  const currentDirectory = path.includes('/') ? path.slice(0, path.lastIndexOf('/')) : ''
  return currentDirectory !== targetDirectory
    && path !== targetDirectory
    && !targetDirectory.startsWith(`${path}/`)
}

function VaultAction({ label, disabled = false, badge = 0, onClick, children }: { label: string; disabled?: boolean; badge?: number; onClick: () => void; children: React.ReactNode }) {
  const accessibleLabel = badge > 0 ? `${label} (${badge})` : label
  return <Tooltip content={accessibleLabel}><button type="button" aria-label={accessibleLabel} disabled={disabled} onClick={onClick}>{children}{badge > 0 && <span className="vault-git-badge">{badge > 99 ? '99+' : badge}</span>}</button></Tooltip>
}

function MenuAction({ icon: Icon, label, disabled = false, onClick }: { icon: typeof Copy; label: string; disabled?: boolean; onClick: () => void }) {
  return <button type="button" disabled={disabled} onClick={onClick}><Icon size={13} /><span>{label}</span></button>
}

function flattenDirectories(nodes: JsonVaultNode[]): string[] {
  return nodes.flatMap((node) => node.kind === 'directory' ? [node.relativePath, ...flattenDirectories(node.children ?? [])] : [])
}

function selectedDirectory(entry: SelectedEntry | null): string {
  if (!entry) return ''
  return entry.kind === 'directory' ? entry.path : parentPath(entry.path)
}

function parentPath(path: string): string {
  return path.includes('/') ? path.slice(0, path.lastIndexOf('/')) : ''
}

function leafName(path: string): string {
  return path.slice(path.lastIndexOf('/') + 1)
}
