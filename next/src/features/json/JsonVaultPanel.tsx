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
import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSettings } from '@/features/settings/SettingsProvider'
import { Dialog } from '@/shared/components/Dialog'
import { Tooltip } from '@/shared/components/Tooltip'
import type { JsonVaultNode } from '@/shared/contracts/jsonVault'
import { useToast } from '@/shared/feedback/ToastProvider'
import { useI18n } from '@/shared/i18n/I18nProvider'
import { VaultGitDialog } from './VaultGitDialog'

type JsonVaultPanelProps = {
  content: string
  onOpen: (content: string) => void
}

type SelectedEntry = { path: string; kind: JsonVaultNode['kind'] }
type TextAction = { type: 'file' | 'folder' | 'rename'; value: string } | null

export function JsonVaultPanel({ content, onOpen }: JsonVaultPanelProps) {
  const { t } = useI18n()
  const { settings } = useSettings()
  const toast = useToast()
  const [nodes, setNodes] = useState<JsonVaultNode[]>([])
  const [selectedEntry, setSelectedEntry] = useState<SelectedEntry | null>(null)
  const [selectedPath, setSelectedPath] = useState('')
  const [savedContent, setSavedContent] = useState('')
  const [expanded, setExpanded] = useState(() => new Set<string>())
  const [gitDialogOpen, setGitDialogOpen] = useState(false)
  const [textAction, setTextAction] = useState<TextAction>(null)
  const [moveOpen, setMoveOpen] = useState(false)
  const [moveTarget, setMoveTarget] = useState('')
  const [sort, setSort] = useState<'name' | 'modified'>('name')
  const dirty = Boolean(selectedPath) && content !== savedContent
  const directories = useMemo(() => ['', ...flattenDirectories(nodes)], [nodes])

  const load = useCallback(async () => {
    try {
      setNodes(await window.mootool.listJsonVault({ hideIgnored: settings.vault.hideGitignoredFiles, sort }))
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t('json.notice.failed'))
    }
  }, [settings.vault.hideGitignoredFiles, sort, t, toast])

  useEffect(() => {
    void load()
  }, [load, settings.vault.jsonPath])

  useEffect(() => window.mootool.onJsonVaultChange(() => {
    void load()
  }), [load])

  async function openFile(path: string): Promise<void> {
    if (dirty && path !== selectedPath && !window.confirm(t('json.vault.confirmDiscard'))) return
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

  async function saveSelected(): Promise<void> {
    if (!selectedPath) {
      beginCreateFile()
      return
    }
    try {
      const file = await window.mootool.saveJsonVaultFile({ relativePath: selectedPath, content })
      setSavedContent(file.content)
      toast.success(t('json.vault.saved'))
      await load()
    } catch (error) {
      reportError(error)
    }
  }

  function beginCreateFile(): void {
    const parent = selectedDirectory(selectedEntry)
    setTextAction({ type: 'file', value: [parent, 'snippet.json'].filter(Boolean).join('/') })
  }

  function beginCreateFolder(): void {
    const parent = selectedDirectory(selectedEntry)
    setTextAction({ type: 'folder', value: [parent, t('json.vault.defaultFolder')].filter(Boolean).join('/') })
  }

  function beginRename(): void {
    if (!selectedEntry) return
    const name = leafName(selectedEntry.path).replace(/\.json$/i, '')
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

  function beginMove(): void {
    if (!selectedEntry) return
    setMoveTarget(parentPath(selectedEntry.path))
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
          <VaultAction label={t('json.git.open')} onClick={() => setGitDialogOpen(true)}><GitBranch size={14} /></VaultAction>
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
            <MenuAction icon={Pencil} label={t('json.vault.rename')} disabled={!selectedEntry} onClick={beginRename} />
            <MenuAction icon={Move} label={t('json.vault.move')} disabled={!selectedEntry} onClick={beginMove} />
            <MenuAction icon={Copy} label={t('json.vault.duplicate')} disabled={selectedEntry?.kind !== 'file'} onClick={() => { void duplicateSelected() }} />
            <MenuAction icon={RefreshCw} label={t('json.vault.refresh')} onClick={() => { void load() }} />
            <MenuAction icon={FolderOpen} label={t('json.vault.openFolder')} onClick={() => { void window.mootool.openJsonVault() }} />
          </div>
        </details>
      </div>
      <div
        className="vault-tree"
        onDragOver={(event) => event.preventDefault()}
        onDrop={(event) => {
          if (event.target !== event.currentTarget) return
          const path = event.dataTransfer.getData('application/x-mootool-vault-path')
          const kind = event.dataTransfer.getData('application/x-mootool-vault-kind') as JsonVaultNode['kind']
          if (path) {
            void moveSpecificEntry({ path, kind }, '')
          }
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
            onDrop={(path, kind, target) => {
              void moveSpecificEntry({ path, kind }, target)
            }}
          />
        ))}
      </div>
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
      <VaultGitDialog open={gitDialogOpen} onClose={() => setGitDialogOpen(false)} onVaultChange={() => { void load() }} />
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
  onDrop: (path: string, kind: JsonVaultNode['kind'], target: string) => void
}

function VaultNode({ node, depth, expanded, selectedEntryPath, activePath, dirty, onToggle, onSelect, onOpen, onDrop }: VaultNodeProps) {
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
        onDragStart={(event) => {
          event.dataTransfer.effectAllowed = 'move'
          event.dataTransfer.setData('application/x-mootool-vault-path', node.relativePath)
          event.dataTransfer.setData('application/x-mootool-vault-kind', node.kind)
        }}
        onDragOver={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          event.dataTransfer.dropEffect = 'move'
        } : undefined}
        onDrop={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          const path = event.dataTransfer.getData('application/x-mootool-vault-path')
          const kind = event.dataTransfer.getData('application/x-mootool-vault-kind') as JsonVaultNode['kind']
          if (path && path !== node.relativePath) onDrop(path, kind, node.relativePath)
        } : undefined}
      >
        {node.kind === 'directory' ? <>{isDirectoryOpen ? <ChevronDown size={13} /> : <ChevronRight size={13} />}<Folder size={13} /></> : <FileJson size={13} />}
        <span>{node.name}</span>{activePath === node.relativePath && dirty && <i />}
      </button>
      {node.kind === 'directory' && isDirectoryOpen && node.children?.map((child) => (
        <VaultNode key={child.relativePath} node={child} depth={depth + 1} expanded={expanded} selectedEntryPath={selectedEntryPath} activePath={activePath} dirty={dirty} onToggle={onToggle} onSelect={onSelect} onOpen={onOpen} onDrop={onDrop} />
      ))}
    </div>
  )
}

function VaultAction({ label, disabled = false, onClick, children }: { label: string; disabled?: boolean; onClick: () => void; children: React.ReactNode }) {
  return <Tooltip content={label}><button type="button" aria-label={label} disabled={disabled} onClick={onClick}>{children}</button></Tooltip>
}

function MenuAction({ icon: Icon, label, disabled = false, onClick }: { icon: typeof Pencil; label: string; disabled?: boolean; onClick: () => void }) {
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
