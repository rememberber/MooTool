import { ChevronDown, ChevronRight, FileText, Folder, FolderOpen } from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import type { QuickNote, QuickNoteFolder } from '../../platform/contracts/localData'
import type { VaultTreeExpandMode } from '../../platform/contracts/settings'
import { ancestorDirectoryPaths, resolveExpandedPaths } from '../../shared/vaultTreeExpand'
import { selectTreePaths, type SelectionGesture } from '../../shared/vaultSelection'

export type NoteTreeAction = 'rename' | 'move' | 'duplicate' | 'export' | 'info' | 'delete' | 'reveal'

export interface NoteTreeNode {
  name: string
  path: string
  kind: 'directory' | 'file'
  note?: QuickNote
  children: NoteTreeNode[]
}

export type NoteSort = 'modified' | 'created' | 'name'

export function buildNoteTree(
  notes: QuickNote[],
  folders: QuickNoteFolder[],
  query: string,
  tag: string,
  options: { includeContent?: boolean; sort?: NoteSort } = {}
): NoteTreeNode[] {
  const includeContent = options.includeContent ?? true
  const sort = options.sort ?? 'modified'
  const needle = query.trim().toLocaleLowerCase()
  const notePath = (note: QuickNote) => note.relativePath || (note.folderPath ? `${note.folderPath}/${note.id}` : note.id)
  const matches = (note: QuickNote) => (!tag || note.tags.includes(tag))
    && (!needle || [note.title, notePath(note), includeContent ? note.content : ''].some((value) => value.toLocaleLowerCase().includes(needle)))
  const visible = notes.filter(matches)
  const directoryPaths = new Set<string>(folders.map((folder) => folder.path))
  for (const note of visible) {
    if (!note.folderPath) continue
    const parts = note.folderPath.split('/')
    for (let index = 1; index <= parts.length; index += 1) directoryPaths.add(parts.slice(0, index).join('/'))
  }
  if (needle || tag) {
    const keep = new Set<string>()
    for (const path of directoryPaths) {
      const pathMatches = Boolean(needle) && path.toLocaleLowerCase().includes(needle)
      const hasNote = visible.some((note) => note.folderPath === path || note.folderPath.startsWith(`${path}/`))
      if (hasNote || pathMatches) keep.add(path)
    }
    for (const path of [...keep]) {
      for (const ancestor of ancestorDirectoryPaths(path)) {
        if (directoryPaths.has(ancestor)) keep.add(ancestor)
      }
    }
    for (const path of [...directoryPaths]) if (!keep.has(path)) directoryPaths.delete(path)
  }
  const roots: NoteTreeNode[] = []
  const byPath = new Map<string, NoteTreeNode>()
  for (const path of [...directoryPaths].sort((left, right) => left.split('/').length - right.split('/').length || left.localeCompare(right))) {
    const node: NoteTreeNode = { name: path.split('/').at(-1) ?? path, path, kind: 'directory', children: [] }
    byPath.set(path, node)
    const parent = path.includes('/') ? path.slice(0, path.lastIndexOf('/')) : ''
    const parentNode = parent ? byPath.get(parent) : undefined
    if (parentNode) parentNode.children.push(node)
    else roots.push(node)
  }
  for (const note of visible) {
    const file: NoteTreeNode = {
      name: noteLabel(note),
      path: note.relativePath || (note.folderPath ? `${note.folderPath}/${note.id}` : note.id),
      kind: 'file',
      note,
      children: []
    }
    const parent = note.folderPath ? byPath.get(note.folderPath) : undefined
    if (parent) parent.children.push(file)
    else roots.push(file)
  }
  sortTree(roots, sort)
  return roots
}

function sortTree(nodes: NoteTreeNode[], sort: NoteSort): void {
  nodes.sort((left, right) => compareNodes(left, right, sort))
  for (const node of nodes) if (node.kind === 'directory') sortTree(node.children, sort)
}

function compareNodes(left: NoteTreeNode, right: NoteTreeNode, sort: NoteSort): number {
  if (left.kind !== right.kind) return left.kind === 'directory' ? -1 : 1
  if (left.kind === 'directory' || sort === 'name') {
    const leftName = left.path.split('/').at(-1) ?? left.name
    const rightName = right.path.split('/').at(-1) ?? right.name
    return leftName.localeCompare(rightName)
  }
  const field = sort === 'created' ? 'createdAt' : 'updatedAt'
  return (right.note?.[field] ?? 0) - (left.note?.[field] ?? 0)
}

function noteLabel(note: QuickNote): string {
  if (note.title.trim()) return note.title
  const leaf = (note.relativePath || note.id).split('/').pop() ?? note.id
  return leaf.replace(/\.txt$/i, '')
}

const quickNotePathType = 'application/x-mootool-quick-note-path'
const quickNoteKindType = 'application/x-mootool-quick-note-kind'

export function canMoveToDirectory(relativePath: string, targetDirectory: string): boolean {
  const currentDirectory = relativePath.includes('/') ? relativePath.slice(0, relativePath.lastIndexOf('/')) : ''
  return currentDirectory !== targetDirectory
    && relativePath !== targetDirectory
    && !targetDirectory.startsWith(`${relativePath}/`)
}

export function QuickNoteTree({ nodes, activeId, selectedFolder, selectedPath, expandMode, locateRequest = 0, onOpenNote, onSelectFolder, onMove, onAction, onGit, labels, gitLabel, selectedLabel }: {
  nodes: NoteTreeNode[]
  activeId: string
  selectedFolder: string
  selectedPath: string
  expandMode: VaultTreeExpandMode
  locateRequest?: number
  onOpenNote(note: QuickNote): void
  onSelectFolder(path: string): void
  onMove(node: NoteTreeNode, targetDirectory: string): void
  onAction(node: NoteTreeNode, action: NoteTreeAction, files?: NoteTreeNode[]): void
  onGit(): void
  labels: Record<NoteTreeAction, string>
  gitLabel: string
  selectedLabel?(count: number): string
}) {
  const [expanded, setExpanded] = useState<ReadonlySet<string>>(() => new Set())
  const [dropTarget, setDropTarget] = useState<string | null>(null)
  const [contextMenu, setContextMenu] = useState<{ node: NoteTreeNode; paths: string[]; left: number; top: number } | null>(null)
  const [selectedPaths, setSelectedPaths] = useState<string[]>([])
  const selectionAnchor = useRef('')
  const menuRef = useRef<HTMLDivElement>(null)
  const treeRef = useRef<HTMLDivElement>(null)
  const handledLocate = useRef(0)
  const knownDirectories = useRef(new Set<string>())
  const appliedMode = useRef<VaultTreeExpandMode | null>(null)
  const draggedPath = useRef('')
  useEffect(() => {
    const paths = directoryPaths(nodes)
    if (appliedMode.current !== expandMode) {
      appliedMode.current = expandMode
      knownDirectories.current = new Set(paths)
      setExpanded(resolveExpandedPaths(paths, expandMode, selectedPath))
      return
    }
    const fresh = paths.filter((path) => !knownDirectories.current.has(path))
    knownDirectories.current = new Set(paths)
    setExpanded((current) => {
      const next = new Set(current)
      if (expandMode === 'expandAll') fresh.forEach((path) => next.add(path))
      for (const path of ancestorDirectoryPaths(selectedPath)) next.add(path)
      return next
    })
  }, [expandMode, nodes, selectedPath])
  useEffect(() => {
    if (!locateRequest || handledLocate.current === locateRequest || !selectedPath) return
    const row = treeRef.current?.querySelector<HTMLElement>(`[data-path="${CSS.escape(selectedPath)}"]`)
    if (!row) return
    handledLocate.current = locateRequest
    row.focus({ preventScroll: true })
    row.scrollIntoView({ block: 'nearest' })
  }, [expanded, locateRequest, nodes, selectedPath])
  useEffect(() => {
    if (!contextMenu) return
    const close = () => setContextMenu(null)
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') close() }
    document.addEventListener('pointerdown', close)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', close)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [contextMenu])
  const visiblePaths = visibleNotePaths(nodes, expanded)
  const visibleKey = visiblePaths.join('\n')
  useEffect(() => {
    setSelectedPaths((current) => {
      const next = current.filter((path) => visiblePaths.includes(path))
      return next.length === current.length ? current : next
    })
  }, [visibleKey])
  function chooseNode(node: NoteTreeNode, gesture: SelectionGesture): boolean {
    setSelectedPaths((current) => selectTreePaths(current, node.path, selectionAnchor.current, visiblePaths, gesture))
    if (!gesture.shiftKey) selectionAnchor.current = node.path
    return !gesture.ctrlKey && !gesture.metaKey && !gesture.shiftKey
  }
  function openContextMenu(node: NoteTreeNode, left: number, top: number): void {
    const paths = selectedPaths.includes(node.path) ? selectedPaths : [node.path]
    if (!selectedPaths.includes(node.path)) {
      setSelectedPaths(paths)
      selectionAnchor.current = node.path
    }
    setContextMenu({ node, paths, left, top })
  }
  function selectedFiles(paths: string[]): NoteTreeNode[] {
    return paths.flatMap((path) => {
      const node = findNode(nodes, path)
      return node?.kind === 'file' ? [node] : []
    })
  }
  function moveDropped(targetDirectory: string): void {
    const node = findNode(nodes, draggedPath.current)
    setDropTarget(null)
    draggedPath.current = ''
    if (node && canMoveToDirectory(node.path, targetDirectory)) onMove(node, targetDirectory)
  }
  return (
    <div
      ref={treeRef}
      className={dropTarget === '' ? 'quick-note-tree quick-note-tree--drop' : 'quick-note-tree'}
      role="tree"
      aria-multiselectable="true"
      onDragOver={(event) => {
        if (!draggedPath.current || !canMoveToDirectory(draggedPath.current, '')) return
        event.preventDefault()
        setDropTarget('')
      }}
      onDrop={(event) => {
        event.preventDefault()
        moveDropped('')
      }}
    >
      {nodes.map((node) => (
        <TreeNode
          key={node.path}
          node={node}
          depth={0}
          activeId={activeId}
          selectedFolder={selectedFolder}
          expanded={expanded}
          selectedPaths={selectedPaths}
          dropTarget={dropTarget}
          onToggle={(path) => setExpanded((current) => {
            const next = new Set(current)
            if (next.has(path)) next.delete(path)
            else next.add(path)
            return next
          })}
          onOpenNote={onOpenNote}
          onSelectFolder={onSelectFolder}
          onDragPath={(path) => { draggedPath.current = path }}
          canDrop={(path) => Boolean(draggedPath.current) && canMoveToDirectory(draggedPath.current, path)}
          onDragEnd={() => { draggedPath.current = ''; setDropTarget(null) }}
          onDragHover={setDropTarget}
          onDropNode={moveDropped}
          onChoose={chooseNode}
          onContextMenu={openContextMenu}
        />
      ))}
      {selectedPaths.length > 1 && selectedLabel && <div className="quick-note-tree__selection">{selectedLabel(selectedPaths.length)}</div>}
      {contextMenu && createPortal(
        <div
          ref={menuRef}
          className="quick-note-tree-menu"
          role="menu"
          style={{ left: contextMenu.left, top: contextMenu.top }}
          onPointerDown={(event) => event.stopPropagation()}
        >
          {(['rename', 'move'] as const).map((action) => (
            <button key={action} type="button" role="menuitem" disabled={contextMenu.paths.length > 1} onClick={() => { onAction(contextMenu.node, action); setContextMenu(null) }}>{labels[action]}</button>
          ))}
          {contextMenu.node.kind === 'file' && (
            <>
              {(['duplicate', 'export'] as const).map((action) => (
                <button key={action} type="button" role="menuitem" disabled={!selectedFiles(contextMenu.paths).length} onClick={() => { const files = selectedFiles(contextMenu.paths); onAction(contextMenu.node, action, files); setContextMenu(null) }}>{labels[action]}</button>
              ))}
              <button type="button" role="menuitem" disabled={contextMenu.paths.length > 1} onClick={() => { onAction(contextMenu.node, 'info'); setContextMenu(null) }}>{labels.info}</button>
              <div className="quick-note-tree-menu__separator" role="separator" />
            </>
          )}
          {(['delete', 'reveal'] as const).map((action) => (
            <button key={action} type="button" role="menuitem" disabled={contextMenu.paths.length > 1} onClick={() => { onAction(contextMenu.node, action); setContextMenu(null) }}>{labels[action]}</button>
          ))}
          <div className="quick-note-tree-menu__separator" role="separator" />
          <button type="button" role="menuitem" onClick={() => { onGit(); setContextMenu(null) }}>{gitLabel}</button>
        </div>,
        document.body
      )}
    </div>
  )
}

function visibleNotePaths(nodes: NoteTreeNode[], expanded: ReadonlySet<string>): string[] {
  return nodes.flatMap((node) => [node.path, ...(node.kind === 'directory' && expanded.has(node.path) ? visibleNotePaths(node.children, expanded) : [])])
}

function directoryPaths(nodes: NoteTreeNode[]): string[] {
  return nodes.flatMap((node) => node.kind === 'directory' ? [node.path, ...directoryPaths(node.children)] : [])
}

function findNode(nodes: NoteTreeNode[], path: string): NoteTreeNode | undefined {
  for (const node of nodes) {
    if (node.path === path) return node
    const child = findNode(node.children, path)
    if (child) return child
  }
  return undefined
}

function TreeNode({ node, depth, activeId, selectedFolder, expanded, selectedPaths, dropTarget, onToggle, onOpenNote, onSelectFolder, onDragPath, canDrop, onDragEnd, onDragHover, onDropNode, onChoose, onContextMenu }: {
  node: NoteTreeNode
  depth: number
  activeId: string
  selectedFolder: string
  expanded: ReadonlySet<string>
  selectedPaths: string[]
  dropTarget: string | null
  onToggle(path: string): void
  onOpenNote(note: QuickNote): void
  onSelectFolder(path: string): void
  onDragPath(path: string): void
  canDrop(path: string): boolean
  onDragEnd(): void
  onDragHover(path: string | null): void
  onDropNode(targetDirectory: string): void
  onChoose(node: NoteTreeNode, gesture: SelectionGesture): boolean
  onContextMenu(node: NoteTreeNode, left: number, top: number): void
}) {
  const open = node.kind === 'directory' && expanded.has(node.path)
  const active = node.kind === 'file' ? node.note?.id === activeId : selectedFolder === node.path
  const selected = selectedPaths.includes(node.path)
  const dropping = node.kind === 'directory' && dropTarget === node.path
  return (
    <div role="treeitem" aria-expanded={node.kind === 'directory' ? open : undefined} aria-selected={selected || active}>
      <button
        data-path={node.path}
        className={`quick-note-tree__row${active ? ' quick-note-tree__row--active' : ''}${selected ? ' quick-note-tree__row--selected' : ''}${dropping ? ' quick-note-tree__row--drop' : ''}`}
        style={{ paddingInlineStart: `${8 + depth * 16}px`, color: node.kind === 'file' ? noteColor(node.note?.color) : undefined }}
        type="button"
        draggable={selectedPaths.length <= 1}
        onDragStart={(event) => {
          event.dataTransfer.effectAllowed = 'move'
          event.dataTransfer.setData(quickNotePathType, node.path)
          event.dataTransfer.setData(quickNoteKindType, node.kind)
          onDragPath(node.path)
        }}
        onDragEnd={onDragEnd}
        onDragOver={node.kind === 'directory' ? (event) => {
          if (!canDrop(node.path)) return
          event.preventDefault()
          event.stopPropagation()
          event.dataTransfer.dropEffect = 'move'
          onDragHover(node.path)
        } : undefined}
        onDrop={node.kind === 'directory' ? (event) => {
          event.preventDefault()
          event.stopPropagation()
          onDropNode(node.path)
        } : undefined}
        onContextMenu={(event) => {
          event.preventDefault()
          event.stopPropagation()
          const menuHeight = node.kind === 'file' ? 248 : 132
          onContextMenu(
            node,
            Math.max(8, Math.min(event.clientX, window.innerWidth - 188)),
            Math.max(8, Math.min(event.clientY, window.innerHeight - menuHeight))
          )
        }}
        onClick={(event) => {
          if (!onChoose(node, { ctrlKey: event.ctrlKey, metaKey: event.metaKey, shiftKey: event.shiftKey })) return
          if (node.kind === 'directory') {
            onSelectFolder(node.path)
            onToggle(node.path)
          } else if (node.note) onOpenNote(node.note)
        }}
      >
        {node.kind === 'directory'
          ? open ? <ChevronDown size={14} /> : <ChevronRight size={14} />
          : <span className="quick-note-tree__spacer" />}
        {node.kind === 'directory' ? open ? <FolderOpen size={15} /> : <Folder size={15} /> : <FileText size={15} />}
        <span>{node.name}</span>
      </button>
      {open && node.children.map((child) => (
        <TreeNode
          key={child.path}
          node={child}
          depth={depth + 1}
          activeId={activeId}
          selectedFolder={selectedFolder}
          expanded={expanded}
          selectedPaths={selectedPaths}
          dropTarget={dropTarget}
          onToggle={onToggle}
          onChoose={onChoose}
          onOpenNote={onOpenNote}
          onSelectFolder={onSelectFolder}
          onDragPath={onDragPath}
          canDrop={canDrop}
          onDragEnd={onDragEnd}
          onDragHover={onDragHover}
          onDropNode={onDropNode}
          onContextMenu={onContextMenu}
        />
      ))}
    </div>
  )
}

function noteColor(color: QuickNote['color'] | undefined): string {
  const colors: Record<string, string> = {
    coral: '#d97868',
    yellow: '#c99535',
    green: '#4e9275',
    blue: '#4f83cc',
    purple: '#8a72b5',
    red: '#c96761'
  }
  return colors[color ?? ''] ?? 'currentColor'
}
